package com.atlassian.plugins.quali.colony.service;

import com.atlassian.plugins.quali.colony.api.CreateSandboxRequest;
import com.atlassian.plugins.quali.colony.api.CreateSandboxResponse;
import com.atlassian.plugins.quali.colony.api.ResponseData;

import java.io.IOException;

public interface SandboxAPIService
{
    ResponseData<CreateSandboxResponse> createSandbox(String spaceName, final CreateSandboxRequest req) throws IOException;
    ResponseData<Void> deleteSandbox(String spaceName, String sandboxId) throws IOException;
    ResponseData<Object> getSandboxById(String spaceName, String sandboxId) throws IOException;
    ResponseData<Object> getSpacesList() throws IOException;
}