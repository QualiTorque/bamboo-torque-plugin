package com.atlassian.plugins.quali.colony.tasks;

import com.atlassian.bamboo.build.logger.BuildLogger;
import com.atlassian.bamboo.task.TaskContext;
import com.atlassian.bamboo.task.TaskException;
import com.atlassian.bamboo.task.TaskResult;
import com.atlassian.bamboo.task.TaskResultBuilder;
import com.atlassian.bamboo.task.TaskType;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.plugins.quali.colony.ColonyServerRetriever;
import com.atlassian.plugins.quali.colony.api.*;
import com.atlassian.plugins.quali.colony.service.SandboxAPIService;
import com.atlassian.plugins.quali.colony.service.SandboxAPIServiceImpl;
import com.atlassian.plugins.quali.colony.service.SandboxServiceConnection;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class WaitForSandboxTask implements TaskType{
    @ComponentImport
    private final PluginSettingsFactory pluginSettingsFactory;

    private SandboxAPIService sandboxAPIService;
    private String prevStatus = "";

    public WaitForSandboxTask(PluginSettingsFactory pluginSettingsFactory) {
        this.pluginSettingsFactory = pluginSettingsFactory;
        this.sandboxAPIService = createAPIService();
    }

    private SandboxAPIService createAPIService() {
        SandboxServiceConnection serviceConnection = ColonyServerRetriever.getColonyServerDetails(pluginSettingsFactory);
        return new SandboxAPIServiceImpl(serviceConnection);
    }

    @Override
    public TaskResult execute(final TaskContext taskContext) throws TaskException {
        final BuildLogger buildLogger = taskContext.getBuildLogger();
        buildLogger.addBuildLogEntry("Task Wait for Sandbox started");
        final String spaceName = taskContext.getConfigurationMap().get("space");
        final String sandboxId = taskContext.getBuildContext().getBuildResult().getCustomBuildData().get("SANDBOX_ID");
        final int timeout = Integer.parseInt(taskContext.getConfigurationMap().get("timeout"));
        buildLogger.addBuildLogEntry(String.format("Waiting for sandbox %s, time limit is %s minutes", sandboxId, timeout));
        try {
            String jsonRes = waitForSandbox(spaceName, sandboxId, timeout, buildLogger);
            buildLogger.addBuildLogEntry(jsonRes);
        } catch (Exception e) {
            throw new TaskException(String.format("Unable to complete a task. Details:\n%s", e.getMessage()));
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
}
