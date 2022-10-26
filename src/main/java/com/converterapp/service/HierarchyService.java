package com.converterapp.service;

import com.converterapp.model.HierarchyElement;
import com.converterapp.model.JsonDto;
import com.converterapp.model.XmlDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class HierarchyUtil {
    public static String createHierarchyFromFileContent(String fileContent) {
        return FileValidator.isXML(fileContent) ?
                Converter.createXmlDtoFromFileContent(fileContent).getHierarchy() :
                FileValidator.isJSON(fileContent) ?
                        Converter.createJsonDtoListFromFileContent(fileContent, true).stream()
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
                elementGroup = elementGroup.replaceFirst(element, "");
            }
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
}
