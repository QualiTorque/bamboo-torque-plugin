package com.atlassian.plugins.quali.torque;

import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.plugins.quali.torque.service.SandboxServiceConnection;
import com.atlassian.plugins.quali.torque.adminui.Const;
import com.atlassian.plugins.quali.torque.adminui.AdminServlet;
import org.jetbrains.annotations.NotNull;

public class TorqueServerRetriever {
    @NotNull
    public static SandboxServiceConnection getTorqueServerDetails(PluginSettingsFactory pluginSettingsFactory) {
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        String address = (String) pluginSettings.get(AdminServlet.Config.class.getName() + '.' + Const.ADDRESS);
        String token = (String) pluginSettings.get(AdminServlet.Config.class.getName() + '.' + Const.TOKEN);

        return new SandboxServiceConnection(address, token, 30,30);
    }
}
