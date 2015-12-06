package com.italankin.dictionary;

public class MyException extends RuntimeException {

    private final int mCode;

    public MyException(String message, int code) {
        super(message);
        mCode = code;
    }

    public int getCode() {
        return mCode;
    }

}
