package com.converterapp.util;

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
        printFilePropmt();
        String filePath = scanner.nextLine();
        while (!FileValidator.filePathExists(filePath)) {
            printFilePropmt();
            printFilePathDoesntExist(filePath);
            filePath = scanner.nextLine();
        }
        return new File(filePath).toPath();
    }

    private void printFilePropmt() {
        System.out.print("Enter input file path: ");
    }

    private void printFilePathDoesntExist(String filePath) {
        System.out.printf("%s doesn't exist.%n", filePath);
    }
}
