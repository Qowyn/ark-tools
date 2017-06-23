package qowyn.ark.tools.data;

import java.util.Map;
import java.util.TreeMap;

public class ArkDataCollector {

  public final Map<Integer, ArkItem> itemMap = new TreeMap<>();

  public final Map<Integer, ArkInventory> inventoryMap = new TreeMap<>();

  public final Map<Integer, ArkCreature> creatureMap = new TreeMap<>();

  public final Map<Integer, ArkStructure> structureMap = new TreeMap<>();

  public final Map<Long, ArkPlayer> playerMap = new TreeMap<>();

  public final Map<Integer, ArkTribe> tribeMap = new TreeMap<>();

}
