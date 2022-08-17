package com.converterapp.service;

import com.converterapp.model.JsonDto;
import com.converterapp.model.XmlDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Converter {
    public static String xmlToJSON(String xmlString) {
        String betweenTagsString = extractStringBetweenTagsXML(xmlString);
        List<String> tagTokens = new ArrayList<>(List.of(betweenTagsString.split(" ")));
        while (tagTokens.contains("=")) {
            tagTokens.remove("=");
        }

        String tagName = tagTokens.get(0);
        HashMap<String, String> tagAttributes = new HashMap<>();
        for (int i = 1; i < tagTokens.size(); i += 2) {
            tagAttributes.put(tagTokens.get(i), tagTokens.get(i + 1));
        }
        xmlString = xmlString.substring(xmlString.indexOf('>') + 1);
        List<XmlDto> children = new ArrayList<>();
        String content = extractContentXML(xmlString);

        XmlDto xmlObject = new XmlDto(tagName, tagAttributes, children, content);
        return new JsonDto(xmlObject).toString();
    }


    private static String extractStringBetweenTagsXML(String xml) {
        return xml.substring(
                xml.indexOf('<') + 1,
                Math.min(xml.indexOf('/'), xml.indexOf('>'))
        );
    }

    private static String extractContentXML(String xml) {
        return xml == null || xml.length() == 0 ? null : xml.substring(0, xml.indexOf('<'));
    }

    public static String jsonToXML(String jsonString) {
        jsonString = jsonString.substring(jsonString.indexOf('{') + 1, jsonString.lastIndexOf('}'));
        Pattern pattern = Pattern.compile("\"(.*?)\"");
        Matcher matcher = pattern.matcher(jsonString);
        String tagName = "";
        if (matcher.find()) {
            tagName = matcher.group(1);
        }
        jsonString = jsonString.substring(matcher.end() + 1);
        matcher = pattern.matcher(jsonString);
        Matcher matcherChildren = Pattern.compile("\\{(.*?)}").matcher(jsonString);
        String value = "";
        HashMap<String, String> tagAttributes = new HashMap<>();
        if (matcherChildren.find()) {
            value = jsonString.substring(jsonString.indexOf('{') + 1, jsonString.lastIndexOf('}'));
            List<String> tagTokens = new ArrayList<>(List.of(value.split(",")));
            for (String line : tagTokens) {
                String[] entry = line.split(":");
                System.out.println(Arrays.toString(entry));
                String key = entry[0].trim();
                key = key.substring(1, key.length() - 1);
                String valueEntry = entry[1].trim();
                valueEntry = valueEntry.startsWith("\"") ? valueEntry.substring(1, Math.max(1, valueEntry.length() - 1)) : valueEntry;
                tagAttributes.put(key, valueEntry);
            }
        } else if (matcher.find()) {
            value = matcher.group(1);
        }
        String content = value;

        List<String> toRemove = new ArrayList<>();
        HashMap<String, String> outputHashMap = new HashMap<>();

        for (Map.Entry<String, String> entry : tagAttributes.entrySet()) {
            if (entry.getKey().startsWith("@")) {
                outputHashMap.put(entry.getKey().substring(1), entry.getValue());
                toRemove.add(entry.getKey());
            } else if (entry.getKey().startsWith("#")) {
                content = entry.getValue().equals("null") ? null : entry.getValue();
                toRemove.add(entry.getKey());
            }
        }
        for (String s : toRemove) {
            tagAttributes.remove(s);
        }
        List<JsonDto> children = new ArrayList<>();
        JsonDto jsonObject = new JsonDto(tagName, outputHashMap, children, content);
        return new XmlDto(jsonObject).toString();
    }
}