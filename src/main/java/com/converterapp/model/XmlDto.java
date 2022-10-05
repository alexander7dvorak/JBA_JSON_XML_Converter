package com.converterapp.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XmlDto extends HierarchyElement {
    private List<XmlDto> children = new ArrayList<>();
    private final boolean isArray;
    private boolean isElement;

    public XmlDto(String tagName, List<XmlDto> arrayElements, final boolean isElement, final boolean isArray) {
        super(isElement ? null : tagName, new HashMap<>(), null);
        this.isArray = isArray;
        this.isElement = isElement;
        this.children.addAll(arrayElements);
    }

    public XmlDto(String tagName, HashMap<String, String> attributes, List<XmlDto> children, String content, final boolean isArray) {
        super(tagName, attributes, content);
        if (children != null) {
            this.children.addAll(children);
        }
        this.isArray = isArray;
        isElement = false;
    }

    public XmlDto(JsonDto json) {
        super(json.isElement() ? "element" : json.getTagName(), json.getAttributes(), json.getContent());
        if (json.getChildren() != null) {
            this.children = toXmlDtoList(json.getChildren());
            this.isArray = json.isArray();
            this.isElement = json.isElement();
        } else {
            this.children = new ArrayList<>();
            this.isArray = false;
            this.isElement = false;
        }
    }

    public boolean isArray() {
        return this.isArray;
    }

    public boolean isElement() {
        return this.isElement;
    }

    private List<XmlDto> toXmlDtoList(List<JsonDto> jsonDtoList) {
        List<XmlDto> xmlDtoList = new ArrayList<>();
        if (jsonDtoList != null) {
            for (JsonDto child : jsonDtoList) {
                xmlDtoList.add(new XmlDto(child));
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
        if (isArray) {
            if (super.getTagName() != null) {
                outputSB.append("<");
                outputSB.append(super.getTagName());
                outputSB.append(">");
            }
            for (XmlDto child : children) {
                outputSB.append(child);
            }
            if (super.getTagName() != null) {
                outputSB.append("</");
                outputSB.append(super.getTagName());
                outputSB.append(">");
            }
        } else {
            outputSB.append("<");
            outputSB.append(super.getTagName());
            if (super.getAttributes().size() > 0) {
                for (Map.Entry<String, String> entry : super.getAttributes().entrySet()) {
                    outputSB.append(" %s = \"%s\"".formatted(entry.getKey(), entry.getValue()));
                }
            }
            if (children.size() > 0 || (super.getContent() != null && !super.getContent().equals("null"))) {
                outputSB.append(">");
                for (XmlDto child : children) {

                    outputSB.append(child);
                }
                if (super.getContent() != null && !super.getContent().equals("null")) {
                    outputSB.append(super.getContent());
                }
                outputSB.append("</%s>".formatted(super.getTagName()));
            } else {
                outputSB.append("/>");
            }
        }
        return outputSB.toString();
    }

    public String getHierarchy() {
        StringBuilder outputSB = new StringBuilder();
        appendElementString(outputSB, this, this.getTagName());
        return outputSB.toString();
    }

    private void appendElementString(StringBuilder sb, XmlDto xml, String path) {
        if (path.length() > 0) {
            sb.append("Element:\n");
            sb.append("path = %s\n".formatted(path));
            sb.append("value = %s\n".formatted(
                            xml.getContent() == null || ((String) xml.getContent()).trim().length() == 0 ?
                                    null :
                                    "\"%s\"".formatted(xml.getContent())
                    )
            );
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
        List<XmlDto> children = xml.getChildren();
        for (XmlDto child : children) {
            appendElementString(sb, child, path + ", " + child.getTagName());
        }
    }

    public void setOnlyContent(boolean b) {
        this.isElement = b;
    }
}