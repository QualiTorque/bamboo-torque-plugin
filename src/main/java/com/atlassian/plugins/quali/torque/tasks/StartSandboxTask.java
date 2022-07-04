package com.atlassian.plugins.quali.torque.tasks;

import com.atlassian.bamboo.task.TaskType;
import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugins.quali.torque.TorqueServerRetriever;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.quali.torque.client.ApiClient;
import com.quali.torque.client.api.EnvironmentApi;
 import com.quali.torque.client.models.QualiColonyGatewayApiModelRequestsCreateSandboxRequest;
import com.quali.torque.client.models.QualiColonyGatewayApiModelResponsesCreateEnvResponse;

import java.util.HashMap;
import java.util.Map;

public class StartSandboxTask implements TaskType
{
    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;

    public StartSandboxTask(PluginSettingsFactory pluginSettingsFactory){
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    private EnvironmentApi createAPIService() {
        ApiClient apiClient = TorqueServerRetriever.getTorqueServerDetails(pluginSettingsFactory);
        return new EnvironmentApi(apiClient);
    }

    public static Map<String,String> parseParametersLine(String params) {
        HashMap<String, String> holder = new HashMap<>();
        String[] keyVals = params.split(", ");
        for (String keyVal : keyVals) {
            if (!keyVal.isEmpty()) {
                String[] parts = keyVal.split("=", 2);
                holder.put(parts[0], parts[1]);
            }
        }
        return holder;
    }

    @Override
    public TaskResult execute(final TaskContext taskContext) throws TaskException {
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        final QualiColonyGatewayApiModelResponsesCreateEnvResponse res;
        final Map<String, String> customBuildData = taskContext.getBuildContext().getBuildResult().getCustomBuildData();
        buildLogger.addBuildLogEntry("Task Start Sandbox started");
        final String spaceName = taskContext.getConfigurationMap().get("space");
        final String blueprintName = taskContext.getConfigurationMap().get("blueprint");
        final String sandboxName = taskContext.getConfigurationMap().get("sandboxname");
        final String varId = taskContext.getConfigurationMap().get("varid");
        final Map<String, String> artifacts = parseParametersLine(taskContext.getConfigurationMap().get("artifacts"));
        final Map<String, String> inputs = parseParametersLine(taskContext.getConfigurationMap().get("inputs"));

        EnvironmentApi sandboxEnvironmentsApi = createAPIService();
        buildLogger.addBuildLogEntry(String.format("Passed parameters are %s , %s , %s", spaceName, blueprintName, sandboxName));
        QualiColonyGatewayApiModelRequestsCreateSandboxRequest req = new QualiColonyGatewayApiModelRequestsCreateSandboxRequest();
        req.setBlueprintName(blueprintName);
        req.setSandboxName(sandboxName);
        req.setArtifacts(artifacts);
        req.setAutomation(true);
        req.setInputs(inputs);
        try {
            res =sandboxEnvironmentsApi.apiSpacesSpaceNameEnvironmentsPost(spaceName, req);
        }
        catch (Exception e) {
            buildLogger.addErrorLogEntry("Unable to start sandbox", e);
            throw new TaskException(e.getMessage(), e);
        }

        String sandboxId = res.getId();

        //final String say = taskContext.getConfigurationMap().get("say");

        buildLogger.addBuildLogEntry(String.format("Sandbox with id %s started successfully", sandboxId));

        customBuildData.put(varId, sandboxId);

        return TaskResultBuilder.create(taskContext).success().build();
    }
}
