package com.converterapp.stage4;

import org.hyperskill.hstest.dynamic.DynamicTest;
import org.hyperskill.hstest.stage.StageTest;
import org.hyperskill.hstest.testcase.CheckResult;
import org.hyperskill.hstest.testing.TestedProgram;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toUnmodifiableList;
import static com.converterapp.stage4.util.Assert.assertEquals;
import static com.converterapp.stage4.util.Assert.assertFalse;

public class ConverterTest extends StageTest {
    private static final Pattern ELEMENTS_DELIMITER = Pattern
            .compile("\\s+(?=Element:)", Pattern.CASE_INSENSITIVE);

    final int[] testCases = {1, 2, 3, 4};

    @DynamicTest(data = "testCases")
    CheckResult simpleTest(final int testCase) throws IOException {
        Files.copy(
                Path.of("src/test/java/com/converterapp/stage4/data/test" + testCase + ".json"),
                Path.of("test.txt"),
                StandardCopyOption.REPLACE_EXISTING);

        final var expectedOutput = Files.readString(
                Path.of("src/test/java/com/converterapp/stage4/data/expected" + testCase + ".txt"));
        final var expectedElements = parseOutput(expectedOutput);

        final var program = new TestedProgram();
        final var actualOutput = program.start("stage_4_test");

        assertFalse(actualOutput.isBlank(), "empty");
        final var actualElements = parseOutput(actualOutput);

        assertFalse(actualElements.size() < expectedElements.size(),
                "lessElements", actualElements.size(), expectedElements.size());

        assertFalse(actualElements.size() < expectedElements.size(),
                "moreElements", actualElements.size(), expectedElements.size());

        for (int i = 0; i < expectedElements.size(); ++i) {
            assertEquals(expectedElements.get(i), expectedElements.get(i),
                    "elementsNotEqual", i + 1, expectedElements.get(i));
        }

        return CheckResult.correct();
    }

    private List<Element> parseOutput(final String data) {
        return ELEMENTS_DELIMITER
                .splitAsStream(data)
                .map(Element::parse)
                .collect(toUnmodifiableList());
    }

}
