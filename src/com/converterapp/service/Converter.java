package com.converterapp.service;

import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class Converter {
    private static final Scanner scanner = new Scanner(System.in);

    @PostConstruct
    public static void run() {
        String inputLine = scanner.nextLine();
        if (isXML(inputLine)) {
            System.out.println(xmlToJSON(inputLine));
        } else if (isJSON(inputLine)) {
            System.out.println(jsonToXML(inputLine));
        }
    }

    private static boolean isXML(String s) {
        return s.startsWith("<");
    }

    private static boolean isJSON(String s) {
        return s.startsWith("{");
    }

    private static String xmlToJSON(String xml) {
        String key = extractElementBetweenTags(xml);
        xml = xml.replace('<' + key + '>', "");
        String value = extractContent(xml);
        return value == null || value.length() == 0 ?
                "{\"%s\" : null}".formatted(key) :
                "{\"%s\" : \"%s\"}".formatted(key, value);
    }

    private static String extractElementBetweenTags(String xml) {
        return xml.substring(
                xml.indexOf('<') + 1,
                Math.min(xml.indexOf('/'), xml.indexOf('>'))
        );
    }

    private static String extractContent(String xml) {
        return xml == null || xml.length() == 0 ? null : xml.substring(0, xml.indexOf('<'));
    }

    private static String jsonToXML(String json) {
        Pattern pattern = Pattern.compile("\"(.*?)\"");
        Matcher matcher = pattern.matcher(json);
        String key = "";
        if (matcher.find()) {
            key = matcher.group(1);
        }
        matcher = pattern.matcher(json.replace('"' + key + '"', ""));
        String value = "";
        if (matcher.find()) {
            value = matcher.group(1);
        }
        return value == null || value.length() == 0 ?
                "<%s/>".formatted(key) :
                "<%s>%s</%s>".formatted(key, value, key);
    }
}