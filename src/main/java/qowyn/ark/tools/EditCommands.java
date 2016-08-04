package qowyn.ark.tools;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import qowyn.ark.ArkSavegame;
import qowyn.ark.GameObject;
import qowyn.ark.properties.PropertyFloat;

public class EditCommands {

  public static void feed(String[] args) {
    if (args.length != 2) {
      System.out.println("Usage: feed <save> <newsave>");
      return;
    }

    Path fileToRead = Paths.get(args[0]).toAbsolutePath();
    Path fileToWrite = Paths.get(args[1]).toAbsolutePath();

    if (fileToRead.equals(fileToWrite)) {
      System.out.println("save and newsave need to be different paths");
      return;
    }

    try {
      ArkSavegame savegame = new ArkSavegame(fileToRead.toString());

      savegame.getObjects().parallelStream().filter(CommonFunctions::onlyTamed).forEach(object -> {
        PropertyFloat currentFood = object.getTypedProperty("CurrentStatusValues", PropertyFloat.class, 4);
        if (currentFood != null) {
          currentFood.setValue(1000000.0f);
        }
      });

      savegame.writeBinary(fileToWrite.toString());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static void exportThing(String[] args) {
    if (args.length != 4) {
      System.out.println("Usage: export dino <save> <name> <outfile>");
      System.out.println("       export object <save> <id> <outfile>");
      return;
    }
    
    String thing = args[0];
    if (!thing.equals("dino") && !thing.equals("object")) {
      System.out.println("Usage: export dino <save> <name> <outfile>");
      System.out.println("       export object <save> <id> <outfile>");
      return;
    }

    Path fileToRead = Paths.get(args[1]).toAbsolutePath();
    Path fileToWrite = Paths.get(args[3]).toAbsolutePath();

    if (fileToRead.equals(fileToWrite)) {
      System.out.println("save and outfile need to be different paths");
      return;
    }

    try {
      ArkSavegame savegame = new ArkSavegame(fileToRead.toString());

      ObjectCollector collector = null;
      if (thing.equals("dino")) {
        String name = args[2];
        
        for (GameObject go: savegame.getObjects()) {
          String objectName = go.getPropertyValue("TamedName", String.class);
          if (name.equals(objectName)) {
            collector = new ObjectCollector(savegame, go);
            break;
          }
        }
        
        if (collector == null) {
          System.err.println("Could not find a dino named " + args[2]);
          return;
        }
        
      } else if (thing.equals("object")) {
        int id = Integer.parseInt(args[2]);
        
        collector = new ObjectCollector(savegame, savegame.getObjects().get(id));
      }
      
      ArkSavegame export = new ArkSavegame();
      
      export.setSaveVersion(savegame.getSaveVersion());
      export.setObjects(collector.remap(0));
      
      CommonFunctions.writeJson(fileToWrite.toString(), export.toJson());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
  
  public static void importThing(String[] args) {
    if (args.length != 3) {
      System.out.println("Usage: import <save> <jsonFile> <outfile>");
      return;
    }

    Path fileToRead = Paths.get(args[0]).toAbsolutePath();
    Path fileToWrite = Paths.get(args[2]).toAbsolutePath();

    if (fileToRead.equals(fileToWrite)) {
      System.out.println("save and outfile need to be different paths");
      return;
    }
    
    try {
      ArkSavegame savegame = new ArkSavegame(fileToRead.toString());
      ArkSavegame jsonFile = new ArkSavegame(CommonFunctions.readJson(args[1]));
      
      ObjectCollector collector = new ObjectCollector(jsonFile, jsonFile.getObjects().get(0));
      
      savegame.getObjects().addAll(collector.remap(savegame.getObjects().size()));
      
      savegame.writeBinary(fileToWrite.toString());
      
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
