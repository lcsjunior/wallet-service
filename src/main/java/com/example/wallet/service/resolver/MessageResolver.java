package com.example.wallet.service.resolver;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

@Service
public class MessageResolver {

  private final MessageSource messageSource;

  public MessageResolver(MessageSource messageSource) {
    this.messageSource = messageSource;
  }

  public String resolve(String code, Object... args) {
    return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
  }
}
