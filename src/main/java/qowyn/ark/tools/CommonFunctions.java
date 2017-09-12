package qowyn.ark.tools;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import qowyn.ark.GameObject;
import qowyn.ark.GameObjectContainer;
import qowyn.ark.structs.StructLinearColor;
import qowyn.ark.types.ObjectReference;

public class CommonFunctions {

  public static final JsonFactory JSON_FACTORY = new JsonFactory();

  public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper(JSON_FACTORY);

  public static boolean onlyTamed(GameObject animal, GameObjectContainer saveFile) {
    return animal.findPropertyValue("TargetingTeam", Integer.class).orElse(0) >= 50000;
  }

  public static boolean onlyWild(GameObject animal, GameObjectContainer saveFile) {
    return animal.findPropertyValue("TargetingTeam", Integer.class).orElse(0) < 50000;
  }

  public static int getBaseLevel(GameObject animal, GameObjectContainer saveFile) {
    GameObject statusComponent = animal.findPropertyValue("MyCharacterStatusComponent", ObjectReference.class).map(saveFile::getObject).orElse(null);

    if (statusComponent == null) {
      return 0;
    }

    return statusComponent.findPropertyValue("BaseCharacterLevel", Integer.class).orElse(0);
  }

  public static int getFullLevel(GameObject animal, GameObjectContainer saveFile) {
    GameObject statusComponent = animal.findPropertyValue("MyCharacterStatusComponent", ObjectReference.class).map(saveFile::getObject).orElse(null);

    if (statusComponent == null) {
      return 1;
    }
    
    int baseLevel = statusComponent.findPropertyValue("BaseCharacterLevel", Integer.class).orElse(1);
    short extraLevel = statusComponent.findPropertyValue("ExtraCharacterLevel", Short.class).orElse((short) 0);
    return baseLevel + extraLevel;
  }

  public static void writeJson(OutputStream out, JsonNode node, OptionHandler oh) throws IOException {
    if (out == null || node == null) {
      throw new NullPointerException();
    }

    ObjectWriter writer = oh.usePretty() ? OBJECT_MAPPER.writerWithDefaultPrettyPrinter() : OBJECT_MAPPER.writer();
    writer.writeValue(out, node);
  }

  public static void writeJson(Path outPath, JsonNode node, OptionHandler oh) throws IOException {
    if (outPath == null || node == null) {
      throw new NullPointerException();
    }

    ObjectWriter writer = oh.usePretty() ? OBJECT_MAPPER.writerWithDefaultPrettyPrinter() : OBJECT_MAPPER.writer();
    writer.writeValue(outPath.toFile(), node);
  }

  public static void writeJson(OutputStream out, WriteJsonCallback writeJson, OptionHandler oh) throws IOException {
    if (out == null || writeJson == null) {
      throw new NullPointerException();
    }

    try (JsonGenerator generator = JSON_FACTORY.createGenerator(out)) {
      if (oh.usePretty()) {
        generator.useDefaultPrettyPrinter();
      }

      writeJson.accept(generator);
    }
  }

  public static void writeJson(Path outPath, WriteJsonCallback writeJson, OptionHandler oh) throws IOException {
    if (outPath == null || writeJson == null) {
      throw new NullPointerException();
    }

    try (JsonGenerator generator = JSON_FACTORY.createGenerator(outPath.toFile(), JsonEncoding.UTF8)) {
      if (oh.usePretty()) {
        generator.useDefaultPrettyPrinter();
      }

      writeJson.accept(generator);
    }
  }

  public static JsonNode readJson(InputStream stream) throws IOException {
    if (stream == null) {
      throw new NullPointerException();
    }

    return OBJECT_MAPPER.readTree(stream);
  }

  public static JsonNode readJson(Path inPath) throws IOException {
    if (inPath == null) {
      throw new NullPointerException();
    }

    return OBJECT_MAPPER.readTree(inPath.toFile());
  }

  public static JsonNode readJsonRelative(String inFile) throws IOException {
    try (InputStream stream = CommonFunctions.class.getResourceAsStream(inFile)) {
      if (stream == null) {
        throw new FileNotFoundException();
      }

      return OBJECT_MAPPER.readTree(stream);
    }
  }

  public static void readJson(InputStream stream, ParseJsonCallback parseJson) throws IOException {
    if (stream == null) {
      throw new NullPointerException();
    }

    try (JsonParser parser = JSON_FACTORY.createParser(stream)) {
      parseJson.accept(parser);
    }
  }

  public static void readJson(Path inPath, ParseJsonCallback parseJson) throws IOException {
    if (inPath == null) {
      throw new NullPointerException();
    }

    try (JsonParser parser = JSON_FACTORY.createParser(inPath.toFile())) {
      parseJson.accept(parser);
    }
  }

  public static String getRGBA(StructLinearColor lc) {
    double clampR = Math.min(1, Math.max(lc.getR(), 0));
    double clampG = Math.min(1, Math.max(lc.getG(), 0));
    double clampB = Math.min(1, Math.max(lc.getB(), 0));
    double clampA = Math.min(1, Math.max(lc.getA(), 0));

    // Gamma correction
    clampR = clampR <= 0.0031308 ? clampR * 12.92 : 1.055 * Math.pow(clampR, 1.0 / 2.4) - 0.055;
    clampG = clampG <= 0.0031308 ? clampG * 12.92 : 1.055 * Math.pow(clampG, 1.0 / 2.4) - 0.055;
    clampB = clampB <= 0.0031308 ? clampB * 12.92 : 1.055 * Math.pow(clampB, 1.0 / 2.4) - 0.055;

    String rs = ("0" + Integer.toHexString((int) Math.floor(clampR * 255.999999)));
    String gs = ("0" + Integer.toHexString((int) Math.floor(clampG * 255.999999)));
    String bs = ("0" + Integer.toHexString((int) Math.floor(clampB * 255.999999)));
    String as = ("0" + Integer.toHexString((int) Math.floor(clampA * 255.999999)));

    return "#" + rs.substring(rs.length() - 2) + gs.substring(gs.length() - 2) + bs.substring(bs.length() - 2) + as.substring(as.length() - 2);
  }

  public static <T> Iterable<T> iterable(Stream<T> stream) {
    return stream::iterator;
  }

}
