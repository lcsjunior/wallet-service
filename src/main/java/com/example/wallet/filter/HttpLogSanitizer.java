package com.example.wallet.filter;

import com.example.wallet.config.HttpLoggingProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.util.Locale;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;

@Component
public class HttpLogSanitizer {

  private static final String UNPARSEABLE_BODY = "<unparseable body>";

  private final HttpLoggingProperties properties;
  private final ObjectMapper objectMapper;

  public HttpLogSanitizer(HttpLoggingProperties properties, ObjectMapper objectMapper) {
    this.properties = properties;
    this.objectMapper = objectMapper;
  }

  public String headers(HttpHeaders headers) {
    var sanitized = new LinkedMultiValueMap<String, String>();
    headers.forEach(
        (name, values) -> {
          if (properties.maskedHeaders().contains(name.toLowerCase(Locale.ROOT))) {
            sanitized.add(name, properties.replacement());
          } else {
            sanitized.addAll(name, values);
          }
        });
    return HttpHeaders.formatHeaders(sanitized);
  }

  public String body(byte[] content) {
    if (content.length == 0) {
      return "";
    }
    try {
      var node = objectMapper.readTree(content);
      mask(node);
      return LogFormatUtils.formatValue(
          objectMapper.writeValueAsString(node), properties.maxBodyLength(), true);
    } catch (IOException ex) {
      return UNPARSEABLE_BODY;
    }
  }

  private void mask(JsonNode node) {
    if (node instanceof ObjectNode objectNode) {
      objectNode
          .properties()
          .forEach(
              property -> {
                if (properties.maskedBodyFields().contains(property.getKey())) {
                  objectNode.put(property.getKey(), properties.replacement());
                } else {
                  mask(property.getValue());
                }
              });
    } else if (node instanceof ArrayNode arrayNode) {
      arrayNode.forEach(this::mask);
    }
  }
}
