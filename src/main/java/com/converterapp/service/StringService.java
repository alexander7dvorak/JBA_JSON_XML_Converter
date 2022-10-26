package com.converterapp.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringService {
    public static String getStringBetweenBraces(String text, int indexOfOpeningBrace, char openingBrace, char closingBrace) {
        int counter = 1;
        int currentIndex = indexOfOpeningBrace;
        char currentChar;
        while (counter != 0) {
            currentChar = text.charAt(++currentIndex);
            if (currentChar == closingBrace) {
                counter--;
            } else if (currentChar == openingBrace) {
                counter++;
            }
        }
        return text.substring(indexOfOpeningBrace + 1, currentIndex);
    }

    public static String getXmlElement(String fileContent) {
        String tempContent = fileContent;

        int counter = 0;
        int indexOfTriangle;
        int indexOfSlash = tempContent.indexOf('/');
        int indexOfEndTriangle = tempContent.indexOf('>');
        do {
            if (indexOfSlash > indexOfEndTriangle) {
                counter++;
                tempContent = tempContent.substring(tempContent.indexOf('>') + 1);
            } else if (indexOfSlash < indexOfEndTriangle) {
                if (!tempContent.substring(0, indexOfEndTriangle + 1).trim().matches("<.*?/>")) {
                    counter--;
                }
                tempContent = tempContent.substring(tempContent.indexOf('>') + 1);
            } else {
                counter--;
                tempContent = tempContent.substring(
                        tempContent.indexOf(
                                '>', tempContent.indexOf(
                                        '/', tempContent.indexOf('>')
                                )
                        ) + 1
                );
            }
            indexOfTriangle = tempContent.indexOf('<');
            indexOfSlash = tempContent.indexOf('/');
            indexOfEndTriangle = tempContent.indexOf('>');
        } while (indexOfTriangle != -1 && counter != 0);

        return fileContent.replaceFirst(tempContent, "").trim();
    }

    public static String extractStringBetweenTagsXML(String xml) {
        return xml.substring(
                xml.indexOf('<') + 1,
                Math.min(xml.indexOf('/') == -1 ? Integer.MAX_VALUE : xml.indexOf('/'), xml.indexOf('>'))
        );
    }

    public static String getTagName(String fileContent) {
        Pattern patternTagName = Pattern.compile("\"(.*?)\"", Pattern.DOTALL);
        Matcher matcherTagName = patternTagName.matcher(fileContent);
        if (matcherTagName.find()) {
            return matcherTagName.group(1);
        } else {
            return null;
        }
    }

    public static String getValueBetweenDoubleDotsAndComa(String fileContent) {
        fileContent = fileContent.replaceAll("\\{", "\\\\{").replaceAll("\\[", "\\\\[");
        Matcher matcher = Pattern.compile("\\\\S").matcher(fileContent.substring(fileContent.indexOf(':') + 1));
        return fileContent.substring(
                matcher.find() ?
                        matcher.start() :
                        fileContent.indexOf(':') + 1,
                fileContent.indexOf(',') == -1 ?
                        fileContent.length() :
                        fileContent.indexOf(',')).trim();
    }
}
