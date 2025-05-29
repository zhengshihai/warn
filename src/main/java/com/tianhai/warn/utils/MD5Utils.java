package com.tianhai.warn.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5Utils {
    private static final int BUFFER_SIZE = 8 * 1024;

    // 对不同来源的文件使用流统一处理 调用方只需提供InputStream即可
    public static String md5Hex(InputStream inputStream) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("MD5");
        try (DigestInputStream digestInputStream = new DigestInputStream(inputStream, md)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (digestInputStream.read(buffer) != -1) {
                // digest 自动更新
            }
        }
        byte[] digest = md.digest();

        return bytesToHex(digest);
    }

    // 本地文件支持
    public static String md5Hex(File file) throws IOException, NoSuchAlgorithmException {
        try (InputStream is = new FileInputStream(file)) {
            return md5Hex(is);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuffer sb = new StringBuffer();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b&0xff));
        }

        return sb.toString();
    }
}
