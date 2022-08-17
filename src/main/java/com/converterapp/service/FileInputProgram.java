package com.converterapp.service;

import com.converterapp.util.ConsoleDialog;
import com.converterapp.util.FileValidator;

import java.io.IOException;
import java.nio.file.Files;

public class FileInputProgram {
    //    @PostConstruct
    public static void run() throws IOException {
        String content = Files.readString(ConsoleDialog.getInstance().userPromptFilePath());
        if (FileValidator.isXML(content)) {
            System.out.println(Converter.xmlToJSON(content));
        } else if (FileValidator.isJSON(content)) {
            System.out.println(Converter.jsonToXML(content));
        } else {
            System.out.println("Content is neither xml nor json");
        }
    }
}
