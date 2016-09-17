package qowyn.ark.tools;

import javax.json.JsonValue;

public class JsonValidator {

  public static boolean expect(JsonValue value, JsonValue.ValueType valueType, String fieldName) {
    if (value.getValueType() != valueType) {
      System.err.println("Expected " + fieldName + " to be " + valueType + " but found " + value.getValueType());
      return false;
    }
    return true;
  }

}
