package qowyn.ark.tools;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;

import qowyn.ark.ArkSavegame;
import qowyn.ark.GameObject;
import qowyn.ark.types.ObjectReference;

public class CommonFunctions {

  public static boolean onlyTamed(GameObject animal) {
    return animal.getProperty("TamedAtTime") != null;
  }

  public static boolean onlyWild(GameObject animal) {
    return animal.getProperty("TamedAtTime") == null;
  }

  public static int getBaseLevel(GameObject animal, ArkSavegame saveFile) {
    ObjectReference statusComponentReference = animal.getPropertyValue("MyCharacterStatusComponent", ObjectReference.class);
    GameObject statusComponent = statusComponentReference != null ? statusComponentReference.getObject(saveFile) : null;
    Integer baseLevel = statusComponent != null ? statusComponent.getPropertyValue("BaseCharacterLevel", Integer.class) : null;
    return baseLevel != null ? baseLevel : 0;
  }

  public static void writeJson(String outFile, JsonObject o) throws IOException {
    try (PrintWriter out = new PrintWriter(outFile)) {
      Map<String, Object> properties = new HashMap<>(1);
      properties.put(JsonGenerator.PRETTY_PRINTING, true);

      JsonWriterFactory jwf = Json.createWriterFactory(properties);
      jwf.createWriter(out).writeObject(o);
    }
  }

  public static void writeJson(String outFile, Consumer<JsonGenerator> writeJson) throws IOException {
    try (PrintWriter out = new PrintWriter(outFile)) {
      Map<String, Object> properties = new HashMap<>(1);
      properties.put(JsonGenerator.PRETTY_PRINTING, true);

      JsonGeneratorFactory jgf = Json.createGeneratorFactory(properties);
      JsonGenerator jg = jgf.createGenerator(out);
      writeJson.accept(jg);
      jg.close();
    }
  }

  public static JsonObject readJson(String inFile) throws IOException {
    try (FileReader fileReader = new FileReader(inFile)) {
      JsonReader reader = Json.createReader(fileReader);
      return reader.readObject();
    }
  }

}
