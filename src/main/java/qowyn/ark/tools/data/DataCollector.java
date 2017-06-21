package qowyn.ark.tools.data;

import java.util.Map;
import java.util.TreeMap;

public class DataCollector {

  public final Map<Integer, Item> itemMap = new TreeMap<>();

  public final Map<Integer, Inventory> inventoryMap = new TreeMap<>();

  public final Map<Integer, Creature> creatureMap = new TreeMap<>();

  public final Map<Integer, Structure> structureMap = new TreeMap<>();

  public final Map<Long, Player> playerMap = new TreeMap<>();

  public final Map<Integer, Tribe> tribeMap = new TreeMap<>();

}
