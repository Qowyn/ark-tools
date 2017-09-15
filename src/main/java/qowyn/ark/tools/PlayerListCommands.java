package qowyn.ark.tools;

import static java.util.Comparator.comparing;
import static java.util.Comparator.reverseOrder;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static qowyn.ark.tools.CommonFunctions.iterable;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonGenerator;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import qowyn.ark.ArkCloudInventory;
import qowyn.ark.ArkContainer;
import qowyn.ark.ArkSavegame;
import qowyn.ark.GameObject;
import qowyn.ark.PropertyContainer;
import qowyn.ark.arrays.ArkArrayInt8;
import qowyn.ark.arrays.ArkArrayObjectReference;
import qowyn.ark.arrays.ArkArrayStruct;
import qowyn.ark.arrays.ArkArrayUInt8;
import qowyn.ark.structs.Struct;
import qowyn.ark.tools.data.Creature;
import qowyn.ark.tools.data.CustomDataContext;
import qowyn.ark.tools.data.DroppedItem;
import qowyn.ark.tools.data.Inventory;
import qowyn.ark.tools.data.Item;
import qowyn.ark.tools.data.Player;
import qowyn.ark.tools.data.Structure;
import qowyn.ark.tools.data.Tribe;
import qowyn.ark.types.ArkName;
import qowyn.ark.types.LocationData;
import qowyn.ark.types.ObjectReference;

public class PlayerListCommands {

  private static final Pattern PROFILE_PATTERN = Pattern.compile("\\d+\\.arkprofile");

  private static final Pattern TRIBE_PATTERN = Pattern.compile("\\d+\\.arktribe");

  private static final Pattern BASE_PATTERN = Pattern.compile("\\s*Base:\\s*(.+)\\s*<br>Size:\\s*(\\d+)\\s*", Pattern.CASE_INSENSITIVE);

  public static void players(OptionHandler optionHandler) {
    OptionSpec<Void> noPrivacySpec = optionHandler.accepts("no-privacy", "Include privacy related data (SteamID, IP).");
    OptionSpec<String> namingSpec = optionHandler.accepts("naming", "Decides how to name the resulting files.")
        .withRequiredArg().describedAs("steamid|playerid").defaultsTo("steamid");
    OptionSpec<String> inventorySpec = optionHandler.accepts("inventory", "Include inventory of players.").withOptionalArg().describedAs("summary|long").defaultsTo("summary");
    OptionSpec<Void> positionsSpec = optionHandler.accepts("positions", "Include current position of players.");
    OptionSpec<Integer> maxAgeSpec = optionHandler.accepts("max-age", "Ignore all player files older then <seconds> seconds.").withRequiredArg().describedAs("seconds").ofType(Integer.class);
    OptionSpec<Void> writeAllFieldsSpec = optionHandler.accepts("write-all-fields", "Writes all the fields.");

    OptionSet options = optionHandler.reparse();

    String naming = options.valueOf(namingSpec);

    List<String> params = optionHandler.getParams(options);
    if (params.size() != 2 || optionHandler.wantsHelp()) {
      optionHandler.printCommandHelp();
      System.exit(1);
      return;
    }

    boolean inventoryLong = options.valueOf(inventorySpec).equals("long");

    DataManager.loadData(optionHandler.lang());

    try {
      Stopwatch stopwatch = new Stopwatch(optionHandler.useStopwatch());

      boolean mapNeeded = options.has(inventorySpec) || options.has(positionsSpec);
      if (!optionHandler.isQuiet() && mapNeeded) {
        System.out.println("Need to load map, this may take some time...");
      }

      Path saveGame = Paths.get(params.get(0)).toAbsolutePath();
      Path outputDirectory = Paths.get(params.get(1)).toAbsolutePath();
      Path saveDir = saveGame.getParent();

      CustomDataContext context = new CustomDataContext();

      if (mapNeeded) {
        context.setObjectContainer(new ArkSavegame(saveGame, optionHandler.readingOptions()));
        context.setLatLonCalculator(LatLonCalculator.forSave(context.getSavegame()));
        stopwatch.stop("Loading map data");
      }

      final Map<Integer, Tribe> tribes;
      final List<Callable<Object>> tasks;
      if (optionHandler.useParallel()) {
        tribes = new ConcurrentHashMap<>();
        tasks = new ArrayList<>();
      } else {
        tribes = new HashMap<>();
        tasks = null;
      }

      Filter<Path> profileFilter = path -> PROFILE_PATTERN.matcher(path.getFileName().toString()).matches();

      try (DirectoryStream<Path> stream = Files.newDirectoryStream(saveDir, profileFilter)) {
        for (Path path : stream) {
          if (options.has(maxAgeSpec)) {
            FileTime fileTime = Files.getLastModifiedTime(path);

            if (fileTime.toInstant().isBefore(Instant.now().minusSeconds(maxAgeSpec.value(options)))) {
              continue;
            }
          }

          Runnable task = () -> {
            try {
              Player player = new Player(path, context, optionHandler.readingOptions());

              long playerId = player.playerDataId;

              String playerFileName;
              if (naming.equals("steamid")) {
                playerFileName = player.uniqueId.getNetId() + ".json";
              } else if (naming.equals("playerid")) {
                playerFileName = Long.toString(playerId) + ".json";
              } else {
                throw new Error();
              }

              if (player.tribeId != 0) {
                tribes.computeIfAbsent(player.tribeId, id -> {
                  Path tribePath = saveDir.resolve(player.tribeId + ".arktribe");
                  if (Files.exists(tribePath)) {
                    try {
                      return new Tribe(tribePath, optionHandler.readingOptions());
                    } catch (RuntimeException | IOException ex) {
                      // Either the header didn't match or one of the properties is missing
                      System.err.println("Found potentially corrupt ArkTribe: " + tribePath);
                      if (optionHandler.isVerbose()) {
                        ex.printStackTrace();
                      }
                      return null;
                    }
                  } else {
                    return null;
                  }
                });
              }

              Path playerPath = outputDirectory.resolve(playerFileName);

              CommonFunctions.writeJson(playerPath, generator -> {
                generator.writeStartObject();

                // Player data

                player.writeAllProperties(generator, context, options.has(writeAllFieldsSpec), options.has(noPrivacySpec));

                // Inventory
                if (options.has(inventorySpec)) {
                  player.writeInventory(generator, context, options.has(writeAllFieldsSpec), !inventoryLong);
                }

                // Tribe

                if (player.tribeId != 0) {
                  Tribe tribe = tribes.get(player.tribeId);
                  if (tribe != null) {
                    generator.writeStringField("tribeName", tribe.tribeName);

                    if (options.has(writeAllFieldsSpec) || tribe.ownerPlayerDataId == playerId) {
                      generator.writeBooleanField("tribeOwner", tribe.ownerPlayerDataId == playerId);
                    }

                    if (options.has(writeAllFieldsSpec) || tribe.tribeAdmins.contains((int) playerId)) {
                      generator.writeBooleanField("tribeAdmin", tribe.tribeAdmins.contains((int) playerId));
                    }
                  }
                }

                generator.writeEndObject();
              }, optionHandler);
            } catch (RuntimeException | IOException ex) {
              System.err.println("Found potentially corrupt ArkProfile: " + path.toString());
              if (optionHandler.isVerbose()) {
                ex.printStackTrace();
              }
            }
          };

          if (tasks != null) {
            tasks.add(Executors.callable(task));
          } else {
            task.run();
          }
        }
      }

      if (tasks != null) {
        ForkJoinPool pool = new ForkJoinPool(optionHandler.threadCount(), ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
        pool.invokeAll(tasks);
        pool.shutdown();
      }

      stopwatch.stop("Loading profiles and writing info");
      stopwatch.print();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

  }

  private static final Comparator<Map.Entry<ArkName, Long>> ENTRY_VALUE_REVERSE = comparing(Map.Entry::getValue, reverseOrder());

  public static void tribes(OptionHandler optionHandler) {
    OptionSpec<String> itemsSpec = optionHandler.accepts("items", "Include a list of all items belonging to the tribe.")
        .withOptionalArg().describedAs("summary|long").defaultsTo("summary");
    OptionSpec<String> inventorySpec = optionHandler.accepts("inventory", "Include inventories of creatures, players and structures.")
        .withOptionalArg().describedAs("summary|long").defaultsTo("summary");
    OptionSpec<String> tamedSpec = optionHandler.accepts("creatures", "Include a list of all tamed dinos of the tribe.")
        .withOptionalArg().describedAs("summary|long").defaultsTo("summary");
    OptionSpec<String> structuresSpec = optionHandler.accepts("structures", "Include a list of all structures belonging to the tribe.")
        .withOptionalArg().describedAs("summary|long").defaultsTo("summary");
    OptionSpec<Void> basesSpec = optionHandler.accepts("bases", "Allows tribes to create 'bases', groups creatures etc by base.");
    OptionSpec<Void> tribelessSpec = optionHandler.accepts("tribeless", "Put all players without a tribe into the 'tribeless' tribe.");
    OptionSpec<Void> nonPlayerSpec = optionHandler.accepts("non-players", "Include stuff owned by non-player factions. Generates a 'non-players.json' file.");
    OptionSpec<Void> writeAllFieldsSpec = optionHandler.accepts("write-all-fields", "Writes all the fields.");

    OptionSet options = optionHandler.reparse();

    List<String> params = optionHandler.getParams(options);
    if (params.size() != 2 || optionHandler.wantsHelp()) {
      optionHandler.printCommandHelp();
      System.exit(1);
      return;
    }

    boolean itemsLong = options.valueOf(itemsSpec).equals("long");
    boolean inventoryLong = options.valueOf(inventorySpec).equals("long");
    boolean tamedLong = options.valueOf(tamedSpec).equals("long");
    boolean structuresLong = options.valueOf(structuresSpec).equals("long");

    try {
      Stopwatch stopwatch = new Stopwatch(optionHandler.useStopwatch());

      boolean mapNeeded = options.has(itemsSpec) || options.has(tamedSpec) || options.has(structuresSpec) || options.has(inventorySpec);
      if (!optionHandler.isQuiet() && mapNeeded) {
        System.out.println("Need to load map, this may take some time...");
      }

      Path saveGame = Paths.get(params.get(0)).toAbsolutePath();
      Path outputDirectory = Paths.get(params.get(1)).toAbsolutePath();
      Path saveDir = saveGame.getParent();

      final Map<Integer, Set<TribeBase>> baseMap;
      CustomDataContext context = new CustomDataContext();

      if (mapNeeded) {
        DataManager.loadData(optionHandler.lang());

        context.setObjectContainer(new ArkSavegame(saveGame, optionHandler.readingOptions()));
        context.setLatLonCalculator(LatLonCalculator.forSave(context.getSavegame()));
        stopwatch.stop("Loading map data");
        if (options.has(basesSpec)) {
          baseMap = new HashMap<>();
          for (GameObject object : context.getSavegame().getObjects()) {
            // Skip items and stuff without a location
            if (object.isItem() || object.getLocation() == null) {
              continue;
            }

            String signText = object.getPropertyValue("SignText", String.class);
            Number targetingTeam = object.getPropertyValue("TargetingTeam", Number.class);

            if (signText != null && targetingTeam != null) {
              // Might be a 'Base' sign
              Matcher matcher = BASE_PATTERN.matcher(signText);
              if (matcher.matches()) {
                // Found a base sign, add it to the set, automatically replacing duplicates
                int tribeId = targetingTeam.intValue();
                LocationData location = object.getLocation();
                String baseName = matcher.group(1);
                float size = Float.parseFloat(matcher.group(2));

                TribeBase base = new TribeBase(baseName, location.getX(), location.getY(), location.getZ(), size);

                baseMap.computeIfAbsent(tribeId, key -> new HashSet<>()).add(base);
              }
            }
          }
          stopwatch.stop("Collecting bases");
        } else {
          baseMap = null;
        }
      } else {
        baseMap = null;
      }

      Filter<Path> tribeFilter = path -> TRIBE_PATTERN.matcher(path.getFileName().toString()).matches();
      final Set<Integer> tribeIds;
      final List<Callable<Object>> tasks;

      if (optionHandler.useParallel()) {
        tribeIds = ConcurrentHashMap.newKeySet();
        tasks = new ArrayList<>();
      } else {
        tribeIds = new HashSet<>();
        tasks = null;
      }

      ExceptionBiConsumer<JsonGenerator, Integer> mapWriter = (generator, tribeId) -> {
        if (mapNeeded) {
          List<GameObject> structures = new ArrayList<>();
          List<GameObject> creatures = new ArrayList<>();
          List<Item> items = new ArrayList<>();
          List<Item> blueprints = new ArrayList<>();
          List<DroppedItem> droppedItems = new ArrayList<>();
          // Apparently there is a behavior in ARK causing certain structures to exist twice
          // within a save
          Set<ArkName> processedList = new HashSet<>();
          // Bases
          Set<TribeBase> bases = options.has(basesSpec) ? baseMap.get(tribeId) : null;

          for (GameObject object : context.getSavegame().getObjects()) {
            if (object.isItem()) {
              continue;
            }

            int targetingTeam = object.findPropertyValue("TargetingTeam", Integer.class).orElse(-1);
            if (targetingTeam == -1) {
              continue;
            }
            if (tribeId == -1 && (targetingTeam < 50000 || tribeIds.contains(targetingTeam))) {
              continue;
            } else if (tribeId == 0 && targetingTeam >= 50000) {
              continue;
            } else if (tribeId > 0 && tribeId != targetingTeam) {
              continue;
            }

            // Determine base if we have bases
            final TribeBase base;
            if (bases != null && object.getLocation() != null) {
              TribeBase matchedBase = null;
              for (TribeBase potentialBase : bases) {
                if (potentialBase.insideBounds(object.getLocation())) {
                  matchedBase = potentialBase;
                  break;
                }
              }
              base = matchedBase;
            } else {
              base = null;
            }

            if (object.getClassString().contains("_Character_") || object.getClassString().equals("Raft_BP_C")) {
              if (!processedList.contains(object.getNames().get(0))) {
                if (base != null) {
                  base.getCreatures().add(object);
                } else {
                  creatures.add(object);
                }
                processedList.add(object.getNames().get(0));
              } else {
                // Duped Creature
                continue;
              }
            } else if (!object.hasAnyProperty("LinkedPlayerDataID") && !object.hasAnyProperty("AssociatedPrimalItem") && !object.hasAnyProperty("MyItem")) {
              // LinkedPlayerDataID: Players ain't structures
              // AssociatedPrimalItem: Items equipped by sleeping players
              // MyItem: dropped item
              if (!processedList.contains(object.getNames().get(0))) {
                if (base != null) {
                  base.getStructures().add(object);
                } else {
                  structures.add(object);
                }
                processedList.add(object.getNames().get(0));
              } else {
                // Duped Structure
                continue;
              }
            } else {
              if (!processedList.contains(object.getNames().get(0))) {
                processedList.add(object.getNames().get(0));
              } else {
                // Duped Player or dropped Item
                continue;
              }
            }

            ObjectReference inventoryReference = object.getPropertyValue("MyInventoryComponent", ObjectReference.class);
            GameObject inventory = inventoryReference != null ? inventoryReference.getObject(context.getSavegame()) : null;

            Consumer<ObjectReference> itemHandler = (itemReference) -> {
              GameObject item = itemReference.getObject(context.getSavegame());
              if (item != null && !Item.isDefaultItem(item)) {
                if (processedList.contains(item.getNames().get(0))) {
                  // happens for players having items in their quick bar
                  return;
                }
                processedList.add(item.getNames().get(0));

                if (item.hasAnyProperty("bIsBlueprint")) {
                  if (base != null) {
                    base.getBlueprints().add(new Item(item));
                  } else {
                    blueprints.add(new Item(item));
                  }
                } else {
                  if (base != null) {
                    base.getItems().add(new Item(item));
                  } else {
                    items.add(new Item(item));
                  }
                }
              }
            };

            Consumer<GameObject> droppedItemHandler = (droppedItemObject) -> {
              DroppedItem droppedItem = new DroppedItem(droppedItemObject, context.getSavegame());
              if (base != null) {
                base.getDroppedItems().add(droppedItem);
              } else {
                droppedItems.add(droppedItem);
              }
            };

            if (inventory != null && options.has(itemsSpec) && !options.has(inventorySpec)) {
              List<ObjectReference> inventoryItems = inventory.getPropertyValue("InventoryItems", ArkArrayObjectReference.class);
              List<ObjectReference> equippedItems = inventory.getPropertyValue("EquippedItems", ArkArrayObjectReference.class);

              Consumer<List<ObjectReference>> itemListHandler = list -> {
                if (list != null) {
                  for (ObjectReference itemReference : list) {
                    itemHandler.accept(itemReference);
                  }
                }
              };

              itemListHandler.accept(inventoryItems);
              itemListHandler.accept(equippedItems);
            }

            ObjectReference myItem = object.getPropertyValue("MyItem", ObjectReference.class);

            if (myItem != null) {
              if (options.has(itemsSpec) && !options.has(inventorySpec)) {
                itemHandler.accept(myItem);
              } else if (options.has(inventorySpec)) {
                droppedItemHandler.accept(object);
              }
            }
          }

          ExceptionConsumer<List<GameObject>> writeStructures = structList -> {
            if (options.has(structuresSpec)) {
              generator.writeArrayFieldStart("structures");

              if (structuresLong) {

                for (GameObject structureObject : structList) {
                  Structure structure = new Structure(structureObject, context.getSavegame());

                  generator.writeStartObject();

                  structure.writeAllProperties(generator, context, options.has(writeAllFieldsSpec));

                  if (options.has(inventorySpec)) {
                    structure.writeInventory(generator, context, options.has(writeAllFieldsSpec), !inventoryLong);
                  }

                  generator.writeEndObject();
                }

              } else {
                Map<ArkName, Long> structMap = structList.stream().collect(groupingBy(GameObject::getClassName, counting()));
                for (Map.Entry<ArkName, Long> entry : iterable(structMap.entrySet().stream().sorted(ENTRY_VALUE_REVERSE))) {
                  generator.writeStartObject();

                  String name = entry.getKey().toString();
                  if (DataManager.hasStructure(name)) {
                    name = DataManager.getStructure(name).getName();
                  }

                  generator.writeStringField("name", name);
                  generator.writeNumberField("count", entry.getValue());

                  generator.writeEndObject();
                }
              }

              generator.writeEndArray();
            }
          };

          ExceptionConsumer<List<GameObject>> writeCreatures = creaList -> {
            if (options.has(tamedSpec)) {
              generator.writeArrayFieldStart("tamed");

              if (tamedLong) {

                for (GameObject creatureObject : creaList) {
                  Creature creature = new Creature(creatureObject, context.getSavegame());

                  generator.writeStartObject();

                  creature.writeAllProperties(generator, context, options.has(writeAllFieldsSpec));

                  if (options.has(inventorySpec)) {
                    creature.writeInventory(generator, context, options.has(writeAllFieldsSpec), !inventoryLong);
                  }

                  generator.writeEndObject();
                }

              } else {
                Map<ArkName, Long> creaMap = creaList.stream().collect(groupingBy(GameObject::getClassName, counting()));
                for (Map.Entry<ArkName, Long> entry : iterable(creaMap.entrySet().stream().sorted(ENTRY_VALUE_REVERSE))) {
                  generator.writeStartObject();

                  String name = entry.getKey().toString();
                  if (DataManager.hasCreature(name)) {
                    name = DataManager.getCreature(name).getName();
                  }

                  generator.writeStringField("name", name);
                  generator.writeNumberField("count", entry.getValue());

                  generator.writeEndObject();
                }
              }

              generator.writeEndArray();
            }
          };

          ExceptionConsumer<List<DroppedItem>> writeDroppedItems = droppedList -> {
            if (options.has(inventorySpec)) {
              generator.writeArrayFieldStart("droppedItems");

              for (DroppedItem droppedItem : droppedList) {
                generator.writeStartObject();

                droppedItem.writeAllProperties(generator, context, options.has(writeAllFieldsSpec));
                droppedItem.writeInventory(generator, context, options.has(writeAllFieldsSpec), !inventoryLong);

                generator.writeEndObject();
              }

              generator.writeEndArray();
            }
          };

          if (options.has(basesSpec) && bases != null) {

            generator.writeArrayFieldStart("bases");

            for (TribeBase base : bases) {
              generator.writeStartObject();

              generator.writeStringField("name", base.getName());
              generator.writeNumberField("x", base.getX());
              generator.writeNumberField("y", base.getY());
              generator.writeNumberField("z", base.getZ());
              generator.writeNumberField("lat", context.getLatLonCalculator().calculateLat(base.getY()));
              generator.writeNumberField("lon", context.getLatLonCalculator().calculateLon(base.getX()));
              generator.writeNumberField("radius", base.getSize());
              writeCreatures.accept(base.getCreatures());
              writeStructures.accept(base.getStructures());
              writeDroppedItems.accept(base.getDroppedItems());
              if (itemsLong) {
                generator.writeFieldName("items");
                Inventory.writeInventoryLong(generator, context, base.getItems(), options.has(writeAllFieldsSpec));
                generator.writeFieldName("blueprints");
                Inventory.writeInventoryLong(generator, context, base.getBlueprints(), options.has(writeAllFieldsSpec));
              } else {
                generator.writeFieldName("items");
                Inventory.writeInventorySummary(generator, base.getItems());
                generator.writeFieldName("blueprints");
                Inventory.writeInventorySummary(generator, base.getBlueprints());
              }

              generator.writeEndObject();
            }

            generator.writeStartObject();

          }

          writeCreatures.accept(creatures);
          writeStructures.accept(structures);
          writeDroppedItems.accept(droppedItems);
          if (itemsLong) {
            generator.writeFieldName("items");
            Inventory.writeInventoryLong(generator, context, items, options.has(writeAllFieldsSpec));
            generator.writeFieldName("blueprints");
            Inventory.writeInventoryLong(generator, context, blueprints, options.has(writeAllFieldsSpec));
          } else {
            generator.writeFieldName("items");
            Inventory.writeInventorySummary(generator, items);
            generator.writeFieldName("blueprints");
            Inventory.writeInventorySummary(generator, blueprints);
          }

          if (options.has(basesSpec) && bases != null) {
            generator.writeEndObject();

            generator.writeEndArray();
          }
        }
      };

      try (DirectoryStream<Path> stream = Files.newDirectoryStream(saveDir, tribeFilter)) {
        for (Path path : stream) {
          Runnable task = () -> {
            try {
              Tribe tribe = new Tribe(path, optionHandler.readingOptions());

              tribeIds.add(tribe.tribeId);

              String tribeFileName = tribe.tribeId + ".json";

              Path tribePath = outputDirectory.resolve(tribeFileName);

              CommonFunctions.writeJson(tribePath, generator -> {
                generator.writeStartObject();

                tribe.writeAllProperties(generator, context, options.has(writeAllFieldsSpec));

                mapWriter.accept(generator, tribe.tribeId);

                generator.writeEndObject();
              }, optionHandler);
            } catch (RuntimeException | IOException ex) {
              System.err.println("Found potentially corrupt ArkTribe: " + path.toString());
              if (optionHandler.isVerbose()) {
                ex.printStackTrace();
              }
            }
          };

          if (tasks != null) {
            tasks.add(Executors.callable(task));
          } else {
            task.run();
          }
        }
      }

      if (tasks != null) {
        ForkJoinPool pool = new ForkJoinPool(optionHandler.threadCount(), ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
        pool.invokeAll(tasks);
        pool.shutdown();
      }

      if (options.has(tribelessSpec)) {
        Path tribePath = outputDirectory.resolve("tribeless.json");

        CommonFunctions.writeJson(tribePath, generator -> {
          generator.writeStartObject();

          mapWriter.accept(generator, -1);

          generator.writeEndObject();
        }, optionHandler);
      }

      if (options.has(nonPlayerSpec)) {
        Path tribePath = outputDirectory.resolve("non-players.json");

        CommonFunctions.writeJson(tribePath, generator -> {
          generator.writeStartObject();

          mapWriter.accept(generator, 0);

          generator.writeEndObject();
        }, optionHandler);
      }

      stopwatch.stop("Loading tribes and writing info");
      stopwatch.print();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void cluster(OptionHandler optionHandler) {
    OptionSpec<Void> writeAllFieldsSpec = optionHandler.accepts("write-all-fields", "Writes all the fields.");

    OptionSet options = optionHandler.reparse();

    List<String> params = optionHandler.getParams(options);
    if (params.size() != 2 || optionHandler.wantsHelp()) {
      optionHandler.printCommandHelp();
      System.exit(1);
      return;
    }

    Path clusterDirectory = Paths.get(params.get(0)).toAbsolutePath();
    Path outputDirectory = Paths.get(params.get(1)).toAbsolutePath();

    DataManager.loadData(optionHandler.lang());

    final List<Callable<Object>> tasks;

    if (optionHandler.useParallel()) {
      tasks = new ArrayList<>();
    } else {
      tasks = null;
    }

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(clusterDirectory)) {
      Stopwatch stopwatch = new Stopwatch(optionHandler.useStopwatch());
      for (Path path : stream) {
        if (!Files.isRegularFile(path)) {
          continue;
        }
        Runnable task = () -> {
          try {
            ArkCloudInventory cloudInventory = new ArkCloudInventory(path, optionHandler.readingOptions());

            CustomDataContext context = new CustomDataContext();
            context.setObjectContainer(cloudInventory);

            PropertyContainer arkData = cloudInventory.getInventoryData().getPropertyValue("MyArkData", PropertyContainer.class);

            CommonFunctions.writeJson(outputDirectory.resolve(path.getFileName().toString() + ".json"), generator -> {

              generator.writeStartObject();

              ArkArrayStruct tamedDinosData = arkData.getPropertyValue("ArkTamedDinosData", ArkArrayStruct.class);
              if (tamedDinosData != null && !tamedDinosData.isEmpty()) {
                generator.writeArrayFieldStart("creatures");
                for (Struct dinoStruct : tamedDinosData) {
                  PropertyContainer dino = (PropertyContainer) dinoStruct;
                  ArkContainer container = null;
                  if (cloudInventory.getInventoryVersion() == 1) {
                    ArkArrayUInt8 byteData = dino.getPropertyValue("DinoData", ArkArrayUInt8.class);

                    container = new ArkContainer(byteData);
                  } else if (cloudInventory.getInventoryVersion() == 3) {
                    ArkArrayInt8 byteData = dino.getPropertyValue("DinoData", ArkArrayInt8.class);

                    container = new ArkContainer(byteData);
                  }

                  ObjectReference dinoClass = dino.getPropertyValue("DinoClass", ObjectReference.class);
                  // Skip "BlueprintGeneratedClass " = 24 chars
                  String dinoClassName = dinoClass.getObjectString().toString().substring(24);
                  generator.writeStartObject();

                  generator.writeStringField("type", DataManager.hasCreatureByPath(dinoClassName) ? DataManager.getCreatureByPath(dinoClassName).getName() : dinoClassName);

                  // NPE for unknown versions
                  Creature creature = new Creature(container.getObjects().get(0), container);
                  generator.writeObjectFieldStart("data");
                  creature.writeAllProperties(generator, context, options.has(writeAllFieldsSpec));
                  generator.writeEndObject();

                  generator.writeEndObject();
                }
                generator.writeEndArray();
              }

              ArkArrayStruct arkItems = arkData.getPropertyValue("ArkItems", ArkArrayStruct.class);
              if (arkItems != null) {
                List<Item> items = new ArrayList<>();
                for (Struct itemStruct : arkItems) {
                  PropertyContainer item = (PropertyContainer) itemStruct;
                  PropertyContainer netItem = item.getPropertyValue("ArkTributeItem", PropertyContainer.class);

                  items.add(new Item(netItem));
                }

                if (!items.isEmpty()) {
                  generator.writeFieldName("items");
                  Inventory.writeInventoryLong(generator, context, items, options.has(writeAllFieldsSpec));
                }
              }

              generator.writeEndObject();

            }, optionHandler);

          } catch (RuntimeException | IOException ex) {
            System.err.println("Found potentially corrupt cluster data: " + path.toString());
            if (optionHandler.isVerbose()) {
              ex.printStackTrace();
            }
          }
        };

        if (tasks != null) {
          tasks.add(Executors.callable(task));
        } else {
          task.run();
        }
      }

      if (tasks != null) {
        ForkJoinPool pool = new ForkJoinPool(optionHandler.threadCount(), ForkJoinPool.defaultForkJoinWorkerThreadFactory, null, true);
        pool.invokeAll(tasks);
        pool.shutdown();
      }

      stopwatch.stop("Loading cluster data and writing info");
      stopwatch.print();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
