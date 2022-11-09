package com.converterapp;

import com.converterapp.model.AppConstants;
import com.converterapp.model.ConsoleArgs;
import com.converterapp.model.JsonDto;
import com.converterapp.service.ConsoleDialog;
import com.converterapp.service.Converter;
import com.converterapp.service.Dialog;
import com.converterapp.service.FileValidator;
import com.converterapp.service.HierarchyService;
import com.converterapp.service.JsonDtoReader;
import com.converterapp.service.XmlDtoReader;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConsoleUIProgram {
    private static final Logger logger
            = Logger.getLogger(ConsoleUIProgram.class.getName());

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
            logger.log(Level.INFO, XmlDtoReader.readXmlDto(new StringBuilder(content)).getHierarchy());
        } else if (FileValidator.isJSON(content)) {
            for (JsonDto json : JsonDtoReader.readJsonDtoList(new StringBuilder(content), true)) {
                logger.log(Level.INFO, json.getHierarchy());
            }
        } else {
            logger.log(Level.INFO, AppConstants.CONTENT_IS_NEITHER_XML_NOR_JSON);
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
                    System.out.println(AppConstants.CONTENT_IS_NEITHER_XML_NOR_JSON);
                }
            }
            case STAGE2TEST, STAGE5TEST, STAGE6TEST -> {
                content = org.assertj.core.util.Files.contentOf(new File(AppConstants.TEST_DOT_TXT), StandardCharsets.UTF_8);
                if (FileValidator.isXML(content)) {
                    System.out.println(Converter.xmlToJSON(content));
                } else if (FileValidator.isJSON(content)) {
                    System.out.println(Converter.jsonToXML(content));
                } else {
                    System.out.println(AppConstants.CONTENT_IS_NEITHER_XML_NOR_JSON);
                }
            }
            case STAGE3TEST, STAGE4TEST -> {
                content = org.assertj.core.util.Files.contentOf(new File(AppConstants.TEST_DOT_TXT), StandardCharsets.UTF_8);
                System.out.println(HierarchyService.createHierarchyFromFileContent(content));
            }
        }
    }
}
