package com.converterapp.util;

import com.converterapp.model.JsonDto;
import com.converterapp.model.XmlDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Converter {
    public static String xmlToJSON(String xmlString) {
        XmlDto xmlObject = createXmlDtoFromFileContent(xmlString);

        return new JsonDto(xmlObject).toString();
    }

    public static String jsonToXML(String jsonString) {
        List<JsonDto> jsonObjectsList = createJsonDtoListFromFileContent(jsonString, true);
        String jsonObjectsListString = jsonObjectsList.stream().map(jsonDto -> new XmlDto(jsonDto).toString()).collect(Collectors.joining());
        return jsonObjectsList.size() > 1 ?
                "<root>%s</root>".formatted(jsonObjectsListString) :
                jsonObjectsListString;
    }

    public static XmlDto createXmlDtoFromFileContent(String fileContent) {
        boolean isArray = fileContent.startsWith("[");
        fileContent = fileContent.replaceAll("<\\?.*?>", "");
        Pattern patternContent = Pattern.compile("((.*>(.*)<.*?)|(>(.*)))$", Pattern.DOTALL);
        Matcher matcherContent = patternContent.matcher(fileContent);
        String content = null;
        if (matcherContent.find()) {
            content = matcherContent.group(3) == null ?
                    (matcherContent.group(2) == null ? null : "") :
                    matcherContent.group(3);
            int indexQ = fileContent.lastIndexOf('<');
            int indexW = fileContent.indexOf('<');
            fileContent = fileContent.substring(0, indexQ == indexW ? fileContent.length() : indexQ);
        }
        String betweenTagsString = StringUtil.extractStringBetweenTagsXML(fileContent);
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
        String tagName = tagTokens.get(0);
        HashMap<String, String> tagAttributes = new HashMap<>();
        for (int i = 1; i < tagTokens.size(); i += 2) {
            tagAttributes.put(tagTokens.get(i), tagTokens.get(i + 1));
        }
        fileContent = fileContent.substring(fileContent.indexOf('>') + 1);
        List<XmlDto> children = new ArrayList<>();
        while (!fileContent.trim().equals("")) {
            String childContent = StringUtil.getXmlElement(fileContent);
            if (childContent.length() > 0) {
                children.add(createXmlDtoFromFileContent(childContent));
                fileContent = fileContent.replaceFirst(childContent, "");
            } else {
                break;
            }
        }
        return new XmlDto(tagName, tagAttributes, children, content, isArray);
    }

    public static List<JsonDto> createJsonDtoListFromFileContent(String fileContent, boolean root) {
        String tagName;
        String content;
        List<JsonDto> output = new ArrayList<>();
        List<JsonDto> children;
        HashMap<String, String> tagAttributes;
        if (root) {
            if (fileContent.indexOf('[') != -1 && fileContent.indexOf('[') < fileContent.indexOf('{')) {
                return List.of(new JsonDto(null, fileContent, true, true, true));
            } else {
                fileContent = fileContent.substring(fileContent.indexOf('{') + 1, fileContent.lastIndexOf('}'));
            }
        }
        int indexOfComma;
        int indexOfOpeningBrace;
        int indexOfOpeningOfArray;
        int indexOfOpeningQuote;
        int currentIndex;
        boolean emptyTagName = false;
        do {
            indexOfOpeningQuote = fileContent.indexOf('"') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('"');
            indexOfOpeningOfArray = fileContent.indexOf('[') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('[');
            if (indexOfOpeningQuote < indexOfOpeningOfArray) {
                tagName = StringUtil.getTagName(fileContent);
                fileContent = fileContent.replaceFirst('"' + tagName + '"', "");
                emptyTagName = tagName == null || tagName.length() == 0;
            } else {
                tagName = null;
            }
            indexOfComma = fileContent.indexOf(',') == -1 ? Integer.MAX_VALUE : fileContent.indexOf(',');
            indexOfOpeningBrace = fileContent.indexOf('{') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('{');
            indexOfOpeningOfArray = fileContent.indexOf('[') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('[');
            indexOfOpeningQuote = fileContent.indexOf('"') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('"');
            currentIndex = Math.min(indexOfOpeningBrace, Math.min(indexOfComma, Math.min(indexOfOpeningQuote, indexOfOpeningOfArray)));
            if (currentIndex == Integer.MAX_VALUE) {
                if (fileContent.indexOf(':') != -1) {
                    children = new ArrayList<>();
                    tagAttributes = new HashMap<>();
                    content = StringUtil.getValueBetweenDoubleDotsAndComa(fileContent);
                    output.add(new JsonDto(tagName, tagAttributes, children, content, root, false));
                }
                break;
            } else if (currentIndex == indexOfOpeningOfArray) {
                content = StringUtil.getStringBetweenBraces(fileContent, currentIndex, '[', ']');
                fileContent = fileContent.replaceFirst("\\[" + content.
                        replaceAll("\\{", "\\\\{").replaceAll("\\[", "\\\\[") + ']', "");
                if (!tagName.equals("#")) {
                    output.add(new JsonDto(tagName, content, false, false, true));
                }
            } else if (currentIndex == indexOfOpeningBrace) {
                boolean wrong = false;
                children = new ArrayList<>();
                tagAttributes = new HashMap<>();
                content = null;
                if (emptyTagName) {
                    fileContent = fileContent.replaceFirst("\\{" +
                            StringUtil.getStringBetweenBraces(fileContent, currentIndex, '{', '}').
                                    replaceAll("\\{", "\\\\{").replaceAll("\\[", "\\\\[") + "}", "");
                } else {
                    String childrenContent = StringUtil.getStringBetweenBraces(fileContent, currentIndex, '{', '}');
                    if (childrenContent.matches("\\s*")) {
                        output.add(new JsonDto(tagName, tagAttributes, children, "", root, false));
                    } else {
                        //childrenDto size = 2 ""@attr1":null" ""#crazyattr9":"v23""
                        List<JsonDto> childrenDto = createJsonDtoListFromFileContent(childrenContent, false);
                        List<String> childrenDtoTagNames = childrenDto.stream().map(JsonDto::getTagName).collect(Collectors.toCollection(ArrayList::new));
                        List<JsonDto> childrenToBeRemoved = new ArrayList<>();
                        if (childrenDto.size() == 0) {
                            output.add(new JsonDto(tagName, tagAttributes, children, "", root, false));
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
                                        if (!childTagName.equals("#" + tagName)) {
                                            wrong = true;
                                        }
                                        if (child.getChildren().size() > 0) {
                                            children = child.getChildren();
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
                                            content = child.getContent() == null ? "null" : (String) child.getContent();
                                        }
                                    } else if (childTagName.startsWith("@") && childTagName.length() > 1) {
                                        if (wrong) {
                                            childTagName = childTagName.substring(1);
                                            child.setTagName(childTagName);
                                        } else {
                                            tagAttributes.put(childTagName.substring(1), (String) (child.getContent() == null ? "" : child.getContent()));
                                        }
                                    }
                                    if (!childTagName.startsWith("#") && !childTagName.startsWith("@")) {
                                        children.add(child);
                                    }
                                }
                            }
                            output.add(new JsonDto(tagName, tagAttributes, children, content, root, false));
                        }
                    }
                    fileContent = fileContent.replaceFirst("\\{" + childrenContent.replaceAll("\\{", "\\\\{").replaceAll("\\[", "\\\\[") + '}', "");
                }
            } else if (currentIndex == indexOfComma) {
                children = new ArrayList<>();
                tagAttributes = new HashMap<>();
                if (emptyTagName) {
                    fileContent = fileContent.replaceFirst(StringUtil.getValueBetweenDoubleDotsAndComa(fileContent), "");
                } else {
                    content = StringUtil.getValueBetweenDoubleDotsAndComa(fileContent).trim();
                    fileContent = fileContent.replaceFirst(content, "");
                    if (tagName.startsWith("@") && content.equals("null")) {
                        content = "";
                    }
                    output.add(new JsonDto(tagName, tagAttributes, children, content, root, false));
                }
            } else if (currentIndex == indexOfOpeningQuote) {
                tagAttributes = new HashMap<>();
                if (emptyTagName) {
                    fileContent = fileContent.substring(fileContent.indexOf('"', currentIndex + 1) + 1);
                } else {
                    content = StringUtil.getStringBetweenBraces(fileContent, currentIndex, '"', '"');
                    output.add(new JsonDto(tagName, tagAttributes, new ArrayList<>(), content, root, false));
                    fileContent = fileContent.replaceFirst("\"" + content + "\"", "");
                }
            }
            fileContent = fileContent.replaceFirst(":", "");
            indexOfOpeningBrace = fileContent.indexOf('{') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('{');
            indexOfOpeningOfArray = fileContent.indexOf('[') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('[');
            indexOfOpeningQuote = fileContent.indexOf('"') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('"');
            indexOfComma = fileContent.indexOf(',') == -1 ? Integer.MAX_VALUE : fileContent.indexOf(',');
            if (indexOfComma < indexOfOpeningBrace && indexOfComma < indexOfOpeningQuote && indexOfComma < indexOfOpeningOfArray) {
                fileContent = fileContent.replaceFirst(",", "");
                indexOfComma = fileContent.indexOf(',') == -1 ? Integer.MAX_VALUE : fileContent.indexOf(',');
                indexOfOpeningBrace = fileContent.indexOf('{') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('{');
                indexOfOpeningOfArray = fileContent.indexOf('[') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('[');
                indexOfOpeningQuote = fileContent.indexOf('"') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('"');
                currentIndex = Math.min(indexOfOpeningBrace, Math.min(indexOfComma, Math.min(indexOfOpeningQuote, indexOfOpeningOfArray)));
            } else {
                break;
            }
        } while (currentIndex != -1 && currentIndex != Integer.MAX_VALUE);
        return output;
    }
}