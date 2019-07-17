package com.atlassian.plugins.quali.colony.adminui;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugins.quali.colony.service.SandboxServiceConnection;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.templaterenderer.TemplateRenderer;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// NOTE(ddovbii): mainly the code was taken from https://github.com/QualiSystems/Bamboo-cloudshell-plugin

@Scanned
public class AdminServlet extends HttpServlet
{
    @ComponentImport
    private final TransactionTemplate transactionTemplate;
    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;
    @ComponentImport
    private final TemplateRenderer renderer;

    private static final long serialVersionUID = 1L;

    @Inject
    public AdminServlet(PluginSettingsFactory pluginSettingsFactory, TemplateRenderer renderer,
                        TransactionTemplate transactionTemplate) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.renderer = renderer;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        Map<String, Object> context = new HashMap<String, Object>();
        response.setContentType("text/html;charset=utf-8");

        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();

        String address = getConfigKey(pluginSettings, Const.ADDRESS);
        String port = getConfigKey(pluginSettings, Const.CS_PORT);
        String token = getConfigKey(pluginSettings, Const.TOKEN);

        ValidateKey(context, address, Const.ADDRESS, Const.ADDRESS_ERROR,
                "Please set a CloudShell Colony server Address");

        ValidateKey(context, port, Const.CS_PORT, Const.CS_PORT_ERROR,
                "Please set a CloudShell Colony port");

        ValidateKey(context, token, Const.TOKEN, Const.CS_TOKEN_ERROR,
                "Please set a CloudShell Colony token");

        context.put(Const.GENERAL_ERROR, "");
        context.put(Const.GENERAL_MSG, "");

        renderer.render(Const.CS_ADMIN_LAYOUT, context, response.getWriter());
    }

    @Override
    public void doPost(final HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        Map<String, Object> context = new HashMap<String, Object>();
        resp.setContentType("text/html;charset=utf-8");

        String address = req.getParameter(Const.ADDRESS).trim();
        String token = req.getParameter(Const.TOKEN).trim();
        int port = Integer.parseInt(req.getParameter(Const.CS_PORT));

        SandboxServiceConnection apiConnection = new SandboxServiceConnection(address, port, token,
                10,30);


        // TODO: implement availability check

//
//        RestResponse restResponse = null;
//        try {
//            restResponse = sandboxApiGateway.TryLogin();
//        } catch (Exception e) {
//            e.printStackTrace(); //TODO: handle
//        }
//
        //if (restResponse.getHttpCode() == 200) {
        if (apiConnection != null) {
            transactionTemplate.execute(new TransactionCallback() {
                public Object doInTransaction() {
                    PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
                    pluginSettings.put(Config.class.getName() + '.' + Const.ADDRESS, req.getParameter(Const.ADDRESS).trim());
                    pluginSettings.put(Config.class.getName() + '.' + Const.CS_PORT, req.getParameter(Const.CS_PORT).trim());
                    pluginSettings.put(Config.class.getName() + '.' + Const.TOKEN, req.getParameter(Const.TOKEN).trim());
                    return null;
                }
            });
            context.put(Const.GENERAL_ERROR, "");
            context.put(Const.GENERAL_MSG, "Connected successfully to CloudShell");

        } else {
            context.put(Const.GENERAL_ERROR, "Failed to test CloudShell login"); //+ restResponse.getContent());
            context.put(Const.GENERAL_MSG, "");
        }

        context.put(Const.CS_TOKEN_ERROR, "");
        context.put(Const.ADDRESS_ERROR, "");

        context.put(Const.ADDRESS, address);
        context.put(Const.CS_PORT, port);
        context.put(Const.TOKEN, token);

        renderer.render(Const.CS_ADMIN_LAYOUT, context, resp.getWriter());
    }

    private void ValidateKey(Map<String, Object> context, String key, String userKey, String userKeyError, String errorMessage) {
        if (key != null) {
            context.put(userKey, key);
            context.put(userKeyError, "");
        } else {
            context.put(userKey, "");
            context.put(userKeyError, errorMessage);
        }
    }

    private String getConfigKey(PluginSettings pluginSettings, String config) {
        return (String) pluginSettings.get(Config.class.getName() + '.' +
                config);
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    public static final class Config {
        @XmlElement
        private String userkey;


        public String getUserkey() {
            return userkey;
        }
        public void setUserkey(String userkey) {
            this.userkey = userkey;
        }
    }
}
