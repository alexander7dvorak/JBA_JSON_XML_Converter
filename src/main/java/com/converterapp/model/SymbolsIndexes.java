package com.converterapp.model;

public class SymbolsIndexes {
    private int indexOfComma;
    private int indexOfOpeningBrace;
    private int indexOfOpeningOfArray;
    private int indexOfOpeningQuote;
    private int currentIndex;
    private final StringBuilder fileContentSB;

    public SymbolsIndexes(StringBuilder fileContentSB) {
        this.fileContentSB = fileContentSB;
        refreshIndexes();
    }

    public int getIndexOfComma() {
        return indexOfComma;
    }

    public int getIndexOfOpeningBrace() {
        return indexOfOpeningBrace;
    }

    public int getIndexOfOpeningOfArray() {
        return indexOfOpeningOfArray;
    }

    public int getIndexOfOpeningQuote() {
        return indexOfOpeningQuote;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void refreshIndexes() {
        refreshIndexOfComma();
        refreshIndexOfOpeningBrace();
        refreshIndexOfOpeningOfArray();
        refreshIndexOfOpeningQuote();
        refreshCurrentIndex();
    }

    public void refreshIndexOfComma() {
        String fileContent = fileContentSB.toString();
        indexOfComma = fileContent.indexOf(',') == -1 ? Integer.MAX_VALUE : fileContent.indexOf(',');
    }

    public void refreshIndexOfOpeningBrace() {
        String fileContent = fileContentSB.toString();
        indexOfOpeningBrace = fileContent.indexOf('{') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('{');
    }

    public void refreshIndexOfOpeningOfArray() {
        String fileContent = fileContentSB.toString();
        indexOfOpeningOfArray = fileContent.indexOf('[') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('[');
    }

    public void refreshIndexOfOpeningQuote() {
        String fileContent = fileContentSB.toString();
        indexOfOpeningQuote = fileContent.indexOf('"') == -1 ? Integer.MAX_VALUE : fileContent.indexOf('"');
    }

    public void refreshCurrentIndex() {
        currentIndex = Math.min(indexOfOpeningBrace, Math.min(indexOfComma, Math.min(indexOfOpeningQuote, indexOfOpeningOfArray)));
    }
}
