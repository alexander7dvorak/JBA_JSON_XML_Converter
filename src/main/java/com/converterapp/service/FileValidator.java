package com.converterapp.service;

import java.io.File;

public class FileValidator {
    public static boolean isXML(String s) {
        return s.startsWith("<");
    }

    public static boolean isJSON(String s) {
        return s.startsWith("{") || s.startsWith("[");
    }

    public static boolean isJSONArray(String s) {
        return s.indexOf('[') != -1 && s.indexOf('[') < s.indexOf('{');
    }

    public static boolean filePathExists(String s) {
        return new File(s).exists();
    }
}
