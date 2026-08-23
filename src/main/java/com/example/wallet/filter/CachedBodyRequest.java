package com.example.wallet.filter;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

class CachedBodyRequest extends HttpServletRequestWrapper {

  private final byte[] body;

  private CachedBodyRequest(HttpServletRequest request, byte[] body) {
    super(request);
    this.body = body;
  }

  static CachedBodyRequest of(HttpServletRequest request) throws IOException {
    return new CachedBodyRequest(request, request.getInputStream().readAllBytes());
  }

  byte[] getBody() {
    return body;
  }

  @Override
  public ServletInputStream getInputStream() {
    var cachedBody = new ByteArrayInputStream(body);
    return new ServletInputStream() {

      @Override
      public boolean isFinished() {
        return cachedBody.available() == 0;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(ReadListener readListener) {
        throw new UnsupportedOperationException();
      }

      @Override
      public int read() {
        return cachedBody.read();
      }
    };
  }

  @Override
  public BufferedReader getReader() {
    return new BufferedReader(new InputStreamReader(getInputStream(), charset()));
  }

  private Charset charset() {
    var encoding = getCharacterEncoding();
    return encoding == null ? StandardCharsets.UTF_8 : Charset.forName(encoding);
  }
}
