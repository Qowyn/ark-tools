package qowyn.ark.tools.data;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.fasterxml.jackson.core.JsonGenerator;

import qowyn.ark.ArkTribe;
import qowyn.ark.PropertyContainer;
import qowyn.ark.ReadingOptions;
import qowyn.ark.arrays.ArkArrayInt8;
import qowyn.ark.arrays.ArkArrayString;
import qowyn.ark.arrays.ArkArrayStruct;
import qowyn.ark.arrays.ArkArrayUInt32;
import qowyn.ark.structs.Struct;
import qowyn.ark.structs.StructPropertyList;

public class Tribe {

  public String tribeName;

  public int ownerPlayerDataId;

  public int tribeId;

  public final List<String> membersPlayerName = new ArrayList<>();

  public final List<Integer> membersPlayerDataId = new ArrayList<>();

  public final List<Integer> tribeAdmins = new ArrayList<>();

  public final List<Byte> membersRankGroups = new ArrayList<>();

  public boolean setGovernment;

  public int tribeGovernPINCode;

  public int tribeGovernDinoOwnership;

  public int tribeGovernStructureOwnership;

  public int tribeGovernDinoTaming;

  public int tribeGovernDinoUnclaimAdminOnly;

  public final List<String> tribeLog = new ArrayList<>();

  public int logIndex;

  public final List<TribeRankGroup> tribeRankGroups = new ArrayList<>();

  public Tribe(Path path) throws IOException {
    this(path, ReadingOptions.create());
  }

  public Tribe(Path path, ReadingOptions ro) throws IOException {
    ArkTribe tribe = new ArkTribe(path, ro);

    StructPropertyList tribeData = tribe.getPropertyValue("TribeData", StructPropertyList.class);

    tribeName = tribeData.findPropertyValue("TribeName", String.class).orElse("");
    ownerPlayerDataId = tribeData.findPropertyValue("OwnerPlayerDataID", Integer.class).orElse(0);
    tribeId = tribeData.findPropertyValue("TribeID", Integer.class).orElse(0);

    ArkArrayString membersNames = tribeData.getPropertyValue("MembersPlayerName", ArkArrayString.class);
    if (membersNames != null) {
      for (String memberName: membersNames) {
        membersPlayerName.add(memberName);
      }
    }

    ArkArrayUInt32 membersData = tribeData.getPropertyValue("MembersPlayerDataID", ArkArrayUInt32.class);
    if (membersData != null) {
      for (int memberDataId: membersData) {
        membersPlayerDataId.add(memberDataId);
      }
    }
    ArkArrayUInt32 tribeAdminIds = tribeData.getPropertyValue("TribeAdmins", ArkArrayUInt32.class);
    if (tribeAdminIds != null) {
      for (int tribeAdmin: tribeAdminIds) {
        tribeAdmins.add(tribeAdmin);
      }
    }

    ArkArrayInt8 memberRankGroups = tribeData.getPropertyValue("MembersRankGroups", ArkArrayInt8.class);
    if (memberRankGroups != null) {
      for (byte memberRankGroup: memberRankGroups) {
        membersRankGroups.add(memberRankGroup);
      }
    }

    setGovernment = tribeData.findPropertyValue("SetGovernment", Boolean.class).orElse(false);

    PropertyContainer tribeGovernment = tribeData.getPropertyValue("TribeGovernment", PropertyContainer.class);
    if (tribeGovernment != null) {
      tribeGovernPINCode = tribeGovernment.findPropertyValue("TribeGovern_PINCode", Integer.class).orElse(0);
      tribeGovernDinoOwnership = tribeGovernment.findPropertyValue("TribeGovern_DinoOwnership", Integer.class).orElse(0);
      tribeGovernStructureOwnership = tribeGovernment.findPropertyValue("TribeGovern_StructureOwnership", Integer.class).orElse(0);
      tribeGovernDinoTaming = tribeGovernment.findPropertyValue("TribeGovern_DinoTaming", Integer.class).orElse(0);
      tribeGovernDinoUnclaimAdminOnly = tribeGovernment.findPropertyValue("TribeGovern_DinoUnclaimAdminOnly", Integer.class).orElse(0);
    } else {
      tribeGovernDinoOwnership = 1;
      tribeGovernStructureOwnership = 1;
    }

    ArkArrayString logEntrys = tribeData.getPropertyValue("TribeLog", ArkArrayString.class);
    if (logEntrys != null) {
      for (String log: logEntrys) {
        tribeLog.add(log);
      }
    }

    logIndex = tribeData.findPropertyValue("LogIndex", Integer.class).orElse(0);

    ArkArrayStruct tribeRankStructs = tribeData.getPropertyValue("TribeRankGroups", ArkArrayStruct.class);
    if (tribeRankStructs != null) {
      for (Struct tribeRankStruct: tribeRankStructs) {
        tribeRankGroups.add(new TribeRankGroup((PropertyContainer) tribeRankStruct));
      }
    }
  }

  public static final SortedMap<String, WriterFunction<Tribe>> PROPERTIES = new TreeMap<>();

  static {
    /**
     * Tribe Properties
     */
    PROPERTIES.put("name", (tribe, generator, context, writeEmpty) -> {
      if (writeEmpty || !tribe.tribeName.isEmpty()) {
        generator.writeStringField("name", tribe.tribeName);
      }
    });
    PROPERTIES.put("ownerPlayerId", (tribe, generator, context, writeEmpty) -> {
      if (writeEmpty || tribe.ownerPlayerDataId != 0) {
        generator.writeNumberField("ownerPlayerId", tribe.ownerPlayerDataId);
      }
    });
    PROPERTIES.put("ownerPlayerName", (tribe, generator, context, writeEmpty) -> {
      if (tribe.ownerPlayerDataId != 0) {
        int index = tribe.membersPlayerDataId.indexOf(tribe.ownerPlayerDataId);
        if (index > -1) {
          generator.writeStringField("ownerPlayerName", tribe.membersPlayerName.get(index));
        } else {
          generator.writeNullField("ownerPlayerName");
        }
      } else if (writeEmpty) {
        generator.writeNullField("ownerPlayerName");
      }
    });
    PROPERTIES.put("tribeId", (tribe, generator, context, writeEmpty) -> {
      if (writeEmpty || tribe.tribeId != 0) {
        generator.writeNumberField("tribeId", tribe.tribeId);
      }
    });
    PROPERTIES.put("memberNames", (tribe, generator, context, writeEmpty) -> {
      if (writeEmpty || !tribe.membersPlayerName.isEmpty()) {
        generator.writeArrayFieldStart("memberNames");
        for (String value: tribe.membersPlayerName) {
          generator.writeString(value);
        }
        generator.writeEndArray();
      }
    });
    PROPERTIES.put("memberIds", (tribe, generator, context, writeEmpty) -> {
      if (writeEmpty || !tribe.membersPlayerDataId.isEmpty()) {
        generator.writeArrayFieldStart("memberIds");
        for (int value: tribe.membersPlayerDataId) {
          generator.writeNumber(value);
        }
        generator.writeEndArray();
      }
    });
    PROPERTIES.put("tribeAdminNames", (tribe, generator, context, writeEmpty) -> {
      if (writeEmpty || !tribe.tribeAdmins.isEmpty()) {
        generator.writeArrayFieldStart("tribeAdminNames");
        for (int value: tribe.tribeAdmins) {
          int index = tribe.membersPlayerDataId.indexOf(value);
          if (index > -1) {
            generator.writeString(tribe.membersPlayerName.get(index));
          } else {
            generator.writeNull();
          }
        }
        generator.writeEndArray();
      }
    });
    PROPERTIES.put("tribeAdminIds", (tribe, generator, context, writeEmpty) -> {
      if (writeEmpty || !tribe.tribeAdmins.isEmpty()) {
        generator.writeArrayFieldStart("tribeAdminIds");
        for (int value: tribe.tribeAdmins) {
          generator.writeNumber(value);
        }
        generator.writeEndArray();
      }
    });
    PROPERTIES.put("membersRankGroups", (tribe, generator, context, writeEmpty) -> {
      if (writeEmpty || !tribe.membersRankGroups.isEmpty()) {
        generator.writeArrayFieldStart("membersRankGroups");
        for (byte value: tribe.membersRankGroups) {
          generator.writeNumber(Byte.toUnsignedInt(value));
        }
        generator.writeEndArray();
      }
    });
    PROPERTIES.put("setGovernment", (tribe, generator, context, writeEmpty) -> {
      if (writeEmpty || !tribe.setGovernment) {
        generator.writeBooleanField("setGovernment", tribe.setGovernment);
      }
    });
    PROPERTIES.put("tribeGovernPINCode", (tribe, generator, context, writeEmpty) -> {
      if (writeEmpty || tribe.tribeGovernPINCode != 0) {
        generator.writeNumberField("tribeGovernPINCode", tribe.tribeGovernPINCode);
      }
    });
    PROPERTIES.put("tribeGovernDinoOwnership", (tribe, generator, context, writeEmpty) -> {
      if (writeEmpty || tribe.tribeGovernDinoOwnership != 0) {
        generator.writeNumberField("tribeGovernDinoOwnership", tribe.tribeGovernDinoOwnership);
      }
    });
    PROPERTIES.put("tribeGovernStructureOwnership", (tribe, generator, context, writeEmpty) -> {
      if (writeEmpty || tribe.tribeGovernStructureOwnership != 0) {
        generator.writeNumberField("tribeGovernStructureOwnership", tribe.tribeGovernStructureOwnership);
      }
    });
    PROPERTIES.put("tribeGovernDinoTaming", (tribe, generator, context, writeEmpty) -> {
      if (writeEmpty || tribe.tribeGovernDinoTaming != 0) {
        generator.writeNumberField("tribeGovernDinoTaming", tribe.tribeGovernDinoTaming);
      }
    });
    PROPERTIES.put("tribeGovernDinoUnclaimAdminOnly", (tribe, generator, context, writeEmpty) -> {
      if (writeEmpty || tribe.tribeGovernDinoUnclaimAdminOnly != 0) {
        generator.writeNumberField("tribeGovernDinoUnclaimAdminOnly", tribe.tribeGovernDinoUnclaimAdminOnly);
      }
    });
    PROPERTIES.put("tribeLog", (tribe, generator, context, writeEmpty) -> {
      if (writeEmpty || !tribe.tribeLog.isEmpty()) {
        generator.writeArrayFieldStart("tribeLog");
        for (String value: tribe.tribeLog) {
          generator.writeString(value);
        }
        generator.writeEndArray();
      }
    });
    PROPERTIES.put("logIndex", (tribe, generator, context, writeEmpty) -> {
      if (writeEmpty || tribe.logIndex != 0) {
        generator.writeNumberField("logIndex", tribe.logIndex);
      }
    });
    PROPERTIES.put("tribeRankGroups", (tribe, generator, context, writeEmpty) -> {
      if (writeEmpty || !tribe.tribeRankGroups.isEmpty()) {
        generator.writeArrayFieldStart("tribeRankGroups");
        for (TribeRankGroup group: tribe.tribeRankGroups) {
          generator.writeStartObject();
          if (writeEmpty || !group.rankGroupName.isEmpty()) {
            generator.writeStringField("rankGroupName", group.rankGroupName);
          }
          if (writeEmpty || group.rankGroupRank != 0) {
            generator.writeNumberField("rankGroupRank", Byte.toUnsignedInt(group.rankGroupRank));
          }
          if (writeEmpty || group.inventoryRank != 0) {
            generator.writeNumberField("inventoryRank", Byte.toUnsignedInt(group.inventoryRank));
          }
          if (writeEmpty || group.structureActivationRank != 0) {
            generator.writeNumberField("structureActivationRank", Byte.toUnsignedInt(group.structureActivationRank));
          }
          if (writeEmpty || group.newStructureActivationRank != 0) {
            generator.writeNumberField("newStructureActivationRank", Byte.toUnsignedInt(group.newStructureActivationRank));
          }
          if (writeEmpty || group.newStructureInventoryRank != 0) {
            generator.writeNumberField("newStructureInventoryRank", Byte.toUnsignedInt(group.newStructureInventoryRank));
          }
          if (writeEmpty || group.petOrderRank != 0) {
            generator.writeNumberField("petOrderRank", Byte.toUnsignedInt(group.petOrderRank));
          }
          if (writeEmpty || group.petRidingRank != 0) {
            generator.writeNumberField("petRidingRank", Byte.toUnsignedInt(group.petRidingRank));
          }
          if (writeEmpty || group.inviteToGroupRank != 0) {
            generator.writeNumberField("inviteToGroupRank", Byte.toUnsignedInt(group.inviteToGroupRank));
          }
          if (writeEmpty || group.maxPromotionGroupRank != 0) {
            generator.writeNumberField("maxPromotionGroupRank", Byte.toUnsignedInt(group.maxPromotionGroupRank));
          }
          if (writeEmpty || group.maxDemotionGroupRank != 0) {
            generator.writeNumberField("maxDemotionGroupRank", Byte.toUnsignedInt(group.maxDemotionGroupRank));
          }
          if (writeEmpty || group.maxBanishmentGroupRank != 0) {
            generator.writeNumberField("maxBanishmentGroupRank", Byte.toUnsignedInt(group.maxBanishmentGroupRank));
          }
          if (writeEmpty || group.numInvitesRemaining != 0) {
            generator.writeNumberField("numInvitesRemaining", Byte.toUnsignedInt(group.numInvitesRemaining));
          }
          if (writeEmpty || group.preventStructureDemolish) {
            generator.writeBooleanField("preventStructureDemolish", group.preventStructureDemolish);
          }
          if (writeEmpty || group.preventStructureAttachment) {
            generator.writeBooleanField("preventStructureAttachment", group.preventStructureAttachment);
          }
          if (writeEmpty || group.preventStructureBuildInRange) {
            generator.writeBooleanField("preventStructureBuildInRange", group.preventStructureBuildInRange);
          }
          if (writeEmpty || group.preventUnclaiming) {
            generator.writeBooleanField("preventUnclaiming", group.preventUnclaiming);
          }
          if (writeEmpty || group.allowInvites) {
            generator.writeBooleanField("allowInvites", group.allowInvites);
          }
          if (writeEmpty || group.limitInvites) {
            generator.writeBooleanField("limitInvites", group.limitInvites);
          }
          if (writeEmpty || group.allowDemotions) {
            generator.writeBooleanField("allowDemotions", group.allowDemotions);
          }
          if (writeEmpty || group.allowPromotions) {
            generator.writeBooleanField("allowPromotions", group.allowPromotions);
          }
          if (writeEmpty || group.allowBanishments) {
            generator.writeBooleanField("allowBanishments", group.allowBanishments);
          }
          if (writeEmpty || group.defaultRank) {
            generator.writeBooleanField("defaultRank", group.defaultRank);
          }
          generator.writeEndObject();
        }
        generator.writeEndArray();
      }
    });
  }

  public static final List<WriterFunction<Tribe>> PROPERTIES_LIST = new ArrayList<>(PROPERTIES.values());

  public void writeAllProperties(JsonGenerator generator, DataContext context, boolean writeEmpty) throws IOException {
    for (WriterFunction<Tribe> writer: PROPERTIES_LIST) {
      writer.accept(this, generator, context, writeEmpty);
    }
  }

}
