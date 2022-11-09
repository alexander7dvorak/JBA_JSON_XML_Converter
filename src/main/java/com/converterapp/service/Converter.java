package com.converterapp.service;

import com.converterapp.model.JsonDto;
import com.converterapp.model.XmlDto;

import java.util.List;
import java.util.stream.Collectors;

public class Converter {
    public static String xmlToJSON(String xmlString) {
        StringBuilder fileContentSB = new StringBuilder(xmlString);
        XmlDto xmlObject = XmlDtoReader.readXmlDto(fileContentSB);
        return JsonDto.generateRootJsonDto(xmlObject).toString();
    }

    public static String jsonToXML(String jsonString) {
        List<JsonDto> jsonObjectsList = JsonDtoReader.readJsonDtoList(new StringBuilder(jsonString), true);
        String jsonObjectsListString = jsonObjectsList.stream().map(
                jsonDto -> XmlDto.generateXmlDto(jsonDto).toString()
        ).collect(Collectors.joining());
        return jsonObjectsList.size() > 1 ?
                "<root>%s</root>".formatted(jsonObjectsListString) :
                jsonObjectsListString;
    }
}