package com.converterapp.service;

import com.converterapp.model.XmlDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XmlDtoReader {
    public static XmlDto readXmlDto(StringBuilder fileContentSB) {
        fileContentSB.replace(0, fileContentSB.length(), fileContentSB.toString().replaceAll("<\\?.*?>", ""));
        String content = processXmlDtoContentFromFileContent(fileContentSB);
        String betweenTagsString = StringService.extractStringBetweenTagsXML(fileContentSB.toString());
        List<String> tagTokens = createTagTokensListForXmlDto(betweenTagsString);
        String tagName = tagTokens.get(0);
        HashMap<String, String> tagAttributes = createTagAttributesHashMapForXmlDto(tagTokens);
        fileContentSB.replace(0, fileContentSB.length(), fileContentSB.substring(fileContentSB.toString().indexOf('>') + 1));
        List<XmlDto> children = createChildrenListForXmlDto(fileContentSB.toString());
        return XmlDto.generateNonElementXmlDto(tagName, tagAttributes, content, false, children);
    }

    private static String processXmlDtoContentFromFileContent(StringBuilder fileContentSB) {
        Pattern patternContent = Pattern.compile("((.*>(.*)<.*?)|(>(.*)))$", Pattern.DOTALL);
        Matcher matcherContent = patternContent.matcher(fileContentSB.toString());
        if (matcherContent.find()) {
            int indexQ = fileContentSB.toString().lastIndexOf('<');
            int indexW = fileContentSB.toString().indexOf('<');
            fileContentSB.replace(0, fileContentSB.length(), fileContentSB.substring(0, indexQ == indexW ? fileContentSB.length() : indexQ));
            return matcherContent.group(3) == null ?
                    (matcherContent.group(2) == null ? null : "") :
                    matcherContent.group(3);
        }
        return null;
    }

    private static List<String> createTagTokensListForXmlDto(String betweenTagsString) {
        List<String> tagTokens = new ArrayList<>(List.of(Arrays.stream(betweenTagsString.split("((=)| )"))
                .filter(s -> !s.isEmpty()).toArray(String[]::new)));
        for (int i = 0; i < tagTokens.size(); i++) {
            String s = tagTokens.get(i);
            if (s.startsWith("\"")) {
                tagTokens.set(i, s.substring(1, s.lastIndexOf("\"")));
            } else if (s.startsWith("'")) {
                tagTokens.set(i, s.substring(1, s.lastIndexOf("'")));
            }
        }
        return tagTokens;
    }

    private static HashMap<String, String> createTagAttributesHashMapForXmlDto(List<String> tagTokens) {
        HashMap<String, String> tagAttributes = new HashMap<>();
        for (int i = 1; i < tagTokens.size(); i += 2) {
            tagAttributes.put(tagTokens.get(i), tagTokens.get(i + 1));
        }
        return tagAttributes;
    }

    private static List<XmlDto> createChildrenListForXmlDto(String fileContent) {
        List<XmlDto> children = new ArrayList<>();
        while (!fileContent.trim().equals("")) {
            String childContent = StringService.getXmlElement(fileContent);
            if (childContent.length() > 0) {
                children.add(readXmlDto(new StringBuilder(childContent)));
                fileContent = fileContent.replaceFirst(childContent, "");
            } else {
                break;
            }
        }
        return children;
    }
}
