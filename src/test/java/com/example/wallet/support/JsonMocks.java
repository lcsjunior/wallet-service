package com.example.wallet.support;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public final class JsonMocks {

  private static final String BASE_PATH = "src/test/resources/mock/json/";

  private JsonMocks() {}

  public static String load(String relativePath) {
    return load(relativePath, Map.of());
  }

  public static String load(String relativePath, Map<String, String> replacements) {
    String content = readFile(relativePath);
    for (Map.Entry<String, String> replacement : replacements.entrySet()) {
      content = content.replace("${" + replacement.getKey() + "}", replacement.getValue());
    }
    return content;
  }

  private static String readFile(String relativePath) {
    try {
      return Files.readString(Path.of(BASE_PATH + relativePath));
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}
