package com.atlassian.plugins.quali.torque.api;

import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

//Error from the sandbox get by id response
public class SandboxErrorService implements Serializable {
    @SerializedName("time")
    public String time;
    @SerializedName("code")
    public String code;
    @SerializedName("message")
    public String message;
}