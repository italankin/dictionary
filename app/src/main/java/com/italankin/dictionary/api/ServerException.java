package com.italankin.dictionary.api;

public class ServerException extends RuntimeException {

    private final int mCode;

    public ServerException(String message, int code) {
        super(message);
        mCode = code;
    }

    public int getCode() {
        return mCode;
    }

}
