package com.converterapp.service;

import com.converterapp.model.AppConstants;

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
    public Path getUserInput() {
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
        System.out.print(AppConstants.ENTER_INPUT_FILE_PATH);
    }

    private void printFilePathDoesntExist(String filePath) {
        System.out.printf(AppConstants.FILE_PATH_DOESNT_EXIST_NEW_LINE, filePath);
    }
}
