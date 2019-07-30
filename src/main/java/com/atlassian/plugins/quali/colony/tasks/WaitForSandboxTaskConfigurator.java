package com.atlassian.plugins.quali.colony.tasks;

import com.atlassian.bamboo.collections.ActionParametersMap;
import com.atlassian.bamboo.task.AbstractTaskConfigurator;
import com.atlassian.bamboo.task.TaskDefinition;
import com.atlassian.bamboo.utils.error.ErrorCollection;
import com.opensymphony.xwork.TextProvider;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class WaitForSandboxTaskConfigurator extends AbstractTaskConfigurator
{
    private TextProvider textProvider;

    @NotNull
    @Override
    public Map<String, String> generateTaskConfigMap(@NotNull final ActionParametersMap params, @Nullable final TaskDefinition previousTaskDefinition)
    {
        final Map<String, String> config = super.generateTaskConfigMap(params, previousTaskDefinition);

        config.put("space", params.getString("space"));
        config.put("timeout", params.getString("timeout"));

        return config;
    }

    @Override
    public void populateContextForCreate(@NotNull final Map<String, Object> context)
    {
        super.populateContextForCreate(context);
    }

    @Override
    public void populateContextForEdit(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition)
    {
        super.populateContextForEdit(context, taskDefinition);
        context.put("space", taskDefinition.getConfiguration().get("space"));
        context.put("timeout", taskDefinition.getConfiguration().get("timeout"));
    }

    @Override
    public void populateContextForView(@NotNull final Map<String, Object> context, @NotNull final TaskDefinition taskDefinition)
    {
        super.populateContextForView(context, taskDefinition);
        context.put("space", taskDefinition.getConfiguration().get("space"));
        context.put("timeout", taskDefinition.getConfiguration().get("timeout"));
    }

    @Override
    public void validate(@NotNull final ActionParametersMap params, @NotNull final ErrorCollection errorCollection)
    {
        super.validate(params, errorCollection);

        final String space = params.getString("space");
        if (StringUtils.isEmpty(space))
        {
            errorCollection.addError("space", textProvider.getText("startsandbox.space.error"));
        }

        final String timeout = params.getString("timeout");
        try {
            Integer.parseInt(timeout);
        } catch (Exception e) {
            errorCollection.addError("timeout", textProvider.getText("waitsandbox.timeout.error"));
        }

    }

    public void setTextProvider(final TextProvider textProvider)
    {
        this.textProvider = textProvider;
    }
}
