package com.atlassian.plugins.quali.torque.api;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class SingleSandbox implements Serializable
{
    @SerializedName("id")
    public String id;
    @SerializedName("name")
    public String name;
    @SerializedName("blueprint_name")
    public String blueprint_name;
    @SerializedName("applications")
    public List<Service> applications;
    @SerializedName("sandbox_status")
    public String sandboxStatus;
    @SerializedName("errors")
    public List<SandboxErrorService> sandboxErrors;
}
