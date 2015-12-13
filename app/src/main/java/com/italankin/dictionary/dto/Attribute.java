package com.italankin.dictionary.dto;

public abstract class Attribute {

    /**
     * Text of article, translation or synonym
     */
    public String text;

    /**
     * Part of speech
     */
    public String pos;

    /**
     * Quantity
     */
    public String num;

    /**
     * Gender, if used
     */
    public String gen;

    /**
     * Aspect
     */
    public String asp;

}
