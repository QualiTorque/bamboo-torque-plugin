package com.atlassian.plugins.quali.torque.api;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;
import java.util.List;

public class Service implements Serializable
{
    @SerializedName("name")
    public String name;
    @SerializedName("shortcuts")
    public List<String> shortcuts;
    @SerializedName("status")
    public String status;
}
