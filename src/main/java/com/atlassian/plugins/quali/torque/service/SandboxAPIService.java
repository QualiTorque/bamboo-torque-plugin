package com.atlassian.plugins.quali.torque.service;

import com.atlassian.plugins.quali.torque.api.CreateSandboxRequest;
import com.atlassian.plugins.quali.torque.api.CreateSandboxResponse;
import com.atlassian.plugins.quali.torque.api.ResponseData;

import java.io.IOException;

public interface SandboxAPIService
{
    ResponseData<CreateSandboxResponse> createSandbox(String spaceName, final CreateSandboxRequest req) throws IOException;
    ResponseData<Void> deleteSandbox(String spaceName, String sandboxId) throws IOException;
    ResponseData<Object> getSandboxById(String spaceName, String sandboxId) throws IOException;
    ResponseData<Object> getSpacesList() throws IOException;
}