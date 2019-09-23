package com.atlassian.plugins.quali.colony;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.plugins.quali.colony.service.SandboxServiceConnection;
import com.atlassian.plugins.quali.colony.adminui.Const;
import com.atlassian.plugins.quali.colony.adminui.AdminServlet;
import org.jetbrains.annotations.NotNull;

public class ColonyServerRetriever {
    @NotNull
    public static SandboxServiceConnection getColonyServerDetails(PluginSettingsFactory pluginSettingsFactory) {
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        String address = (String) pluginSettings.get(AdminServlet.Config.class.getName() + '.' + Const.ADDRESS);
        String token = (String) pluginSettings.get(AdminServlet.Config.class.getName() + '.' + Const.TOKEN);

        return new SandboxServiceConnection(address, token, 30,30);
    }
}
