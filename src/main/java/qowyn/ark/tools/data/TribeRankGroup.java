package qowyn.ark.tools.data;

import qowyn.ark.PropertyContainer;
import qowyn.ark.types.ArkByteValue;

public class TribeRankGroup {

  public String rankGroupName;

  public byte rankGroupRank;

  public byte inventoryRank;

  public byte structureActivationRank;

  public byte newStructureActivationRank;

  public byte newStructureInventoryRank;

  public byte petOrderRank;

  public byte petRidingRank;

  public byte inviteToGroupRank;

  public byte maxPromotionGroupRank;

  public byte maxDemotionGroupRank;

  public byte maxBanishmentGroupRank;

  public byte numInvitesRemaining;

  public boolean preventStructureDemolish;

  public boolean preventStructureAttachment;

  public boolean preventStructureBuildInRange;

  public boolean preventUnclaiming;

  public boolean allowInvites;

  public boolean limitInvites;

  public boolean allowDemotions;

  public boolean allowPromotions;

  public boolean allowBanishments;

  public boolean defaultRank;

  public TribeRankGroup(PropertyContainer tribeRankGroup) {
    rankGroupName = tribeRankGroup.findPropertyValue("RankGroupName", String.class).orElse("");

    rankGroupRank = tribeRankGroup.findPropertyValue("RankGroupRank", ArkByteValue.class).map(ArkByteValue::getByteValue).orElse((byte) 0);
    inventoryRank = tribeRankGroup.findPropertyValue("InventoryRank", ArkByteValue.class).map(ArkByteValue::getByteValue).orElse((byte) 0);
    structureActivationRank = tribeRankGroup.findPropertyValue("StructureActivationRank", ArkByteValue.class).map(ArkByteValue::getByteValue).orElse((byte) 0);
    newStructureActivationRank = tribeRankGroup.findPropertyValue("NewStructureActivationRank", ArkByteValue.class).map(ArkByteValue::getByteValue).orElse((byte) 0);
    newStructureInventoryRank = tribeRankGroup.findPropertyValue("NewStructureInventoryRank", ArkByteValue.class).map(ArkByteValue::getByteValue).orElse((byte) 0);
    petOrderRank = tribeRankGroup.findPropertyValue("PetOrderRank", ArkByteValue.class).map(ArkByteValue::getByteValue).orElse((byte) 0);
    petRidingRank = tribeRankGroup.findPropertyValue("PetRidingRank", ArkByteValue.class).map(ArkByteValue::getByteValue).orElse((byte) 0);
    inviteToGroupRank = tribeRankGroup.findPropertyValue("InviteToGroupRank", ArkByteValue.class).map(ArkByteValue::getByteValue).orElse((byte) 0);
    maxPromotionGroupRank = tribeRankGroup.findPropertyValue("MaxPromotionGroupRank", ArkByteValue.class).map(ArkByteValue::getByteValue).orElse((byte) 0);
    maxDemotionGroupRank = tribeRankGroup.findPropertyValue("MaxDemotionGroupRank", ArkByteValue.class).map(ArkByteValue::getByteValue).orElse((byte) 0);
    maxBanishmentGroupRank = tribeRankGroup.findPropertyValue("MaxBanishmentGroupRank", ArkByteValue.class).map(ArkByteValue::getByteValue).orElse((byte) 0);
    numInvitesRemaining = tribeRankGroup.findPropertyValue("NumInvitesRemaining", ArkByteValue.class).map(ArkByteValue::getByteValue).orElse((byte) 0);

    preventStructureDemolish = tribeRankGroup.findPropertyValue("bPreventStructureDemolish", Boolean.class).orElse(false);
    preventStructureAttachment = tribeRankGroup.findPropertyValue("bPreventStructureAttachment", Boolean.class).orElse(false);
    preventStructureBuildInRange = tribeRankGroup.findPropertyValue("bPreventStructureBuildInRange", Boolean.class).orElse(false);
    preventUnclaiming = tribeRankGroup.findPropertyValue("bPreventUnclaiming", Boolean.class).orElse(false);
    allowInvites = tribeRankGroup.findPropertyValue("bAllowInvites", Boolean.class).orElse(false);
    limitInvites = tribeRankGroup.findPropertyValue("bLimitInvites", Boolean.class).orElse(false);
    allowDemotions = tribeRankGroup.findPropertyValue("bAllowDemotions", Boolean.class).orElse(false);
    allowPromotions = tribeRankGroup.findPropertyValue("bAllowPromotions", Boolean.class).orElse(false);
    allowBanishments = tribeRankGroup.findPropertyValue("bAllowBanishments", Boolean.class).orElse(false);
    defaultRank = tribeRankGroup.findPropertyValue("bDefaultRank", Boolean.class).orElse(false);
  }

}
