package qowyn.ark.tools;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import qowyn.ark.ArkProfile;
import qowyn.ark.ArkTribe;
import qowyn.ark.arrays.ArkArrayInteger;
import qowyn.ark.arrays.ArkArrayObjectReference;
import qowyn.ark.properties.Property;
import qowyn.ark.properties.PropertyByte;
import qowyn.ark.structs.StructPropertyList;
import qowyn.ark.structs.StructUniqueNetIdRepl;
import qowyn.ark.types.ObjectReference;

public class PlayerListCommands {

  private static final Pattern PROFILE_PATTERN = Pattern.compile("\\d+\\.arkprofile");

  private static final Pattern TRIBE_PATTERN = Pattern.compile("\\d+\\.arktribe");

  public static void players(OptionHandler oh) {
    OptionSpec<Void> noPrivacySpec = oh.accepts("no-privacy", "Include privacy related data (SteamID, IP).");
    OptionSpec<String> namingSpec = oh.accepts("naming", "Decides how to name the resulting files.")
        .withRequiredArg().describedAs("steamid|playerid").defaultsTo("steamid");

    OptionSet options = oh.reparse();

    String naming = options.valueOf(namingSpec);

    List<String> params = oh.getParams(options);
    if (params.size() != 2 || oh.wantsHelp()) {
      oh.printCommandHelp();
      System.exit(1);
      return;
    }

    try {
      Path saveGame = Paths.get(params.get(0)).toAbsolutePath();
      Path outputDirectory = Paths.get(params.get(1)).toAbsolutePath();
      Path saveDir = saveGame.getParent();

      Map<Integer, StructPropertyList> tribes = new HashMap<>();
      Filter<Path> tribeFilter = path -> TRIBE_PATTERN.matcher(path.getFileName().toString()).matches();

      try (DirectoryStream<Path> stream = Files.newDirectoryStream(saveDir, tribeFilter)) {
        for (Path path : stream) {
          ArkTribe tribe = new ArkTribe(path.toString());
          StructPropertyList tribeData = tribe.getPropertyValue("TribeData", StructPropertyList.class);
          Number tribeId = tribeData.getPropertyValue("TribeID", Number.class);
          tribes.put(tribeId.intValue(), tribeData);
        }
      }

      Filter<Path> profileFilter = path -> PROFILE_PATTERN.matcher(path.getFileName().toString()).matches();

      try (DirectoryStream<Path> stream = Files.newDirectoryStream(saveDir, profileFilter)) {
        for (Path path : stream) {
          ArkProfile profile = new ArkProfile(path.toString());
          StructPropertyList myData = profile.getPropertyValue("MyData", StructPropertyList.class);

          int playerId = myData.getPropertyValue("PlayerDataID", Number.class).intValue();

          String playerFileName;
          if (naming.equals("steamid")) {
            playerFileName = myData.getPropertyValue("UniqueID", StructUniqueNetIdRepl.class).getNetId() + ".json";
          } else if (naming.equals("playerid")) {
            playerFileName = Integer.toString(playerId) + ".json";
          } else {
            throw new Error();
          }

          Path playerPath = outputDirectory.resolve(playerFileName);

          CommonFunctions.writeJson(playerPath.toString(), generator -> {
            generator.writeStartObject();

            // Player data

            generator.write("id", playerId);
            generator.write("playerName", myData.getPropertyValue("PlayerName", String.class));

            if (options.has(noPrivacySpec)) {
              generator.write("steamId", myData.getPropertyValue("UniqueID", StructUniqueNetIdRepl.class).getNetId());
              generator.write("lastIp", myData.getPropertyValue("SavedNetworkAddress", String.class));
            }

            StructPropertyList characterConfig = myData.getPropertyValue("MyPlayerCharacterConfig", StructPropertyList.class);
            StructPropertyList characterStats = myData.getPropertyValue("MyPersistentCharacterStats", StructPropertyList.class);

            // Character data

            generator.write("name", characterConfig.getPropertyValue("PlayerCharacterName", String.class));
            Number extraLevel = characterStats.getPropertyValue("CharacterStatusComponent_ExtraCharacterLevel", Number.class);
            generator.write("level", extraLevel != null ? extraLevel.intValue() + 1 : 1);
            generator.write("experience", characterStats.getPropertyValue("CharacterStatusComponent_ExperiencePoints", Number.class).floatValue());

            // Engrams

            List<ObjectReference> learnedEngrams = characterStats.getPropertyValue("PlayerState_EngramBlueprints", ArkArrayObjectReference.class);

            if (learnedEngrams != null && !learnedEngrams.isEmpty()) {
              generator.writeStartArray("engrams");
              for (ObjectReference reference : learnedEngrams) {
                String engram = reference.getObjectString().toString();

                if (DataManager.hasItemByBGC(engram)) {
                  engram = DataManager.getItemByBGC(engram).getName();
                }

                generator.write(engram);
              }
              generator.writeEnd();
            }

            // Attributes

            generator.writeStartObject("attributes");
            for (Property<?> property : characterStats.getProperties()) {
              if (property instanceof PropertyByte && property.getNameString().equals("CharacterStatusComponent_NumberOfLevelUpPointsApplied")) {
                PropertyByte attribute = (PropertyByte) property;

                String name = AttributeNames.get(attribute.getIndex());
                if (name == null) {
                  generator.write(Integer.toString(attribute.getIndex()), attribute.getValue().getByteValue());
                } else {
                  generator.write(name, attribute.getValue().getByteValue());
                }
              }
            }
            generator.writeEnd();

            // Tribe

            Number tribeId = myData.getPropertyValue("TribeID", Number.class);
            if (tribeId != null) {
              generator.write("tribeId", tribeId.intValue());
              StructPropertyList tribe = tribes.get(tribeId.intValue());
              if (tribe != null) {
                generator.write("tribeName", tribe.getPropertyValue("TribeName", String.class));

                Number tribeOwnerId = tribe.getPropertyValue("OwnerPlayerDataID", Number.class);
                if (tribeOwnerId != null && tribeOwnerId.intValue() == playerId) {
                  generator.write("tribeOwner", true);
                }

                List<Integer> tribeAdmins = tribe.getPropertyValue("TribeAdmins", ArkArrayInteger.class);
                if (tribeAdmins != null && tribeAdmins.contains(playerId)) {
                  generator.write("tribeAdmin", true);
                }
              }
            }

            generator.writeEnd();
          }, oh);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

}
