package com.converterapp.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JsonArrayDto {
    List<Object> arrayList = new ArrayList<>();

    JsonArrayDto(String fileContent) {

    }

    JsonArrayDto(Object[] array) {
        this.arrayList.addAll(List.of(array));
    }

    JsonArrayDto(List<Object> arrayList) {
        this.arrayList.addAll(arrayList);
    }

    @Override
    public String toString() {
        return Arrays.toString(arrayList.toArray());
    }
}