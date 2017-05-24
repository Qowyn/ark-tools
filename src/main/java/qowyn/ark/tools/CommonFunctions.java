package qowyn.ark.tools;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.json.Json;
import javax.json.JsonReader;
import javax.json.JsonStructure;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.json.stream.JsonParser;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import qowyn.ark.GameObject;
import qowyn.ark.GameObjectContainer;
import qowyn.ark.types.ObjectReference;

public class CommonFunctions {

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

  public static void writeJson(OutputStream out, JsonStructure structure, OptionHandler oh) throws IOException {
    if (out == null) {
      throw new NullPointerException();
    }

    if (oh.usePretty()) {
      Map<String, Object> properties = new HashMap<>(1);
      properties.put(JsonGenerator.PRETTY_PRINTING, true);

      JsonWriterFactory jwf = Json.createWriterFactory(properties);
      try (JsonWriter writer = jwf.createWriter(out)) {
        writer.write(structure);
      }
    } else {
      try (JsonWriter writer = Json.createWriter(out)) {
        writer.write(structure);
      }
    }
  }

  public static void writeJson(String outFile, JsonStructure structure, OptionHandler oh) throws IOException {
    try (OutputStream out = new FileOutputStream(outFile)) {
      writeJson(out, structure, oh);
    }
  }

  public static void writeJson(OutputStream out, Consumer<JsonGenerator> writeJson, OptionHandler oh) throws IOException {
    if (out == null) {
      throw new NullPointerException();
    }

    if (oh.usePretty()) {
      Map<String, Object> properties = new HashMap<>(1);
      properties.put(JsonGenerator.PRETTY_PRINTING, true);

      JsonGeneratorFactory jgf = Json.createGeneratorFactory(properties);
      try (JsonGenerator jg = jgf.createGenerator(out)) {
        writeJson.accept(jg);
      }
    } else {
      try (JsonGenerator jg = Json.createGenerator(out)) {
        writeJson.accept(jg);
      }
    }
  }

  public static void writeJson(String outFile, Consumer<JsonGenerator> writeJson, OptionHandler oh) throws IOException {
    try (OutputStream out = new FileOutputStream(outFile)) {
      writeJson(out, writeJson, oh);
    }
  }

  public static JsonStructure readJson(InputStream stream) throws IOException {
    if (stream == null) {
      throw new NullPointerException();
    }

    try (JsonReader reader = Json.createReader(stream)) {
      return reader.read();
    }
  }

  public static JsonStructure readJson(String inFile) throws IOException {
    try (InputStream stream = new FileInputStream(inFile)) {
      return readJson(stream);
    }
  }

  public static JsonStructure readJsonRelative(String inFile) throws IOException {
    try (InputStream stream = CommonFunctions.class.getResourceAsStream(inFile)) {
      if (stream == null) {
        throw new FileNotFoundException();
      }

      return readJson(stream);
    }
  }

  public static void readJson(InputStream stream, Consumer<JsonParser> parseJson) throws IOException {
    if (stream == null) {
      throw new NullPointerException();
    }

    try (JsonParser parser = Json.createParser(stream)) {
      parseJson.accept(parser);
    }
  }

  public static void readJson(String inFile, Consumer<JsonParser> parseJson) throws IOException {
    try (InputStream stream = new FileInputStream(inFile)) {
      readJson(stream, parseJson);
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> void processAsyncronously(EventFactory<T> factory, EventHandler<T> consumer, Consumer<RingBuffer<T>> producer, int bufferSize) {

    Disruptor<T> disruptor = new Disruptor<>(factory, bufferSize, Executors.defaultThreadFactory(), ProducerType.SINGLE, new YieldingWaitStrategy());

    disruptor.handleEventsWith(consumer);

    disruptor.start();

    RingBuffer<T> ringBuffer = disruptor.getRingBuffer();

    producer.accept(ringBuffer);

    disruptor.shutdown();
  }

}
