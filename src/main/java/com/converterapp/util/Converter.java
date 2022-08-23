package com.converterapp.util;

import com.converterapp.model.JsonDto;
import com.converterapp.model.XmlDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Converter {
    public static String xmlToJSON(String xmlString) {
        XmlDto xmlObject = createXmlDtoFromFileContent(xmlString);
        return new JsonDto(xmlObject).toString();
    }

    public static XmlDto createXmlDtoFromFileContent(String fileContent) {
        Pattern patternContent = Pattern.compile("((.*>(.*)<.*?)|(>(.*)))$", Pattern.DOTALL);
        Matcher matcherContent = patternContent.matcher(fileContent);
        String content = "";
        if (matcherContent.find()) {
            content = matcherContent.group(3) == null ? matcherContent.group(5) : matcherContent.group(3);
            int indexQ = fileContent.lastIndexOf('<');
            int indexW = fileContent.indexOf('<');
            fileContent = fileContent.substring(0, indexQ == indexW ? fileContent.length() : indexQ);
        }


        String betweenTagsString = extractStringBetweenTagsXML(fileContent);
        List<String> tagTokens = new ArrayList<>(List.of(Arrays.stream(betweenTagsString.split("((=)|(\")| )")).filter(s -> !s.isEmpty()).toArray(String[]::new)));
        while (tagTokens.contains("=")) {
            tagTokens.remove("=");
        }

        String tagName = tagTokens.get(0);
        HashMap<String, String> tagAttributes = new HashMap<>();
        for (int i = 1; i < tagTokens.size(); i += 2) {
            tagAttributes.put(tagTokens.get(i), tagTokens.get(i + 1));
        }
        fileContent = fileContent.substring(fileContent.indexOf('>') + 1);

        List<XmlDto> children = new ArrayList<>();
        Pattern patternChild = Pattern.compile("(?:(?:(?:<(\\w+)\\s[^>/]*>.*?<(\\/?)\\1)|(?:<(\\w+)>.*?<(\\/?)\\3))\\/?>)|(<\\/.*?>)|(<[^>]*?\\/>)", Pattern.DOTALL);
        Matcher matcherChild = patternChild.matcher(fileContent);
        while (matcherChild.find()) {
            children.add(createXmlDtoFromFileContent(matcherChild.group()));

        }
        return new XmlDto(tagName, tagAttributes, children, content);
    }

    private static String extractStringBetweenTagsXML(String xml) {
        return xml.substring(
                xml.indexOf('<') + 1,
                Math.min(xml.indexOf('/') == -1 ? Integer.MAX_VALUE : xml.indexOf('/'), xml.indexOf('>'))
        );
    }

    public static String jsonToXML(String jsonString) {
        List<JsonDto> jsonObjectList = createJsonDtoListFromFileContent(jsonString, true);
        return jsonObjectList.stream().map(jsonDto -> new XmlDto(jsonDto).toString()).collect(Collectors.joining());
    }

    private static String getTagName(String fileContent) {
        Pattern patternTagName = Pattern.compile("\"(.*?)\"", Pattern.DOTALL);
        Matcher matcherTagName = patternTagName.matcher(fileContent);
        if (matcherTagName.find()) {
            return matcherTagName.group(1);
        } else {
            return null;
        }
    }

    private static String getValueBetweenDoubleDotsAndComa(String fileContent) {
        Matcher matcher = Pattern.compile("\\\\S").matcher(fileContent.substring(fileContent.indexOf(':') + 1));
        return fileContent.substring(
                matcher.find() ?
                        matcher.start() :
                        fileContent.indexOf(':') + 1,
                fileContent.indexOf(',') == -1 ?
                        fileContent.length() :
                        fileContent.indexOf(',')).trim();
    }

    public static List<JsonDto> createJsonDtoListFromFileContent(String fileContent, boolean root) {
        String tagName;
        String content;
        List<JsonDto> output = new ArrayList<>();
        List<JsonDto> children = new ArrayList<>();
        HashMap<String, String> tagAttributes = new HashMap<>();
        if (root) {
            if (fileContent.indexOf('[') != -1 && fileContent.indexOf('[') < fileContent.indexOf('{')) {
                content = fileContent;
                tagName = "array";
                return List.of(new JsonDto(tagName, tagAttributes, output, content));
            } else {
                fileContent = fileContent.substring(fileContent.indexOf('{') + 1, fileContent.lastIndexOf('}'));
            }
        }
        int indexOfComma;
        int indexOfOpeningBrace;
        int indexOfOpeningQuote;
        int currentIndex;
        boolean emptyTagName;
        do {
            tagName = getTagName(fileContent);
            fileContent = fileContent.replaceFirst('"' + tagName + '"', "");
            emptyTagName = tagName == null || tagName.length() == 0;
            indexOfComma = fileContent.indexOf(',') == -1 ? Integer.MAX_VALUE : fileContent.indexOf(',');
            indexOfOpeningBrace = fileContent.indexOf('{') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('{');
            indexOfOpeningQuote = fileContent.indexOf('"') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('"');
            currentIndex = Math.min(indexOfOpeningBrace, Math.min(indexOfComma, indexOfOpeningQuote));
            //        System.out.println(currentIndex);
            if (currentIndex == Integer.MAX_VALUE) {
                if (fileContent.indexOf(':') != -1) {
                    content = getValueBetweenDoubleDotsAndComa(fileContent);
                    output.add(new JsonDto(tagName, tagAttributes, children, content));
                }
                break;
            } else if (currentIndex == indexOfOpeningBrace) {
                boolean wrong = false;
                children = new ArrayList<>();
                tagAttributes = new HashMap<>();
                content = null;
                if (emptyTagName) {
                    fileContent = fileContent.replaceFirst("\\{" +
                            getStringBetweenBraces(fileContent, currentIndex, '{', '}').
                                    replaceAll("\\{", "\\\\{") + "}", "");
                } else {
                    String childrenContent = getStringBetweenBraces(fileContent, currentIndex, '{', '}');
                    if (childrenContent.matches("\\s*")) {
                        output.add(new JsonDto(tagName, tagAttributes, children, ""));
                    } else {
                        List<JsonDto> childrenDto = createJsonDtoListFromFileContent(childrenContent, false);
                        List<String> childrenDtoTagNames = childrenDto.stream().map(JsonDto::getTagName).collect(Collectors.toCollection(ArrayList::new));
                        List<JsonDto> childrenToBeRemoved = new ArrayList<>();
                        if (childrenDto.size() == 0) {
                            output.add(new JsonDto(tagName, tagAttributes, children, ""));
                        } else {
                            int hashtagTagNames = 0;
                            for (JsonDto child : childrenDto) {
                                if (child.getTagName() != null) {
                                    String childTagName = child.getTagName();
                                    if (childTagName.startsWith("#")) {

                                        if (childrenDtoTagNames.contains(childTagName.substring(1))) {
                                            childrenToBeRemoved.add(child);
                                        }
                                        hashtagTagNames++;
                                        if (!childTagName.equals("#" + tagName) || child.getChildren().size() > 0) {
                                            wrong = true;
                                        }
                                    } else if (childTagName.startsWith("@")) {
                                        if (childrenDtoTagNames.contains(childTagName.substring(1))) {
                                            childrenToBeRemoved.add(child);
                                        }
                                        if (childTagName.equals("@") || child.getChildren().size() > 0) {
                                            wrong = true;
                                        }
                                    } else {
                                        wrong = true;
                                    }
                                }
                            }
                            for (JsonDto childToBeRemoved : childrenToBeRemoved) {
                                childrenDto.remove(childToBeRemoved);
                            }
                            if (hashtagTagNames != 1) {
                                wrong = true;
                            }
                            for (JsonDto child : childrenDto) {
                                if (child.getTagName() != null) {
                                    String childTagName = child.getTagName();
                                    if (childTagName.startsWith("#") && childTagName.length() > 1) {
                                        if (wrong) {
                                            childTagName = childTagName.substring(1);
                                            child.setTagName(childTagName);
                                        } else {
                                            content = child.getContent() == null ? "null" : child.getContent();
                                        }
                                    } else if (childTagName.startsWith("@") && childTagName.length() > 1 && child.getChildren().size() == 0) {
                                        if (wrong) {
                                            childTagName = childTagName.substring(1);
                                            child.setTagName(childTagName);
                                        } else {
                                            tagAttributes.put(childTagName.substring(1), child.getContent());
                                        }
                                    }
                                    if (!childTagName.startsWith("#") && !childTagName.startsWith("@")) {
                                        children.add(child);
                                    }
                                }
                            }
                            output.add(new JsonDto(tagName, tagAttributes, children, content));
                        }
                    }
                    fileContent = fileContent.replaceFirst("\\{" + childrenContent.replaceAll("\\{", "\\\\{") + "}", "");
                }
            } else if (currentIndex == indexOfComma) {
                children = new ArrayList<>();
                tagAttributes = new HashMap<>();
                if (emptyTagName) {
                    fileContent = fileContent.substring(currentIndex + 1);
                } else {
                    content = getValueBetweenDoubleDotsAndComa(fileContent).trim();
                    if (tagName.startsWith("@") && content.equals("null")) {
                        content = "";
                    }
                    output.add(new JsonDto(tagName, tagAttributes, children, content));
                    fileContent = fileContent.replaceFirst(content, "");
                }
            } else if (currentIndex == indexOfOpeningQuote) {
                children = new ArrayList<>();
                tagAttributes = new HashMap<>();
                if (emptyTagName) {
                    fileContent = fileContent.substring(fileContent.indexOf('"', currentIndex + 1) + 1);
                } else {
                    content = getStringBetweenBraces(fileContent, currentIndex, '"', '"');
                    output.add(new JsonDto(tagName, tagAttributes, new ArrayList<>(), content));
                    fileContent = fileContent.replaceFirst("\"" + content + "\"", "");

                }
            }

            fileContent = fileContent.replaceFirst(":", "");

            indexOfOpeningBrace = fileContent.indexOf('{') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('{');
            indexOfOpeningQuote = fileContent.indexOf('"') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('"');
            indexOfComma = fileContent.indexOf(',') == -1 ? Integer.MAX_VALUE : fileContent.indexOf(',');
            if (indexOfComma < indexOfOpeningBrace && indexOfComma < indexOfOpeningQuote) {
                fileContent = fileContent.replaceFirst(",", "");
                indexOfOpeningBrace = fileContent.indexOf('{') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('{');
                indexOfOpeningQuote = fileContent.indexOf('"') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('"');
                indexOfComma = fileContent.indexOf(',') == -1 ? Integer.MAX_VALUE : fileContent.indexOf(',');
                currentIndex = Math.min(indexOfOpeningBrace, Math.min(indexOfOpeningQuote, indexOfComma));
            } else {
                break;
            }
        } while (currentIndex != -1 && currentIndex != Integer.MAX_VALUE);
        return output;
    }

    private static String getStringBetweenBraces(String fileContent, int indexOfOpeningBrace, char openingBrace, char closingBrace) {
        int counter = 1;
        int currentIndex = indexOfOpeningBrace + 1;
        while (counter != 0) {
            if (fileContent.charAt(currentIndex) == closingBrace) {
                counter--;
            } else if (fileContent.charAt(currentIndex) == openingBrace) {
                counter++;
            }
            currentIndex++;
        }
        return fileContent.substring(indexOfOpeningBrace + 1, currentIndex - 1);
    }
}