package qowyn.ark.tools.driver;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import qowyn.ark.tools.CommonFunctions;
import qowyn.ark.tools.data.Creature;
import qowyn.ark.tools.data.DataCollector;
import qowyn.ark.tools.data.DroppedItem;
import qowyn.ark.tools.data.Inventory;
import qowyn.ark.tools.data.Item;
import qowyn.ark.tools.data.Player;
import qowyn.ark.tools.data.Structure;
import qowyn.ark.tools.data.Tribe;
import qowyn.ark.tools.data.WriterFunction;

public class JsonDriver implements DBDriver {

  private static final List<String> PROTOCOL_LIST;

  private static final Map<String, String> PARAMETER_MAP;

  private static void testAndAddProtocol(String protocol, String testURL, List<String> protocolList) {
    try {
      new URL(testURL);
      protocolList.add(protocol);
    } catch (MalformedURLException e) {
    }
  }

  static {
    List<String> protocols = new ArrayList<>();

    testAndAddProtocol("http", "http://localhost", protocols);
    testAndAddProtocol("https", "https://localhost", protocols);
    testAndAddProtocol("file", "file://", protocols);
    testAndAddProtocol("mailto", "mailto:root@localhost", protocols);
    testAndAddProtocol("ftp", "ftp://localhost", protocols);

    PROTOCOL_LIST = Collections.unmodifiableList(protocols);

    Map<String, String> parameters = new LinkedHashMap<>();

    parameters.put("creatureFields", "comma delimited list of fields to write - default: " + Creature.PROPERTIES.keySet().stream().collect(Collectors.joining(",")));
    parameters.put("inventoryFields", "comma delimited list of fields to write - default: " + Inventory.PROPERTIES.keySet().stream().collect(Collectors.joining(",")));
    parameters.put("itemFields", "comma delimited list of fields to write - default: " + Item.PROPERTIES.keySet().stream().collect(Collectors.joining(",")));
    parameters.put("droppedItemFields", "comma delimited list of fields to write - default: " + DroppedItem.PROPERTIES.keySet().stream().collect(Collectors.joining(",")));
    parameters.put("playerFields", "comma delimited list of fields to write - default: " + Player.PROPERTIES.keySet().stream().collect(Collectors.joining(",")));
    parameters.put("structureFields", "comma delimited list of fields to write - default: " + Structure.PROPERTIES.keySet().stream().collect(Collectors.joining(",")));
    parameters.put("tribeFields", "comma delimited list of fields to write - default: " + Tribe.PROPERTIES.keySet().stream().collect(Collectors.joining(",")));

    parameters.put("writeEmpty", "force writing of empty fields");

    PARAMETER_MAP = Collections.unmodifiableMap(parameters);
  }

  private URLConnection conn;

  private OutputStream os;

  private Map<String, String> params = new HashMap<>();

  public JsonDriver() {
    params.put("writeEmpty", Boolean.toString(false));
  }

  @Override
  public void openConnection(URI uri) {
    try {
      conn = uri.toURL().openConnection();

      if (conn instanceof HttpURLConnection) {
        Manifest manifest = new Manifest(JsonDriver.class.getResourceAsStream("/META-INF/MANIFEST.MF"));
        String myVersion = manifest.getMainAttributes().getValue("Implementation-Version");

        HttpURLConnection httpConn = (HttpURLConnection) conn;
        httpConn.setRequestMethod("POST");
        httpConn.addRequestProperty("User-Agent", "ark-tools/" + myVersion);
      }

      conn.setDoOutput(true);

      os = conn.getOutputStream();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  @Override
  public void openConnection(Path path) throws IOException {
    os = new FileOutputStream(path.toFile());
  }

  @Override
  public List<String> getUrlSchemeList() {
    return PROTOCOL_LIST;
  }

  @Override
  public boolean canHandlePath() {
    return true;
  }

  @Override
  public String getParameter(String name) {
    return params.get(name);
  }

  @Override
  public DBDriver setParameter(String name, String value) {
    if (!PARAMETER_MAP.containsKey(name)) {
      throw new UnsupportedOperationException("Unknown parameter: " + name);
    }
    params.put(name, value);
    return this;
  }

  @Override
  public Map<String, String> getSupportedParameters() {
    return PARAMETER_MAP;
  }

  private <T> List<WriterFunction<T>> generateList(String paramName, SortedMap<String, WriterFunction<T>> map) {
    List<WriterFunction<T>> result = new ArrayList<>();

    String paramValue = params.get(paramName);

    if (paramValue == null || paramValue.isEmpty()) {
      for (String key: map.keySet()) {
        result.add(map.get(key));
      }
    } else {
      String[] keys = paramValue.split(",");
      for (String key: keys) {
        if (map.containsKey(key)) {
          result.add(map.get(key));
        }
      }
    }

    return result;
  }

  @Override
  public void write(DataCollector data) throws IOException {
    List<WriterFunction<Creature>> creatureWriters = generateList("creatureFields", Creature.PROPERTIES);
    List<WriterFunction<Inventory>> inventoryWriters = generateList("inventoryFields", Inventory.PROPERTIES);
    List<WriterFunction<Item>> itemWriters = generateList("itemFields", Item.PROPERTIES);
    List<WriterFunction<DroppedItem>> droppedItemWriters = generateList("droppedItemFields", DroppedItem.PROPERTIES);
    List<WriterFunction<Player>> playerWriters = generateList("playerFields", Player.PROPERTIES);
    List<WriterFunction<Structure>> structureWriters = generateList("structureFields", Structure.PROPERTIES);
    List<WriterFunction<Tribe>> tribeWriters = generateList("tribeFields", Tribe.PROPERTIES);
    boolean writeEmpty = Boolean.valueOf(params.get("writeEmpty"));

    CommonFunctions.writeJson(os, generator -> {
      generator.writeStartObject();

      generator.writeObjectFieldStart("creatures");

      for (int index: data.creatureMap.keySet()) {
        Creature creature = data.creatureMap.get(index);

        generator.writeObjectFieldStart(Integer.toString(index));

        for (WriterFunction<Creature> writer: creatureWriters) {
          writer.accept(creature, generator, data, writeEmpty);
        }

        generator.writeEndObject();
      }

      generator.writeEndObject();

      generator.writeObjectFieldStart("inventories");

      for (int index: data.inventoryMap.keySet()) {
        Inventory inventory = data.inventoryMap.get(index);

        generator.writeObjectFieldStart(Integer.toString(index));

        for (WriterFunction<Inventory> writer: inventoryWriters) {
          writer.accept(inventory, generator, data, writeEmpty);
        }

        generator.writeEndObject();
      }

      generator.writeEndObject();

      generator.writeObjectFieldStart("items");

      for (int index: data.itemMap.keySet()) {
        Item item = data.itemMap.get(index);

        generator.writeObjectFieldStart(Integer.toString(index));

        for (WriterFunction<Item> writer: itemWriters) {
          writer.accept(item, generator, data, writeEmpty);
        }

        generator.writeEndObject();
      }

      generator.writeEndObject();

      generator.writeObjectFieldStart("droppedItems");

      for (int index: data.droppedItemMap.keySet()) {
        DroppedItem droppedItem = data.droppedItemMap.get(index);

        generator.writeObjectFieldStart(Integer.toString(index));

        for (WriterFunction<DroppedItem> writer: droppedItemWriters) {
          writer.accept(droppedItem, generator, data, writeEmpty);
        }

        generator.writeEndObject();
      }

      generator.writeEndObject();

      generator.writeObjectFieldStart("players");

      for (long index: data.playerMap.keySet()) {
        Player player = data.playerMap.get(index);

        generator.writeObjectFieldStart(Long.toString(index));

        for (WriterFunction<Player> writer: playerWriters) {
          writer.accept(player, generator, data, writeEmpty);
        }

        generator.writeEndObject();
      }

      generator.writeEndObject();

      generator.writeObjectFieldStart("structures");

      for (int index: data.structureMap.keySet()) {
        Structure structure = data.structureMap.get(index);

        generator.writeObjectFieldStart(Integer.toString(index));

        for (WriterFunction<Structure> writer: structureWriters) {
          writer.accept(structure, generator, data, writeEmpty);
        }

        generator.writeEndObject();
      }

      generator.writeEndObject();

      generator.writeObjectFieldStart("tribes");

      for (int index: data.tribeMap.keySet()) {
        Tribe tribe = data.tribeMap.get(index);

        generator.writeObjectFieldStart(Integer.toString(index));

        for (WriterFunction<Tribe> writer: tribeWriters) {
          writer.accept(tribe, generator, data, writeEmpty);
        }

        generator.writeEndObject();
      }

      generator.writeEndObject();

      generator.writeEndObject();
    }, data.oh);
  }
  
  @Override
  public void close() {
    try {
      os.flush();
      os.close();
      if (conn != null) {
        conn.getInputStream().close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
