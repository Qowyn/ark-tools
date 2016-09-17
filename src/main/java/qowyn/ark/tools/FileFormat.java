package qowyn.ark.tools;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public enum FileFormat {
  MAP("map", ".ark", "Saved data of a map"),
  TRIBE("tribe", ".arktribe", "Saved data of a tribe"),
  PROFILE("profile", ".arkprofile", "Saved data of a profile"),
  CLUSTER("cluster", "", "Saved data belonging to a player in a cluster"),
  LOCALPROFILE("localprofile", null, "Local profile");

  private static final Map<String, FileFormat> EXTENSION_FORMAT_MAP = new HashMap<>();

  static {
    for (FileFormat format: FileFormat.values()) {
      if (format.getExtension() != null) {
        EXTENSION_FORMAT_MAP.put(format.getExtension(), format);
      }
    }
  }

  public static FileFormat fromExtension(Path path) {
    String filename = path.getFileName().toString();
    
    // Hack for LOCALPROFILE
    if (filename.equals("PlayerLocalData.arkprofile")) {
      return FileFormat.LOCALPROFILE;
    }
    
    int lastDot = filename.lastIndexOf('.');

    return lastDot > -1 ? EXTENSION_FORMAT_MAP.get(filename.substring(lastDot)) : EXTENSION_FORMAT_MAP.get("");
  }

  private FileFormat(String identifier, String extension, String description) {
    this.identifier = identifier;
    this.extension = extension;
    this.description = description;
  }

  private final String identifier;
  
  private final String extension;
  
  private final String description;

  public String getIdentifier() {
    return identifier;
  }

  public String getExtension() {
    return extension;
  }

  public String getDescription() {
    return description;
  }
  
  @Override
  public String toString() {
    return identifier;
  }

}
