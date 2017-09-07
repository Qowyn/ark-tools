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

import javax.json.JsonObject;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import qowyn.ark.tools.data.DataCollector;
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
          System.out.print("\t" + entry.getKey() + " - " + entry.getValue());
          String defaultValue = driver.getParameter(entry.getKey());
          if (defaultValue != null) {
            System.out.println(" - default: " + defaultValue);
          } else {
            System.out.println();
          }
        }
        System.out.println();
      }
    }
    
    DBDrivers.close();
  }

  public static void run(OptionHandler oh){
    OptionSpec<String> configSpec = oh.accepts("config", "Reads params from the provided JSON configuration file.").withRequiredArg();
    OptionSpec<String> paramSpec = oh.accepts("param", "Provides a driver-specific parameter to the driver.").withRequiredArg();

    OptionSet options = oh.reparse();

    if (oh.getParams(options).size() < 3 || oh.getParams(options).size() > 4 || oh.wantsHelp()) {
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
    command.options = options;
    command.configSpec = configSpec;
    command.paramSpec = paramSpec;
    command.run();
    DBDrivers.close();
  }

  private OptionHandler oh;

  private OptionSet options;

  private OptionSpec<String> configSpec;

  private OptionSpec<String> paramSpec;

  public void run() {
    List<String> params = oh.getParams(options);
    String driverName = params.get(0);
    Path savePath = Paths.get(params.get(1));
    String pathOrUri = params.get(2);
    Path clusterPath = params.size() == 3 ? null : Paths.get(params.get(3));

    DataManager.loadData(oh.lang());

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

    if (options.has(configSpec)) {
      try {
        JsonObject config = (JsonObject) CommonFunctions.readJson(options.valueOf(configSpec));

        for (String paramName: config.keySet()) {
          driver.setParameter(paramName, config.getString(paramName));
        }
      } catch (IOException ex) {
        System.err.println("Error: Unable to read config " + options.valueOf(configSpec));
        System.exit(2);
        return;
      }
    }

    if (options.has(paramSpec)) {
      for (String param: options.valuesOf(paramSpec)) {
        int index = param.indexOf('=');

        if (index < 0) {
          System.err.println("Error: param should be a key-value pair delimited by '=', but was " + param);
          System.exit(2);
          return;
        }

        String paramName = param.substring(0, index);
        String paramValue = param.substring(index + 1);

        driver.setParameter(paramName, paramValue);
      }
    }

    if (path != null && !driver.canHandlePath()) {
      System.err.println("Error: Driver " + driverName + " cannot handle path " + pathOrUri);
      System.exit(2);
      return;
    } else if (path == null && !driver.getUrlSchemeList().contains(uri.getScheme())) {
      System.err.println("Error: Driver " + driverName + " cannot handle scheme " + pathOrUri);
      System.exit(2);
      return;
    }
    
    DataCollector collector = new DataCollector(oh);

    try {
      collector.loadSavegame(savePath);
      collector.loadPlayers(savePath.getParent());
      collector.loadTribes(savePath.getParent());
      if (clusterPath != null) {
        collector.loadCluster(clusterPath);
      }
    } catch (IOException ex) {
      ex.printStackTrace();
      System.exit(2);
      return;
    }

    if (path != null) {
      try {
        driver.openConnection(path);
      } catch (IOException e) {
        e.printStackTrace();
        System.exit(2);
        return;
      }
    } else {
      try {
        driver.openConnection(uri);
      } catch (IOException e) {
        e.printStackTrace();
        System.exit(2);
        return;
      }
    }

    try {
      driver.write(collector);
    } catch (IOException e) {
      e.printStackTrace();
      System.exit(2);
      return;
    }
    driver.close();
  }

}
