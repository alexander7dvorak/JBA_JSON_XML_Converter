package com.converterapp.util;

import com.converterapp.model.HierarchyElement;
import com.converterapp.model.JsonDto;
import com.converterapp.model.XmlDto;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Converter {
/*    public static XmlDto hierarchyToXmlDto(String hierarchy) {
        XmlDto output = new XmlDto();
        return output;
    }*/

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

    public static String createHierarchyFromFileContent(String fileContent) {
        return FileValidator.isXML(fileContent) ?
                createXmlDtoFromFileContent(fileContent).getHierarchy() :
                FileValidator.isJSON(fileContent) ?
                        createJsonDtoListFromFileContent(fileContent, true).stream()
                                .map(JsonDto::getHierarchy).collect(Collectors.joining()) : "";
    }

    public static XmlDto createXmlDtoFromHierarchy(String hierarchy) {
        List<XmlDto> listXml = createDtoListFromHierarchy(hierarchy, new XmlDto("", new HashMap<>(), new ArrayList<>(), "", false));
        return listXml.size() == 1 ?
                listXml.get(0) :
                new XmlDto("root", null, listXml, null, false);
    }

    public static List<JsonDto> createJsonDtoListFromHierarchy(String hierarchy) {
        return createDtoListFromHierarchy(hierarchy, new JsonDto("", new HashMap<>(), new ArrayList<>(), null, true, false));
    }


    private static <T extends HierarchyElement> List<T> createDtoListFromHierarchy(String hierarchy, T dtoTypeObject) {
        Pattern elementPattern = Pattern.compile("Element([^:]*?):(.*?)(?=Element)", Pattern.DOTALL);
        Pattern pathPattern = Pattern.compile("path.*?=(.*?)\\n", Pattern.DOTALL);
        Pattern valuePattern = Pattern.compile("value.*?=(.*?)\\n", Pattern.DOTALL);
        Pattern attributesPattern = Pattern.compile("(attributes.*?:.*?\\n)(((.*?)(?=Element))|.*)", Pattern.DOTALL);

        Pattern elementGroupPattern = Pattern.compile("[^\\n]*\\n(path([^=]*?)=([^\\n]*?))\\n(.*?)(?!.*\\3)(.*?)(?=(Element|($)))", Pattern.DOTALL);
        Matcher elementGroupMatcher = elementGroupPattern.matcher(hierarchy);

        String tagName = null, content = null;
        HashMap<String, String> attributes = null;
        List<T> listOutput = new ArrayList<>();
        List<T> children = new ArrayList<>();
        String currentText = "";

        while (elementGroupMatcher.find()) {
            Matcher elementMatcher = elementPattern.matcher(elementGroupMatcher.group());
            String elementGroup = elementGroupMatcher.group();
            if (elementMatcher.find()) {
                String path, value, attributesEntriesString;
                String element = elementMatcher.group(2);
                Matcher pathMatcher = pathPattern.matcher(element);
                if (pathMatcher.find()) {
                    path = pathMatcher.group(1);
                    tagName = path.contains(",") ?
                            path.substring(path.lastIndexOf(",")) :
                            path;
                }
                Matcher valueMatcher = valuePattern.matcher(element);
                if (valueMatcher.find()) {
                    value = valueMatcher.group(1);
                    content = value;
                }
                Matcher attributesMatcher = attributesPattern.matcher(element);
                if (attributesMatcher.find()) {
                    attributesEntriesString = attributesMatcher.group(2);
                    String[] attributesEntries = (String[]) Arrays.stream(attributesEntriesString.split("[=\\n]")).filter(s -> !s.isEmpty()).toArray();
                    HashMap<String, String> hierarchyAttributes = new HashMap<>();
                    for (int i = 0; i < attributesEntries.length; i += 2) {
                        hierarchyAttributes.put(attributesEntries[i], attributesEntries[i + 1]);
                    }
                    attributes = hierarchyAttributes;
                }
                currentText = hierarchy.replaceFirst(element, "");
                elementGroup = elementGroup.replaceFirst(element, "");
            }
            String element, path;
            elementGroupMatcher = elementGroupPattern.matcher(elementGroup);

            while (elementGroupMatcher.find()) {
                elementGroup = elementGroupMatcher.group();
                children.add(createDtoListFromHierarchy(elementGroup, dtoTypeObject).get(0));
            }
            listOutput.add(
                    dtoTypeObject instanceof XmlDto ?
                            (T) new XmlDto(tagName, attributes, (List<XmlDto>) children, content, false) :
                            (T) new JsonDto(tagName, attributes, (List<JsonDto>) children, content, false, false)
            );

        }
        return listOutput;
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


        String betweenTagsString = extractStringBetweenTagsXML(fileContent); // <inner10 attr5='' />\n" +
        //        "        <inner11 attr11=\"value11\">
        List<String> tagTokens = new ArrayList<>(List.of(Arrays.stream(betweenTagsString.split("((=)| )"))
                .filter(s -> !s.isEmpty()).toArray(String[]::new)));
        /*
        while (tagTokens.contains("=")) {
            tagTokens.remove("=");
        }*/
//array 2 value5
        for (int i = 0; i < tagTokens.size(); i++) {
            String s = tagTokens.get(i);
            if (s.startsWith("\"")) {
                tagTokens.set(i, s.substring(1, s.lastIndexOf("\"")));
            } else if (s.startsWith("\'")) {
                tagTokens.set(i, s.substring(1, s.lastIndexOf("\'")));
            }
        }
        String tagName = tagTokens.get(0);
        HashMap<String, String> tagAttributes = new HashMap<>();
        for (int i = 1; i < tagTokens.size(); i += 2) {
            tagAttributes.put(tagTokens.get(i), tagTokens.get(i + 1));
        }
        fileContent = fileContent.substring(fileContent.indexOf('>') + 1);

        List<XmlDto> children = new ArrayList<>();
        Pattern patternChild = Pattern.compile("(?:((<(\\w+)\\s[^>/]*>.*?<(\\/?)\\3)|(?:<(\\w+)>.*?<(\\/?)\\5))\\/?>)|(<\\/.*?>)|(<[^>]*?\\/>)", Pattern.DOTALL);
        Matcher matcherChild = patternChild.matcher(fileContent);
        while (!fileContent.trim().equals("")) {
            String childContent = getXmlElement(fileContent);
            if (childContent.length() > 0) {
//                System.out.println("CHILD CONTENT : " + childContent);
                children.add(createXmlDtoFromFileContent(childContent));
                fileContent = fileContent.replaceFirst(childContent, "");
//                System.out.println("FILE CONTENT : " + fileContent);
            } else {
                break;
            }
        }
/*
        while (matcherChild.find()) {
            numberOfChildren++;
            if (matcherChild.group(3) != null || matcherChild.group(5) != null) {
                content = "";
            }
            children.add(createXmlDtoFromFileContent(matcherChild.group()));
        }*/
        return new XmlDto(tagName, tagAttributes, children, content, isArray);
    }

    private static String getXmlElement(String fileContent) {
        String tempContent = fileContent;

        int counter = 0;
        int indexOfTriangle = tempContent.indexOf('<');
        int indexOfSlash = tempContent.indexOf('/');
        int indexOfEndTriangle = tempContent.indexOf('>');
        boolean previous = false;
        do {
            //System.out.println("TEMP CONTENT :" + tempContent);
            if (indexOfSlash > indexOfEndTriangle) {
                counter++;
                tempContent = tempContent.substring(tempContent.indexOf('>') + 1);
                previous = false;
            } else if (indexOfSlash < indexOfEndTriangle) {
                if (!tempContent.substring(0, indexOfEndTriangle + 1).trim().matches("<.*?/>")) {
                    counter--;
                }
                tempContent = tempContent.substring(tempContent.indexOf('>') + 1);
                previous = true;
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

        //System.out.println("TEMP CONTENT: " + tempContent);
        return fileContent.replaceFirst(tempContent, "").trim();
    }

    private static String extractStringBetweenTagsXML(String xml) {
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
                tagName = getTagName(fileContent);
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
                    content = getValueBetweenDoubleDotsAndComa(fileContent);
                    output.add(new JsonDto(tagName, tagAttributes, children, content, root, false));
                }
                break;
            } else if (currentIndex == indexOfOpeningOfArray) {
                content = Converter.getStringBetweenBraces(fileContent, currentIndex, '[', ']');
                fileContent = fileContent.replaceFirst("\\[" + content.
                        replaceAll("\\{", "\\\\{").replaceAll("\\[", "\\\\[") + ']', "");
                if (!tagName.equals("#")) {
                    //SHOULD BE CHANGED
                    output.add(new JsonDto(tagName, content, false, false, true));
                }
            } else if (currentIndex == indexOfOpeningBrace) {
                boolean wrong = false;
                children = new ArrayList<>();
                tagAttributes = new HashMap<>();
                content = null;
                if (emptyTagName) {
                    fileContent = fileContent.replaceFirst("\\{" +
                            getStringBetweenBraces(fileContent, currentIndex, '{', '}').
                                    replaceAll("\\{", "\\\\{").replaceAll("\\[", "\\\\[") + "}", "");
                } else {
                    String childrenContent = getStringBetweenBraces(fileContent, currentIndex, '{', '}');
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
                                            tagAttributes.put(childTagName.substring(1), (String) child.getContent());
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
                    fileContent = fileContent.replaceFirst(getValueBetweenDoubleDotsAndComa(fileContent), "");//currentIndex+1
                } else {
                    content = getValueBetweenDoubleDotsAndComa(fileContent).trim();
                    fileContent = fileContent.replaceFirst(content, "");
                    if (tagName.startsWith("@") && content.equals("null")) {
                        content = "";
                    }
                    output.add(new JsonDto(tagName, tagAttributes, children, content, root, false));
                }
            } else if (currentIndex == indexOfOpeningQuote) {
                children = new ArrayList<>();
                tagAttributes = new HashMap<>();
                if (emptyTagName) {
                    fileContent = fileContent.substring(fileContent.indexOf('"', currentIndex + 1) + 1);
                } else {
                    content = getStringBetweenBraces(fileContent, currentIndex, '"', '"');
                    output.add(new JsonDto(tagName, tagAttributes, new ArrayList<>(), content, root, false));
                    fileContent = fileContent.replaceFirst("\"" + content + "\"", "");

                }
            }

            //wrong erasure if empty tag, shouldnot erase double dot before this, only between double dot and coma
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

    public static String getStringBetweenBraces(String fileContent, int indexOfOpeningBrace, char openingBrace, char closingBrace) {
        int counter = 1;
        int currentIndex = indexOfOpeningBrace;
        while (counter != 0) {
            currentIndex++;
            if (fileContent.charAt(currentIndex) == closingBrace) {
                counter--;
            } else if (fileContent.charAt(currentIndex) == openingBrace) {
                counter++;
            }
        }
        return fileContent.substring(indexOfOpeningBrace + 1, currentIndex);
    }
}