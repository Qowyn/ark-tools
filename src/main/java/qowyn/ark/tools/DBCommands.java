package qowyn.ark.tools;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import qowyn.ark.tools.driver.DBDriver;
import qowyn.ark.tools.driver.DBDrivers;

public class DBCommands {

  public static void list(OptionHandler oh) {
    if (oh.getParams().size() > 0 || oh.wantsHelp()) {
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    try {
      DBDrivers.discoverDrivers();
    } catch (ClassNotFoundException | URISyntaxException | IOException e) {
      e.printStackTrace();
    }

    List<String> driverNames = new ArrayList<>(DBDrivers.getDriverNames());

    driverNames.sort(null);

    for (String driverName: driverNames) {
      DBDriver driver = DBDrivers.getDriver(driverName);

      if (!driver.canHandlePath() && driver.getUrlSchemeList().isEmpty()) {
        System.err.println("Warning: Driver " + driverName + " supports neither path nor url schemes, disabling.");
        continue;
      }

      System.out.print(driverName);

      if (driver.canHandlePath()) {
        System.out.print(" - can use path");
      }
      if (!driver.getUrlSchemeList().isEmpty()) {
        if (driver.canHandlePath()) {
          System.out.print(" and scheme" + (driver.getUrlSchemeList().size() > 1 ? "s " : " "));
        } else {
          System.out.print(" - can use scheme" + (driver.getUrlSchemeList().size() > 1 ? "s " : " "));
        }
        System.out.print(driver.getUrlSchemeList().stream().collect(Collectors.joining(", ")));
      }

      System.out.println();
      System.out.println();

      if (!driver.getSupportedParameters().isEmpty()) {
        System.out.println("Supported Parameters");
        for (Entry<String,String> entry: driver.getSupportedParameters().entrySet()) {
          System.out.println("\t" + entry.getKey() + " - " + entry.getValue());
        }
        System.out.println();
      }
    }
    
    DBDrivers.close();
  }

  public static void run(OptionHandler oh){
    if (oh.getParams().size() < 3 || oh.getParams().size() > 4 || oh.wantsHelp()) {
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    try {
      DBDrivers.discoverDrivers();
    } catch (ClassNotFoundException | URISyntaxException | IOException e) {
      e.printStackTrace();
    }

    DBCommands command = new DBCommands();
    command.oh = oh;
    command.run();
    DBDrivers.close();
  }

  private OptionHandler oh;

  public void run() {
    List<String> params = oh.getParams();
    String driverName = params.get(0);
    String savePath = params.get(1);
    String pathOrUri = params.get(2);
    String clusterPath = params.size() == 3 ? null : params.get(3);

    DBDriver driver = DBDrivers.getDriver(driverName);

    URI uri = null;
    Path path = null;

    int firstColon = pathOrUri.indexOf(':');

    if (firstColon > -1) {
      try {
        uri = new URI(pathOrUri);
      } catch (URISyntaxException e) {
        try {
          path = Paths.get(pathOrUri);
        } catch (InvalidPathException ipe) {
          System.err.println("Error: " + pathOrUri + " is not a valid path or uri");
          System.exit(2);
          return;
        }
      }
    } else {
      try {
        path = Paths.get(pathOrUri);
      } catch (InvalidPathException ipe) {
        System.err.println("Error: " + pathOrUri + " is not a valid path or uri");
        System.exit(2);
        return;
      }
    }

    if (path != null) {
      if (!driver.canHandlePath()) {
        System.err.println("Error: Driver " + driverName + " cannot handle path " + pathOrUri);
        System.exit(2);
        return;
      }

      driver.openConnection(path);
    } else {
      if (!driver.getUrlSchemeList().contains(uri.getScheme())) {
        System.err.println("Error: Driver " + driverName + " cannot handle scheme " + pathOrUri);
        System.exit(2);
        return;
      }

      driver.openConnection(uri);
    }

    
  }

}
