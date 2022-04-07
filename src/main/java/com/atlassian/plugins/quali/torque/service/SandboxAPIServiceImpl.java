package com.atlassian.plugins.quali.torque.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import com.atlassian.plugins.quali.torque.api.*;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class SandboxAPIServiceImpl implements SandboxAPIService{
    private SandboxAPISpec sandboxAPI;
    private SandboxServiceConnection connection;

    public SandboxAPIServiceImpl(SandboxServiceConnection connection) {
        this.connection = connection;

        Gson gson = new GsonBuilder().setLenient().create();

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.connectTimeout(connection.connectionTimeoutSec,TimeUnit.SECONDS);
        builder.readTimeout(connection.readTimeoutSec, TimeUnit.SECONDS);

        OkHttpClientBuilderExtensions.injectHeader(builder,
                "User-Agent", "Torque-Plugin-Bamboo/" + VersionUtils.PACKAGE_VERSION);

        OkHttpClient client= builder.build();

        String baseUrl = String.format("%1$s",connection.address);
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .client(client)
                .build();


        sandboxAPI = retrofit.create(SandboxAPISpec.class);
    }

    public ResponseData<Object> getSandboxById(String spaceName, String sandboxId) throws RuntimeException, IOException {
        return execute(sandboxAPI.getSandboxById(this.connection.getAuthorizationHeader(), spaceName, sandboxId));
    }

    public ResponseData<CreateSandboxResponse> createSandbox(String spaceName, final CreateSandboxRequest req) throws IOException {
        return execute(sandboxAPI.createSandbox(this.connection.getAuthorizationHeader(), spaceName, req));
    }

    public ResponseData<Void> deleteSandbox(String spaceName, String sandboxId) throws IOException {
        return execute(sandboxAPI.deleteSandbox(this.connection.getAuthorizationHeader(), spaceName, sandboxId));
    }

    public ResponseData<Object> getSpacesList() throws IOException {
        return execute(sandboxAPI.getSpacesList(this.connection.getAuthorizationHeader()));
    }

    private static <T> ResponseData<T> parseResponse(final Response<T> response) throws IOException {
        String message = response.message();
        if (!response.isSuccessful()) {
            final String err = response.errorBody().string();
            return ResponseData.error(response.code(),err).setMessage(message);
        }
        String rawBodyJson  = new GsonBuilder().setPrettyPrinting().create().toJson(response.body());
        return ResponseData.ok(response.body(),response.code(), rawBodyJson).setMessage(message);
    }
    private static <T> ResponseData<T> createErrorResponse(Exception e) {
        String errorMessage = e.toString();
        return ResponseData.error(-1,errorMessage);
    }
    public <T> ResponseData<T> execute(Call<T> call) throws IOException {
        return parseResponse(call.execute());
    }
}
