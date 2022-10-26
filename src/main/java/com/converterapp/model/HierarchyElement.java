package com.converterapp.model;

import java.util.HashMap;

public abstract class HierarchyElement {
    private String tagName;
    final private HashMap<String, String> attributes;
    private String content;
    private boolean isArray;
    private boolean isElement;

    HierarchyElement(String tagName, HashMap<String, String> attributes, String content, boolean isArray, boolean isElement) {
        this.tagName = tagName;
        this.attributes = attributes;
        this.content = content;
        this.isArray = isArray;
        this.isElement = isElement;
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

    public String getContent() {
        return content;
    }

    public boolean isArray() {
        return this.isArray;
    }

    public void setArray(boolean b) {
        this.isArray = b;
    }

    public boolean isElement() {
        return this.isElement;
    }

    public boolean isOnlyContent() {
        return this.isElement;
    }

    public void setOnlyContent(boolean b) {
        this.isElement = b;
    }


    public void setElement(boolean isElement) {
        this.isElement = isElement;
    }

    public void setContent(String s) {
        this.content = s;
    }
}