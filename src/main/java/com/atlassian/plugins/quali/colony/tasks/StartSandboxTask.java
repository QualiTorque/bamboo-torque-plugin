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

import java.util.HashMap;
import java.util.Map;

public class StartSandboxTask implements TaskType
{
    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;

    public StartSandboxTask(PluginSettingsFactory pluginSettingsFactory){
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    private SandboxAPIService createAPIService() {
        SandboxServiceConnection serviceConnection = ColonyServerRetriever.getColonyServerDetails(pluginSettingsFactory);
        return new SandboxAPIServiceImpl(serviceConnection);
    }

    public static Map<String,String> parseParametersLine(String params) {
        HashMap<String, String> holder = new HashMap<>();
        String[] keyVals = params.split(", ");
        for (String keyVal : keyVals) {
            String[] parts = keyVal.split("=", 2);
            holder.put(parts[0], parts[1]);
        }
        return holder;
    }

    @Override
    public TaskResult execute(final TaskContext taskContext) throws TaskException {
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        final ResponseData<CreateSandboxResponse> res;
        final Map<String, String> customBuildData = taskContext.getBuildContext().getBuildResult().getCustomBuildData();
        buildLogger.addBuildLogEntry("Task Start Sandbox started");
        final String spaceName = taskContext.getConfigurationMap().get("space");
        final String blueprintName = taskContext.getConfigurationMap().get("blueprint");
        final String sandboxName = taskContext.getConfigurationMap().get("sandboxname");
        final Map<String, String> artifacts = parseParametersLine(taskContext.getConfigurationMap().get("artifacts"));
        final Map<String, String> inputs = parseParametersLine(taskContext.getConfigurationMap().get("inputs"));

        SandboxAPIService sandboxAPIService = createAPIService();
        buildLogger.addBuildLogEntry(String.format("Passed parameters are %s , %s , %s", spaceName, blueprintName, sandboxName));
        CreateSandboxRequest req = new CreateSandboxRequest(blueprintName, sandboxName, artifacts, true, inputs);
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

        buildLogger.addBuildLogEntry(String.format("Sandbox with id %s started successfully", sandboxId));
        customBuildData.put("SANDBOX_ID", sandboxId);

        return TaskResultBuilder.create(taskContext).success().build();
    }
}
