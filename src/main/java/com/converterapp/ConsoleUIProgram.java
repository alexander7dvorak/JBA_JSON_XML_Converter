package com.converterapp;

import com.converterapp.model.ConsoleArgs;
import com.converterapp.model.JsonDto;
import com.converterapp.service.*;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConsoleUIProgram {

    public static void main(String[] args) throws IOException {
        Dialog dialog = ConsoleDialog.getInstance();
        if (args.length == 0) {
            run(dialog);
        } else {
            test(args, dialog);
        }
    }

    private static void run(Dialog cd) throws IOException {
        Path path = cd.getUserInput();
        String content = Files.readString(path);
        if (FileValidator.isXML(content)) {
            System.out.println(XmlDtoReader.readXmlDto(new StringBuilder(content)).getHierarchy());
        } else if (FileValidator.isJSON(content)) {
            for (JsonDto json : JsonDtoReader.readJsonDtoList(new StringBuilder(content), true)) {
                System.out.println(json.getHierarchy());
            }
        } else {
            System.out.println("Content is neither xml nor json");
        }
    }

    private static void test(String[] args, Dialog cd) {
        String content;
        switch (ConsoleArgs.valueOf(args[0].toUpperCase())) {
            case STAGE1TEST -> {
                content = cd.userPromptLine();
                if (FileValidator.isXML(content)) {
                    System.out.println(Converter.xmlToJSON(content));
                } else if (FileValidator.isJSON(content)) {
                    System.out.println(Converter.jsonToXML(content));
                } else {
                    System.out.println("Content is neither xml nor json");
                }
            }
            case STAGE2TEST, STAGE5TEST, STAGE6TEST -> {
                content = org.assertj.core.util.Files.contentOf(new File("test.txt"), StandardCharsets.UTF_8);
                if (FileValidator.isXML(content)) {
                    System.out.println(Converter.xmlToJSON(content));
                } else if (FileValidator.isJSON(content)) {
                    System.out.println(Converter.jsonToXML(content));
                } else {
                    System.out.println("Content is neither xml nor json");
                }
            }
            case STAGE3TEST, STAGE4TEST -> {
                content = org.assertj.core.util.Files.contentOf(new File("test.txt"), StandardCharsets.UTF_8);
                System.out.println(HierarchyService.createHierarchyFromFileContent(content));
            }
        }
    }
}
