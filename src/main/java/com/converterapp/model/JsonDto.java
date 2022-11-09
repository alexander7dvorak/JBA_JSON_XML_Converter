package com.converterapp.model;

import com.converterapp.service.JsonDtoReader;
import com.converterapp.service.StringService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonDto extends HierarchyElement {
    private final List<JsonDto> children;
    private final boolean root;

    private JsonDto(String tagName, HashMap<String, String> attributes, String content, List<JsonDto> children, boolean root, boolean onlyContent, boolean isArray) {
        super(tagName, attributes, content, isArray, onlyContent);
        this.root = root;
        this.children = children;
    }

    public static JsonDto generateNotRootJsonDto(XmlDto xml) {
        List<JsonDto> childrenJSON = new ArrayList<>();
        boolean isArray;
        if (xml.isArray()) {
            List<XmlDto> childrenXML = xml.getChildren();
            for (XmlDto child : childrenXML) {
                childrenJSON.add(generateNotRootJsonDto(child));
            }
            isArray = true;
        } else {
            List<XmlDto> tempChildren = xml.getChildren();
            List<XmlDto> childrenXML = tempChildren.size() > 0 ?
                    formArraysFromSimilarChildren(tempChildren) :
                    new ArrayList<>();
            for (XmlDto child : childrenXML) {
                childrenJSON.add(generateNotRootJsonDto(child));
            }
            isArray = childrenJSON.size() != 0 && childrenJSON.size() == childrenXML.size();
            for (JsonDto child : childrenJSON) {
                if (!child.getTagName().equals("element")) {
                    isArray = false;
                    break;
                }
            }
        }
        return new JsonDto(xml.getTagName(), xml.getAttributes(), xml.getContent(), childrenJSON, false, xml.isElement(), isArray);
    }

    public static JsonDto generateRootJsonDto(XmlDto xml) {
        List<JsonDto> childrenJSON = new ArrayList<>();
        List<XmlDto> childrenXML = formArraysFromSimilarChildren(xml.getChildren());
        boolean tempIsArray = true;
        for (XmlDto child : childrenXML) {
            if (!child.isElement()) {
                tempIsArray = false;
                break;
            }
        }
        for (XmlDto child : childrenXML) {
            childrenJSON.add(generateNotRootJsonDto(child));
        }
        return new JsonDto(xml.getTagName(), xml.getAttributes(),
                xml.getContent() == null ? null : xml.getContent().trim(),
                childrenJSON, true, xml.isElement(),
                childrenXML.size() != 0 && tempIsArray && childrenXML.size() == xml.getChildren().size()
        );
    }

    public static JsonDto generateJsonDto(String tagName, String fileContent,
                                          final boolean root, final boolean onlyContent, final boolean isArray) {
        List<JsonDto> childrenJSON = new ArrayList<>();
        if (isArray) {
            if (fileContent != null && !fileContent.isBlank()) {
                childrenJSON.addAll(createArrayChildrenList(fileContent));
            }
        }
        return new JsonDto(tagName == null ? "element" : tagName,
                new HashMap<>(),
                onlyContent ? fileContent : null,
                childrenJSON, root,
                onlyContent, isArray
        );
    }

    public static JsonDto generateJsonDto(String tagName, HashMap<String, String> attributes, String content,
                                          final boolean root, final boolean onlyContent, List<JsonDto> children) {
        return new JsonDto(onlyContent || tagName == null ? "element" : tagName,
                attributes,
                content,
                children, root,
                onlyContent, false
        );
    }

    private static List<JsonDto> createArrayChildrenList(String fileContent) {
        String content;
        List<JsonDto> output = new ArrayList<>();
        int indexOfComma;
        int indexOfOpeningBrace;
        int indexOfOpeningOfArray;
        int indexOfOpeningQuote;
        int currentIndex;
        do {
            indexOfComma = fileContent.indexOf(',') == -1 ? Integer.MAX_VALUE : fileContent.indexOf(',');
            indexOfOpeningBrace = fileContent.indexOf('{') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('{');
            indexOfOpeningOfArray = fileContent.indexOf('[') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('[');
            indexOfOpeningQuote = fileContent.indexOf('"') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('"');
            currentIndex = Math.min(indexOfOpeningBrace, Math.min(indexOfComma, Math.min(indexOfOpeningQuote, indexOfOpeningOfArray)));
            if (currentIndex == Integer.MAX_VALUE) {
                output.add(generateJsonDto(null, fileContent.trim(), false, true, false));
                break;
            } else if (currentIndex == indexOfOpeningOfArray) {
                content = StringService.getStringBetweenBraces(fileContent, currentIndex, '[', ']');
                fileContent = fileContent.replaceFirst("\\[" + content.replaceAll("\\{", "\\\\{").replaceAll("\\[", "\\\\[") + ']', "");
                if (content.matches("\\s*")) {
                    output.add(generateJsonDto(null, null, false, true, true));
                } else {
                    output.add(generateJsonDto(null, content, false, false, true));
                }
            } else if (currentIndex == indexOfOpeningBrace) {
                String childrenContent = StringService.getStringBetweenBraces(fileContent, currentIndex, '{', '}');
                if (childrenContent.matches("\\s*")) {
                    output.add(generateJsonDto(null, new HashMap<>(), "", true, false, null));
                } else {
                    content = null;
                    List<JsonDto> childrenInsideBraces = new ArrayList<>();
                    List<JsonDto> childrenDto = JsonDtoReader.readJsonDtoList(new StringBuilder(childrenContent), false);
                    List<String> childrenDtoTagNames = childrenDto.stream().map(JsonDto::getTagName).collect(Collectors.toCollection(ArrayList::new));
                    List<JsonDto> childrenToBeRemoved = new ArrayList<>();
                    HashMap<String, String> tagAttributes = new HashMap<>();
                    boolean wrong = false;
                    int hashtagTagNames = 0;
                    for (JsonDto child : childrenDto) {
                        if (child.getTagName() != null) {
                            String childTagName = child.getTagName();
                            if (childTagName.startsWith("#")) {
                                if (childrenDtoTagNames.contains(childTagName.substring(1))) {
                                    childrenToBeRemoved.add(child);
                                }
                                hashtagTagNames++;
                                if (!childTagName.equals("#element")) {
                                    wrong = true;
                                }
                                if (child.getChildren().size() > 0) {
                                    childrenInsideBraces.addAll(child.getChildren());
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
                                    content = child.getContent() == null ? "" : child.getContent();
                                }
                            } else if (childTagName.startsWith("@") && childTagName.length() > 1 && child.getChildren().size() == 0) {
                                if (wrong) {
                                    childTagName = childTagName.substring(1);
                                    child.setTagName(childTagName);
                                } else {
                                    tagAttributes.put(childTagName.substring(1), child.getContent().equals("null") ? "" : child.getContent());
                                }
                            }
                            if (!childTagName.startsWith("#") && !childTagName.startsWith("@")) {
                                childrenInsideBraces.add(child);
                            }
                        }
                    }
                    output.add(generateJsonDto("element", tagAttributes, content, false, true, childrenInsideBraces));

                }
                fileContent = fileContent.replaceFirst("\\{" + childrenContent.replaceAll("\\{", "\\\\{").replaceAll("\\[", "\\\\[") + '}', "");

            } else if (currentIndex == indexOfComma) {
                output.add(generateJsonDto(null, fileContent.substring(0, fileContent.indexOf(',')).trim(), false, true, false));
                fileContent = fileContent.substring(indexOfComma);
            } else if (currentIndex == indexOfOpeningQuote) {
                String tagName = StringService.getTagName(fileContent);
                fileContent = fileContent.replaceFirst('"' + tagName + '"', "");
                output.add(generateJsonDto(null, tagName, false, true, false));
            }
            indexOfOpeningBrace = fileContent.indexOf('{') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('{');
            indexOfOpeningOfArray = fileContent.indexOf('[') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('[');
            indexOfOpeningQuote = fileContent.indexOf('"') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('"');
            indexOfComma = fileContent.indexOf(',') == -1 ? Integer.MAX_VALUE : fileContent.indexOf(',');
            if (indexOfComma < indexOfOpeningBrace && indexOfComma < indexOfOpeningQuote && indexOfComma < indexOfOpeningOfArray) {
                fileContent = fileContent.replaceFirst(",", "");
            } else {
                break;
            }
        } while (currentIndex != -1);
        return output;
    }

    private static List<XmlDto> formArraysFromSimilarChildren(List<XmlDto> childrenList) {
        if (childrenList.size() == 0) {
            return new ArrayList<>();
        }
        List<XmlDto> xmlListWithArrays = new ArrayList<>();
        List<XmlDto> elementsInArray = new ArrayList<>();
        elementsInArray.add(childrenList.get(0));
        boolean notArray = false;
        for (int i = 1; i < childrenList.size(); i++) {
            XmlDto currentChild = childrenList.get(i);
            XmlDto previousChild = childrenList.get(i - 1);
            if (currentChild.getTagName().equals(previousChild.getTagName())) {
                elementsInArray.add(currentChild);
            } else {
                notArray = true;
                if (elementsInArray.size() == 1) {
                    xmlListWithArrays.add(elementsInArray.get(0));
                } else {
                    for (XmlDto element : elementsInArray) {
                        element.setTagName("element");
                        element.setOnlyContent(true);
                        if (element.getContent() == null) {
                            element.setContent("null");
                        }
                    }
                    xmlListWithArrays.add(XmlDto.generateXmlDtoArray("element", elementsInArray, true));
                }
                elementsInArray = new ArrayList<>();
                elementsInArray.add(currentChild);
            }
        }
        if (elementsInArray.size() == 1) {
            xmlListWithArrays.add(elementsInArray.get(0));
        } else {
            for (XmlDto element : elementsInArray) {
                element.setTagName("element");
                element.setOnlyContent(true);
                if (element.getContent() == null) {
                    element.setContent("null");
                }
            }
            if (notArray) {
                xmlListWithArrays.add(XmlDto.generateXmlDtoArray("element", elementsInArray, false));
            } else {
                xmlListWithArrays.addAll(elementsInArray);
            }
        }
        return xmlListWithArrays;
    }

    public List<JsonDto> getChildren() {
        return children;
    }


    @Override
    public String toString() {
        StringBuilder outputSB = new StringBuilder();
        if (root) {
            outputSB.append("{%s".formatted("\n"));
        }
        if (isArray()) {
            arrayToString(outputSB);
        } else if (isElement()) {
            elementToString(outputSB);
        } else {
            complexToString(outputSB);
        }
        if (root) {
            outputSB.append("%s}".formatted("\n"));
        }
        return outputSB.toString();
    }

    private void arrayToString(StringBuilder outputSB) {
        int counter = 0;
        int numberOfAttributes = getAttributes().size();
        if (super.getAttributes().size() > 0) {
            outputSB.append("\"%s\" : {".formatted(getTagName()));
            for (Map.Entry<String, String> entry : getAttributes().entrySet()) {
                counter++;
                outputSB.append("\n\"@%s\":\"%s\"".formatted(entry.getKey(), entry.getValue()));
                if (counter != numberOfAttributes) {
                    outputSB.append(",");
                }
            }
            counter = 0;
            outputSB.append(",\n\"#%s\":".formatted(getTagName()));
        } else if (getTagName() != null && !getTagName().equals("element")) {
            outputSB.append("\"");
            outputSB.append(getTagName());
            outputSB.append("\":");
        }
        outputSB.append("[%s".formatted("\n"));

        for (JsonDto child : children) {
            counter++;
            if (child.getAttributes().size() == 0 && child.getContent() != null && !child.getContent().equals("null") && child.getChildren().size() == 0) {
                outputSB.append("\"");
            }
            outputSB.append(child);
            if (child.getAttributes().size() == 0 && child.getContent() != null && !child.getContent().equals("null") && child.getChildren().size() == 0) {
                outputSB.append("\"");
            }
            if (counter != children.size()) {
                outputSB.append(",%s".formatted("\n"));
            }
        }
        outputSB.append("%s]".formatted("\n"));
        if (super.getAttributes().size() > 0) {
            outputSB.append("%s}".formatted("\n"));
        }
    }

    private void elementToString(StringBuilder outputSB) {
        int counter = 0;
        int numberOfAttributes = getAttributes().size();
        if (super.getAttributes().size() > 0) {
            outputSB.append("{");
            for (Map.Entry<String, String> entry : getAttributes().entrySet()) {
                counter++;
                outputSB.append("\n\"@%s\":\"%s\"".formatted(entry.getKey(), entry.getValue()));
                if (counter != numberOfAttributes) {
                    outputSB.append(",");
                }
            }
            outputSB.append(",\n\"#element\":%s".formatted(
                    getContent() == null ?
                            null :
                            "\"" + getContent() + "\"")
            );
            outputSB.append("\n}\n");
        } else {
            if (children.size() > 0) {
                outputSB.append("{%s".formatted("\n"));
            }
            for (JsonDto child : children) {
                counter++;
                outputSB.append(child);
                if (counter != children.size()) {
                    outputSB.append(",%s".formatted("\n"));
                }
            }
            if (getContent() != null) {
                outputSB.append(getContent());
            }
            if (children.size() > 0) {
                outputSB.append("%s}".formatted("\n"));
            }
        }
    }

    private void complexToString(StringBuilder outputSB) {
        int counter = 0;
        outputSB.append("\"");
        outputSB.append(super.getTagName());
        if (super.getAttributes().size() > 0 || children.size() > 0) {
            outputSB.append("\":{%s".formatted("\n"));
            for (Map.Entry<String, String> entry : super.getAttributes().entrySet()) {
                outputSB.append("\"@%s\":%s".formatted(entry.getKey(), '"' + entry.getValue() + '"'));
                counter++;
                if (children.size() > 0 || counter != super.getAttributes().size() + 1) {
                    outputSB.append(",%s".formatted("\n"));
                }
            }
            if (children.size() > 0 && super.getAttributes().size() > 0) {
                outputSB.append("\"#%s\": {".formatted(super.getTagName()));
            }
        }
        if (children.size() == 0) {
            if (super.getAttributes().size() == 0) {
                outputSB.append("\":%s".formatted(getContent() == null ? null : '"' + getContent() + '"'));
            } else {
                outputSB.append("\"#%s\":%s".formatted(super.getTagName(), getContent() == null ? null : '"' + getContent() + '"'));
            }
        }

        counter = 0;
        for (JsonDto child : children) {
            counter++;
            outputSB.append(child);
            if (counter != children.size()) {
                outputSB.append(",%s".formatted("\n"));
            }
        }


        if (super.getAttributes().size() > 0) {
            outputSB.append("%s}".formatted("\n"));
        }
        if (children.size() > 0) {
            outputSB.append("%s}".formatted("\n"));
        }
    }

    public String getHierarchy() {
        return getElementString(this, this.getTagName());
    }

    private String getElementString(JsonDto json, String path) {
        return appendElementString(new StringBuilder(), json, path).toString();
    }

    private StringBuilder appendElementString(StringBuilder sb, JsonDto json, String path) {
        appendElementPath(sb, path);
        appendElementContent(json, sb);
        appendElementAttributes(json, sb);
        appendElementChildren(json.getChildren(), path, sb);
        return sb;
    }

    private void appendElementPath(StringBuilder sb, String path) {
        sb.append("Element:\n");
        sb.append("path = %s\n".formatted(path));
    }

    private void appendElementContent(JsonDto json, StringBuilder sb) {
        if (json.getContent() != null) {
            sb.append("value = %s\n".formatted(
                            json.getContent().equals("null") ?
                                    null :
                                    "\"%s\"".formatted(json.getContent())
                    )
            );
        }
    }

    private void appendElementAttributes(JsonDto json, StringBuilder sb) {
        if (json.getAttributes().size() > 0) {
            sb.append("attributes:\n");
            for (Map.Entry<String, String> attributeEntry : json.getAttributes().entrySet()) {
                sb.append(
                        "%s = %s\n".formatted(attributeEntry.getKey(),
                                "\"%s\"".formatted(attributeEntry.getValue())
                        )
                );
            }
        }
    }

    private void appendElementChildren(List<JsonDto> children, String path, StringBuilder sb) {
        for (JsonDto child : children) {
            if (child.getTagName().length() != 0) {
                sb.append(getElementString(child, path + ", " + child.getTagName())).append("\n");
            }
        }
    }
}