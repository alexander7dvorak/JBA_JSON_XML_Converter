package com.converterapp.service;

import com.converterapp.util.Converter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ConverterTest {

    @Nested
    @DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
    class XML_to_JSON_test {
        @Test
        void XML_to_JSON_Test_1() {
            String xmlString = "<host>127.0.0.1</host>";
            String expectedJsonResultString = "{\"host\":\"127.0.0.1\"}";
            assertExpectedJsonEqualsResultOfConversionFromXmlToJson(expectedJsonResultString, xmlString);
        }

        @Test
        void XML_to_JSON_Test_2() {
            String xmlString = "<success / >";
            String expectedJsonResultString = "{\"success\":null}";
            assertExpectedJsonEqualsResultOfConversionFromXmlToJson(expectedJsonResultString, xmlString);
        }

        @Test
        void XML_to_JSON_Test_3() {
            String xmlString = "<employee department = \"manager\">Garry Smith</employee>";
            String expectedJsonResultString = "{\"employee\":{\"@department\":\"manager\",\"#employee\":\"Garry Smith\"}}";
            assertExpectedJsonEqualsResultOfConversionFromXmlToJson(expectedJsonResultString, xmlString);
        }

        @Test
        void XML_to_JSON_Test_4() {
            String xmlString = "<person rate = \"1\" name = \"Torvalds\"/>";
            String expectedJsonResultString = "{\"person\":{\"@rate\":\"1\",\"@name\":\"Torvalds\",\"#person\":null}}";
            assertExpectedJsonEqualsResultOfConversionFromXmlToJson(expectedJsonResultString, xmlString);
        }

        private void assertExpectedJsonEqualsResultOfConversionFromXmlToJson(String expectedJsonResultString, String xmlString) {
            Assertions.assertEquals(
                    expectedJsonResultString,
                    Converter.xmlToJSON(xmlString)
            );
        }
    }

    @Nested
    @DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
    class JSON_to_XML_test {
        @Test
        void JSON_to_XML_Test_1() {
            String jsonString = "{\"jdk\":\"1.8.9\"}";
            String expectedXmlResultString = "<jdk>1.8.9</jdk>";
            assertExpectedXmlEqualsResultOfConversionFromJsonToXml(expectedXmlResultString, jsonString);
        }

        @Test
        void JSON_to_XML_Test_2() {
            String jsonString = "{\"storage\":null}";
            String expectedXmlResultString = "<storage/>";
            assertExpectedXmlEqualsResultOfConversionFromJsonToXml(expectedXmlResultString, jsonString);
        }

        @Test
        void JSON_to_XML_Test_3() {
            String jsonString = "{\"employee\":{\"@department\":\"manager\",\"#employee\":\"Garry Smith\"}}";
            String expectedXmlResultString = "<employee department = \"manager\">Garry Smith</employee>";
            assertExpectedXmlEqualsResultOfConversionFromJsonToXml(expectedXmlResultString, jsonString);
        }

        @Test
        void JSON_to_XML_Test_4() {
            String jsonString = "{\"person\":{\"@rate\":1,\"@name\":\"Torvalds\",\"#person\":null}}";
            String expectedXmlResultString = "<person rate = \"1\" name = \"Torvalds\"/>";
            assertExpectedXmlEqualsResultOfConversionFromJsonToXml(expectedXmlResultString, jsonString);
        }

        private void assertExpectedXmlEqualsResultOfConversionFromJsonToXml(String expectedXmlResultString, String jsonString) {
            Assertions.assertEquals(
                    expectedXmlResultString,
                    Converter.jsonToXML(jsonString)
            );
        }
    }
}