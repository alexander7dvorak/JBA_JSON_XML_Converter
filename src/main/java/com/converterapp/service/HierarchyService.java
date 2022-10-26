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

public class HierarchyService {
    public static String createHierarchyFromFileContent(String fileContent) {
        return FileValidator.isXML(fileContent) ?
                XmlDtoReader.readXmlDto(new StringBuilder(fileContent)).getHierarchy() :
                FileValidator.isJSON(fileContent) ?
                        JsonDtoReader.readJsonDtoList(new StringBuilder(fileContent), true).stream()
                                .map(JsonDto::getHierarchy).collect(Collectors.joining()) : "";
    }

    public static XmlDto createXmlDtoFromHierarchy(String hierarchy) {
        List<XmlDto> listXml = createDtoListFromHierarchy(hierarchy, XmlDto.generateNonElementXmlDto("", new HashMap<>(), "", false, new ArrayList<>()));
        return listXml.size() == 1 ?
                listXml.get(0) :
                XmlDto.generateNonElementXmlDto("root", null, null, false, listXml);
    }

    public static List<JsonDto> createJsonDtoListFromHierarchy(String hierarchy) {
        return createDtoListFromHierarchy(hierarchy, JsonDto.generateJsonDto("", new HashMap<>(), null, true, false, new ArrayList<>()));
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
                            (T) XmlDto.generateNonElementXmlDto(tagName, attributes, content, false, (List<XmlDto>) children) :
                            (T) JsonDto.generateJsonDto(tagName, attributes, content, false, false, (List<JsonDto>) children)
            );

        }
        return listOutput;
    }
}
