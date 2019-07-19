package com.atlassian.plugins.quali.colony.api;

import groovy.transform.builder.Builder;

import java.io.Serializable;

/**
 * Created by shay-k on 21/06/2017.
 */
@Builder
public class ResponseData<T>implements Serializable {

    private T data;
    private int statusCode;
    private String rawBodyJson;
    private String error;
    private String message;

    public T getData() {
        return data;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getError() {
        return error;
    }


    private ResponseData(int statusCode){
        this.statusCode = statusCode;
    }

    private ResponseData(T data, int statusCode, String rawBodyJson){
        this.data = data;
        this.statusCode = statusCode;
        this.rawBodyJson = rawBodyJson;
    }
    private ResponseData(int statusCode, String error){
        this.statusCode = statusCode;
        this.error = error;
    }

    public static <T> ResponseData<T> ok(T data, int statusCode, String rawBodyJson){
        return new ResponseData<T>(data,statusCode, rawBodyJson);
    }

    public static <T> ResponseData<T> error(int statusCode, String error){
        return new ResponseData<T>(statusCode,error);
    }

    public boolean isSuccessful(){
        return this.error == null;
    }


    public String getMessage() {
        return message;
    }

    public <T> ResponseData<T> setMessage(String message) {
        this.message = message;
        return (ResponseData<T>) this;
    }

    public String getRawBodyJson() {
        return rawBodyJson;
    }
}
