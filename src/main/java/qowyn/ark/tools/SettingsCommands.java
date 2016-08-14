package qowyn.ark.tools;

import java.io.IOException;

public class SettingsCommands {

  public static void latlon(OptionHandler oh) {
    if (oh.getParams().size() > 0 || oh.wantsHelp()) {
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    try {
      CommonFunctions.writeJson("latLonCalculator.json", LatLonCalculator.exportList(), oh);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
