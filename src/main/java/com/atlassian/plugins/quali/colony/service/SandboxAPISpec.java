package com.atlassian.plugins.quali.colony.service;

import com.atlassian.plugins.quali.colony.api.CreateSandboxRequest;
import com.atlassian.plugins.quali.colony.api.CreateSandboxResponse;
import retrofit2.Call;
import retrofit2.http.*;

/**
 * Created by shay-k on 21/06/2017.
 */
public interface SandboxAPISpec {

    @POST("api/spaces/{spaceName}/sandbox")
    Call<CreateSandboxResponse> createSandbox(@Header("Authorization") String token,
                                              @Path("spaceName") String spaceName,
                                              @Body CreateSandboxRequest request);

    @DELETE("api/spaces/{spaceName}/sandbox/{sandboxId}")
    Call<Void> deleteSandbox(@Header("Authorization") String token,
                             @Path("spaceName") String spaceName,
                             @Path("sandboxId") String sandboxId);

    @GET("api/spaces/{spaceName}/sandbox/{sandboxId}")
    Call<Object> getSandboxById(@Header("Authorization") String token,
                                       @Path("spaceName") String spaceName,
                                       @Path("sandboxId") String sandboxId);
}
