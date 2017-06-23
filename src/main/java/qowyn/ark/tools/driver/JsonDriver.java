package qowyn.ark.tools.driver;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import javax.json.stream.JsonGenerator;

import qowyn.ark.tools.CommonFunctions;
import qowyn.ark.tools.data.AttributeNames;
import qowyn.ark.tools.data.Creature;
import qowyn.ark.tools.data.DataCollector;
import qowyn.ark.tools.data.Inventory;
import qowyn.ark.tools.data.Item;
import qowyn.ark.tools.data.ItemStatDefinitions;
import qowyn.ark.tools.data.Player;
import qowyn.ark.tools.data.Structure;
import qowyn.ark.tools.data.Tribe;

public class JsonDriver implements DBDriver {

  private static final List<String> PROTOCOL_LIST;

  private static final Map<String, String> PARAMETER_MAP;

  private static final SortedMap<String, BiConsumer<Creature, JsonGenerator>> CREATURE_PROPERTIES = new TreeMap<>();

  private static final SortedMap<String, BiConsumer<Inventory, JsonGenerator>> INVENTORY_PROPERTIES = new TreeMap<>();

  private static final SortedMap<String, BiConsumer<Item, JsonGenerator>> ITEM_PROPERTIES = new TreeMap<>();

  private static final SortedMap<String, BiConsumer<Player, JsonGenerator>> PLAYER_PROPERTIES = new TreeMap<>();

  private static final SortedMap<String, BiConsumer<Structure, JsonGenerator>> STRUCTURE_PROPERTIES = new TreeMap<>();

  private static final SortedMap<String, BiConsumer<Tribe, JsonGenerator>> TRIBE_PROPERTIES = new TreeMap<>();

  private static void testAndAddProtocol(String protocol, String testURL, List<String> protocolList) {
    try {
      new URL(testURL);
      protocolList.add(protocol);
    } catch (MalformedURLException e) {
    }
  }

  static {
    /**
     * Inventory Properties
     */
    INVENTORY_PROPERTIES.put("inventoryItems", (inventory, generator) -> {
      generator.writeStartArray("inventoryItems");

      for (int itemId: inventory.inventoryItems) {
        generator.write(itemId);
      }

      generator.writeEnd();
    });
    INVENTORY_PROPERTIES.put("equippedItems", (inventory, generator) -> {
      generator.writeStartArray("equippedItems");

      for (int itemId: inventory.equippedItems) {
        generator.write(itemId);
      }

      generator.writeEnd();
    });
    INVENTORY_PROPERTIES.put("itemSlots", (inventory, generator) -> {
      generator.writeStartArray("itemSlots");

      for (int itemId: inventory.itemSlots) {
        generator.write(itemId);
      }

      generator.writeEnd();
    });
    INVENTORY_PROPERTIES.put("lastInventoryRefreshTime", (inventory, generator) -> {
      generator.write("lastInventoryRefreshTime", inventory.lastInventoryRefreshTime);
    });
    /**
     * Item Properties
     */
    ITEM_PROPERTIES.put("itemId", (item, generator) -> {
      generator.write("itemId", item.itemId);
    });
    ITEM_PROPERTIES.put("canEquip", (item, generator) -> {
      generator.write("canEquip", item.canEquip);
    });
    ITEM_PROPERTIES.put("canSlot", (item, generator) -> {
      generator.write("canSlot", item.canSlot);
    });
    ITEM_PROPERTIES.put("isEngram", (item, generator) -> {
      generator.write("isEngram", item.isEngram);
    });
    ITEM_PROPERTIES.put("isBlueprint", (item, generator) -> {
      generator.write("isBlueprint", item.isBlueprint);
    });
    ITEM_PROPERTIES.put("canRemove", (item, generator) -> {
      generator.write("canRemove", item.canRemove);
    });
    ITEM_PROPERTIES.put("canRemoveFromCluster", (item, generator) -> {
      generator.write("canRemoveFromCluster", item.canRemoveFromCluster);
    });
    ITEM_PROPERTIES.put("isHidden", (item, generator) -> {
      generator.write("isHidden", item.isHidden);
    });
    ITEM_PROPERTIES.put("className", (item, generator) -> {
      generator.write("className", item.className.toString());
    });
    ITEM_PROPERTIES.put("blueprintGeneratedClass", (item, generator) -> {
      generator.write("blueprintGeneratedClass", item.blueprintGeneratedClass);
    });
    ITEM_PROPERTIES.put("quantity", (item, generator) -> {
      generator.write("quantity", item.quantity);
    });
    ITEM_PROPERTIES.put("customName", (item, generator) -> {
      generator.write("customName", item.customName);
    });
    ITEM_PROPERTIES.put("customDescription", (item, generator) -> {
      generator.write("customDescription", item.customDescription);
    });
    ITEM_PROPERTIES.put("durability", (item, generator) -> {
      generator.write("durability", item.durability);
    });
    ITEM_PROPERTIES.put("rating", (item, generator) -> {
      generator.write("rating", item.rating);
    });
    ITEM_PROPERTIES.put("quality", (item, generator) -> {
      generator.write("quality", Byte.toUnsignedInt(item.quality));
    });
    for (int index = 0; index < ItemStatDefinitions.size(); index++) {
      final int finalIndex = index;
      ITEM_PROPERTIES.put("itemStatValue_" + finalIndex, (item, generator) -> {
        generator.write("itemStatValue_" + finalIndex, Short.toUnsignedInt(item.itemStatValues[finalIndex]));
      });
    }
    for (int index = 0; index < Item.COLOR_SLOT_COUNT; index++) {
      final int finalIndex = index;
      ITEM_PROPERTIES.put("itemColor_" + finalIndex, (item, generator) -> {
        generator.write("itemColor_" + finalIndex, Short.toUnsignedInt(item.itemColors[finalIndex]));
      });
      ITEM_PROPERTIES.put("preSkinItemColor_" + finalIndex, (item, generator) -> {
        generator.write("preSkinItemColor_" + finalIndex, Short.toUnsignedInt(item.preSkinItemColors[finalIndex]));
      });
      ITEM_PROPERTIES.put("eggColor_" + finalIndex, (item, generator) -> {
        generator.write("eggColor_" + finalIndex, Byte.toUnsignedInt(item.eggColors[finalIndex]));
      });
    }
    for (int index = 0; index < AttributeNames.size(); index++) {
      final int finalIndex = index;
      ITEM_PROPERTIES.put("eggLevelup_" + finalIndex, (item, generator) -> {
        generator.write("eggLevelup_" + finalIndex, Byte.toUnsignedInt(item.eggLevelups[finalIndex]));
      });
    }
    ITEM_PROPERTIES.put("crafterCharacterName", (item, generator) -> {
      generator.write("crafterCharacterName", item.crafterCharacterName);
    });
    ITEM_PROPERTIES.put("craftedSkillBonus", (item, generator) -> {
      generator.write("craftedSkillBonus", item.craftedSkillBonus);
    });
    ITEM_PROPERTIES.put("uploadOffset", (item, generator) -> {
      generator.write("uploadOffset", item.uploadOffset);
    });

    List<String> protocols = new ArrayList<>();

    testAndAddProtocol("http", "http://localhost", protocols);
    testAndAddProtocol("https", "https://localhost", protocols);
    testAndAddProtocol("file", "file://", protocols);
    testAndAddProtocol("mailto", "mailto:root@localhost", protocols);
    testAndAddProtocol("ftp", "ftp://localhost", protocols);

    PROTOCOL_LIST = Collections.unmodifiableList(protocols);

    Map<String, String> parameters = new LinkedHashMap<>();

    parameters.put("username", "username to use for http, https and ftp");
    parameters.put("password", "password to use for http, https and ftp");

    parameters.put("creatureFields", "comma delimited list of fields to write - default: " + CREATURE_PROPERTIES.keySet().stream().collect(Collectors.joining(",")));
    parameters.put("inventoryFields", "comma delimited list of fields to write - default: " + INVENTORY_PROPERTIES.keySet().stream().collect(Collectors.joining(",")));
    parameters.put("itemFields", "comma delimited list of fields to write - default: " + ITEM_PROPERTIES.keySet().stream().collect(Collectors.joining(",")));
    parameters.put("playerFields", "comma delimited list of fields to write - default: " + PLAYER_PROPERTIES.keySet().stream().collect(Collectors.joining(",")));
    parameters.put("structureFields", "comma delimited list of fields to write - default: " + STRUCTURE_PROPERTIES.keySet().stream().collect(Collectors.joining(",")));
    parameters.put("tribeFields", "comma delimited list of fields to write - default: " + TRIBE_PROPERTIES.keySet().stream().collect(Collectors.joining(",")));

    PARAMETER_MAP = Collections.unmodifiableMap(parameters);
  }

  private OutputStream os;

  private Map<String, String> params = new HashMap<>();

  @Override
  public void openConnection(URI uri) {
    try {
      URLConnection conn = uri.toURL().openConnection();

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

  private <T> List<BiConsumer<T, JsonGenerator>> generateList(String paramName, SortedMap<String, BiConsumer<T, JsonGenerator>> map) {
    List<BiConsumer<T, JsonGenerator>> result = new ArrayList<>();

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
    List<BiConsumer<Creature, JsonGenerator>> creatureWriters = generateList("creatureFields", CREATURE_PROPERTIES);
    List<BiConsumer<Inventory, JsonGenerator>> inventoryWriters = generateList("inventoryFields", INVENTORY_PROPERTIES);
    List<BiConsumer<Item, JsonGenerator>> itemWriters = generateList("itemFields", ITEM_PROPERTIES);
    List<BiConsumer<Player, JsonGenerator>> playerWriters = generateList("playerFields", PLAYER_PROPERTIES);
    List<BiConsumer<Structure, JsonGenerator>> structureWriters = generateList("structureFields", STRUCTURE_PROPERTIES);
    List<BiConsumer<Tribe, JsonGenerator>> tribeWriters = generateList("tribeFields", TRIBE_PROPERTIES);

    CommonFunctions.writeJson(os, generator -> {
      generator.writeStartObject();

      generator.writeStartObject("creatures");

      for (int index: data.creatureMap.keySet()) {
        Creature creature = data.creatureMap.get(index);

        generator.writeStartObject(Integer.toString(index));

        for (BiConsumer<Creature, JsonGenerator> writer: creatureWriters) {
          writer.accept(creature, generator);
        }

        generator.writeEnd();
      }

      generator.writeEnd();

      generator.writeStartObject("inventories");

      for (int index: data.inventoryMap.keySet()) {
        Inventory inventory = data.inventoryMap.get(index);

        generator.writeStartObject(Integer.toString(index));

        for (BiConsumer<Inventory, JsonGenerator> writer: inventoryWriters) {
          writer.accept(inventory, generator);
        }

        generator.writeEnd();
      }

      generator.writeEnd();

      generator.writeStartObject("items");

      for (int index: data.itemMap.keySet()) {
        Item item = data.itemMap.get(index);

        generator.writeStartObject(Integer.toString(index));

        for (BiConsumer<Item, JsonGenerator> writer: itemWriters) {
          writer.accept(item, generator);
        }

        generator.writeEnd();
      }

      generator.writeEnd();

      generator.writeStartObject("players");

      for (long index: data.playerMap.keySet()) {
        Player player = data.playerMap.get(index);

        generator.writeStartObject(Long.toString(index));

        for (BiConsumer<Player, JsonGenerator> writer: playerWriters) {
          writer.accept(player, generator);
        }

        generator.writeEnd();
      }

      generator.writeEnd();

      generator.writeStartObject("structures");

      for (int index: data.structureMap.keySet()) {
        Structure structure = data.structureMap.get(index);

        generator.writeStartObject(Integer.toString(index));

        for (BiConsumer<Structure, JsonGenerator> writer: structureWriters) {
          writer.accept(structure, generator);
        }

        generator.writeEnd();
      }

      generator.writeEnd();

      generator.writeStartObject("tribes");

      for (int index: data.tribeMap.keySet()) {
        Tribe tribe = data.tribeMap.get(index);

        generator.writeStartObject(Integer.toString(index));

        for (BiConsumer<Tribe, JsonGenerator> writer: tribeWriters) {
          writer.accept(tribe, generator);
        }

        generator.writeEnd();
      }

      generator.writeEnd();

      generator.writeEnd();
    }, data.oh);
  }
  
  @Override
  public void close() {
    try {
      os.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
