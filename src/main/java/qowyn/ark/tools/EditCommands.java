package qowyn.ark.tools;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import qowyn.ark.ArkSavegame;
import qowyn.ark.GameObject;
import qowyn.ark.properties.PropertyDouble;
import qowyn.ark.properties.PropertyFloat;
import qowyn.ark.properties.PropertyInt32;
import qowyn.ark.structs.StructPropertyList;
import qowyn.ark.types.ArkName;
import qowyn.ark.types.ObjectReference;

public class EditCommands {

  public static void feed(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 2 || oh.wantsHelp()) {
      System.out.println("Feeds all dinos in 'save' by setting their current food value to 1,000,000 and bringing them from the past to the present. Mainly useful if you left your server running with no players online.");
      System.out.println("Usage: ark-tools feed <save> <newsave> [options]");
      oh.printHelp();
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

      savegame.getObjects().parallelStream().filter(CommonFunctions::onlyTamed).forEach(object -> {
        PropertyFloat currentFood = object.getTypedProperty("CurrentStatusValues", PropertyFloat.class, 4);
        if (currentFood != null) {
          currentFood.setValue(1000000.0f);
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
    OptionSpec<String> dinoSpec = oh.accepts("dino", "Export dino by Name").withRequiredArg();
    OptionSpec<Integer> objectSpec = oh.accepts("object", "Export object by Id").withRequiredArg().ofType(Integer.class);

    OptionSet options = oh.reparse();

    List<String> params = oh.getParams(options);

    if (params.size() != 2 || oh.wantsHelp() || !(options.has(dinoSpec) || options.has(objectSpec))) {
      System.out.println("Export a specified object/dino and everything attached to it. Can be used to 'revive' dinos from backups or to import bases from another save file. Manually editing exported file might be required.");
      System.out.println("Usage: ark-tools export <save> --dino <name> <outfile> [options]");
      System.out.println("       ark-tools export <save> --object <id> <outfile> [options]");
      oh.printHelp();
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
      if (options.has(dinoSpec)) {
        String name = dinoSpec.value(options);

        for (GameObject go : savegame.getObjects()) {
          String objectName = go.getPropertyValue("TamedName", String.class);
          if (name.equals(objectName)) {
            collector = new ObjectCollector(savegame, go);
            break;
          }
        }

        if (collector == null) {
          System.err.println("Could not find a dino named " + name);
          System.exit(2);
          return;
        }

      } else if (options.has(objectSpec)) {
        int id = objectSpec.value(options);

        if (id < 0 || id >= savegame.getObjects().size()) {
          System.err.println("Invalid object id " + id);
          System.exit(2);
          return;
        }

        collector = new ObjectCollector(savegame, savegame.getObjects().get(id));
      } else {
        // Makes FindBugs happy
        return;
      }

      stopwatch.stop("Collecting");

      ArkSavegame export = new ArkSavegame();

      export.setSaveVersion(savegame.getSaveVersion());
      export.setObjects(collector.remap(0));

      stopwatch.stop("Remapping");

      CommonFunctions.writeJson(fileToWrite.toString(), g -> export.writeJson(g, oh.writingOptions()));

      stopwatch.stop("Writing");

      stopwatch.print();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void importThing(OptionHandler oh) {
    List<String> params = oh.getParams();
    if (params.size() != 3 || oh.wantsHelp()) {
      System.out.println("Imports all objects from specified 'jsonFile'.");
      System.out.println("Usage: ark-tools import <save> <jsonFile> <outfile> [options]");
      oh.printHelp();
      System.exit(1);
      return;
    }

    Path fileToRead = Paths.get(params.get(0)).toAbsolutePath();
    Path fileToWrite = Paths.get(params.get(2)).toAbsolutePath();

    if (fileToRead.equals(fileToWrite)) {
      System.out.println("save and outfile need to be different paths");
      System.exit(2);
      return;
    }

    try {
      Stopwatch stopwatch = new Stopwatch(oh.useStopwatch());

      ArkSavegame savegame = new ArkSavegame(fileToRead.toString(), oh.readingOptions());
      stopwatch.stop("Reading save");

      ArkSavegame jsonFile = new ArkSavegame(CommonFunctions.readJson(params.get(1)), oh.readingOptions());
      stopwatch.stop("Reading import container");

      ObjectCollector collector = new ObjectCollector(jsonFile, jsonFile.getObjects().get(0));

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

      savegame.writeBinary(fileToWrite.toString(), oh.writingOptions());

      stopwatch.stop("Writing");
      stopwatch.print();

    } catch (IOException e) {
      throw new RuntimeException(e);
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

}
