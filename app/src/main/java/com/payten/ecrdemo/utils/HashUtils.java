package com.payten.ecrdemo.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {
    public static String performSHA512(String inputData){
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-512");
            byte[] messageDigestArray = messageDigest.digest(inputData.getBytes(StandardCharsets.UTF_8));
            String hashText;
            StringBuilder sb = new StringBuilder();

            for (byte b : messageDigestArray) {
                sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            hashText = sb.toString();
            return hashText;

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static byte[] digestBytes(String digestType, byte[] data){
        MessageDigest md;
        try {
            md = MessageDigest.getInstance(digestType);
            md.update(data);
            byte[] digest = md.digest();

            return digest;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return null;
    }
}
