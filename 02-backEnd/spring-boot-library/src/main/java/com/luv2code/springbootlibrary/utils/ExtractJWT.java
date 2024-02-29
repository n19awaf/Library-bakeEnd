package com.luv2code.springbootlibrary.utils;

public class ExtractJWT {


    public static String payloadJWTExtraction(String token){

        token.replace("Bearer","");

        String[] chunks = token.split("\\.");
    }
}
