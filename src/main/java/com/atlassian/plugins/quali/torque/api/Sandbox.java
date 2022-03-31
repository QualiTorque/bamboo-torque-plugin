package com.atlassian.plugins.quali.torque.api;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class Sandbox implements Serializable
{
    @SerializedName("id")
    public String id;
    @SerializedName("name")
    public String name;
    @SerializedName("blueprint")
    public String blueprint;
    @SerializedName("services")
    public List<Service> services;
    @SerializedName("sandbox_status")
    public String sandboxStatus;
}

