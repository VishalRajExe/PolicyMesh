package com.policymesh.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Converter(autoApply = false)
public class StringSetConverter implements AttributeConverter<Set<String>, String> {

  @Override
  public String convertToDatabaseColumn(Set<String> attribute) {
    if (attribute == null || attribute.isEmpty()) {
      return "";
    }
    return String.join(",", attribute);
  }

  @Override
  public Set<String> convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isBlank()) {
      return new TreeSet<>();
    }
    return Arrays.stream(dbData.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toCollection(TreeSet::new));
  }
}
