package com.atlassian.plugins.quali.colony.api;

import java.util.Map;

public class CreateSandboxRequest
{
    public final String blueprint_name;
    public final String sandbox_name;
    public final Map<String, String> artifacts;
    public final boolean automation;
    public final Map<String, String> inputs;
    public final String description;

    public CreateSandboxRequest(String blueprintName,
                                String sandboxName,
                                Map<String, String> artifacts,
                                boolean automation,
                                Map<String, String> inputs){

        this.blueprint_name = blueprintName;
        this.sandbox_name = sandboxName;
        this.artifacts = artifacts;
        this.automation = automation;
        this.inputs = inputs;
        this.description="BambooOriginSandbox";
    }
}

