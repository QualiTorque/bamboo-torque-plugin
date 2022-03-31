package com.atlassian.plugins.quali.torque.tasks;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.opensymphony.xwork.TextProvider;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

// TODO: add processing and validation for artifacts and inputs (key1=value1, ...)

public class StartSandboxTaskConfigurator extends AbstractTaskConfigurator
{
    private TextProvider textProvider;
    private List<String> keys = Arrays.asList("space", "blueprint", "sandboxname", "artifacts", "inputs", "varid");

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params, @Nullable final TaskDefinition previousTaskDefinition)
    {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);

        for (String key: keys) {
            config.put(key, params.getString(key));
        }
        return config;
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context)
    {
        super.populateContextForCreate(context);
        context.put("varid", "Sandbox_Id");
    }

    @Override
    public void populateContextForEdit(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition)
    {
        super.populateContextForEdit(context, taskDefinition);
        for (String key: keys) {
            context.put(key, taskDefinition.getConfiguration().get(key));
        }
    }

    @Override
    public void populateContextForView(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition)
    {
        super.populateContextForView(context, taskDefinition);
        for (String key: keys) {
            context.put(key, taskDefinition.getConfiguration().get(key));
        }
    }

    @Override
    public void validate(@NotNull final ActionParametersMap params, @NotNull final ErrorCollection errorCollection)
    {
        super.validate(params, errorCollection);

        final String sayValue = params.getString("space");
        if (StringUtils.isEmpty(sayValue))
        {
            errorCollection.addError("space", textProvider.getText("startsandbox.space.error"));
        }
    }

    public void setTextProvider(final TextProvider textProvider)
    {
        this.textProvider = textProvider;
    }
}
