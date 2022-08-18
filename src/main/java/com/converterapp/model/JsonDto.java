package com.converterapp.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonDto {
    private final String tagName;
    private final HashMap<String, String> attributes;
    private final List<JsonDto> children;
    private final String content;

    public JsonDto(String tagName, HashMap<String, String> attributes, List<JsonDto> children, String content) {
        this.tagName = tagName;
        this.attributes = attributes;
        this.children = children;
        this.content = content == null || content.length() == 0 ? null : content;
    }

    public JsonDto(XmlDto xml) {
        this.tagName = xml.getTagName();
        this.attributes = xml.getAttributes();
        List<JsonDto> childrenJSON = new ArrayList<>();
        List<XmlDto> childrenXML = xml.getChildren();
        for (XmlDto child : childrenXML) {
            childrenJSON.add(new JsonDto(child));
        }
        this.children = childrenJSON;
        this.content = xml.getContent();
    }

    public String getTagName() {
        return tagName;
    }

    public HashMap<String, String> getAttributes() {
        return attributes;
    }

    public List<JsonDto> getChildren() {
        return children;
    }

    public String getContent() {
        return content;
    }

    @Override
    public String toString() {
        StringBuilder outputSB = new StringBuilder();
        outputSB.append("{\"");
        outputSB.append(tagName);
        if (attributes.size() > 0) {
            outputSB.append("\":{");
            int counter = 0;
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                outputSB.append("\"@%s\":\"%s\"".formatted(entry.getKey(), entry.getValue()));
                counter++;
                if (counter != attributes.size() + 1) {
                    outputSB.append(",");
                }
            }
        }
        if (attributes.size() == 0) {
            outputSB.append("\":%s".formatted(content == null || content.length() == 0 ? null : '"' + content + '"'));
        } else {
            outputSB.append("\"#%s\":%s".formatted(tagName, content == null || content.length() == 0 ? null : '"' + content + '"'));
        }
        for (JsonDto child : children) {
            outputSB.append(child);
        }
        if (attributes.size() > 0) {
            outputSB.append("}");
        }
        outputSB.append("}");
        return outputSB.toString();
    }
}
