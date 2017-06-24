package qowyn.ark.tools.data;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

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
    ArkTribe tribe = new ArkTribe(path.toString(), ro);

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

}
