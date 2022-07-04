package com.atlassian.plugins.quali.torque.tasks;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugins.quali.torque.TorqueServerRetriever;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bamboo.variable.VariableContext;
import com.atlassian.bamboo.variable.VariableDefinitionContext;
import com.google.gson.GsonBuilder;
import com.quali.torque.client.ApiClient;
import com.quali.torque.client.ApiException;
import com.quali.torque.client.api.EnvironmentApi;
import com.quali.torque.client.models.QualiColonyGatewayApiModelResponsesEnvironmentResponse;
import com.quali.torque.client.models.QualiColonyServicesSandboxesApiContractsEnvironmentErrorResponse;
import com.quali.torque.client.models.QualiColonyServicesSandboxesApiContractsEnvironmentGrainResponse;
import com.quali.torque.client.models.QualiColonyServicesSandboxesApiContractsEnvironmentOutputResponse;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

@Scanned
public class WaitForSandboxTask implements TaskType{

    public static class SandboxStatus
    {
        public static final String LAUNCHING = "Launching";
        public static final String ACTIVE = "Active";
        public static final String ACTIVE_WITH_ERROR = "ActiveWithError";
    }

    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;

    private final EnvironmentApi sandboxEnvironmentsApi;
    private String prevStatus = "";

    @ComponentImport
    private CustomVariableContext customVariableContext;

    public WaitForSandboxTask(@ComponentImport PluginSettingsFactory pluginSettingsFactory,
                              @ComponentImport CustomVariableContext customVariableContext) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.sandboxEnvironmentsApi = createAPIService();
        this.customVariableContext = customVariableContext;
    }

    private EnvironmentApi createAPIService() {
        ApiClient apiClient = TorqueServerRetriever.getTorqueServerDetails(pluginSettingsFactory);
        return new EnvironmentApi(apiClient);
    }

    @Override
    public TaskResult execute(final TaskContext taskContext) throws TaskException {
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        String jsonRes = "{}";
        buildLogger.addBuildLogEntry("Task Wait for Sandbox started");
        final String spaceName = taskContext.getConfigurationMap().get("space");
        final String sandboxId = taskContext.getConfigurationMap().get("sandboxid");
        final int timeout = Integer.parseInt(taskContext.getConfigurationMap().get("timeout"));
        final String varDetails = taskContext.getConfigurationMap().get("vardetails");
        buildLogger.addBuildLogEntry(String.format("Waiting for sandbox %s, time limit is %s minutes", sandboxId, timeout));
        QualiColonyGatewayApiModelResponsesEnvironmentResponse environment;
        try {
            environment = waitForSandbox(spaceName, sandboxId, timeout, buildLogger);
            jsonRes = new GsonBuilder().setPrettyPrinting().create().toJson(environment);
        } catch (Exception e) {
            throw new TaskException(String.format("Unable to complete a task. Details:\n%s", e.getMessage()), e);
        }
        setVariable(taskContext, varDetails, jsonRes);
        Map<String, String> endpoints = getSandboxEndpoints(environment, buildLogger);
        for (Map.Entry<String, String> ep : endpoints.entrySet()) {
            String appName = ep.getKey().replaceAll("-", "_");
            setVariable(taskContext, String.format("shortcut_%s", appName), ep.getValue());
        }
        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    private QualiColonyGatewayApiModelResponsesEnvironmentResponse waitForSandbox(String spaceName, String sandboxId, int timeoutMinutes, BuildLogger logger)
            throws IOException, InterruptedException, TimeoutException, TaskException {
        long startTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - startTime) < timeoutMinutes * 1000 * 60)
        {
            QualiColonyGatewayApiModelResponsesEnvironmentResponse environment = getSandbox(spaceName, sandboxId);
            if(environment != null)
            {
                if (!environment.getDetails().getComputedStatus().equals(this.prevStatus)) {
                    prevStatus = environment.getDetails().getComputedStatus();
                }
                if(waitForSandbox(environment))
                    return environment;
            }
            logger.addBuildLogEntry("****");
            Thread.sleep(10000);
        }
        throw new TimeoutException("Stopped by Timeout");
    }

    private QualiColonyGatewayApiModelResponsesEnvironmentResponse getSandbox(String spaceName, String sandboxId) throws TaskException {
        ApiException apiException = null;
        QualiColonyGatewayApiModelResponsesEnvironmentResponse sandboxByIdRes;

        for (int i = 0; i < 5; i++) {
            try {
                sandboxByIdRes = sandboxEnvironmentsApi.apiSpacesSpaceNameEnvironmentsEnvironmentIdGet(spaceName, sandboxId);
                return sandboxByIdRes;
            } catch (ApiException e) {
                apiException = e;
            }

        }
        throw new TaskException(String.format("failed after 5 retries. status_code: %s error: %s",
                apiException.getCode(), apiException.getMessage()));
    }

    private boolean waitForSandbox(QualiColonyGatewayApiModelResponsesEnvironmentResponse environment) throws IOException, TaskException  {
        if(environment.getDetails().getComputedStatus().equals(SandboxStatus.LAUNCHING))
            return false;
        if(environment.getDetails().getComputedStatus().equals(SandboxStatus.ACTIVE))
            return true;
        if(environment.getDetails().getComputedStatus().equals(SandboxStatus.ACTIVE_WITH_ERROR)) {
            String app_statuses_str = formatAppsDeploymentStatuses(environment);
            throw new TaskException(String.format("Sandbox deployment failed with status %s, apps deployment statuses are: %s",
                    environment.getDetails().getComputedStatus(), app_statuses_str));
        }

        throw new TaskException(String.format("Sandbox with id %s has unknown sandbox status %s",
                environment.getDetails().getId(), environment.getDetails().getComputedStatus()));
    }

    private Map<String, String> getSandboxEndpoints(QualiColonyGatewayApiModelResponsesEnvironmentResponse environment, BuildLogger logger) {
        Map<String, String> sc = new HashMap<String, String>();
        for (QualiColonyServicesSandboxesApiContractsEnvironmentOutputResponse output: environment.getDetails().getState().getOutputs())
        {
            if (output.getKind() == "link") {
                logger.addBuildLogEntry(String.format("Link output %s found: %s",
                        output.getName(), output.getValue()));
                sc.put(output.getName(), output.getValue());
            }
        }
        return sc;
    }

    private String formatAppsDeploymentStatuses(QualiColonyGatewayApiModelResponsesEnvironmentResponse environment)throws IOException{
        StringBuilder builder = new StringBuilder();
        boolean isFirst = true;
        for (QualiColonyServicesSandboxesApiContractsEnvironmentGrainResponse grain:environment.getDetails().getState().getGrains()){
            if (isFirst)
                isFirst= false;
            else
                builder.append(", ");
            builder.append(String.format("%s: %s", grain.getName(),
                    grain.getState().getCurrentState()));
        }
        if (!environment.getDetails().getState().getErrors().isEmpty()) {
            builder.append(System.getProperty("line.separator"));
            builder.append("Sandbox Errors: ");
            for (QualiColonyServicesSandboxesApiContractsEnvironmentErrorResponse error : environment.getDetails().getState().getErrors()) {
                if (isFirst)
                    isFirst = false;
                else
                    builder.append(", ");

                builder.append(String.format("%s", error.getMessage()));
            }
        }
        return builder.toString();
    }
    private void setVariable(TaskContext taskContext, String key, String value)
    {
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        final VariableContext variableContext = taskContext.getBuildContext().getVariableContext();
        final Map<String, VariableDefinitionContext> definitions = variableContext.getEffectiveVariables();

        final VariableDefinitionContext definition = definitions.get(key);
        if (definition != null)
        {
            if (StringUtils.isEmpty(definition.getValue()))
            {
                definition.setValue(value);
                buildLogger.addBuildLogEntry(String.format("Set variable %s=%s", key, value));
            }
            else
            {
                buildLogger.addBuildLogEntry(String.format("Not setting variable '%s' because it is already set", key));
            }
            return;
        }
        this.customVariableContext.addCustomData(key, value);
        buildLogger.addBuildLogEntry(String.format("Set custom variable %s=%s", key, value));
    }
}
