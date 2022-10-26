package com.converterapp.service;

import java.nio.file.Path;

public interface Dialog {
    Path userPromptFilePath();

    String userPromptLine();
}
