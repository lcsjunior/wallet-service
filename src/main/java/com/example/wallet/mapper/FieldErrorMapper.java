package com.example.wallet.mapper;

import com.example.wallet.dto.FieldErrorDetail;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.validation.FieldError;

@Mapper(componentModel = "spring")
public interface FieldErrorMapper {

  @Mapping(target = "message", source = "defaultMessage")
  FieldErrorDetail toFieldErrorDetail(FieldError fieldError);

  List<FieldErrorDetail> toFieldErrorDetails(List<FieldError> fieldErrors);
}
