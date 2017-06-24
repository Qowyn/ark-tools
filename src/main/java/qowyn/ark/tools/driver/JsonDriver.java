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
import java.util.TreeMap;
import java.util.jar.Manifest;
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
import qowyn.ark.tools.data.PlayerBodyColorRegions;
import qowyn.ark.tools.data.PlayerBoneModifierNames;
import qowyn.ark.tools.data.Structure;
import qowyn.ark.tools.data.Tribe;

public class JsonDriver implements DBDriver {

  private static interface WriterFunction<T> {

    public void accept(T object, JsonGenerator generator, DataCollector collector, boolean writeEmpty);

  }

  private static final List<String> PROTOCOL_LIST;

  private static final Map<String, String> PARAMETER_MAP;

  private static final SortedMap<String, WriterFunction<Creature>> CREATURE_PROPERTIES = new TreeMap<>();

  private static final SortedMap<String, WriterFunction<Inventory>> INVENTORY_PROPERTIES = new TreeMap<>();

  private static final SortedMap<String, WriterFunction<Item>> ITEM_PROPERTIES = new TreeMap<>();

  private static final SortedMap<String, WriterFunction<Player>> PLAYER_PROPERTIES = new TreeMap<>();

  private static final SortedMap<String, WriterFunction<Structure>> STRUCTURE_PROPERTIES = new TreeMap<>();

  private static final SortedMap<String, WriterFunction<Tribe>> TRIBE_PROPERTIES = new TreeMap<>();

  private static void testAndAddProtocol(String protocol, String testURL, List<String> protocolList) {
    try {
      new URL(testURL);
      protocolList.add(protocol);
    } catch (MalformedURLException e) {
    }
  }

  static {
    /**
     * Creature Properties
     */
    CREATURE_PROPERTIES.put("type", (creature, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || !creature.type.isEmpty()) {
        generator.write("type", creature.type);
      }
    });
    CREATURE_PROPERTIES.put("location", (creature, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || creature.location != null) {
        if (creature.location == null) {
          generator.writeNull("location");
        } else {
          generator.writeStartObject("location");
          generator.write("x", creature.location.getX());
          generator.write("y", creature.location.getY());
          generator.write("z", creature.location.getZ());
          generator.write("lat", dataCollector.latLonCalculator.calculateLat(creature.location.getX()));
          generator.write("lon", dataCollector.latLonCalculator.calculateLon(creature.location.getY()));
          generator.writeEnd();
        }
      }
    });
    CREATURE_PROPERTIES.put("dinoId", (creature, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || creature.dinoId != 0) {
        generator.write("dinoId", creature.dinoId);
      }
    });
    CREATURE_PROPERTIES.put("tamed", (creature, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || creature.tamed) {
        generator.write("tamed", creature.tamed);
      }
    });
    CREATURE_PROPERTIES.put("targetingTeam", (creature, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || creature.targetingTeam != 0) {
        generator.write("targetingTeam", creature.targetingTeam);
      }
    });
    CREATURE_PROPERTIES.put("owningPlayerId", (creature, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || creature.owningPlayerId != 0) {
        generator.write("owningPlayerId", creature.owningPlayerId);
      }
    });
    CREATURE_PROPERTIES.put("isFemale", (creature, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || creature.isFemale) {
        generator.write("isFemale", creature.isFemale);
      }
    });
    CREATURE_PROPERTIES.put("colorSetIndices", (creature, generator, dataCollector, writeEmpty) -> {
      boolean empty = !writeEmpty;
      if (!empty) {
        generator.writeStartObject("colorSetIndices");
      }
      for (int index = 0; index < creature.colorSetIndices.length; index++) {
        if (writeEmpty || creature.colorSetIndices[index] != 0) {
          if (empty) {
            empty = false;
            generator.writeStartObject("colorSetIndices");
          }
          generator.write(Integer.toString(index), Byte.toUnsignedInt(creature.colorSetIndices[index]));
        }
      }
      if (!empty) {
        generator.writeEnd();
      }
    });
    CREATURE_PROPERTIES.put("tamedAtTime", (creature, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || creature.tamedAtTime != 0.0) {
        generator.write("tamedAtTime", creature.tamedAtTime);
      }
    });
    CREATURE_PROPERTIES.put("tribeName", (creature, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || !creature.tribeName.isEmpty()) {
        generator.write("tribeName", creature.tribeName);
      }
    });
    CREATURE_PROPERTIES.put("tamerString", (creature, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || !creature.tamerString.isEmpty()) {
        generator.write("tamerString", creature.tamerString);
      }
    });
    CREATURE_PROPERTIES.put("owningPlayerName", (creature, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || !creature.owningPlayerName.isEmpty()) {
        generator.write("owningPlayerName", creature.owningPlayerName);
      }
    });
    CREATURE_PROPERTIES.put("tamedName", (creature, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || !creature.tamedName.isEmpty()) {
        generator.write("tamedName", creature.tamedName);
      }
    });
    CREATURE_PROPERTIES.put("imprinterName", (creature, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || !creature.imprinterName.isEmpty()) {
        generator.write("imprinterName", creature.imprinterName);
      }
    });
    CREATURE_PROPERTIES.put("baseCharacterLevel", (creature, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || creature.baseCharacterLevel != 0) {
        generator.write("baseCharacterLevel", creature.baseCharacterLevel);
      }
    });
    CREATURE_PROPERTIES.put("numberOfLevelUpPointsApplied", (creature, generator, dataCollector, writeEmpty) -> {
      boolean empty = !writeEmpty;
      if (!empty) {
        generator.writeStartObject("numberOfLevelUpPointsApplied");
      }
      for (int index = 0; index < creature.numberOfLevelUpPointsApplied.length; index++) {
        if (writeEmpty || creature.numberOfLevelUpPointsApplied[index] != 0) {
          if (empty) {
            empty = false;
            generator.writeStartObject("numberOfLevelUpPointsApplied");
          }
          generator.write(AttributeNames.get(index), Byte.toUnsignedInt(creature.numberOfLevelUpPointsApplied[index]));
        }
      }
      if (!empty) {
        generator.writeEnd();
      }
    });
    CREATURE_PROPERTIES.put("extraCharacterLevel", (creature, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || creature.extraCharacterLevel != 0) {
        generator.write("extraCharacterLevel", creature.extraCharacterLevel);
      }
    });
    CREATURE_PROPERTIES.put("numberOfLevelUpPointsAppliedTamed", (creature, generator, dataCollector, writeEmpty) -> {
      boolean empty = !writeEmpty;
      if (!empty) {
        generator.writeStartObject("numberOfLevelUpPointsAppliedTamed");
      }
      for (int index = 0; index < creature.numberOfLevelUpPointsAppliedTamed.length; index++) {
        if (writeEmpty || creature.numberOfLevelUpPointsAppliedTamed[index] != 0) {
          if (empty) {
            empty = false;
            generator.writeStartObject("numberOfLevelUpPointsAppliedTamed");
          }
          generator.write(AttributeNames.get(index), Byte.toUnsignedInt(creature.numberOfLevelUpPointsAppliedTamed[index]));
        }
      }
      if (!empty) {
        generator.writeEnd();
      }
    });
    CREATURE_PROPERTIES.put("dinoImprintingQuality", (creature, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || creature.dinoImprintingQuality != 0.0f) {
        generator.write("dinoImprintingQuality", creature.dinoImprintingQuality);
      }
    });
    CREATURE_PROPERTIES.put("wildRandomScale", (creature, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || creature.wildRandomScale != 1.0f) {
        generator.write("wildRandomScale", creature.wildRandomScale);
      }
    });
    CREATURE_PROPERTIES.put("requiredTameAffinity", (creature, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || creature.requiredTameAffinity != 0.0f) {
        generator.write("requiredTameAffinity", creature.requiredTameAffinity);
      }
    });
    CREATURE_PROPERTIES.put("lastEnterStasisTime", (creature, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || creature.lastEnterStasisTime != 0.0) {
        generator.write("lastEnterStasisTime", creature.lastEnterStasisTime);
      }
    });
    /**
     * Inventory Properties
     */
    INVENTORY_PROPERTIES.put("inventoryItems", (inventory, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || !inventory.inventoryItems.isEmpty()) {
        generator.writeStartArray("inventoryItems");
        for (int itemId: inventory.inventoryItems) {
          generator.write(itemId);
        }
        generator.writeEnd();
      }
    });
    INVENTORY_PROPERTIES.put("equippedItems", (inventory, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || !inventory.equippedItems.isEmpty()) {
        generator.writeStartArray("equippedItems");
        for (int itemId: inventory.equippedItems) {
          generator.write(itemId);
        }
        generator.writeEnd();
      }
    });
    INVENTORY_PROPERTIES.put("itemSlots", (inventory, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || !inventory.itemSlots.isEmpty()) {
        generator.writeStartArray("itemSlots");
        for (int itemId: inventory.itemSlots) {
          generator.write(itemId);
        }
        generator.writeEnd();
      }
    });
    INVENTORY_PROPERTIES.put("lastInventoryRefreshTime", (inventory, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || inventory.lastInventoryRefreshTime != 0.0) {
        generator.write("lastInventoryRefreshTime", inventory.lastInventoryRefreshTime);
      }
    });
    /**
     * Item Properties
     */
    ITEM_PROPERTIES.put("itemId", (item, generator, dataCollector, writeEmpty) -> {
      generator.write("itemId", item.itemId);
    });
    ITEM_PROPERTIES.put("canEquip", (item, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || item.canEquip) {
        generator.write("canEquip", item.canEquip);
      }
    });
    ITEM_PROPERTIES.put("canSlot", (item, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || item.canSlot) {
        generator.write("canSlot", item.canSlot);
      }
    });
    ITEM_PROPERTIES.put("isEngram", (item, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || item.isEngram) {
        generator.write("isEngram", item.isEngram);
      }
    });
    ITEM_PROPERTIES.put("isBlueprint", (item, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || item.isBlueprint) {
        generator.write("isBlueprint", item.isBlueprint);
      }
    });
    ITEM_PROPERTIES.put("canRemove", (item, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || item.canRemove) {
        generator.write("canRemove", item.canRemove);
      }
    });
    ITEM_PROPERTIES.put("canRemoveFromCluster", (item, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || item.canRemoveFromCluster) {
        generator.write("canRemoveFromCluster", item.canRemoveFromCluster);
      }
    });
    ITEM_PROPERTIES.put("isHidden", (item, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || item.isHidden) {
        generator.write("isHidden", item.isHidden);
      }
    });
    ITEM_PROPERTIES.put("className", (item, generator, dataCollector, writeEmpty) -> {
      generator.write("className", item.className.toString());
    });
    ITEM_PROPERTIES.put("blueprintGeneratedClass", (item, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || item.blueprintGeneratedClass != null && !item.blueprintGeneratedClass.isEmpty()) {
        if (item.blueprintGeneratedClass != null) {
          generator.write("blueprintGeneratedClass", item.blueprintGeneratedClass);
        } else {
          generator.writeNull("blueprintGeneratedClass");
        }
      }
    });
    ITEM_PROPERTIES.put("quantity", (item, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || item.quantity != 0) {
        generator.write("quantity", item.quantity);
      }
    });
    ITEM_PROPERTIES.put("customName", (item, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || !item.customName.isEmpty()) {
        generator.write("customName", item.customName);
      }
    });
    ITEM_PROPERTIES.put("customDescription", (item, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || !item.customDescription.isEmpty()) {
        generator.write("customDescription", item.customDescription);
      }
    });
    ITEM_PROPERTIES.put("durability", (item, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || item.durability != 0.0f) {
        generator.write("durability", item.durability);
      }
    });
    ITEM_PROPERTIES.put("rating", (item, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || item.rating != 0.0f) {
        generator.write("rating", item.rating);
      }
    });
    ITEM_PROPERTIES.put("quality", (item, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || item.quality != 0) {
        generator.write("quality", Byte.toUnsignedInt(item.quality));
      }
    });
    ITEM_PROPERTIES.put("itemStatValues", (item, generator, dataCollector, writeEmpty) -> {
      boolean empty = !writeEmpty;
      if (!empty) {
        generator.writeStartObject("itemStatValues");
      }
      for (int index = 0; index < item.itemStatValues.length; index++) {
        if (writeEmpty || item.itemStatValues[index] != 0) {
          if (empty) {
            empty = false;
            generator.writeStartObject("itemStatValues");
          }
          generator.write(ItemStatDefinitions.get(index), Short.toUnsignedInt(item.itemStatValues[index]));
        }
      }
      if (!empty) {
        generator.writeEnd();
      }
    });
    ITEM_PROPERTIES.put("itemColors", (item, generator, dataCollector, writeEmpty) -> {
      boolean empty = !writeEmpty;
      if (!empty) {
        generator.writeStartObject("itemColors");
      }
      for (int index = 0; index < item.itemColors.length; index++) {
        if (writeEmpty || item.itemColors[index] != 0) {
          if (empty) {
            empty = false;
            generator.writeStartObject("itemColors");
          }
          generator.write(Integer.toString(index), Short.toUnsignedInt(item.itemColors[index]));
        }
      }
      if (!empty) {
        generator.writeEnd();
      }
    });
    ITEM_PROPERTIES.put("preSkinItemColors", (item, generator, dataCollector, writeEmpty) -> {
      boolean empty = !writeEmpty;
      if (!empty) {
        generator.writeStartObject("preSkinItemColors");
      }
      for (int index = 0; index < item.preSkinItemColors.length; index++) {
        if (writeEmpty || item.preSkinItemColors[index] != 0) {
          if (empty) {
            empty = false;
            generator.writeStartObject("preSkinItemColors");
          }
          generator.write(Integer.toString(index), Short.toUnsignedInt(item.preSkinItemColors[index]));
        }
      }
      if (!empty) {
        generator.writeEnd();
      }
    });
    ITEM_PROPERTIES.put("eggColors", (item, generator, dataCollector, writeEmpty) -> {
      boolean empty = !writeEmpty;
      if (!empty) {
        generator.writeStartObject("eggColors");
      }
      for (int index = 0; index < item.eggColors.length; index++) {
        if (writeEmpty || item.eggColors[index] != 0) {
          if (empty) {
            empty = false;
            generator.writeStartObject("eggColors");
          }
          generator.write(Integer.toString(index), Byte.toUnsignedInt(item.eggColors[index]));
        }
      }
      if (!empty) {
        generator.writeEnd();
      }
    });
    ITEM_PROPERTIES.put("eggLevelups", (item, generator, dataCollector, writeEmpty) -> {
      boolean empty = !writeEmpty;
      if (!empty) {
        generator.writeStartObject("eggLevelups");
      }
      for (int index = 0; index < item.eggLevelups.length; index++) {
        if (writeEmpty || item.eggLevelups[index] != 0) {
          if (empty) {
            empty = false;
            generator.writeStartObject("eggLevelups");
          }
          generator.write(AttributeNames.get(index), Byte.toUnsignedInt(item.eggLevelups[index]));
        }
      }
      if (!empty) {
        generator.writeEnd();
      }
    });
    ITEM_PROPERTIES.put("crafterCharacterName", (item, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || !item.crafterCharacterName.isEmpty()) {
        generator.write("crafterCharacterName", item.crafterCharacterName);
      }
    });
    ITEM_PROPERTIES.put("craftedSkillBonus", (item, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || item.craftedSkillBonus != 0.0f) {
        generator.write("craftedSkillBonus", item.craftedSkillBonus);
      }
    });
    ITEM_PROPERTIES.put("uploadOffset", (item, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || item.uploadOffset != 0) {
        generator.write("uploadOffset", item.uploadOffset);
      }
    });
    /**
     * Player Properties
     */
    PLAYER_PROPERTIES.put("steamId", (player, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || player.uniqueId != null) {
        if (player.uniqueId == null) {
          generator.writeNull("steamId");
        } else {
          generator.write("steamId", player.uniqueId.getNetId());
        }
      }
    });
    PLAYER_PROPERTIES.put("savedNetworkAddress", (player, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || player.savedNetworkAddress != null) {
        if (player.savedNetworkAddress == null) {
          generator.writeNull("savedNetworkAddress");
        } else {
          generator.write("savedNetworkAddress", player.savedNetworkAddress);
        }
      }
    });
    PLAYER_PROPERTIES.put("playerName", (player, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || player.playerName != null) {
        if (player.playerName == null) {
          generator.writeNull("playerName");
        } else {
          generator.write("playerName", player.playerName);
        }
      }
    });
    PLAYER_PROPERTIES.put("tribeId", (player, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || player.tribeId != 0) {
        generator.write("tribeId", player.tribeId);
      }
    });
    PLAYER_PROPERTIES.put("playerDataVersion", (player, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || player.playerDataVersion != 0) {
        generator.write("playerDataVersion", player.playerDataVersion);
      }
    });
    PLAYER_PROPERTIES.put("spawnDayNumber", (player, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || player.spawnDayNumber != 0) {
        generator.write("spawnDayNumber", player.spawnDayNumber);
      }
    });
    PLAYER_PROPERTIES.put("spawnDayTime", (player, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || player.spawnDayTime != 0.0f) {
        generator.write("spawnDayTime", player.spawnDayTime);
      }
    });
    PLAYER_PROPERTIES.put("isFemale", (player, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || player.isFemale) {
        generator.write("isFemale", player.isFemale);
      }
    });
    PLAYER_PROPERTIES.put("bodyColors", (player, generator, dataCollector, writeEmpty) -> {
      boolean empty = !writeEmpty;
      if (!empty) {
        generator.writeStartObject("bodyColors");
      }
      for (int index = 0; index < player.bodyColors.length; index++) {
        if (writeEmpty || player.bodyColors[index] != null) {
          if (empty) {
            empty = false;
            generator.writeStartObject("bodyColors");
          }
          if (player.bodyColors[index] == null) {
            generator.writeNull(PlayerBodyColorRegions.get(index));
          } else {
            generator.write(PlayerBodyColorRegions.get(index), CommonFunctions.getRGBA(player.bodyColors[index]));
          }
        }
      }
      if (!empty) {
        generator.writeEnd();
      }
    });
    PLAYER_PROPERTIES.put("overrideHeadHairColor", (player, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || player.overrideHeadHairColor != null) {
        if (player.overrideHeadHairColor == null) {
          generator.writeNull("overrideHeadHairColor");
        } else {
          generator.write("overrideHeadHairColor", CommonFunctions.getRGBA(player.overrideHeadHairColor));
        }
      }
    });
    PLAYER_PROPERTIES.put("overrideFacialHairColor", (player, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || player.overrideFacialHairColor != null) {
        if (player.overrideFacialHairColor == null) {
          generator.writeNull("overrideFacialHairColor");
        } else {
          generator.write("overrideFacialHairColor", CommonFunctions.getRGBA(player.overrideFacialHairColor));
        }
      }
    });
    PLAYER_PROPERTIES.put("headHairIndex", (player, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || player.headHairIndex != 0) {
        generator.write("headHairIndex", Byte.toUnsignedInt(player.headHairIndex));
      }
    });
    PLAYER_PROPERTIES.put("facialHairIndex", (player, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || player.facialHairIndex != 0) {
        generator.write("facialHairIndex", Byte.toUnsignedInt(player.facialHairIndex));
      }
    });
    PLAYER_PROPERTIES.put("playerCharacterName", (player, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || !player.playerCharacterName.isEmpty()) {
        generator.write("playerCharacterName", player.playerCharacterName);
      }
    });
    PLAYER_PROPERTIES.put("rawBoneModifiers", (player, generator, dataCollector, writeEmpty) -> {
      boolean empty = !writeEmpty;
      if (!empty) {
        generator.writeStartObject("rawBoneModifiers");
      }
      for (int index = 0; index < player.rawBoneModifiers.length; index++) {
        if (writeEmpty || player.rawBoneModifiers[index] != 0.0f) {
          if (empty) {
            empty = false;
            generator.writeStartObject("rawBoneModifiers");
          }
          generator.write(PlayerBoneModifierNames.get(index), player.rawBoneModifiers[index]);
        }
      }
      if (!empty) {
        generator.writeEnd();
      }
    });
    PLAYER_PROPERTIES.put("playerSpawnRegionIndex", (player, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || player.playerSpawnRegionIndex != 0) {
        generator.write("playerSpawnRegionIndex", player.playerSpawnRegionIndex);
      }
    });
    PLAYER_PROPERTIES.put("totalEngramPoints", (player, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || player.totalEngramPoints != 0) {
        generator.write("totalEngramPoints", player.totalEngramPoints);
      }
    });
    PLAYER_PROPERTIES.put("engramBlueprints", (player, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || !player.engramBlueprints.isEmpty()) {
        generator.writeStartArray("engramBlueprints");
        for (String engramBlueprint: player.engramBlueprints) {
          generator.write(engramBlueprint);
        }
        generator.writeEnd();
      }
    });
    PLAYER_PROPERTIES.put("numberOfLevelUpPointsApplied", (player, generator, dataCollector, writeEmpty) -> {
      boolean empty = !writeEmpty;
      if (!empty) {
        generator.writeStartObject("numberOfLevelUpPointsApplied");
      }
      for (int index = 0; index < player.numberOfLevelUpPointsApplied.length; index++) {
        if (writeEmpty || player.numberOfLevelUpPointsApplied[index] != 0) {
          if (empty) {
            empty = false;
            generator.writeStartObject("numberOfLevelUpPointsApplied");
          }
          generator.write(AttributeNames.get(index), Byte.toUnsignedInt(player.numberOfLevelUpPointsApplied[index]));
        }
      }
      if (!empty) {
        generator.writeEnd();
      }
    });
    PLAYER_PROPERTIES.put("currentStatusValues", (player, generator, dataCollector, writeEmpty) -> {
      boolean empty = !writeEmpty;
      if (!empty) {
        generator.writeStartObject("currentStatusValues");
      }
      for (int index = 0; index < player.currentStatusValues.length; index++) {
        if (writeEmpty || player.currentStatusValues[index] != 0.0f) {
          if (empty) {
            empty = false;
            generator.writeStartObject("currentStatusValues");
          }
          generator.write(AttributeNames.get(index), player.currentStatusValues[index]);
        }
      }
      if (!empty) {
        generator.writeEnd();
      }
    });
    PLAYER_PROPERTIES.put("percentageOfHeadHairGrowth", (player, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || player.percentageOfHeadHairGrowth != 0.0f) {
        generator.write("percentageOfHeadHairGrowth", player.percentageOfHeadHairGrowth);
      }
    });
    PLAYER_PROPERTIES.put("percentageOfFacialHairGrowth", (player, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || player.percentageOfFacialHairGrowth != 0.0f) {
        generator.write("percentageOfFacialHairGrowth", player.percentageOfFacialHairGrowth);
      }
    });
    PLAYER_PROPERTIES.put("inventoryId", (player, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || player.inventoryId != -1) {
        generator.write("inventoryId", player.inventoryId);
      }
    });
    PLAYER_PROPERTIES.put("location", (player, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || player.location != null) {
        if (player.location == null) {
          generator.writeNull("location");
        } else {
          generator.write("location", player.location.toJson());
        }
      }
    });
    PLAYER_PROPERTIES.put("characterLevel", (player, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || player.characterLevel != 0) {
        generator.write("characterLevel", player.characterLevel);
      }
    });
    PLAYER_PROPERTIES.put("experiencePoints", (player, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || player.experiencePoints != 0.0f) {
        generator.write("experiencePoints", player.experiencePoints);
      }
    });
    PLAYER_PROPERTIES.put("lastHypothermalCharacterInsulationValue", (player, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || player.lastHypothermalCharacterInsulationValue != 0.0f) {
        generator.write("lastHypothermalCharacterInsulationValue", player.lastHypothermalCharacterInsulationValue);
      }
    });
    PLAYER_PROPERTIES.put("lastHyperthermalCharacterInsulationValue", (player, generator, dataCollector, writeEmpty) -> {
      if (writeEmpty || player.lastHyperthermalCharacterInsulationValue != 0.0f) {
        generator.write("lastHyperthermalCharacterInsulationValue", player.lastHyperthermalCharacterInsulationValue);
      }
    });

    List<String> protocols = new ArrayList<>();

    testAndAddProtocol("http", "http://localhost", protocols);
    testAndAddProtocol("https", "https://localhost", protocols);
    testAndAddProtocol("file", "file://", protocols);
    testAndAddProtocol("mailto", "mailto:root@localhost", protocols);
    testAndAddProtocol("ftp", "ftp://localhost", protocols);

    PROTOCOL_LIST = Collections.unmodifiableList(protocols);

    Map<String, String> parameters = new LinkedHashMap<>();

    parameters.put("creatureFields", "comma delimited list of fields to write - default: " + CREATURE_PROPERTIES.keySet().stream().collect(Collectors.joining(",")));
    parameters.put("inventoryFields", "comma delimited list of fields to write - default: " + INVENTORY_PROPERTIES.keySet().stream().collect(Collectors.joining(",")));
    parameters.put("itemFields", "comma delimited list of fields to write - default: " + ITEM_PROPERTIES.keySet().stream().collect(Collectors.joining(",")));
    parameters.put("playerFields", "comma delimited list of fields to write - default: " + PLAYER_PROPERTIES.keySet().stream().collect(Collectors.joining(",")));
    parameters.put("structureFields", "comma delimited list of fields to write - default: " + STRUCTURE_PROPERTIES.keySet().stream().collect(Collectors.joining(",")));
    parameters.put("tribeFields", "comma delimited list of fields to write - default: " + TRIBE_PROPERTIES.keySet().stream().collect(Collectors.joining(",")));

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
    List<WriterFunction<Creature>> creatureWriters = generateList("creatureFields", CREATURE_PROPERTIES);
    List<WriterFunction<Inventory>> inventoryWriters = generateList("inventoryFields", INVENTORY_PROPERTIES);
    List<WriterFunction<Item>> itemWriters = generateList("itemFields", ITEM_PROPERTIES);
    List<WriterFunction<Player>> playerWriters = generateList("playerFields", PLAYER_PROPERTIES);
    List<WriterFunction<Structure>> structureWriters = generateList("structureFields", STRUCTURE_PROPERTIES);
    List<WriterFunction<Tribe>> tribeWriters = generateList("tribeFields", TRIBE_PROPERTIES);
    boolean writeEmpty = Boolean.valueOf(params.get("writeEmpty"));

    CommonFunctions.writeJson(os, generator -> {
      generator.writeStartObject();

      generator.writeStartObject("creatures");

      for (int index: data.creatureMap.keySet()) {
        Creature creature = data.creatureMap.get(index);

        generator.writeStartObject(Integer.toString(index));

        for (WriterFunction<Creature> writer: creatureWriters) {
          writer.accept(creature, generator, data, writeEmpty);
        }

        generator.writeEnd();
      }

      generator.writeEnd();

      generator.writeStartObject("inventories");

      for (int index: data.inventoryMap.keySet()) {
        Inventory inventory = data.inventoryMap.get(index);

        generator.writeStartObject(Integer.toString(index));

        for (WriterFunction<Inventory> writer: inventoryWriters) {
          writer.accept(inventory, generator, data, writeEmpty);
        }

        generator.writeEnd();
      }

      generator.writeEnd();

      generator.writeStartObject("items");

      for (int index: data.itemMap.keySet()) {
        Item item = data.itemMap.get(index);

        generator.writeStartObject(Integer.toString(index));

        for (WriterFunction<Item> writer: itemWriters) {
          writer.accept(item, generator, data, writeEmpty);
        }

        generator.writeEnd();
      }

      generator.writeEnd();

      generator.writeStartObject("players");

      for (long index: data.playerMap.keySet()) {
        Player player = data.playerMap.get(index);

        generator.writeStartObject(Long.toString(index));

        for (WriterFunction<Player> writer: playerWriters) {
          writer.accept(player, generator, data, writeEmpty);
        }

        generator.writeEnd();
      }

      generator.writeEnd();

      generator.writeStartObject("structures");

      for (int index: data.structureMap.keySet()) {
        Structure structure = data.structureMap.get(index);

        generator.writeStartObject(Integer.toString(index));

        for (WriterFunction<Structure> writer: structureWriters) {
          writer.accept(structure, generator, data, writeEmpty);
        }

        generator.writeEnd();
      }

      generator.writeEnd();

      generator.writeStartObject("tribes");

      for (int index: data.tribeMap.keySet()) {
        Tribe tribe = data.tribeMap.get(index);

        generator.writeStartObject(Integer.toString(index));

        for (WriterFunction<Tribe> writer: tribeWriters) {
          writer.accept(tribe, generator, data, writeEmpty);
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
