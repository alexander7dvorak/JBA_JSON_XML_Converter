package com.converterapp.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonDto {
    private String tagName;
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

    public void setTagName(String tagName) {
        this.tagName = tagName;
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
                outputSB.append("\"@%s\":%s".formatted(entry.getKey(), entry.getValue()));
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
                sb.append(appendElementString(child, path + ", " + child.getTagName()));
                sb.append("\n");
            }
        }
        return sb.toString();
    }
}
