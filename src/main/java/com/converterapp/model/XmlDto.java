package com.converterapp.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmlDto extends HierarchyElement {
    private final List<XmlDto> children = new ArrayList<>();

    private XmlDto(String tagName, HashMap<String, String> attributes, String content, boolean isArray, boolean isElement, List<XmlDto> children) {
        super(tagName, attributes, content, isArray, isElement);
        this.children.addAll(children);
    }

    public static XmlDto generateXmlDto(JsonDto json) {
        return new XmlDto(json.isElement() ? "element" : json.getTagName(),
                json.getAttributes(),
                json.getContent(), json.getChildren() != null && json.isArray(),
                json.getChildren() != null && json.isElement(),
                json.getChildren() != null ? toXmlDtoList(json.getChildren()) : new ArrayList<>());
    }

    public static XmlDto generateXmlDtoArray(String tagName, List<XmlDto> arrayElements, final boolean isElement) {
        return new XmlDto(isElement ? null : tagName, new HashMap<>(), null, true, isElement, arrayElements);
    }

    public static XmlDto generateNonElementXmlDto(String tagName, HashMap<String, String> attributes, String content, final boolean isArray, List<XmlDto> children) {
        return new XmlDto(tagName, attributes, content, isArray, false, children);
    }

    private static List<XmlDto> toXmlDtoList(List<JsonDto> jsonDtoList) {
        List<XmlDto> xmlDtoList = new ArrayList<>();
        if (jsonDtoList != null) {
            for (JsonDto child : jsonDtoList) {
                xmlDtoList.add(generateXmlDto(child));
            }
        }
        return xmlDtoList;
    }

    public List<XmlDto> getChildren() {
        return children;
    }

    @Override
    public String toString() {
        StringBuilder outputSB = new StringBuilder();
        if (isArray()) {
            appendArrayToOutputString(outputSB);
        } else {
            appendXmlToOutputString(outputSB);
        }
        return outputSB.toString();
    }

    private void appendArrayToOutputString(StringBuilder outputSB) {
        if (super.getTagName() != null) {
            appendSimpleOpeningTag(outputSB);
            appendChildren(outputSB);
            appendSimpleClosingTag(outputSB);
        }
    }

    private void appendSimpleOpeningTag(StringBuilder outputSB) {
        outputSB.append("<");
        outputSB.append(super.getTagName());
        outputSB.append(">");
    }

    private void appendSimpleClosingTag(StringBuilder outputSB) {
        outputSB.append("</");
        outputSB.append(super.getTagName());
        outputSB.append(">");
    }

    private void appendChildren(StringBuilder outputSB) {
        for (XmlDto child : children) {
            outputSB.append(child);
        }
    }

    private void appendXmlToOutputString(StringBuilder outputSB) {
        outputSB.append("<");
        outputSB.append(super.getTagName());
        appendAttributes(outputSB);
        appendBodyAndCloseTag(outputSB);
    }

    private void appendAttributes(StringBuilder outputSB) {
        if (super.getAttributes().size() > 0) {
            for (Map.Entry<String, String> entry : super.getAttributes().entrySet()) {
                outputSB.append(" %s = \"%s\"".formatted(entry.getKey(), entry.getValue()));
            }
        }
    }

    private void appendBodyAndCloseTag(StringBuilder outputSB) {
        if (children.size() > 0 || (super.getContent() != null && !super.getContent().equals("null"))) {
            outputSB.append(">");
            appendBody(outputSB);
            outputSB.append("</%s>".formatted(super.getTagName()));
        } else {
            outputSB.append("/>");
        }
    }

    private void appendBody(StringBuilder outputSB) {
        appendChildren(outputSB);
        appendContent(outputSB);
    }

    private void appendContent(StringBuilder outputSB) {
        if (super.getContent() != null && !super.getContent().equals("null")) {
            outputSB.append(super.getContent());
        }
    }

    public String getHierarchy() {
        StringBuilder outputSB = new StringBuilder();
        appendElementString(outputSB, this, this.getTagName());
        return outputSB.toString();
    }

    private void appendElementString(StringBuilder sb, XmlDto xml, String path) {
        if (path.length() > 0) {
            appendElementPath(sb, path);
            appendElementContent(sb, xml);
            appendElementAttributes(sb, xml);
            appendElementChildren(sb, xml, path);
        }
    }

    private void appendElementChildren(StringBuilder sb, XmlDto xml, String path) {
        List<XmlDto> children = xml.getChildren();
        for (XmlDto child : children) {
            appendElementString(sb, child, path + ", " + child.getTagName());
        }
    }

    private void appendElementPath(StringBuilder sb, String path) {
        sb.append("Element:\n");
        sb.append("path = %s\n".formatted(path));
    }

    private void appendElementContent(StringBuilder sb, XmlDto xml) {
        sb.append("value = %s\n".formatted(
                        xml.getContent() == null || xml.getContent().trim().length() == 0 ?
                                null :
                                "\"%s\"".formatted(xml.getContent())
                )
        );
    }

    private void appendElementAttributes(StringBuilder sb, XmlDto xml) {
        if (xml.getAttributes().size() > 0) {
            sb.append("attributes:\n");
            for (Map.Entry<String, String> attributeEntry : xml.getAttributes().entrySet()) {
                sb.append("%s = %s\n".formatted(attributeEntry.getKey(),
                                attributeEntry.getValue() == null ?
                                        null :
                                        "\"%s\"".formatted(attributeEntry.getValue())
                        )
                );
            }
        }
    }
}