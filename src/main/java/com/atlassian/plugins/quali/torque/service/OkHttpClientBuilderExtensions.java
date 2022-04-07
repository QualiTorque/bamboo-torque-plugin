package com.atlassian.plugins.quali.torque.service;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.io.IOException;

public class OkHttpClientBuilderExtensions {
    public static void injectHeader(OkHttpClient.Builder builder, String name, String value)
    {
        builder.addInterceptor(new Interceptor() {
            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request request = chain.request().newBuilder().addHeader(name, value).build();
                return chain.proceed(request);
            }
        });
    }
}
