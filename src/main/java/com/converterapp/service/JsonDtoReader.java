package com.converterapp.service;

import com.converterapp.model.JsonDto;
import com.converterapp.model.SymbolsIndexes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class JsonDtoReader {
    public static List<JsonDto> readJsonDtoList(StringBuilder fileContentSB, boolean root) {
        List<JsonDto> output = new ArrayList<>();
        if (root) {
            if (FileValidator.isJSONArray(fileContentSB.toString())) {
                return List.of(JsonDto.generateJsonDto(null, fileContentSB.toString(), true, true, true));
            } else {
                fileContentSB.replace(0, fileContentSB.length(),
                        fileContentSB.substring(
                                fileContentSB.toString().indexOf('{') + 1, fileContentSB.toString().lastIndexOf('}')
                        ));
            }
        }
        processJsonDtoList(fileContentSB, output, root);
        return output;
    }

    private static void processJsonDtoList(StringBuilder fileContentSB, List<JsonDto> output, boolean root) {
        String tagName;
        boolean emptyTagName;
        SymbolsIndexes indexes = new SymbolsIndexes(fileContentSB);
        do {
            tagName = readTagName(fileContentSB, indexes);
            emptyTagName = tagName == null || tagName.length() == 0;
            if (!processJsonDto(fileContentSB, indexes, tagName, output, root, emptyTagName)) {
                break;
            }
            fileContentSB.replace(0, fileContentSB.length(), fileContentSB.toString().replaceFirst(":", ""));
            indexes.refreshIndexes();
            if (!processComma(fileContentSB, indexes)) {
                break;
            }
        } while (indexes.getCurrentIndex() != -1 && indexes.getCurrentIndex() != Integer.MAX_VALUE);
    }

    private static boolean processComma(StringBuilder fileContentSB, SymbolsIndexes indexes) {
        if (isCommaIndexFirst(indexes)) {
            fileContentSB.replace(0, fileContentSB.length(), fileContentSB.toString().replaceFirst(",", ""));
            indexes.refreshIndexes();
            return true;
        } else {
            return false;
        }
    }

    private static boolean isCommaIndexFirst(SymbolsIndexes indexes) {
        return indexes.getIndexOfComma() < indexes.getIndexOfOpeningBrace()
                && indexes.getIndexOfComma() < indexes.getIndexOfOpeningQuote()
                && indexes.getIndexOfComma() < indexes.getIndexOfOpeningOfArray();
    }

    private static boolean processJsonDto(StringBuilder fileContentSB, SymbolsIndexes indexes, String tagName, List<JsonDto> output, boolean root, boolean emptyTagName) {
        if (indexes.getCurrentIndex() == Integer.MAX_VALUE) {
            processJsonDtoAfterDoubleDots(fileContentSB, tagName, root, output);
            return false;
        } else if (indexes.getCurrentIndex() == indexes.getIndexOfOpeningOfArray()) {
            processJsonDtoArray(fileContentSB, indexes, tagName, output);
        } else if (indexes.getCurrentIndex() == indexes.getIndexOfOpeningBrace()) {
            processComplexJsonDto(fileContentSB, indexes, tagName, output, root, emptyTagName);
        } else if (indexes.getCurrentIndex() == indexes.getIndexOfComma()) {
            processSimpleJsonDtoWithoutQuotes(fileContentSB, tagName, output, root, emptyTagName);
        } else if (indexes.getCurrentIndex() == indexes.getIndexOfOpeningQuote()) {
            processSimpleJsonDtoWithQuotes(fileContentSB, indexes, tagName, output, root, emptyTagName);
        }
        return true;
    }

    private static void processSimpleJsonDtoWithQuotes(StringBuilder fileContentSB, SymbolsIndexes indexes, String tagName, List<JsonDto> output, boolean root, boolean emptyTagName) {
        if (emptyTagName) {
            fileContentSB.replace(0, fileContentSB.length(), fileContentSB.substring(fileContentSB.toString().indexOf('"', indexes.getCurrentIndex() + 1) + 1));
        } else {
            HashMap<String, String> tagAttributes = new HashMap<>();
            String content = StringService.getStringBetweenBraces(fileContentSB.toString(), indexes.getCurrentIndex(), '"', '"');
            output.add(JsonDto.generateJsonDto(tagName, tagAttributes, content, root, false, new ArrayList<>()));
            fileContentSB.replace(0, fileContentSB.length(), fileContentSB.toString().replaceFirst("\"" + content + "\"", ""));
        }
    }

    private static void processSimpleJsonDtoWithoutQuotes(StringBuilder fileContentSB, String tagName, List<JsonDto> output, boolean root, boolean emptyTagName) {
        if (emptyTagName) {
            fileContentSB.replace(0, fileContentSB.length(), fileContentSB.toString().replaceFirst(StringService.getValueBetweenDoubleDotsAndComa(fileContentSB.toString()), ""));
        } else {
            List<JsonDto> children = new ArrayList<>();
            HashMap<String, String> tagAttributes = new HashMap<>();
            String content = StringService.getValueBetweenDoubleDotsAndComa(fileContentSB.toString()).trim();
            fileContentSB.replace(0, fileContentSB.length(), fileContentSB.toString().replaceFirst(content, ""));
            if (tagName.startsWith("@") && content.equals("null")) {
                content = "";
            }
            output.add(JsonDto.generateJsonDto(tagName, tagAttributes, content, root, false, children));
        }
    }

    private static void processComplexJsonDto(StringBuilder fileContentSB, SymbolsIndexes indexes, String tagName, List<JsonDto> output, boolean root, boolean emptyTagName) {
        if (emptyTagName) {
            fileContentSB.replace(0, fileContentSB.length(), fileContentSB.toString().replaceFirst("\\{" +
                    StringService.getStringBetweenBraces(fileContentSB.toString(), indexes.getCurrentIndex(), '{', '}').
                            replaceAll("\\{", "\\\\{").replaceAll("\\[", "\\\\[") + "}", ""));
        } else {
            boolean wrong = false;
            List<JsonDto> children = new ArrayList<>();
            HashMap<String, String> tagAttributes = new HashMap<>();
            String content = null;
            String childrenContent = StringService.getStringBetweenBraces(fileContentSB.toString(), indexes.getCurrentIndex(), '{', '}');
            if (childrenContent.matches("\\s*")) {
                output.add(JsonDto.generateJsonDto(tagName, tagAttributes, "", root, false, children));
            } else {
                List<JsonDto> childrenDtoList = readJsonDtoList(new StringBuilder(childrenContent), false);
                List<String> childrenDtoTagNames = childrenDtoList.stream().map(JsonDto::getTagName).collect(Collectors.toCollection(ArrayList::new));
                List<JsonDto> childrenToBeRemoved = new ArrayList<>();
                if (childrenDtoList.size() == 0) {
                    output.add(JsonDto.generateJsonDto(tagName, tagAttributes, "", root, false, children));
                } else {
                    int hashtagTagNames = 0;
                    for (JsonDto child : childrenDtoList) {
                        if (child.getTagName() != null) {
                            String childTagName = child.getTagName();
                            if (childTagName.startsWith("#")) {
                                if (childrenDtoTagNames.contains(childTagName.substring(1))) {
                                    childrenToBeRemoved.add(child);
                                }
                                hashtagTagNames++;
                                if (!childTagName.equals("#" + tagName)) {
                                    wrong = true;
                                }
                                if (child.getChildren().size() > 0) {
                                    children = child.getChildren();
                                }
                            } else if (childTagName.startsWith("@")) {
                                if (childrenDtoTagNames.contains(childTagName.substring(1))) {
                                    childrenToBeRemoved.add(child);
                                }
                                if (childTagName.equals("@") || child.getChildren().size() > 0) {
                                    wrong = true;
                                }
                            } else {
                                wrong = true;
                            }
                        }
                    }
                    for (JsonDto childToBeRemoved : childrenToBeRemoved) {
                        childrenDtoList.remove(childToBeRemoved);
                    }
                    if (hashtagTagNames != 1) {
                        wrong = true;
                    }
                    for (JsonDto child : childrenDtoList) {
                        if (child.getTagName() != null) {
                            String childTagName = child.getTagName();
                            if (childTagName.startsWith("#") && childTagName.length() > 1) {
                                if (wrong) {
                                    childTagName = childTagName.substring(1);
                                    child.setTagName(childTagName);
                                } else {
                                    content = child.getContent() == null ? "null" : child.getContent();
                                }
                            } else if (childTagName.startsWith("@") && childTagName.length() > 1) {
                                if (wrong) {
                                    childTagName = childTagName.substring(1);
                                    child.setTagName(childTagName);
                                } else {
                                    tagAttributes.put(childTagName.substring(1), child.getContent() == null ? "" : child.getContent());
                                }
                            }
                            if (!childTagName.startsWith("#") && !childTagName.startsWith("@")) {
                                children.add(child);
                            }
                        }
                    }
                    output.add(JsonDto.generateJsonDto(tagName, tagAttributes, content, root, false, children));
                }
            }
            fileContentSB.replace(0, fileContentSB.length(), fileContentSB.toString().replaceFirst("\\{" + childrenContent.replaceAll("\\{", "\\\\{").replaceAll("\\[", "\\\\[") + '}', ""));
        }
    }

    private static void processJsonDtoArray(StringBuilder fileContentSB, SymbolsIndexes indexes, String tagName, List<JsonDto> output) {
        String content = StringService.getStringBetweenBraces(fileContentSB.toString(), indexes.getCurrentIndex(), '[', ']');
        fileContentSB.replace(0, fileContentSB.length(), fileContentSB.toString().replaceFirst("\\[" + content.
                replaceAll("\\{", "\\\\{").replaceAll("\\[", "\\\\[") + ']', ""));
        if (!"#".equals(tagName)) {
            output.add(JsonDto.generateJsonDto(tagName, content, false, false, true));
        }
    }

    private static void processJsonDtoAfterDoubleDots(StringBuilder fileContentSB, String tagName, boolean root, List<JsonDto> output) {
        if (fileContentSB.toString().indexOf(':') != -1) {
            List<JsonDto> children = new ArrayList<>();
            HashMap<String, String> tagAttributes = new HashMap<>();
            String content = StringService.getValueBetweenDoubleDotsAndComa(fileContentSB.toString());
            output.add(JsonDto.generateJsonDto(tagName, tagAttributes, content, root, false, children));
        }
    }

    private static String readTagName(StringBuilder fileContentSB, SymbolsIndexes indexes) {
        indexes.refreshIndexOfOpeningQuote();
        indexes.refreshIndexOfOpeningOfArray();
        String tagName;
        if (indexes.getIndexOfOpeningQuote() < indexes.getIndexOfOpeningOfArray()) {
            tagName = StringService.getTagName(fileContentSB.toString());
            fileContentSB.replace(0, fileContentSB.length(), fileContentSB.toString().replaceFirst('"' + tagName + '"', ""));
        } else {
            tagName = null;
        }
        indexes.refreshIndexes();
        return tagName;
    }
}
