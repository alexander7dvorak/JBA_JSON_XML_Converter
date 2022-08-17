package com.converterapp.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmlDto {
    private final String tagName;
    private final HashMap<String, String> attributes;
    private List<XmlDto> children = new ArrayList<>();
    private final String content;

    public XmlDto(String tagName, HashMap<String, String> attributes, List<XmlDto> children, String content) {
        this.tagName = tagName;
        this.attributes = attributes;
        if (children != null) {
            this.children.addAll(children);
        }
        this.content = content == null || content.length() == 0 ? null : content;
    }

    public XmlDto(JsonDto json) {
        this.tagName = json.getTagName();
        this.attributes = json.getAttributes();
        List<XmlDto> childrenXML = new ArrayList<>();
        List<JsonDto> childrenJSON = json.getChildren();
        for (JsonDto child : childrenJSON) {
            childrenXML.add(new XmlDto(child));
        }
        this.children = childrenXML;
        this.content = json.getContent();
    }

    public String getTagName() {
        return tagName;
    }

    public HashMap<String, String> getAttributes() {
        return attributes;
    }

    public List<XmlDto> getChildren() {
        return children;
    }

    public void setChildren(List<XmlDto> children) {
        this.children = children;
    }

    public String getContent() {
        return content;
    }

    public String toString() {
        StringBuilder outputSB = new StringBuilder();
        outputSB.append("<");
        outputSB.append(tagName);
        if (attributes.size() > 0) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                outputSB.append(" %s = \"%s\"".formatted(entry.getKey(), entry.getValue()));
            }
        }
        if (content != null && content.length() > 0) {
            outputSB.append(">");
            for (XmlDto child : children) {
                outputSB.append(child);
            }
            outputSB.append(content);
            outputSB.append("</%s>".formatted(tagName));
        } else {
            outputSB.append("/>");
        }
        return outputSB.toString();
    }
}