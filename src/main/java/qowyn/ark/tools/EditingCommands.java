package qowyn.ark.tools;

import static qowyn.ark.tools.CommonFunctions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;

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
import qowyn.ark.properties.Property;
import qowyn.ark.properties.PropertyArray;
import qowyn.ark.properties.PropertyDouble;
import qowyn.ark.properties.PropertyFloat;
import qowyn.ark.properties.PropertyInt;
import qowyn.ark.properties.PropertyObject;
import qowyn.ark.properties.PropertyStr;
import qowyn.ark.properties.PropertyStruct;
import qowyn.ark.properties.PropertyUInt32;
import qowyn.ark.structs.Struct;
import qowyn.ark.structs.StructPropertyList;
import qowyn.ark.tools.data.Item;
import qowyn.ark.tools.modification.DeleteOperation;
import qowyn.ark.tools.modification.ModificationFile;
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

      ArkSavegame savegame = new ArkSavegame(fileToRead, oh.readingOptions());

      stopwatch.stop("Reading");

      savegame.getObjects().parallelStream().filter(CommonFunctions::isTamed).forEach(object -> {
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

      savegame.writeBinary(fileToWrite, oh.writingOptions());

      stopwatch.stop("Writing");

      stopwatch.print();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void exportThing(OptionHandler oh) {
    OptionSpec<String> creatureSpec = oh.accepts("creature", "Export creature by Name - required if other options not set").withRequiredArg().describedAs("name");
    OptionSpec<Long> dinoIdSpec = oh.accepts("dinoid", "Export creature by DinoID - required if other options not set").withRequiredArg().ofType(Long.class).describedAs("id");
    OptionSpec<Integer> objectSpec = oh.accepts("object", "Export object by index - required if other options not set").withRequiredArg().ofType(Integer.class).describedAs("index");
    OptionSpec<String> classSpec = oh.accepts("class", "Export objects by class - required if other options not set").withRequiredArg().describedAs("class");

    OptionSpec<Void> withoutReferences = oh.accepts("without-references", "Do not grab additional objects by following references");
    OptionSpec<Void> withoutComponents = oh.accepts("without-components", "Do not grab additional objects by traversing components");

    OptionSet options = oh.reparse();

    List<String> params = oh.getParams(options);

    if (params.size() != 2 || oh.wantsHelp() || !(options.has(creatureSpec) || options.has(objectSpec) || options.has(dinoIdSpec) || options.has(classSpec))) {
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

      ArkSavegame savegame = new ArkSavegame(fileToRead, oh.readingOptions());

      stopwatch.stop("Loading");

      ObjectCollector collector = null;
      if (options.has(creatureSpec)) {
        String name = creatureSpec.value(options);

        for (GameObject go : savegame.getObjects()) {
          String objectName = go.getPropertyValue("TamedName", String.class);
          if (name.equals(objectName)) {
            collector = new ObjectCollector(savegame, go, !options.has(withoutReferences), !options.has(withoutComponents));
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
                collector = new ObjectCollector(savegame, creature, !options.has(withoutReferences), !options.has(withoutComponents));
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

        collector = new ObjectCollector(savegame, savegame.getObjects().get(index), !options.has(withoutReferences), !options.has(withoutComponents));
      } else if (options.has(classSpec)) {
        collector = new ObjectCollector(savegame, ArkName.from(options.valueOf(classSpec)), !options.has(withoutReferences), !options.has(withoutComponents));
      } else {
        // Makes FindBugs happy
        return;
      }

      stopwatch.stop("Collecting");

      ArkContainer export = new ArkContainer();

      export.getObjects().addAll(collector.remap(0));

      stopwatch.stop("Remapping");

      CommonFunctions.writeJson(fileToWrite, export::writeJson, oh);

      stopwatch.stop("Writing");

      stopwatch.print();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static final ArkName CLOUD_INVENTORY_CLASS = ArkName.constantPlain("ArkCloudInventoryData");

  private static final ArkName CLOUD_INVENTORY_NAME = ArkName.constant("ArkCloudInventoryData", 42);

  public static void importThing(OptionHandler oh) {
    OptionSpec<String> fileFormatSpec = oh.accepts("file-format", "Select format of input and output files.").withRequiredArg().describedAs(FileFormat.FILE_FORMAT_DESCRIBE);

    OptionSet options = oh.reparse();

    List<String> params = oh.getParams(options);
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

      FileFormat fileFormat = options.has(fileFormatSpec) ? FileFormat.valueOf(options.valueOf(fileFormatSpec).toUpperCase()) : FileFormat.fromExtension(fileToRead);

      ArkContainer jsonFile = new ArkContainer(CommonFunctions.readJson(Paths.get(params.get(1))));
      stopwatch.stop("Reading import container");

      if (fileFormat == FileFormat.MAP) {
        ArkSavegame savegame = new ArkSavegame(fileToRead, oh.readingOptions());
        stopwatch.stop("Reading save");

        importIntoSavegame(savegame, jsonFile, stopwatch);

        savegame.writeBinary(fileToWrite, oh.writingOptions());

        stopwatch.stop("Writing");
      } else if (fileFormat == FileFormat.CLUSTER) {
        DataManager.loadData(oh.lang());
        ArkCloudInventory cloudInventory;
        if (Files.exists(fileToRead) && Files.size(fileToRead) > 0) {
          cloudInventory = new ArkCloudInventory(fileToRead, oh.readingOptions());
        } else {
          cloudInventory = new ArkCloudInventory();
          cloudInventory.setInventoryVersion(4);
          cloudInventory.setInventoryData(new GameObject());
          cloudInventory.getInventoryData().setClassName(CLOUD_INVENTORY_CLASS);
          cloudInventory.getInventoryData().setItem(true);
          cloudInventory.getInventoryData().getNames().add(CLOUD_INVENTORY_NAME);
          cloudInventory.getInventoryData().setExtraData(new ExtraDataZero());
        }

        importIntoClusterData(cloudInventory, jsonFile, stopwatch);

        cloudInventory.writeBinary(fileToWrite, oh.writingOptions());

        stopwatch.stop("Writing");
      } else if (fileFormat == FileFormat.LOCALPROFILE) {
        DataManager.loadData(oh.lang());
        ArkLocalProfile localInventory = new ArkLocalProfile(fileToRead, oh.readingOptions());

        importIntoClusterData(localInventory, jsonFile, stopwatch);

        localInventory.writeBinary(fileToWrite, oh.writingOptions());

        stopwatch.stop("Writing");
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

      PropertyInt dinoID1 = object.getTypedProperty("DinoID1", PropertyInt.class);
      if (dinoID1 != null) {
        PropertyInt dinoID2 = object.getTypedProperty("DinoID2", PropertyInt.class);
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
        PropertyInt itemID1 = itemID.getTypedProperty("ItemID1", PropertyInt.class);
        PropertyInt itemID2 = itemID.getTypedProperty("ItemID2", PropertyInt.class);
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

  private static final ArkName ARK_INVENTORY_DATA = ArkName.constantPlain("ArkInventoryData");

  private static void importIntoClusterData(PropertyContainer container, ArkContainer importFile, Stopwatch stopwatch) {

    StructPropertyList arkData = container.findPropertyValue("MyArkData", StructPropertyList.class).orElseGet(() -> {
      StructPropertyList temp = new StructPropertyList();
      container.getProperties().add(new PropertyStruct("MyArkData", temp, ARK_INVENTORY_DATA));
      return temp;
    });

    Supplier<ArkArrayStruct> getTamedDinosData = () -> {
      ArkArrayStruct tamedDinosData = arkData.getPropertyValue("ArkTamedDinosData", ArkArrayStruct.class);
      if (tamedDinosData == null) {
        tamedDinosData = new ArkArrayStruct();
        arkData.getProperties().add(new PropertyArray("ArkTamedDinosData", tamedDinosData));
      }

      return tamedDinosData;
    };

    Supplier<ArkArrayStruct> getArkItems = () -> {
      ArkArrayStruct arkItems = arkData.getPropertyValue("ArkItems", ArkArrayStruct.class);
      if (arkItems == null) {
        arkItems = new ArkArrayStruct();
        arkData.getProperties().add(new PropertyArray("ArkItems", arkItems));
      }

      return arkItems;
    };

    for (GameObject object : importFile.getObjects()) {
      if (isCreature(object)) {
        CreatureData creatureData = DataManager.getCreature(object.getClassString());

        if (creatureData == null) {
          System.err.println("Unknown creature class " + object.getClassString());
          System.exit(1);
          return;
        }

        StructPropertyList creatureStruct = new StructPropertyList();
        List<Property<?>> creatureProperties = creatureStruct.getProperties();

        creatureProperties.add(new PropertyStr("DinoClassName", "Blueprint'" + creatureData.getPackagePath() + "." + creatureData.getBlueprint() + "'"));

        ObjectReference dinoClass = new ObjectReference();
        dinoClass.setObjectType(ObjectReference.TYPE_PATH);
        dinoClass.setObjectString(ArkName.from("BlueprintGeneratedClass " + creatureData.getPackagePath() + "." + creatureData.getClassName()));
        creatureProperties.add(new PropertyObject("DinoClass", dinoClass));

        creatureProperties.add(new PropertyArray("DinoData", importFile.toByteArray()));

        String tamedName = object.findPropertyValue("TamedName", String.class).orElse(creatureData.getName());
        String fullName = tamedName + " - Lvl " + CommonFunctions.getFullLevel(object, importFile) + " (" + creatureData.getName() + ")";
        creatureProperties.add(new PropertyStr("DinoName", fullName));

        creatureProperties.add(new PropertyStr("DinoNameInMap", object.getNames().get(0).toString()));

        for (int i = 0; i < AttributeNames.size(); i++) {
          creatureProperties.add(new PropertyStr("DinoStats", i, AttributeNames.get(i)));
        }

        float experience = object.findPropertyValue("MyCharacterStatusComponent", ObjectReference.class)
            .map(importFile::getObject).map(o -> o.getPropertyValue("ExperiencePoints", Float.class)).orElse(0.0f);
        creatureProperties.add(new PropertyFloat("DinoExperiencePoints", experience));

        creatureProperties.add(new PropertyFloat("Version", 2.0f));
        creatureProperties.add(new PropertyUInt32("DinoID1", object.findPropertyValue("DinoID1", Integer.class).orElse(0)));
        creatureProperties.add(new PropertyUInt32("DinoID2", object.findPropertyValue("DinoID2", Integer.class).orElse(0)));
        creatureProperties.add(new PropertyInt("UploadTime", (int) Instant.now().getEpochSecond()));

        ArkArrayStruct tamedDinosData = getTamedDinosData.get();
        tamedDinosData.add(creatureStruct);
      } else if (object.isItem()) {
        Item item = new Item(object);

        ArkArrayStruct arkItems = getArkItems.get();
        arkItems.add(item.toClusterData());
      }
    }
  }

  private static void checkNames(GameObject object, List<GameObject> remappedObjects, Set<ArkName> names, int startIndex) {
    Predicate<ArkName> dinoComponentFilter =
        name -> name.toString().startsWith("DinoTamedInventory") || name.toString().startsWith("DinoCharacterStatus");

    Function<ArkName, ArkName> findFreeName = name -> {
      for (int i = 1; i < Integer.MAX_VALUE; i++) {
        ArkName tempName = ArkName.from(name.getName(), i);
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
    OptionSpec<Void> requireEmptySpec = oh.accepts("require-empty", "Refuse to modify non-empty cluster files. Return exit code 3 for non-empty files.");
    OptionSpec<String> fileFormatSpec = oh.accepts("file-format", "Select format of input and output files.").withRequiredArg().describedAs(FileFormat.FILE_FORMAT_DESCRIBE);

    OptionSet options = oh.reparse();

    List<String> params = oh.getParams(options);
    if (params.size() != 3 || oh.wantsHelp()) {
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    Path fileToRead = Paths.get(params.get(0)).toAbsolutePath();
    Path modificationPath = Paths.get(params.get(1)).toAbsolutePath();
    Path fileToWrite = Paths.get(params.get(2)).toAbsolutePath();

    try {
      JsonNode node = CommonFunctions.readJson(modificationPath);

      if (!node.isObject()) {
        System.err.println("Expected object in " + modificationPath + " but found " + node.getNodeType());
        System.exit(2);
        return;
      }

      ModificationFile modificationFile = new ModificationFile();
      modificationFile.readJson(node);

      FileFormat fileFormat = options.has(fileFormatSpec) ? FileFormat.valueOf(options.valueOf(fileFormatSpec)) : FileFormat.fromExtension(fileToRead);

      if (fileFormat == FileFormat.CLUSTER) {
        ArkCloudInventory cloudInventory;
        if (Files.exists(fileToRead) && Files.size(fileToRead) > 0) {
          if (options.has(requireEmptySpec)) {
            System.exit(3);
            return;
          }
          cloudInventory = new ArkCloudInventory(fileToRead, oh.readingOptions().buildComponentTree(true));
        } else {
          cloudInventory = new ArkCloudInventory();
          cloudInventory.setInventoryVersion(4);
          cloudInventory.setInventoryData(new GameObject());
          cloudInventory.getInventoryData().setClassName(CLOUD_INVENTORY_CLASS);
          cloudInventory.getInventoryData().setItem(true);
          cloudInventory.getInventoryData().setNames(new ArrayList<>());
          cloudInventory.getInventoryData().getNames().add(CLOUD_INVENTORY_NAME);
          cloudInventory.getInventoryData().setExtraData(new ExtraDataZero());
        }

        int modifications = modifyClusterData(cloudInventory, modificationFile);
        if (!oh.isQuiet()) {
          System.out.println("Modifications done: " + modifications);
        }

        if (options.has(requireEmptySpec) && fileToWrite.equals(fileToRead) && Files.exists(fileToRead) && Files.size(fileToRead) > 0) {
          System.exit(3);
          return;
        }
        cloudInventory.writeBinary(fileToWrite, oh.writingOptions());
      } else if (fileFormat == FileFormat.LOCALPROFILE) {
        ArkLocalProfile localInventory = new ArkLocalProfile(fileToRead, oh.readingOptions().buildComponentTree(true));

        int modifications = modifyClusterData(localInventory, modificationFile);
        if (!oh.isQuiet()) {
          System.out.println("Modifications done: " + modifications);
        }

        localInventory.writeBinary(fileToWrite, oh.writingOptions());
      } else if (fileFormat == FileFormat.MAP) {
        ArkSavegame savegame = new ArkSavegame(fileToRead, oh.readingOptions().buildComponentTree(true));

        int modifications = modifySavegame(savegame, modificationFile);
        if (!oh.isQuiet()) {
          System.out.println("Modifications done: " + modifications);
        }

        savegame.writeBinary(fileToWrite, oh.writingOptions());
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

    StructPropertyList arkData = container.getPropertyValue("MyArkData", StructPropertyList.class);
    if (arkData == null) {
      arkData = new StructPropertyList();
      container.getProperties().add(new PropertyStruct("MyArkData", arkData, ARK_INVENTORY_DATA));
    }

    int modifications = 0;

    ArkArrayStruct tamedDinosData = arkData.getPropertyValue("ArkTamedDinosData", ArkArrayStruct.class);
    if (tamedDinosData != null) {
      Iterator<Struct> iter = tamedDinosData.iterator();

      while (iter.hasNext()) {
        PropertyContainer dino = (PropertyContainer) iter.next();

        String dinoClassName = dino.getPropertyValue("DinoClassName", String.class);

        boolean removed = false;
        for (DeleteOperation delete: modificationFile.deleteOperations) {
          if (delete.checkAndDecrease(ArkName.from(dinoClassName))) {
            modifications++;
            iter.remove();
            removed = true;
            break;
          }
        }

        if (removed) {
          continue;
        }

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
            dinoClass.setObjectString(ArkName.from("BlueprintGeneratedClass " + blueprintGeneratedClass + "_C"));
          }
        }
      }
    }

    ArkArrayStruct arkItems = arkData.getPropertyValue("ArkItems", ArkArrayStruct.class);
    if (arkItems != null) {
      Iterator<Struct> iter = arkItems.iterator();

      while (iter.hasNext()) {
        PropertyContainer item = (PropertyContainer) iter.next();
        PropertyContainer netItem = item.getPropertyValue("ArkTributeItem", PropertyContainer.class);

        ObjectReference archetypeReference = netItem.getPropertyValue("ItemArchetype", ObjectReference.class);

        boolean removed = false;
        for (DeleteOperation delete: modificationFile.deleteOperations) {
          if (delete.checkAndDecrease(archetypeReference.getObjectString())) {
            modifications++;
            iter.remove();
            removed = true;
            break;
          }
        }

        if (removed) {
          continue;
        }

        ArkName newArchetype = modificationFile.remapItemArchetypes.get(archetypeReference.getObjectString());

        if (newArchetype != null) {
          modifications++;
          archetypeReference.setObjectString(newArchetype);
        }
      }
    }

    if (!modificationFile.addItems.isEmpty()) {
      if (arkItems == null) {
        arkItems = new ArkArrayStruct();
        arkData.getProperties().add(new PropertyArray("ArkItems", arkItems));
        modifications++;
      }

      for (Item item : modificationFile.addItems) {
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
    Map<GameObject, List<Item>> replaceDefaultInventories = new LinkedHashMap<>();
    Map<GameObject, List<Item>> replaceInventories = new LinkedHashMap<>();
    Map<GameObject, List<Item>> addDefaultInventories = new LinkedHashMap<>();
    Map<GameObject, List<Item>> addInventories = new LinkedHashMap<>();
    ObjectCollector collector = new ObjectCollector(savegame);

    if (!modificationFile.deleteOperations.isEmpty()) {
      for (GameObject object : savegame) {
        if (object.getParent() != null || object.isItem()) {
          continue;
        }

        for (DeleteOperation delete: modificationFile.deleteOperations) {
          // Skip if object does not match conditions or delete has been used up
          if (!delete.canApplyTo(object)) {
            continue;
          }

          // Stop deleteOperations if object itself got deleted
          if (delete.apply(object, collector)) {
            break;
          }
        }
      }
    }

    BiFunction<Map<ArkName, List<Item>>, GameObject, List<Item>> mapChecker = (map, object) -> {
      if (map.containsKey(object.getClassName())) {
        return map.get(object.getClassName());
      }
      if (object.getClassString().contains("InventoryComponent") && object.getNames().size() == 2 && map.containsKey(object.getNames().get(1))) {
        return map.get(object.getNames().get(1));
      }
      return null;
    };

    for (GameObject object : collector) {
      List<Item> replaceDefault = mapChecker.apply(modificationFile.replaceDefaultInventoriesMap, object);
      List<Item> addDefault = mapChecker.apply(modificationFile.addDefaultInventoriesMap, object);

      if (replaceDefault != null) {
        replaceDefaultInventories.put(object, replaceDefault);
      } else if (addDefault != null) {
        addDefaultInventories.put(object, addDefault);
      }

      List<Item> replace = mapChecker.apply(modificationFile.replaceInventoriesMap, object);
      List<Item> add = mapChecker.apply(modificationFile.addInventoriesMap, object);

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

      for (int i = 0; i < defaultItemCount; i++) {
        collector.remove(inventoryItems.get(i).getObjectId());
      }

      ArkArrayObjectReference newInventoryItems = new ArkArrayObjectReference();

      for (Item newItem : replaceDefaultInventories.get(inventory)) {
        ObjectReference newItemReference = new ObjectReference();
        newItemReference.setLength(8);
        newItemReference.setObjectType(ObjectReference.TYPE_ID);
        newItemReference.setObjectId(collector.add(newItem.toGameObject(collector.getMappedObjects().values(), inventory.getId())));
        newInventoryItems.add(newItemReference);
      }

      int newDefaultItemCount = newInventoryItems.size();
      // Add all non-default items
      if (defaultItemCount > 0) {
        newInventoryItems.addAll(inventoryItems.subList(defaultItemCount, inventoryItems.size()));
      }

      PropertyInt displayDefaultItemInventoryCount = inventory.getTypedProperty("DisplayDefaultItemInventoryCount", PropertyInt.class);
      if (displayDefaultItemInventoryCount != null) {
        displayDefaultItemInventoryCount.setValue(newDefaultItemCount);
      } else {
        inventory.getProperties().add(new PropertyInt("DisplayDefaultItemInventoryCount", newDefaultItemCount));
      }

      PropertyArray inventoryItemsProperty = inventory.getTypedProperty("InventoryItems", PropertyArray.class);
      if (inventoryItemsProperty != null) {
        inventoryItemsProperty.setValue(newInventoryItems);
      } else {
        inventory.getProperties().add(new PropertyArray("InventoryItems", newInventoryItems));
      }
    }

    for (GameObject inventory : addDefaultInventories.keySet()) {
      int defaultItemCount = inventory.findPropertyValue("DisplayDefaultItemInventoryCount", Integer.class).orElse(0);
      ArkArrayObjectReference inventoryItems = inventory.getPropertyValue("InventoryItems", ArkArrayObjectReference.class);

      if (inventoryItems == null) {
        inventoryItems = new ArkArrayObjectReference();
        inventory.getProperties().add(new PropertyArray("InventoryItems", inventoryItems));
      }

      for (Item newItem : addDefaultInventories.get(inventory)) {
        ObjectReference newItemReference = new ObjectReference();
        newItemReference.setLength(8);
        newItemReference.setObjectType(ObjectReference.TYPE_ID);
        newItemReference.setObjectId(collector.add(newItem.toGameObject(collector.getMappedObjects().values(), inventory.getId())));
        inventoryItems.add(defaultItemCount, newItemReference);
        defaultItemCount++;
      }

      PropertyInt displayDefaultItemInventoryCount = inventory.getTypedProperty("DisplayDefaultItemInventoryCount", PropertyInt.class);
      if (displayDefaultItemInventoryCount != null) {
        displayDefaultItemInventoryCount.setValue(defaultItemCount);
      } else {
        inventory.getProperties().add(new PropertyInt("DisplayDefaultItemInventoryCount", defaultItemCount));
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
        inventory.getProperties().add(new PropertyArray("InventoryItems", inventoryItems));
      }

      for (int i = inventoryItems.size() - 1; i >= defaultItemCount; i--) {
        collector.remove(inventoryItems.get(i).getObjectId());
        inventoryItems.remove(i);
      }

      for (Item newItem : replaceInventories.get(inventory)) {
        ObjectReference newItemReference = new ObjectReference();
        newItemReference.setLength(8);
        newItemReference.setObjectType(ObjectReference.TYPE_ID);
        newItemReference.setObjectId(collector.add(newItem.toGameObject(collector.getMappedObjects().values(), inventory.getId())));
        inventoryItems.add(newItemReference);
      }
    }

    for (GameObject inventory : addInventories.keySet()) {
      ArkArrayObjectReference inventoryItems = inventory.getPropertyValue("InventoryItems", ArkArrayObjectReference.class);

      if (inventoryItems == null) {
        inventoryItems = new ArkArrayObjectReference();
        inventory.getProperties().add(new PropertyArray("InventoryItems", inventoryItems));
      }

      for (Item newItem : addInventories.get(inventory)) {
        ObjectReference newItemReference = new ObjectReference();
        newItemReference.setLength(8);
        newItemReference.setObjectType(ObjectReference.TYPE_ID);
        newItemReference.setObjectId(collector.add(newItem.toGameObject(collector.getMappedObjects().values(), inventory.getId())));
        inventoryItems.add(newItemReference);
      }
    }

    savegame.getObjects().clear();
    savegame.getObjects().addAll(collector.remap(0));

    return modifications + collector.getAdded() + collector.getDeleted();
  }

}
