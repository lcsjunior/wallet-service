package com.example.wallet.testutils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JsonUtils {

  private static final String BASE_PATH = "src/test/resources/mock/";

  private JsonUtils() {}

  public static String loadJson(String relativePath) {
    try {
      return Files.readString(Path.of(BASE_PATH + relativePath));
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }

  public static String emptyJson() {
    return "{}";
  }
}
