package qowyn.ark.tools;

import java.util.Arrays;
import java.util.stream.Collectors;

import javax.json.JsonValue;

public class JsonValidator {

  public static boolean expect(JsonValue value, String fieldName, JsonValue.ValueType valueType) {
    if (value == null) {
      return false;
    }

    if (value.getValueType() != valueType) {
      System.err.println("Expected " + fieldName + " to be " + valueType + " but found " + value.getValueType());
      return false;
    }
    return true;
  }

  public static boolean expect(JsonValue value, String fieldName, JsonValue.ValueType... valueTypes) {
    if (value == null) {
      return false;
    }

    for (JsonValue.ValueType valueType : valueTypes) {
      if (value.getValueType() == valueType) {
        return true;
      }
    }

    String typeNames = Arrays.stream(valueTypes).map(JsonValue.ValueType::toString).collect(Collectors.joining(", "));

    System.err.println("Expected " + fieldName + " to be one of " + typeNames + " but found " + value.getValueType());
    return false;
  }

}
