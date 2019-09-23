package com.atlassian.plugins.quali.colony.tasks;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugins.quali.colony.ColonyServerRetriever;
import com.atlassian.plugins.quali.colony.api.ResponseData;
import com.atlassian.plugins.quali.colony.service.SandboxAPIService;
import com.atlassian.plugins.quali.colony.service.SandboxAPIServiceImpl;
import com.atlassian.plugins.quali.colony.service.SandboxServiceConnection;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import java.util.Map;

public class EndSandboxTask implements TaskType
{
    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;

    public EndSandboxTask(PluginSettingsFactory pluginSettingsFactory){
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    private SandboxAPIService createAPIService() {
        SandboxServiceConnection serviceConnection = ColonyServerRetriever.getColonyServerDetails(pluginSettingsFactory);
        return new SandboxAPIServiceImpl(serviceConnection);
    }

    @Override
    public TaskResult execute(final TaskContext taskContext) throws TaskException
    {
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        final ResponseData<Void> res;
        final String spaceName = taskContext.getConfigurationMap().get("space");
        final String sandboxId = taskContext.getConfigurationMap().get("sandboxid");
        SandboxAPIService sandboxAPIService = createAPIService();
        try {
            res = sandboxAPIService.deleteSandbox(spaceName, sandboxId);
        }
        catch (Exception e) {
            buildLogger.addBuildLogEntry("Unable to stop sandbox");
            throw new TaskException(e.getMessage());
        }

        if(!res.isSuccessful())
            throw new TaskException(String.format("status_code: %s error: %s", res.getStatusCode(), res.getError()));

        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }
}
