package com.converterapp.model;

import java.util.HashMap;

public abstract class HierarchyElement {
    private String tagName;
    private HashMap<String, String> attributes;
    private Object content;

    HierarchyElement(String tagName, HashMap<String, String> attributes, Object content) {
        this.tagName = tagName;
        this.attributes = attributes;
        this.content = content;
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

    public Object getContent() {
        return content;
    }

    public void setContent(Object o) {
        this.content = o;
    }

}