package com.atlassian.plugins.quali.torque.tasks;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
//import jdk.nashorn.api.scripting.JSObject;
import org.springframework.beans.factory.annotation.Autowired;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugins.quali.torque.TorqueServerRetriever;
import com.atlassian.plugins.quali.torque.api.*;
import com.atlassian.plugins.quali.torque.service.SandboxAPIService;
import com.atlassian.plugins.quali.torque.service.SandboxAPIServiceImpl;
import com.atlassian.plugins.quali.torque.service.SandboxServiceConnection;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.google.gson.Gson;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.bamboo.variable.substitutor.VariableSubstitutorFactory;
import com.atlassian.bamboo.variable.VariableContext;
import com.atlassian.bamboo.variable.VariableDefinitionContext;
import org.apache.commons.lang.StringUtils;

import java.util.*;
import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.json.*;

@Scanned
public class WaitForSandboxTask implements TaskType{
    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;

    private SandboxAPIService sandboxAPIService;
    private String prevStatus = "";

    @ComponentImport
    private CustomVariableContext customVariableContext;

    public WaitForSandboxTask(@ComponentImport PluginSettingsFactory pluginSettingsFactory,
                              @ComponentImport CustomVariableContext customVariableContext) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.sandboxAPIService = createAPIService();
        this.customVariableContext = customVariableContext;
    }

    private SandboxAPIService createAPIService() {
        SandboxServiceConnection serviceConnection = TorqueServerRetriever.getTorqueServerDetails(pluginSettingsFactory);
        return new SandboxAPIServiceImpl(serviceConnection);
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
        try {
            jsonRes = waitForSandbox(spaceName, sandboxId, timeout, buildLogger);
        } catch (Exception e) {
            throw new TaskException(String.format("Unable to complete a task. Details:\n%s", e.getMessage()));
        }
        setVariable(taskContext, varDetails, jsonRes);
        Map<String,List<String>> endpoints = getSandboxEndpoints(jsonRes, buildLogger);
        for (Map.Entry<String,List<String>> ep : endpoints.entrySet()) {
            for (int i = 0; i < ep.getValue().size(); i++) {
                String appName = ep.getKey().replaceAll("-", "_");
                setVariable(taskContext, String.format("%s_shortcut_%d", appName, i+1), ep.getValue().get(i));
            }
        }
        return TaskResultBuilder.newBuilder(taskContext).success().build();
    }

    private String waitForSandbox(String spaceName, String sandboxId, int timeoutMinutes, BuildLogger logger)
            throws IOException, InterruptedException, TimeoutException, TaskException {
        SingleSandbox sandboxData = null;
        long startTime = System.currentTimeMillis();
        while ((System.currentTimeMillis() - startTime) < timeoutMinutes * 1000 * 60)
        {
            ResponseData<Object> sandbox = getSandbox(spaceName, sandboxId);
            if(sandbox != null)
            {
                sandboxData = new Gson().fromJson(sandbox.getRawBodyJson(), SingleSandbox.class);
                if (!sandboxData.sandboxStatus.equals(this.prevStatus)) {
                    prevStatus = sandboxData.sandboxStatus;
                }
                if(waitForSandbox(sandboxData))
                    return sandbox.getRawBodyJson();
            }
            logger.addBuildLogEntry("****");
            Thread.sleep(10000);
        }
        throw new TimeoutException("Stopped by Timeout");
    }

    private ResponseData<Object> getSandbox(String spaceName, String sandboxId) throws IOException, TaskException {
        ResponseData<Object> sandboxByIdRes = sandboxAPIService.getSandboxById(spaceName, sandboxId);
        if (!sandboxByIdRes.isSuccessful()){
            for(int i=0; i<5; i++){
                sandboxByIdRes = sandboxAPIService.getSandboxById(spaceName, sandboxId);
                if (sandboxByIdRes.isSuccessful()){
                    return sandboxByIdRes;
                }
            }
            throw new TaskException(String.format("failed after 5 retries. status_code: %s error: %s",
                    sandboxByIdRes.getStatusCode(), sandboxByIdRes.getError()));
        }
        return sandboxByIdRes;
    }

    private boolean waitForSandbox(SingleSandbox sandbox) throws IOException, TaskException  {
        if(sandbox.sandboxStatus.equals(SandboxStatus.LAUNCHING))
            return false;
        if(sandbox.sandboxStatus.equals(SandboxStatus.ACTIVE))
            return true;
        if(sandbox.sandboxStatus.equals(SandboxStatus.ACTIVE_WITH_ERROR)) {
            String app_statuses_str = formatAppsDeploymentStatuses(sandbox);
            throw new TaskException(String.format("Sandbox deployment failed with status %s, apps deployment statuses are: %s",
                    sandbox.sandboxStatus, app_statuses_str));
        }

        throw new TaskException(String.format("Sandbox with id %s has unknown sandbox status %s",
                sandbox.id, sandbox.sandboxStatus));
    }

    private Map<String,List<String>> getSandboxEndpoints(String jsonDef, BuildLogger logger) {
        Map<String,List<String>> shortcuts = new HashMap<String, List<String>>();
        JSONObject sbDef = new JSONObject(jsonDef);
        JSONArray apps =  sbDef.getJSONArray("applications");
        for (int i = 0; i < apps.length(); i++)
        {
            JSONObject app = apps.getJSONObject(i);
            JSONArray links = app.getJSONArray("shortcuts");
            List<String> sc = new ArrayList<String>();
            for (int j = 0; j < links.length(); j++) {
                logger.addBuildLogEntry(String.format("Shortcut %s found for app %s",
                        links.getString(j), app.getString("name")));
                sc.add(links.getString(j));
            }
            shortcuts.put(app.getString("name"), sc);
        }
        return shortcuts;
    }

    private String formatAppsDeploymentStatuses(SingleSandbox sandbox)throws IOException{
        StringBuilder builder = new StringBuilder();
        boolean isFirst = true;
        for (Service service:sandbox.applications){
            if (isFirst)
                isFirst= false;
            else
                builder.append(", ");
            builder.append(String.format("%s: %s", service.name, service.status));
        }
        if (!sandbox.sandboxErrors.isEmpty()) {
            builder.append(System.getProperty("line.separator"));
            builder.append("Sandbox Errors: ");
            for (SandboxErrorService service : sandbox.sandboxErrors) {
                if (isFirst)
                    isFirst = false;
                else
                    builder.append(", ");

                builder.append(String.format("%s: %s", service.time, service.code, service.message));
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
