package com.atlassian.plugins.quali.colony.tasks;

import com.atlassian.bamboo.task.TaskType;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugins.quali.colony.ColonyServerRetriever;
import com.atlassian.plugins.quali.colony.api.CreateSandboxRequest;
import com.atlassian.plugins.quali.colony.api.CreateSandboxResponse;
import com.atlassian.plugins.quali.colony.api.ResponseData;
import com.atlassian.plugins.quali.colony.service.SandboxAPIService;
import com.atlassian.plugins.quali.colony.service.SandboxAPIServiceImpl;
import com.atlassian.plugins.quali.colony.service.SandboxServiceConnection;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

import java.util.Collections;
import java.util.Map;

public class StartSandboxTask implements TaskType
{
    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;

    private String spaceName;
    private String blueprint;
    private String sandboxName;
    private Map<String, String> artifacts;
    private Map<String, String> inputs;

    public StartSandboxTask(PluginSettingsFactory pluginSettingsFactory){
        this.pluginSettingsFactory = pluginSettingsFactory;

    }

    private SandboxAPIService createAPIService() {
        SandboxServiceConnection serviceConnection = ColonyServerRetriever.getColonyServerDetails(pluginSettingsFactory);
        return new SandboxAPIServiceImpl(serviceConnection);
    }

    @Override
    public TaskResult execute(final TaskContext taskContext) throws TaskException {
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        final ResponseData<CreateSandboxResponse> res;
        buildLogger.addBuildLogEntry("Task started");
        final String spaceName = taskContext.getConfigurationMap().get("space");
        final String blueprintName = taskContext.getConfigurationMap().get("blueprint");
        final String sandboxName = taskContext.getConfigurationMap().get("sandboxname");
        SandboxAPIService sandboxAPIService = createAPIService();
        buildLogger.addBuildLogEntry(String.format("Passed parameters are %s , %s , %s", spaceName, blueprintName, sandboxName));
        CreateSandboxRequest req = new CreateSandboxRequest(blueprintName,sandboxName, Collections.<String, String>emptyMap(),
                true, Collections.<String, String>emptyMap());
        try {
            res = sandboxAPIService.createSandbox(spaceName, req);
        }
        catch (Exception e) {
            buildLogger.addErrorLogEntry("Unable to start sandbox");
            throw new TaskException(e.getMessage());
        }

        if(!res.isSuccessful())
            throw new TaskException(String.format("status_code: %s error: %s", res.getStatusCode(), res.getError()));
        String sandboxId = res.getData().id;

        //final String say = taskContext.getConfigurationMap().get("say");

        buildLogger.addBuildLogEntry(String.format("Sandbox %s started successfully", sandboxId));

        return TaskResultBuilder.create(taskContext).success().build();
    }
}
