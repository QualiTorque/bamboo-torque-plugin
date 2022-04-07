package com.atlassian.plugins.quali.torque.service;

import java.io.IOException;
import java.util.Properties;

public class VersionUtils {

    public static final String PACKAGE_VERSION;

    static {
        try {
            PACKAGE_VERSION = getPackageVersion();
        } catch (IOException e) {
            throw new RuntimeException("Error while initializing VersionUtils.", e);
        }
    }

    private static String getPackageVersion() throws IOException {
        Properties properties = new Properties();
        properties.load(VersionUtils.class.getClassLoader().getResourceAsStream("bamboo-torque-plugin.properties"));
        return properties.getProperty("package.version");
    }
}
