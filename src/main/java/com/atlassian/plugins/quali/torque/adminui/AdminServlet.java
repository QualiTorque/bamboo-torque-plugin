package com.atlassian.plugins.quali.torque.adminui;

import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugins.quali.torque.api.ResponseData;
import com.atlassian.plugins.quali.torque.service.SandboxServiceConnection;
import com.atlassian.plugins.quali.torque.service.SandboxAPIService;
import com.atlassian.plugins.quali.torque.service.SandboxAPIServiceImpl;
import com.atlassian.sal.api.auth.LoginUriProvider;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.templaterenderer.TemplateRenderer;
import java.net.URI;
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

@Scanned
public class AdminServlet extends HttpServlet
{
    @ComponentImport
    private final TransactionTemplate transactionTemplate;
    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;
    @ComponentImport
    private final TemplateRenderer renderer;
    @ComponentImport
    private final LoginUriProvider loginUriProvider;
    @ComponentImport
    private final UserManager userManager;

    private static final long serialVersionUID = 1L;

    @Inject
    public AdminServlet(PluginSettingsFactory pluginSettingsFactory, TemplateRenderer renderer,
                        TransactionTemplate transactionTemplate, LoginUriProvider loginUriProvider,
                        UserManager userManager) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.renderer = renderer;
        this.transactionTemplate = transactionTemplate;
        this.loginUriProvider = loginUriProvider;
        this.userManager = userManager;

    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        String username = userManager.getRemoteUsername(request);
        if (username == null || !userManager.isSystemAdmin(username))
        {
            redirectToLogin(request, response);
            return;
        }

        Map<String, Object> context = new HashMap<String, Object>();
        response.setContentType("text/html;charset=utf-8");

        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();

        String address = getConfigKey(pluginSettings, Const.ADDRESS);
        String token = getConfigKey(pluginSettings, Const.TOKEN);

        ValidateKey(context, address, Const.ADDRESS, Const.ADDRESS_ERROR,
                "Please set a Torque server Address");

        ValidateKey(context, token, Const.TOKEN, Const.TORQUE_TOKEN_ERROR,
                "Please set a Torque token");

        context.put(Const.GENERAL_ERROR, "");
        context.put(Const.GENERAL_MSG, "");

        renderer.render(Const.TORQUE_ADMIN_LAYOUT, context, response.getWriter());
    }

    private void redirectToLogin(HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        response.sendRedirect(loginUriProvider.getLoginUri(getUri(request)).toASCIIString());
    }

    private URI getUri(HttpServletRequest request)
    {
        StringBuffer builder = request.getRequestURL();
        if (request.getQueryString() != null)
        {
            builder.append("?");
            builder.append(request.getQueryString());
        }
        return URI.create(builder.toString());
    }

    @Override
    public void doPost(final HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
        Map<String, Object> context = new HashMap<String, Object>();
        resp.setContentType("text/html;charset=utf-8");

        String address = req.getParameter(Const.ADDRESS).trim();
        String token = req.getParameter(Const.TOKEN).trim();

        SandboxServiceConnection connection = new SandboxServiceConnection(address, token,
                10,30);
        SandboxAPIService sandboxApi = new SandboxAPIServiceImpl(connection);
        final ResponseData<Object> res;
        // Check availability using getSpacesList call
        try {
            res = sandboxApi.getSpacesList();
            if (res.getStatusCode() == 200 ) {
                transactionTemplate.execute(new TransactionCallback() {
                    public Object doInTransaction() {
                        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
                        pluginSettings.put(Config.class.getName() + '.' + Const.ADDRESS, req.getParameter(Const.ADDRESS).trim());
                        pluginSettings.put(Config.class.getName() + '.' + Const.TOKEN, req.getParameter(Const.TOKEN).trim());
                        return null;
                    }
                })  ;
                context.put(Const.GENERAL_ERROR, "");
                context.put(Const.GENERAL_MSG, "Connection saved");
                context.put(Const.TORQUE_TOKEN_ERROR, "");
                context.put(Const.ADDRESS_ERROR, "");
            } else {
                context.put(Const.GENERAL_ERROR, "Failed to save Torque connection. Check settings");
                if (res.getStatusCode() == 401 || res.getStatusCode() == 403) {
                    context.put(Const.TORQUE_TOKEN_ERROR, "Check token");
                }
                context.put(Const.ADDRESS_ERROR, "");
                context.put(Const.GENERAL_MSG, "");
            }
        }
        catch (Exception e) {
            context.put(Const.GENERAL_MSG, "");
            context.put(Const.ADDRESS_ERROR, "Check address");
            context.put(Const.GENERAL_ERROR, "Failed to save Torque connection.");
            context.put(Const.TORQUE_TOKEN_ERROR, "");
        }
        context.put(Const.ADDRESS, address);
        context.put(Const.TOKEN, token);
        renderer.render(Const.TORQUE_ADMIN_LAYOUT, context, resp.getWriter());
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
