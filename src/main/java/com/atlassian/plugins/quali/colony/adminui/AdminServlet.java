package com.atlassian.plugins.quali.colony.adminui;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import java.io.IOException;
import javax.inject.Inject;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;


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
    }

    @Override
    public void doPost(final HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException
    {
    }
}
