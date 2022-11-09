package com.converterapp.service;

import java.nio.file.Path;

public interface Dialog {
    Path getUserInput();

    String userPromptLine();
}
