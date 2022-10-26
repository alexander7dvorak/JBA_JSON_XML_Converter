package com.converterapp.service;

import java.io.File;
import java.nio.file.Path;
import java.util.Scanner;

public class ConsoleDialog implements Dialog {
    private static final ConsoleDialog instance = new ConsoleDialog();
    private static final Scanner scanner = new Scanner(System.in);

    private ConsoleDialog() {

    }

    public static ConsoleDialog getInstance() {
        return instance;
    }

    @Override
    public Path userPromptFilePath() {
        printFilePrompt();
        String filePath = scanner.nextLine();
        while (!FileValidator.filePathExists(filePath)) {
            printFilePathDoesntExist(filePath);
            printFilePrompt();
            filePath = scanner.nextLine();
        }
        return new File(filePath).toPath();
    }

    public String userPromptLine() {
        return scanner.nextLine();
    }

    private void printFilePrompt() {
        System.out.print("Enter input file path: ");
    }

    private void printFilePathDoesntExist(String filePath) {
        System.out.printf("%s doesn't exist.%n", filePath);
    }
}
