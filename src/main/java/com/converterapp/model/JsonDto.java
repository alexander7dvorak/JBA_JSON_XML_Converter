package com.converterapp.model;

import com.converterapp.util.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonDto extends HierarchyElement {
    private final List<JsonDto> children;
    private final boolean root;
    private final boolean onlyContent;
    private final boolean isArray;

    public boolean isArray() {
        return this.isArray;
    }

    public boolean isElement() {
        return this.onlyContent;
    }

    private JsonDto(XmlDto xml, final boolean root) {
        super(xml.getTagName(), xml.getAttributes(), xml.getContent());
        this.onlyContent = xml.isElement();
        List<JsonDto> childrenJSON = new ArrayList<>();
        boolean isArray;
        if (xml.isArray()) {
            List<XmlDto> childrenXML = xml.getChildren();
            for (XmlDto child : childrenXML) {
                childrenJSON.add(new JsonDto(child, false));
            }
            isArray = true;
        } else {
            List<XmlDto> tempChildren = xml.getChildren();
            List<XmlDto> childrenXML = tempChildren.size() > 0 ?
                    formArraysFromSimilarChildren(tempChildren) :
                    new ArrayList<>();
            for (XmlDto child : childrenXML) {
                childrenJSON.add(new JsonDto(child, false));
            }
            isArray = childrenJSON.size() != 0 && childrenJSON.size() == childrenXML.size();
            for (JsonDto child : childrenJSON) {
                if (!child.getTagName().equals("element")) {
                    isArray = false;
                    break;
                }
            }
        }
        this.isArray = isArray;
        this.children = childrenJSON;
        this.root = root;
    }

    public JsonDto(XmlDto xml) {
        super(xml.getTagName(), xml.getAttributes(), xml.getContent() == null ? null : ((String) xml.getContent()).trim());
        this.onlyContent = xml.isElement();
        this.children = new ArrayList<>();
        List<JsonDto> childrenJSON = new ArrayList<>();
        List<XmlDto> childrenXML = formArraysFromSimilarChildren(xml.getChildren());
        boolean tempIsArray = true;
        for (XmlDto child : childrenXML) {
            if (!child.isElement()) {
                tempIsArray = false;
                break;
            }
        }
        this.isArray = childrenXML.size() != 0 && tempIsArray && childrenXML.size() == xml.getChildren().size();
        for (XmlDto child : childrenXML) {
            childrenJSON.add(new JsonDto(child, false));
        }
        this.children.addAll(childrenJSON);
        this.root = true;
    }

    public JsonDto(String tagName, String fileContent, final boolean root, final boolean onlyContent, final boolean isArray) {
        super(tagName == null ? "element" : tagName, new HashMap<>(), onlyContent ? fileContent : null);
        this.root = root;
        this.onlyContent = onlyContent;
        this.isArray = isArray;
        this.children = new ArrayList<>();
        if (isArray) {
            if (fileContent != null) {
                this.children.addAll(createArrayChildrenList(fileContent));
            }
        }
    }

    public JsonDto(List<JsonDto> children, final boolean isArray) {
        super(null, new HashMap<>(), "");
        this.isArray = isArray;
        this.onlyContent = true;
        this.root = false;
        this.children = children;
    }

    private List<JsonDto> createArrayChildrenList(String fileContent) {
/*        if (fileContent.matches("\\s*")) {
            return "";
        }*/
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
                output.add(new JsonDto(null, fileContent.trim(), false, true, false));
                break;
            } else if (currentIndex == indexOfOpeningOfArray) {
                content = Converter.getStringBetweenBraces(fileContent, currentIndex, '[', ']');
                fileContent = fileContent.replaceFirst("\\[" + content.replaceAll("\\{", "\\\\{").replaceAll("\\[", "\\\\[") + ']', "");
                if (content.matches("\\s*")) {
                    output.add(new JsonDto(null, null, false, true, true));
                } else {
                    output.add(new JsonDto(null, content, false, false, true));
                }
            } else if (currentIndex == indexOfOpeningBrace) {
                String childrenContent = Converter.getStringBetweenBraces(fileContent, currentIndex, '{', '}');
                if (childrenContent.matches("\\s*")) {
                    output.add(new JsonDto(null, new HashMap<>(), null, "", true, false));
                } else {
                    content = null;
                    List<JsonDto> childrenInsideBraces = new ArrayList<>();
                    List<JsonDto> childrenDto = Converter.createJsonDtoListFromFileContent(childrenContent, false);
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
                                    //feoiafjoeajfiae
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
                                    content = child.getContent() == null ? "" : (String) child.getContent();
                                }
                            } else if (childTagName.startsWith("@") && childTagName.length() > 1 && child.getChildren().size() == 0) {
                                if (wrong) {
                                    childTagName = childTagName.substring(1);
                                    child.setTagName(childTagName);
                                } else {
                                    tagAttributes.put(childTagName.substring(1), (String) child.getContent());
                                }
                            }
                            if (!childTagName.startsWith("#") && !childTagName.startsWith("@")) {
                                childrenInsideBraces.add(child);
                            }
                        }
                    }
                    output.add(new JsonDto("element", tagAttributes, childrenInsideBraces, content, root, true));

                }
                fileContent = fileContent.replaceFirst("\\{" + childrenContent.replaceAll("\\{", "\\\\{").replaceAll("\\[", "\\\\[") + '}', "");

            } else if (currentIndex == indexOfComma) {
                output.add(new JsonDto(null, fileContent.substring(0, fileContent.indexOf(',')).trim(), false, true, false));
                fileContent = fileContent.substring(indexOfComma);
            } else if (currentIndex == indexOfOpeningQuote) {
                String tagName = Converter.getTagName(fileContent);
                fileContent = fileContent.replaceFirst('"' + tagName + '"', "");
                output.add(new JsonDto(null, tagName, false, true, false));
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

    public JsonDto(String tagName, HashMap<String, String> attributes,
                   List<JsonDto> children, String content,
                   final boolean root, final boolean onlyContent) {
        super(onlyContent || tagName == null ? "element" : tagName, attributes, content);
        this.onlyContent = onlyContent;
        this.children = children;
        this.root = root;
        this.isArray = false;
    }

    private List<XmlDto> formArraysFromSimilarChildren(List<XmlDto> childrenList) {
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
                    xmlListWithArrays.add(new XmlDto("element", elementsInArray, false, true));
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
                xmlListWithArrays.add(new XmlDto("element", elementsInArray, false, false));
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
        int counter = 0;
        StringBuilder outputSB = new StringBuilder();
        if (root) {
            outputSB.append("{%s".formatted("\n"));
        }
        int attrN = getAttributes().size();
        if (isArray) {
            if (super.getAttributes().size() > 0) {
                outputSB.append("\"%s\" : {".formatted(getTagName()));
                for (Map.Entry<String, String> entry : getAttributes().entrySet()) {
                    counter++;
                    outputSB.append("\n\"@%s\":\"%s\"".formatted(entry.getKey(), entry.getValue()));
                    if (counter != attrN) {
                        outputSB.append(",");
                    }
                }
                counter = 0;
                outputSB.append(",\n\"#%s\":".formatted(getTagName()));
                //super.getTagName(), getContent() == null ? null : '"' + getContent().toString() + '"'));
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
        } else if (onlyContent) {
            if (super.getAttributes().size() > 0) {
                outputSB.append("{");
                for (Map.Entry<String, String> entry : getAttributes().entrySet()) {
                    counter++;
                    outputSB.append("\n\"@%s\":\"%s\"".formatted(entry.getKey(), entry.getValue()));
                    if (counter != attrN) {
                        outputSB.append(",");
                    }
                }
                outputSB.append(",\n\"#element\":%s".formatted(
                        getContent() == null ?
                                null :
                                "\"" + getContent().toString() + "\"")
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
                    outputSB.append(getContent().toString());
                }
                if (children.size() > 0) {
                    outputSB.append("%s}".formatted("\n"));
                }
            }
        } else {
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
            String content = ((String) super.getContent());
            if (children.size() == 0) {
                if (super.getAttributes().size() == 0) {
                    outputSB.append("\":%s".formatted(content == null ? null : '"' + content + '"'));
                } else {
                    outputSB.append("\"#%s\":%s".formatted(super.getTagName(), content == null ? null : '"' + content + '"'));
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
        if (root) {
            outputSB.append("%s}".formatted("\n"));
        }

        return outputSB.toString();
    }

    public String getHierarchy() {
        return appendElementString(this, this.getTagName());
    }

    private String appendElementString(JsonDto json, String path) {
        List<JsonDto> children = json.getChildren();
        StringBuilder sb = new StringBuilder();
        sb.append("Element:\n");
        sb.append("path = %s\n".formatted(path));
        if (json.getContent() != null) {
            sb.append("value = %s\n".formatted(
                            json.getContent().equals("null") ?
                                    null :
                                    "\"%s\"".formatted(json.getContent())
                    )
            );
        }

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
        for (JsonDto child : children) {
            if (child.getTagName().length() != 0) {
                sb.append(appendElementString(child, path + ", " + child.getTagName()) + "\n");
            }
        }
        return sb.toString();
    }
}