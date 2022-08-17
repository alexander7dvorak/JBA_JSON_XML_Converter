package com.converterapp.util;

import java.io.File;

public class FileValidator {
    public static boolean isXML(String s) {
        return s.startsWith("<");
    }

    public static boolean isJSON(String s) {
        return s.startsWith("{");
    }

    public static boolean filePathExists(String s) {
        return new File(s).exists();
    }
}
