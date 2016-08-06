package qowyn.ark.tools;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.json.JsonObject;

import qowyn.ark.ArkSavegame;

public class ConversionCommands {

  public static void binary2json(String[] args) {
    if (args.length != 2) {
      System.out.println("Usage: b2j <save> <outfile>");
      return;
    }

    try {
      System.out.println("This may take some time...");
      Instant start = Instant.now();
      ArkSavegame saveFile = new ArkSavegame(args[0]);
      Instant readFinished = Instant.now();
      CommonFunctions.writeJson(args[1], saveFile::writeJson);
      Instant dumpFinished = Instant.now();

      System.out.println("Read after " + ChronoUnit.MILLIS.between(start, readFinished) + " ms");
      System.out.println("Dumped after " + ChronoUnit.MILLIS.between(readFinished, dumpFinished) + " ms");
      System.out.println("Total time " + ChronoUnit.MILLIS.between(start, dumpFinished) + " ms");
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void json2binary(String[] args) {
    if (args.length != 2) {
      System.out.println("Usage: j2b <json> <outfile>");
      return;
    }

    try {
      System.out.println("This may take some time...");
      Instant start = Instant.now();
      JsonObject object = CommonFunctions.readJson(args[0]);
      Instant json = Instant.now();
      ArkSavegame saveFile = new ArkSavegame(object);
      Instant loaded = Instant.now();
      saveFile.writeBinary(args[1]);
      Instant written = Instant.now();

      System.out.println("Parsed after " + ChronoUnit.MILLIS.between(start, json));
      System.out.println("Loaded after " + ChronoUnit.MILLIS.between(json, loaded));
      System.out.println("Written after " + ChronoUnit.MILLIS.between(loaded, written));
      System.out.println("Total time " + ChronoUnit.MILLIS.between(start, written));
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

}
