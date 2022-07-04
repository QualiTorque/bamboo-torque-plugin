package com.atlassian.plugins.quali.torque;

import com.atlassian.plugins.quali.torque.service.VersionUtils;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.plugins.quali.torque.adminui.Const;
import com.atlassian.plugins.quali.torque.adminui.AdminServlet;
import com.quali.torque.client.ApiClient;
import com.quali.torque.client.Configuration;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

public class TorqueServerRetriever {
    @NotNull
    public static ApiClient getTorqueServerDetails(PluginSettingsFactory pluginSettingsFactory) {
        PluginSettings pluginSettings = pluginSettingsFactory.createGlobalSettings();
        String address = (String) pluginSettings.get(AdminServlet.Config.class.getName() + '.' + Const.ADDRESS);
        String token = (String) pluginSettings.get(AdminServlet.Config.class.getName() + '.' + Const.TOKEN);
        ApiClient defaultClient = Configuration.getDefaultApiClient();
        defaultClient.setBasePath(address);
        defaultClient.setUserAgent("Torque-Plugin-Bamboo/" + VersionUtils.PACKAGE_VERSION);
        defaultClient.setApiKey(String.format("Bearer %s", token));
        defaultClient.setConnectTimeout(60 * 1000);
        defaultClient.setReadTimeout(60 * 1000);
        DateTimeFormatter f = new DateTimeFormatterBuilder()
                .appendPattern("[yyyy-MM-dd'T'HH:mm:ss.SSSSSSSXXX][yyyy-MM-dd'T'HH:mm:ss.SSSSSS][yyyy-MM-dd'T'HH:mm:ss]")
                .parseLenient()
                .parseDefaulting(ChronoField.OFFSET_SECONDS, 0)
                .toFormatter();
        defaultClient.setOffsetDateTimeFormat(f);
        return defaultClient;
    }
}
