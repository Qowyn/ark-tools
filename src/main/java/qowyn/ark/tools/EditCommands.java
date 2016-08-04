package qowyn.ark.tools;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import qowyn.ark.ArkSavegame;
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

}
