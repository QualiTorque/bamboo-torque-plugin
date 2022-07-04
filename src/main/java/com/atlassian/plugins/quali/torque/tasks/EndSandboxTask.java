package com.atlassian.plugins.quali.torque.tasks;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugins.quali.torque.TorqueServerRetriever;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.quali.torque.client.ApiClient;
import com.quali.torque.client.api.EnvironmentApi;

public class EndSandboxTask implements TaskType
{
    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;

    public EndSandboxTask(PluginSettingsFactory pluginSettingsFactory){
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    private EnvironmentApi createAPIService() {
        ApiClient apiClient = TorqueServerRetriever.getTorqueServerDetails(pluginSettingsFactory);
        return new EnvironmentApi(apiClient);
    }

    @Override
    public TaskResult execute(final TaskContext taskContext) throws TaskException
    {
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        final String spaceName = taskContext.getConfigurationMap().get("space");
        final String sandboxId = taskContext.getConfigurationMap().get("sandboxid");
        EnvironmentApi sandboxEnvironmentsApi = createAPIService();
        try {
            sandboxEnvironmentsApi.apiSpacesSpaceNameEnvironmentsEnvironmentIdDelete(spaceName, sandboxId);
        }
        catch (Exception e) {
            buildLogger.addErrorLogEntry("Unable to stop sandbox", e);
            throw new TaskException(e.getMessage(), e);
        }

        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }
}
