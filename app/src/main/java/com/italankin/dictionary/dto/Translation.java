package com.italankin.dictionary.dto;

public class Translation extends Attribute {

    public Synonym[] syn;
    public Mean[] mean;
    public Example[] ex;

    public static class Synonym extends Attribute {
    }

    public static class Mean extends Attribute {
    }

    public static class Example extends Attribute {
        public Translation[] tr;
    }

}
