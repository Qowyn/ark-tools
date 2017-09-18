package qowyn.ark.tools;

import java.util.Arrays;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;

public class JsonValidator {

  public static boolean expect(JsonNode node, String fieldName, JsonNodeType nodeType) {
    if (node == null || node.isMissingNode()) {
      return false;
    }

    if (node.getNodeType() != nodeType) {
      System.err.println("Expected " + fieldName + " to be " + nodeType + " but found " + node.getNodeType());
      return false;
    }
    return true;
  }

  public static boolean expect(JsonNode node, String fieldName, JsonNodeType... nodeTypes) {
    if (node == null || node.isMissingNode()) {
      return false;
    }

    for (JsonNodeType nodeType : nodeTypes) {
      if (node.getNodeType() == nodeType) {
        return true;
      }
    }

    String typeNames = Arrays.stream(nodeTypes).map(JsonNodeType::toString).collect(Collectors.joining(", "));

    System.err.println("Expected " + fieldName + " to be one of " + typeNames + " but found " + node.getNodeType());
    return false;
  }

  public static boolean expectArray(JsonNode node, String fieldName, JsonNodeType nodeType) {
    if (node == null || node.isMissingNode()) {
      return false;
    }

    if (node.getNodeType() != nodeType) {
      System.err.println("Expected values of " + fieldName + " to be " + nodeType + " but found " + node.getNodeType());
      return false;
    }
    return true;
  }

  public static boolean expectArray(JsonNode node, String fieldName, JsonNodeType... nodeTypes) {
    if (node == null || node.isMissingNode()) {
      return false;
    }

    for (JsonNodeType nodeType : nodeTypes) {
      if (node.getNodeType() == nodeType) {
        return true;
      }
    }

    String typeNames = Arrays.stream(nodeTypes).map(JsonNodeType::toString).collect(Collectors.joining(", "));

    System.err.println("Expected values of " + fieldName + " to be one of " + typeNames + " but found " + node.getNodeType());
    return false;
  }

}
