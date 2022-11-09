package com.converterapp.service;

import com.converterapp.model.JsonDto;

import java.util.stream.Collectors;

public class HierarchyService {
    public static String createHierarchyFromFileContent(String fileContent) {
        return FileValidator.isXML(fileContent) ?
                XmlDtoReader.readXmlDto(new StringBuilder(fileContent)).getHierarchy() :
                FileValidator.isJSON(fileContent) ?
                        JsonDtoReader.readJsonDtoList(new StringBuilder(fileContent), true).stream()
                                .map(JsonDto::getHierarchy).collect(Collectors.joining()) : "";
    }
}
