package qowyn.ark.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.jar.Manifest;

import javax.json.JsonObject;
import javax.net.ssl.HttpsURLConnection;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;

public class UpdateCommands {

  private static final URI BASE_URI = URI.create("https://ark-tools.seen-von-ragan.de/data-download/");

  private static final URI MAIN_URI = BASE_URI.resolve("ark_data.json");

  private static final URI LANG_URI = BASE_URI.resolve("ark_data_languages.json");

  public static void version(OptionHandler oh) {
    if (oh.getParams().size() > 0 || oh.wantsHelp()) {
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    try {
      Manifest manifest = new Manifest(UpdateCommands.class.getResourceAsStream("/META-INF/MANIFEST.MF"));
      String myVersion = manifest.getMainAttributes().getValue("Implementation-Version");
      System.out.println(myVersion);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void updateData(OptionHandler oh) {
    OptionSpec<String> withLanguageSpec = oh.accepts("with-language", "Downloads specified languages").withRequiredArg().withValuesSeparatedBy(',');
    OptionSpec<Void> allLanguagesSpec = oh.accepts("all-languages", "Downloads all available languages").availableUnless(withLanguageSpec);

    OptionSet options = oh.reparse();

    if (oh.getParams(options).size() > 0 || oh.wantsHelp()) {
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    try {
      Manifest manifest = new Manifest(UpdateCommands.class.getResourceAsStream("/META-INF/MANIFEST.MF"));
      String myVersion = manifest.getMainAttributes().getValue("Implementation-Version");

      Path basePath = Paths.get(UpdateCommands.class.getResource("/").toURI());

      tryDownload(MAIN_URI, basePath.resolve("ark_data.json"), myVersion, oh);

      if (!options.has(allLanguagesSpec) && !options.has(withLanguageSpec)) {
        System.exit(0);
        return;
      }

      tryDownload(LANG_URI, basePath.resolve("ark_data_languages.json"), myVersion, oh);

      JsonObject languageList = (JsonObject) CommonFunctions.readJsonRelative("/ark_data_languages.json");

      Collection<String> languages; 
      if (options.has(allLanguagesSpec)) {
        languages = languageList.keySet();
      } else {
        languages = options.valuesOf(withLanguageSpec);
      }

      List<String> languageFiles = new ArrayList<>();
      boolean valid = true;
      for (String language: languages) {
        if (languageList.containsKey(language)) {
          languageFiles.add(languageList.getString(language));
        } else {
          valid = false;
          System.err.println("Unknown language " + language);
        }
      }

      if (!valid) {
        System.exit(2);
        return;
      }

      for (String languageFile: languageFiles) {
        tryDownload(BASE_URI.resolve(languageFile), basePath.resolve(languageFile), myVersion, oh);
      }
      System.exit(0);
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException(e);
    }

  }

  private static void tryDownload(URI uri, Path path, String myVersion, OptionHandler oh) throws IOException {
    HttpsURLConnection connection = (HttpsURLConnection) uri.toURL().openConnection();
    connection.addRequestProperty("User-Agent", "ark-tools/" + myVersion);

    if (Files.exists(path)) {
      connection.setIfModifiedSince(Files.getLastModifiedTime(path).toMillis());
    }

    connection.setReadTimeout(10000);
    connection.setConnectTimeout(10000);

    try (InputStream stream = connection.getInputStream()) {
      switch (connection.getResponseCode()) {
        case HttpsURLConnection.HTTP_OK:
          Files.copy(stream, path, StandardCopyOption.REPLACE_EXISTING);
          Files.setLastModifiedTime(path, FileTime.fromMillis(connection.getLastModified()));
          if (!oh.isQuiet()) {
            System.out.println("Updated " + path.getFileName().toString());
          }
          break;
        case HttpsURLConnection.HTTP_NOT_MODIFIED:
          break;
        default:
          throw new RuntimeException("Error during update of " + path.getFileName().toString() + ", HTTP Code: " + connection.getResponseCode());
      }
    }
  }

}
