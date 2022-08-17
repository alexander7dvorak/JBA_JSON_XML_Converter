package com.converterapp.service;

import com.converterapp.util.FileValidator;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Scanner;

@Service
public class ConsoleInputProgram {
    private static final Scanner scanner = new Scanner(System.in);

    @PostConstruct
    public static void run() {
        String content = scanner.nextLine();
        if (FileValidator.isXML(content)) {
            System.out.println(Converter.xmlToJSON(content));
        } else if (FileValidator.isJSON(content)) {
            System.out.println(Converter.jsonToXML(content));
        } else {
            System.out.println("Content is neither xml nor json");
        }
    }
}
