package qowyn.ark.tools;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonStructure;
import javax.json.JsonValue;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import qowyn.ark.ArkCloudInventory;
import qowyn.ark.ArkContainer;
import qowyn.ark.ArkLocalProfile;
import qowyn.ark.ArkSavegame;
import qowyn.ark.GameObject;
import qowyn.ark.GameObjectContainer;
import qowyn.ark.PropertyContainer;
import qowyn.ark.arrays.ArkArrayObjectReference;
import qowyn.ark.arrays.ArkArrayStruct;
import qowyn.ark.data.ExtraDataZero;
import qowyn.ark.properties.PropertyArray;
import qowyn.ark.properties.PropertyDouble;
import qowyn.ark.properties.PropertyFloat;
import qowyn.ark.properties.PropertyInt32;
import qowyn.ark.properties.PropertyObject;
import qowyn.ark.properties.PropertyStr;
import qowyn.ark.properties.PropertyStruct;
import qowyn.ark.structs.Struct;
import qowyn.ark.structs.StructPropertyList;
import qowyn.ark.tools.data.ArkItem;
import qowyn.ark.tools.data.AttributeNames;
import qowyn.ark.types.ArkName;
import qowyn.ark.types.ObjectReference;

public class EditingCommands {

  public static void feed(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 2 || oh.wantsHelp()) {
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    Path fileToRead = Paths.get(params.get(0)).toAbsolutePath();
    Path fileToWrite = Paths.get(params.get(1)).toAbsolutePath();

    if (fileToRead.equals(fileToWrite)) {
      System.err.println("save and newsave need to be different paths");
      System.exit(2);
      return;
    }

    try {
      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());

      ArkSavegame savegame = new ArkSavegame(fileToRead.toString(), oh.readingOptions());

      stopwatch.stop("Reading");

      savegame.getObjects().parallelStream().filter(c -> CommonFunctions.onlyTamed(c, savegame)).forEach(object -> {
        ObjectReference statusComponentReference = object.getPropertyValue("MyCharacterStatusComponent", ObjectReference.class);

        if (statusComponentReference != null) {
          GameObject status = statusComponentReference.getObject(savegame);
          if (status != null) {
            PropertyFloat currentFood = status.getTypedProperty("CurrentStatusValues", PropertyFloat.class, 4);
            if (currentFood != null) {
              currentFood.setValue(1000000.0f);
            }
          }
        }

        PropertyDouble lastEnterStasisTime = object.getTypedProperty("LastEnterStasisTime", PropertyDouble.class);
        if (lastEnterStasisTime != null) {
          lastEnterStasisTime.setValue((double) savegame.getGameTime());
        }
      });

      stopwatch.stop("Setting values");

      savegame.writeBinary(fileToWrite.toString(), oh.writingOptions());

      stopwatch.stop("Writing");

      stopwatch.print();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void exportThing(OptionHandler oh) {
    OptionSpec<String> creatureSpec = oh.accepts("creature", "Export creature by Name - required if object not set").withRequiredArg().describedAs("name");
    OptionSpec<Long> dinoIdSpec = oh.accepts("dinoid", "Export creature by DinoID - required if object not set").withRequiredArg().ofType(Long.class).describedAs("id");
    OptionSpec<Integer> objectSpec = oh.accepts("object", "Export object by index - required if creature not set").withRequiredArg().ofType(Integer.class).describedAs("index");

    OptionSet options = oh.reparse();

    List<String> params = oh.getParams(options);

    if (params.size() != 2 || oh.wantsHelp() || !(options.has(creatureSpec) || options.has(objectSpec) || options.has(dinoIdSpec))) {
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    Path fileToRead = Paths.get(params.get(0)).toAbsolutePath();
    Path fileToWrite = Paths.get(params.get(1)).toAbsolutePath();

    if (fileToRead.equals(fileToWrite)) {
      System.err.println("save and outfile need to be different paths");
      System.exit(2);
      return;
    }

    try {
      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());

      ArkSavegame savegame = new ArkSavegame(fileToRead.toString(), oh.readingOptions());

      stopwatch.stop("Loading");

      ObjectCollector collector = null;
      if (options.has(creatureSpec)) {
        String name = creatureSpec.value(options);

        for (GameObject go : savegame.getObjects()) {
          String objectName = go.getPropertyValue("TamedName", String.class);
          if (name.equals(objectName)) {
            collector = new ObjectCollector(savegame, go);
            break;
          }
        }

        if (collector == null) {
          System.err.println("Could not find a creature named " + name);
          System.exit(2);
          return;
        }

      } else if (options.has(dinoIdSpec)) {
        long id = dinoIdSpec.value(options);

        for (GameObject creature : savegame.getObjects()) {
          Integer dinoID1 = creature.getPropertyValue("DinoID1", Integer.class);
          if (dinoID1 != null) {
            Integer dinoID2 = creature.getPropertyValue("DinoID2", Integer.class);
            if (dinoID2 != null) {
              long otherId = (long) dinoID1 << Integer.SIZE | (dinoID2 & 0xFFFFFFFFL);
              if (id == otherId) {
                collector = new ObjectCollector(savegame, creature);
                break;
              }
            }
          }
        }

        if (collector == null) {
          System.err.println("Could not find a creature with DinoID " + id);
          System.exit(2);
          return;
        }
      } else if (options.has(objectSpec)) {
        int index = objectSpec.value(options);

        if (index < 0 || index >= savegame.getObjects().size()) {
          System.err.println("Invalid object index " + index);
          System.exit(2);
          return;
        }

        collector = new ObjectCollector(savegame, savegame.getObjects().get(index));
      } else {
        // Makes FindBugs happy
        return;
      }

      stopwatch.stop("Collecting");

      ArkSavegame export = new ArkSavegame();

      export.setSaveVersion(savegame.getSaveVersion());
      export.setObjects(collector.remap(0));

      stopwatch.stop("Remapping");

      CommonFunctions.writeJson(fileToWrite.toString(), g -> export.writeJson(g, oh.writingOptions()), oh);

      stopwatch.stop("Writing");

      stopwatch.print();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void importThing(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 3 || oh.wantsHelp()) {
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    Path fileToRead = Paths.get(params.get(0)).toAbsolutePath();
    Path fileToWrite = Paths.get(params.get(2)).toAbsolutePath();

    if (fileToRead.equals(fileToWrite)) {
      System.err.println("save and outfile need to be different paths");
      System.exit(2);
      return;
    }

    try {
      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());

      FileFormat fileFormat = FileFormat.fromExtension(fileToRead);

      ArkContainer jsonFile = new ArkContainer((JsonArray) CommonFunctions.readJson(params.get(1)));
      stopwatch.stop("Reading import container");

      boolean containsCreature = jsonFile.getObjects().stream().anyMatch(o -> o.getClassString().contains("_Character_"));

      if (fileFormat == FileFormat.MAP) {
        ArkSavegame savegame = new ArkSavegame(fileToRead.toString(), oh.readingOptions());
        stopwatch.stop("Reading save");

        importIntoSavegame(savegame, jsonFile, stopwatch);

        savegame.writeBinary(fileToWrite.toString(), oh.writingOptions());

        stopwatch.stop("Writing");
      } else if (fileFormat == FileFormat.CLUSTER) {
        if (!containsCreature) {
          System.err.println("No creature in import file.");
          System.exit(1);
        } else {
          DataManager.loadData(oh.lang());
          ArkCloudInventory cloudInventory;
          if (Files.exists(fileToRead)) {
            cloudInventory = new ArkCloudInventory(fileToRead.toString(), oh.readingOptions());
          } else {
            cloudInventory = new ArkCloudInventory();
            cloudInventory.setInventoryVersion(1);
            cloudInventory.setInventoryData(new GameObject());
            cloudInventory.getInventoryData().setClassString("ArkCloudInventoryData");
            cloudInventory.getInventoryData().setItem(true);
            cloudInventory.getInventoryData().setNames(new ArrayList<>());
            cloudInventory.getInventoryData().getNames().add(new ArkName("ArkCloudInventoryData", 42));
            cloudInventory.getInventoryData().setExtraData(new ExtraDataZero());
          }

          importIntoClusterData(cloudInventory, jsonFile, stopwatch);

          cloudInventory.writeBinary(fileToWrite.toString(), oh.writingOptions());

          stopwatch.stop("Writing");
        }
      } else if (fileFormat == FileFormat.LOCALPROFILE) {
        if (!containsCreature) {
          System.err.println("No creature in import file.");
          System.exit(1);
        } else {
          DataManager.loadData(oh.lang());
          ArkLocalProfile localInventory = new ArkLocalProfile(fileToRead.toString(), oh.readingOptions());

          importIntoClusterData(localInventory, jsonFile, stopwatch);

          localInventory.writeBinary(fileToWrite.toString(), oh.writingOptions());

          stopwatch.stop("Writing");
        }
      } else {
        System.err.println("Modifying " + fileFormat + " is not yet implemented.");
        System.exit(1);
      }


      stopwatch.print();

    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void importIntoSavegame(ArkSavegame savegame, GameObjectContainer importFile, Stopwatch stopwatch) {
    ObjectCollector collector = new ObjectCollector(importFile);

    int startIndex = savegame.getObjects().size();

    List<GameObject> remappedObjects = collector.remap(startIndex);

    stopwatch.stop("Rewriting object ids");

    Set<Long> dinoIDs = new HashSet<>(); // Stored as 2 UInt32
    Set<Long> itemIDs = new HashSet<>(); // Stored as StructPropertyList with 2 UInt32
    Set<ArkName> names = new HashSet<>();

    for (GameObject object : savegame.getObjects()) {
      for (ArkName name : object.getNames()) {
        names.add(name);
      }

      Integer dinoID1 = object.getPropertyValue("DinoID1", Integer.class);
      if (dinoID1 != null) {
        Integer dinoID2 = object.getPropertyValue("DinoID2", Integer.class);
        if (dinoID2 != null) {
          long id = (long) dinoID1 << Integer.SIZE | (dinoID2 & 0xFFFFFFFFL);
          dinoIDs.add(id);
          continue;
        }
      }

      StructPropertyList itemID = object.getPropertyValue("ItemId", StructPropertyList.class);
      if (itemID != null) {
        Integer itemID1 = itemID.getPropertyValue("ItemID1", Integer.class);
        Integer itemID2 = itemID.getPropertyValue("ItemID2", Integer.class);
        if (itemID1 != null && itemID2 != null) {
          long id = (long) itemID1 << Integer.SIZE | (itemID2 & 0xFFFFFFFFL);
          itemIDs.add(id);
          continue;
        }
      }
    }

    Random random = new Random();
    for (GameObject object : remappedObjects) {
      checkNames(object, remappedObjects, names, startIndex);

      PropertyInt32 dinoID1 = object.getTypedProperty("DinoID1", PropertyInt32.class);
      if (dinoID1 != null) {
        PropertyInt32 dinoID2 = object.getTypedProperty("DinoID2", PropertyInt32.class);
        if (dinoID2 != null) {
          long id = (long) dinoID1.getValue() << Integer.SIZE | (dinoID2.getValue() & 0xFFFFFFFFL);
          if (dinoIDs.contains(id)) {
            long randomId = random.nextLong();
            while (dinoIDs.contains(randomId)) {
              randomId = random.nextLong();
            }

            dinoIDs.add(randomId);
            dinoID1.setValue((int) (randomId >> 32));
            dinoID2.setValue((int) randomId);
          }
          continue;
        }
      }

      StructPropertyList itemID = object.getPropertyValue("ItemId", StructPropertyList.class);
      if (itemID != null) {
        PropertyInt32 itemID1 = itemID.getTypedProperty("ItemID1", PropertyInt32.class);
        PropertyInt32 itemID2 = itemID.getTypedProperty("ItemID2", PropertyInt32.class);
        if (itemID1 != null && itemID2 != null) {
          long id = (long) itemID1.getValue() << Integer.SIZE | (itemID2.getValue() & 0xFFFFFFFFL);
          if (itemIDs.contains(id)) {
            long randomId = random.nextLong();
            while (itemIDs.contains(randomId)) {
              randomId = random.nextLong();
            }

            itemIDs.add(randomId);
            itemID1.setValue((int) (randomId >> 32));
            itemID2.setValue((int) randomId);
          }
          continue;
        }
      }
    }

    stopwatch.stop("Rewriting properties");

    savegame.getObjects().addAll(remappedObjects);
  }

  private static void importIntoClusterData(PropertyContainer container, ArkContainer importFile, Stopwatch stopwatch) {

    StructPropertyList arkData = container.getPropertyValue("MyArkData", StructPropertyList.class);
    if (arkData == null) {
      arkData = new StructPropertyList(Json.createArrayBuilder().build(), new ArkName("ArkInventoryData"));
      container.getProperties().add(new PropertyStruct("MyArkData", "StructProperty", arkData));
    }

    ArkArrayStruct tamedDinosData = arkData.getPropertyValue("ArkTamedDinosData", ArkArrayStruct.class);
    if (tamedDinosData == null) {
      tamedDinosData = new ArkArrayStruct();
      arkData.getProperties().add(new PropertyArray("ArkTamedDinosData", "ArrayProperty", tamedDinosData, new ArkName("StructProperty")));
    }

    for (GameObject creature : importFile.getObjects()) {
      if (creature.getClassString().contains("_Character_")) {
        CreatureData creatureData = DataManager.getCreature(creature.getClassString());

        if (creatureData == null) {
          System.err.println("Unknown creature class " + creature.getClassString());
          System.exit(1);
          return;
        }

        StructPropertyList creatureStruct = new StructPropertyList(Json.createArrayBuilder().build(), null);

        creatureStruct.getProperties().add(new PropertyStr("DinoClassName", "StrProperty", "Blueprint'" + creatureData.getPackagePath() + "." + creatureData.getBlueprint() + "'"));

        ObjectReference dinoClass = new ObjectReference();
        dinoClass.setObjectType(ObjectReference.TYPE_PATH);
        dinoClass.setObjectString(new ArkName("BlueprintGeneratedClass " + creatureData.getPackagePath() + "." + creatureData.getClassName()));
        creatureStruct.getProperties().add(new PropertyObject("DinoClass", "ObjectProperty", dinoClass));

        creatureStruct.getProperties().add(new PropertyArray("DinoData", "ArrayProperty", importFile.toByteArray(), new ArkName("ByteProperty")));

        String tamedName = creature.findPropertyValue("TamedName", String.class).orElse(creatureData.getName());
        String fullName = tamedName + " - Lvl " + CommonFunctions.getFullLevel(creature, importFile) + " (" + creatureData.getName() + ")";
        creatureStruct.getProperties().add(new PropertyStr("DinoName", "StrProperty", fullName));

        creatureStruct.getProperties().add(new PropertyStr("DinoNameInMap", "StrProperty", creature.getNames().get(0).toString()));

        for (int i = 0; i < AttributeNames.size(); i++) {
          creatureStruct.getProperties().add(new PropertyStr("DinoStats", "StrProperty", i, AttributeNames.get(i)));
        }

        float experience = creature.findPropertyValue("MyCharacterStatusComponent", ObjectReference.class)
            .map(importFile::getObject).map(o -> o.getPropertyValue("ExperiencePoints", Float.class)).orElse(0.0f);
        creatureStruct.getProperties().add(new PropertyFloat("DinoExperiencePoints", "FloatProperty", experience));

        creatureStruct.getProperties().add(new PropertyFloat("Version", "FloatProperty", 2.0f));
        creatureStruct.getProperties().add(new PropertyInt32("DinoID1", "UInt32Property", creature.findPropertyValue("DinoID1", Integer.class).orElse(0)));
        creatureStruct.getProperties().add(new PropertyInt32("DinoID2", "UInt32Property", creature.findPropertyValue("DinoID2", Integer.class).orElse(0)));
        creatureStruct.getProperties().add(new PropertyInt32("UploadTime", "IntProperty", (int) Instant.now().getEpochSecond()));

        tamedDinosData.add(creatureStruct);
      }
    }
  }

  private static void checkNames(GameObject object, List<GameObject> remappedObjects, Set<ArkName> names, int startIndex) {
    Predicate<ArkName> dinoComponentFilter =
        name -> name.toString().startsWith("DinoTamedInventory") || name.toString().startsWith("DinoCharacterStatus");

    Function<ArkName, ArkName> findFreeName = name -> {
      for (int i = 1; i < Integer.MAX_VALUE; i++) {
        ArkName tempName = new ArkName(name.getNameString(), i);
        if (!names.contains(tempName)) {
          return tempName;
        }
      }

      throw new Error("This is insane.");
    };

    // Skip string comparison for items
    if (!object.isItem() && object.getClassString().contains("_Character_")) {
      ArkName name = object.getNames().get(0);
      if (names.contains(name)) {
        ArkName newName = findFreeName.apply(name);
        names.add(newName);
        object.getNames().set(0, newName);

        ObjectReference statusReference = object.getPropertyValue("MyCharacterStatusComponent", ObjectReference.class);

        if (statusReference != null) {
          remappedObjects.get(statusReference.getObjectId() - startIndex).getNames().set(1, newName);
        }

        ObjectReference inventoryReference = object.getPropertyValue("MyInventoryComponent", ObjectReference.class);

        if (inventoryReference != null) {
          remappedObjects.get(inventoryReference.getObjectId() - startIndex).getNames().set(1, newName);
        }
        return;
      }
    } else if (!object.isItem() && dinoComponentFilter.test(object.getClassName())) {
      return;
    } else {
      if (object.getNames().size() > 1) {
        System.err.println("Unhandled Object with more than 1 Name: " + object.getClassString());
        return;
      }

      ArkName name = object.getNames().get(0);
      if (names.contains(name)) {
        ArkName newName = findFreeName.apply(name);
        names.add(newName);
        object.getNames().set(0, newName);
      }
    }
  }

  public static void modify(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 3 || oh.wantsHelp()) {
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    Path fileToRead = Paths.get(params.get(0)).toAbsolutePath();
    Path modificationPath = Paths.get(params.get(1)).toAbsolutePath();
    Path fileToWrite = Paths.get(params.get(2)).toAbsolutePath();

    if (fileToRead.equals(fileToWrite)) {
      System.err.println("save and outfile need to be different paths");
      System.exit(2);
      return;
    }

    try {
      JsonStructure structure = CommonFunctions.readJson(modificationPath.toString());

      if (structure.getValueType() != JsonValue.ValueType.OBJECT) {
        System.err.println("Expected object in " + modificationPath + " but found " + structure.getValueType());
        System.exit(2);
        return;
      }

      ModificationFile modificationFile = new ModificationFile();
      modificationFile.readJson((JsonObject) structure);

      FileFormat fileFormat = FileFormat.fromExtension(fileToRead);

      if (fileFormat == FileFormat.CLUSTER) {
        ArkCloudInventory cloudInventory = new ArkCloudInventory(fileToRead.toString(), oh.readingOptions());

        int modifications = modifyClusterData(cloudInventory, modificationFile);
        if (!oh.isQuiet()) {
          System.out.println("Modifications done: " + modifications);
        }

        cloudInventory.writeBinary(fileToWrite.toString(), oh.writingOptions());
      } else if (fileFormat == FileFormat.LOCALPROFILE) {
        ArkLocalProfile localInventory = new ArkLocalProfile(fileToRead.toString(), oh.readingOptions());

        int modifications = modifyClusterData(localInventory, modificationFile);
        if (!oh.isQuiet()) {
          System.out.println("Modifications done: " + modifications);
        }

        localInventory.writeBinary(fileToWrite.toString(), oh.writingOptions());
      } else if (fileFormat == FileFormat.MAP) {
        ArkSavegame savegame = new ArkSavegame(fileToRead.toString(), oh.readingOptions());

        int modifications = modifySavegame(savegame, modificationFile);
        if (!oh.isQuiet()) {
          System.out.println("Modifications done: " + modifications);
        }

        savegame.writeBinary(fileToWrite.toString(), oh.writingOptions());
      } else {
        System.err.println("Modifying " + fileFormat + " is not yet implemented.");
        System.exit(1);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static final Pattern BLUEPRINT_PATTERN = Pattern.compile("Blueprint'([^']+)'");

  private static int modifyClusterData(PropertyContainer container, ModificationFile modificationFile) {

    PropertyContainer arkData = container.getPropertyValue("MyArkData", PropertyContainer.class);

    int modifications = 0;

    ArkArrayStruct tamedDinosData = arkData.getPropertyValue("ArkTamedDinosData", ArkArrayStruct.class);
    if (tamedDinosData != null) {
      for (Struct dinoStruct : tamedDinosData) {
        PropertyContainer dino = (PropertyContainer) dinoStruct;

        String dinoClassName = dino.getPropertyValue("DinoClassName", String.class);
        String replacementName = modificationFile.remapDinoClassName.get(dinoClassName);

        if (replacementName != null) {
          modifications++;
          dino.getTypedProperty("DinoClassName", PropertyStr.class).setValue(replacementName);

          // Guess the correct name
          Matcher matcher = BLUEPRINT_PATTERN.matcher(replacementName);
          if (matcher.matches()) {
            String blueprintGeneratedClass = matcher.group(1);
            ObjectReference dinoClass = dino.getPropertyValue("DinoClass", ObjectReference.class);
            dinoClass.setObjectType(ObjectReference.TYPE_PATH);
            dinoClass.setObjectString(new ArkName("BlueprintGeneratedClass " + blueprintGeneratedClass + "_C"));
          }
        }
      }
    }

    ArkArrayStruct arkItems = arkData.getPropertyValue("ArkItems", ArkArrayStruct.class);
    if (arkItems != null) {
      for (Struct itemStruct : arkItems) {
        PropertyContainer item = (PropertyContainer) itemStruct;
        PropertyContainer netItem = item.getPropertyValue("ArkTributeItem", PropertyContainer.class);

        ObjectReference archetypeReference = netItem.getPropertyValue("ItemArchetype", ObjectReference.class);
        ArkName newArchetype = modificationFile.remapItemArchetype.get(archetypeReference.getObjectString());

        if (newArchetype != null) {
          modifications++;
          archetypeReference.setObjectString(newArchetype);
        }
      }
    }

    if (!modificationFile.addItems.isEmpty()) {
      if (arkItems == null) {
        arkItems = new ArkArrayStruct();
        arkData.getProperties().add(new PropertyArray("ArkItems", "ArrayProperty", arkItems, new ArkName("StructProperty")));
        modifications++;
      }

      for (ArkItem item : modificationFile.addItems) {
        StructPropertyList itemData = item.toClusterData();
        if (itemData != null) {
          arkItems.add(itemData);
          modifications++;
        }
      }
    }

    return modifications;
  }

  private static int modifySavegame(ArkSavegame savegame, ModificationFile modificationFile) {
    int modifications = 0;
    Map<GameObject, List<ArkItem>> replaceDefaultInventories = new HashMap<>();
    Map<GameObject, List<ArkItem>> replaceInventories = new HashMap<>();
    Map<GameObject, List<ArkItem>> addDefaultInventories = new HashMap<>();
    Map<GameObject, List<ArkItem>> addInventories = new HashMap<>();
    ObjectCollector collector = new ObjectCollector(savegame);

    BiFunction<Map<ArkName, List<ArkItem>>, GameObject, List<ArkItem>> mapChecker = (map, object) -> {
      if (map.containsKey(object.getClassName())) {
        return map.get(object.getClassName());
      }
      if (object.getClassString().contains("InventoryComponent") && object.getNames().size() == 2 && map.containsKey(object.getNames().get(1))) {
        return map.get(object.getNames().get(1));
      }
      return null;
    };

    for (GameObject object : savegame.getObjects()) {
      List<ArkItem> replaceDefault = mapChecker.apply(modificationFile.replaceDefaultInventoriesMap, object);
      List<ArkItem> addDefault = mapChecker.apply(modificationFile.addDefaultInventoriesMap, object);

      if (replaceDefault != null) {
        replaceDefaultInventories.put(object, replaceDefault);
      } else if (addDefault != null) {
        addDefaultInventories.put(object, addDefault);
      }

      List<ArkItem> replace = mapChecker.apply(modificationFile.replaceInventoriesMap, object);
      List<ArkItem> add = mapChecker.apply(modificationFile.addInventoriesMap, object);

      if (replace != null) {
        replaceInventories.put(object, replace);
      } else if (add != null) {
        addInventories.put(object, add);
      }
    }

    for (GameObject inventory : replaceDefaultInventories.keySet()) {
      int defaultItemCount = inventory.findPropertyValue("DisplayDefaultItemInventoryCount", Integer.class).orElse(0);
      ArkArrayObjectReference inventoryItems = inventory.getPropertyValue("InventoryItems", ArkArrayObjectReference.class);

      // Broken inventory
      if (inventoryItems == null && defaultItemCount > 0) {
        continue;
      }

      modifications += defaultItemCount;
      for (int i = 0; i < defaultItemCount; i++) {
        collector.remove(inventoryItems.get(i).getObjectId());
      }

      ArkArrayObjectReference newInventoryItems = new ArkArrayObjectReference();

      for (ArkItem newItem : replaceDefaultInventories.get(inventory)) {
        ObjectReference newItemReference = new ObjectReference();
        newItemReference.setLength(8);
        newItemReference.setObjectType(ObjectReference.TYPE_ID);
        newItemReference.setObjectId(collector.add(newItem.toGameObject(collector.getMappedObjects().values(), inventory.getId())));
        newInventoryItems.add(newItemReference);
      }

      int newDefaultItemCount = newInventoryItems.size();
      modifications += newDefaultItemCount;
      // Add all non-default items
      if (defaultItemCount > 0) {
        newInventoryItems.addAll(inventoryItems.subList(defaultItemCount, inventoryItems.size()));
      }

      PropertyInt32 displayDefaultItemInventoryCount = inventory.getTypedProperty("DisplayDefaultItemInventoryCount", PropertyInt32.class);
      if (displayDefaultItemInventoryCount != null) {
        displayDefaultItemInventoryCount.setValue(newDefaultItemCount);
      } else {
        inventory.getProperties().add(new PropertyInt32("DisplayDefaultItemInventoryCount", "IntProperty", newDefaultItemCount));
      }

      PropertyArray inventoryItemsProperty = inventory.getTypedProperty("InventoryItems", PropertyArray.class);
      if (inventoryItemsProperty != null) {
        inventoryItemsProperty.setValue(newInventoryItems);
      } else {
        inventory.getProperties().add(new PropertyArray("InventoryItems", "ArrayProperty", newInventoryItems, new ArkName("ObjectProperty")));
      }
    }

    for (GameObject inventory : addDefaultInventories.keySet()) {
      int defaultItemCount = inventory.findPropertyValue("DisplayDefaultItemInventoryCount", Integer.class).orElse(0);
      ArkArrayObjectReference inventoryItems = inventory.getPropertyValue("InventoryItems", ArkArrayObjectReference.class);

      if (inventoryItems == null) {
        inventoryItems = new ArkArrayObjectReference();
        inventory.getProperties().add(new PropertyArray("InventoryItems", "ArrayProperty", inventoryItems, new ArkName("ObjectProperty")));
      }

      for (ArkItem newItem : addDefaultInventories.get(inventory)) {
        ObjectReference newItemReference = new ObjectReference();
        newItemReference.setLength(8);
        newItemReference.setObjectType(ObjectReference.TYPE_ID);
        newItemReference.setObjectId(collector.add(newItem.toGameObject(collector.getMappedObjects().values(), inventory.getId())));
        inventoryItems.add(defaultItemCount, newItemReference);
        modifications++;
        defaultItemCount++;
      }

      PropertyInt32 displayDefaultItemInventoryCount = inventory.getTypedProperty("DisplayDefaultItemInventoryCount", PropertyInt32.class);
      if (displayDefaultItemInventoryCount != null) {
        displayDefaultItemInventoryCount.setValue(defaultItemCount);
      } else {
        inventory.getProperties().add(new PropertyInt32("DisplayDefaultItemInventoryCount", "IntProperty", defaultItemCount));
      }
    }

    for (GameObject inventory : replaceInventories.keySet()) {
      int defaultItemCount = inventory.findPropertyValue("DisplayDefaultItemInventoryCount", Integer.class).orElse(0);
      ArkArrayObjectReference inventoryItems = inventory.getPropertyValue("InventoryItems", ArkArrayObjectReference.class);

      // Broken inventory
      if (inventoryItems == null && defaultItemCount > 0) {
        continue;
      }

      if (inventoryItems == null) {
        inventoryItems = new ArkArrayObjectReference();
        inventory.getProperties().add(new PropertyArray("InventoryItems", "ArrayProperty", inventoryItems, new ArkName("ObjectProperty")));
      }

      modifications += inventoryItems.size() - defaultItemCount;
      for (int i = inventoryItems.size() - 1; i >= defaultItemCount; i--) {
        collector.remove(inventoryItems.get(i).getObjectId());
        inventoryItems.remove(i);
      }

      for (ArkItem newItem : replaceInventories.get(inventory)) {
        ObjectReference newItemReference = new ObjectReference();
        newItemReference.setLength(8);
        newItemReference.setObjectType(ObjectReference.TYPE_ID);
        newItemReference.setObjectId(collector.add(newItem.toGameObject(collector.getMappedObjects().values(), inventory.getId())));
        inventoryItems.add(newItemReference);
        modifications++;
      }
    }

    for (GameObject inventory : addInventories.keySet()) {
      ArkArrayObjectReference inventoryItems = inventory.getPropertyValue("InventoryItems", ArkArrayObjectReference.class);

      if (inventoryItems == null) {
        inventoryItems = new ArkArrayObjectReference();
        inventory.getProperties().add(new PropertyArray("InventoryItems", "ArrayProperty", inventoryItems, new ArkName("ObjectProperty")));
      }

      for (ArkItem newItem : addInventories.get(inventory)) {
        ObjectReference newItemReference = new ObjectReference();
        newItemReference.setLength(8);
        newItemReference.setObjectType(ObjectReference.TYPE_ID);
        newItemReference.setObjectId(collector.add(newItem.toGameObject(collector.getMappedObjects().values(), inventory.getId())));
        inventoryItems.add(newItemReference);
        modifications++;
      }
    }

    savegame.setObjects(collector.remap(0));

    return modifications;
  }

}
