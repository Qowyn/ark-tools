package qowyn.ark.tools.driver;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;

public class DBDrivers {

  private static final Map<String, Supplier<DBDriver>> DRIVERS = new HashMap<>();

  private static URLClassLoader classLoader;

  public static void addDriver(String name, Supplier<DBDriver> driverSupplier) {
    DRIVERS.put(name.toLowerCase(Locale.ENGLISH), driverSupplier);
  }

  static {
    addDriver("json", JsonDriver::new);
  }

  public static Set<String> getDriverNames() {
    return DRIVERS.keySet();
  }

  public static DBDriver getDriver(String name) {
    return DRIVERS.getOrDefault(name.toLowerCase(Locale.ENGLISH), () -> {
      throw new UnsupportedOperationException("Unknown Driver " + name);
    }).get();
  }

  public static void discoverDrivers() throws URISyntaxException, IOException, ClassNotFoundException {
    URL jarDirectoryURL = DBDrivers.class.getResource("/");
    Path jarPath = Paths.get(jarDirectoryURL.toURI());
    List<URL> urls = new ArrayList<>();
    List<String> driverInitClasses = new ArrayList<>();

    try (Stream<Path> pathStream = Files.walk(jarPath, 1, FileVisitOption.FOLLOW_LINKS)) {
      Iterator<Path> pathIterator = pathStream.iterator();

      while (pathIterator.hasNext()) {
        Path path = pathIterator.next();

        if (!path.equals(jarPath) && path.getFileName().toString().endsWith("-ark-tools-driver.jar")) {
          try (JarFile jarFile = new JarFile(path.toFile())) {
            Manifest jarManifest = jarFile.getManifest();
            String dependencies = jarManifest.getMainAttributes().getValue("Driver-Dependencies");
            String driverInit = jarManifest.getMainAttributes().getValue("Driver-Init");
            List<URL> loadList = new ArrayList<>();
            loadList.add(path.toUri().toURL());
  
            boolean dependenciesFound = true;
            if (dependencies != null) {
              for (String dependency: dependencies.split(",")) {
                Path dependencyPath = jarPath.resolve(dependency.trim());
                if (!Files.exists(dependencyPath)) {
                  dependenciesFound = false;
                  System.err.println("Warning: Driver " + path.getFileName().toString() + " requires " + dependencyPath.getFileName().toString());
                } else {
                  loadList.add(dependencyPath.toUri().toURL());
                }
              }
            }
  
            if (dependenciesFound) {
              urls.addAll(loadList);
              driverInitClasses.add(driverInit);
            }
          }
        }
      }
    }

    classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]), DBDrivers.class.getClassLoader());
    for (String driverInit: driverInitClasses) {
      Class.forName(driverInit, true, classLoader);
    }
  }

  public static void close() {
    if (classLoader != null) {
      try {
        classLoader.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

}
