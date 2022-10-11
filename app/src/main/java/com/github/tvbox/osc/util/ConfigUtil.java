package com.github.tvbox.osc.util;

import android.annotation.TargetApi;
import android.os.Build;
import android.util.Base64;

import com.github.catvod.crawler.SpiderDebug;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class ConfigUtil {

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String decryptConfig(String str) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            IvParameterSpec iv = new IvParameterSpec("2143658709123456".getBytes(StandardCharsets.UTF_8));
            byte[] keyInByte = "CryptedIsN@thing".getBytes(StandardCharsets.UTF_8);
            SecretKey originalKey = new SecretKeySpec(keyInByte, 0, keyInByte.length, "AES");
            cipher.init(Cipher.DECRYPT_MODE, originalKey, iv);
            byte[] plainText = cipher.doFinal(Base64.decode(str, 0));
            return new String(plainText);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String decodeConfig(String secretKey, String data) {
        try {
            if (data.startsWith("{"))
                return data;
            String decodedStr = data;

            int startedPoint = data.indexOf("**");
            if (startedPoint > 0) {
                decodedStr = new String(Base64.decode(data.substring(startedPoint + 2), 0));
            }

            if (decodedStr.startsWith("{"))
                return decodedStr;

            if (secretKey.isEmpty()) {
                if (decodedStr.startsWith("2423")) {
                    int suffixPos = decodedStr.indexOf("2324");
                    String pwdInHax = decodedStr.substring(4, suffixPos);
                    String pwd = new String(hexStringToByteArray(pwdInHax), StandardCharsets.UTF_8);
                    String roundtimeInHax = decodedStr.substring(decodedStr.length() - 26);
                    String roundtime = new String(hexStringToByteArray(roundtimeInHax), StandardCharsets.UTF_8);
                    decodedStr = decodedStr.substring(suffixPos + 4, decodedStr.length() - 26);
                    byte[] ivInByte = (roundtime + "0000000000000000".substring(0, 16 - roundtime.length()))
                            .getBytes(StandardCharsets.UTF_8);
                    byte[] key = (pwd + "0000000000000000".substring(0, 16 - pwd.length()))
                            .getBytes(StandardCharsets.UTF_8);

                    Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
                    IvParameterSpec iv = new IvParameterSpec(ivInByte);
                    SecretKey originalKey = new SecretKeySpec(key, 0, key.length, "AES");
                    cipher.init(Cipher.DECRYPT_MODE, originalKey, iv);
                    byte[] plainText = cipher.doFinal(hexStringToByteArray(decodedStr));
                    return new String(plainText);
                }
            } else {
                Cipher cipher = Cipher.getInstance("AES/ECB/PKCS7Padding");
                byte[] keyInByte = (secretKey + "0000000000000000".substring(0, 16 - secretKey.length()))
                        .getBytes(StandardCharsets.UTF_8);
                SecretKey originalKey = new SecretKeySpec(keyInByte, 0, keyInByte.length, "AES");
                cipher.init(Cipher.DECRYPT_MODE, originalKey);
                byte[] plainText = cipher.doFinal(hexStringToByteArray(decodedStr));
                return new String(plainText);
            }
        } catch (Exception ex) {
            SpiderDebug.log("Failed to decrypt config: " + ex.getMessage());
        }

        return "";
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static byte[] decodeSpider(String data) {

        try {
            int startedPoint = data.indexOf("**");
            if (startedPoint > 0) {
                return Base64.decode(data.substring(startedPoint + 2), 0);
            }
            return data.getBytes(StandardCharsets.UTF_8);
        }catch (Exception ex) {
            SpiderDebug.log("Failed to decrypt jar: " + ex.getMessage());
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    private static String getMD5(String data) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(data.getBytes(StandardCharsets.UTF_8));
        byte[] digest = md.digest();
        return String.format("%032x", new BigInteger(1, digest)).toLowerCase();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len/2];

        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }

        return data;
    }
}
