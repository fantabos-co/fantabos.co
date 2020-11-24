package com.matt.forgehax.util.entity;

import static com.matt.forgehax.Helper.getLocalPlayer;

import com.matt.forgehax.Globals;
import com.matt.forgehax.util.entity.EnchantmentUtils.EntityEnchantment;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public class PlayerUtils implements Globals {
  
  /**
   * Use EntityUtils::isLocalPlayer
   */
  @Deprecated
  public static boolean isLocalPlayer(Entity player) {
    EntityPlayer localPlayer = getLocalPlayer();
    return localPlayer != null && localPlayer.equals(player);
  }
  
  @Deprecated
  public static boolean isFriend(EntityPlayer player) {
    return false;
  }

  public static String getGmode(EntityPlayer in) {
    if (in.isCreative()) return TextFormatting.GRAY + "[" + TextFormatting.DARK_AQUA + "C" + TextFormatting.GRAY + "]" + TextFormatting.RESET;
    if (in.isSpectator()) return TextFormatting.GRAY + "[" + TextFormatting.WHITE + "W" + TextFormatting.GRAY + "]" + TextFormatting.RESET;
    return TextFormatting.GRAY + "[" + TextFormatting.DARK_GRAY + "S" + TextFormatting.GRAY + "]" + TextFormatting.RESET;
  }

  public static String getColorPing(EntityPlayer in) {
    if (MC.getConnection() == null
        || MC.getConnection().getPlayerInfo(in.getUniqueID()) == null)
          return TextFormatting.BLACK + "N/A" + TextFormatting.RESET;
    int ping = MC.getConnection().getPlayerInfo(in.getUniqueID()).getResponseTime();
    if (ping > 1000) return TextFormatting.DARK_GRAY.toString() + ping + "ms" + TextFormatting.RESET;
    if (ping > 500) return TextFormatting.DARK_RED.toString() + ping + "ms" + TextFormatting.RESET;
    if (ping > 300) return TextFormatting.RED.toString() + ping + "ms" + TextFormatting.RESET;
    if (ping > 180) return TextFormatting.GOLD.toString() + ping + "ms" + TextFormatting.RESET;
    if (ping > 100) return TextFormatting.YELLOW.toString() + ping + "ms" + TextFormatting.RESET;
    if (ping > 70) return TextFormatting.GREEN.toString() + ping + "ms" + TextFormatting.RESET;
    if (ping > 40) return TextFormatting.DARK_GREEN.toString() + ping + "ms" + TextFormatting.RESET;
    return TextFormatting.DARK_AQUA.toString() + ping + "ms" + TextFormatting.RESET;
  }

  public static String getHPColor(EntityLivingBase in) {
    float hp = in.getHealth() + in.getAbsorptionAmount();
    if (hp > 20F) return TextFormatting.GRAY + "[" + TextFormatting.YELLOW + String.format("%.0f", hp) + TextFormatting.GRAY + "]" + TextFormatting.RESET;
    if (hp > 17F) return TextFormatting.GRAY + "[" + TextFormatting.DARK_GREEN + String.format("%.0f", hp) + TextFormatting.GRAY + "]" + TextFormatting.RESET;
    if (hp > 12F) return TextFormatting.GRAY + "[" + TextFormatting.GREEN + String.format("%.0f", hp) + TextFormatting.GRAY + "]" + TextFormatting.RESET;
    if (hp > 8F) return TextFormatting.GRAY + "[" + TextFormatting.GOLD + String.format("%.0f", hp) + TextFormatting.GRAY + "]" + TextFormatting.RESET;
    if (hp > 5F) return TextFormatting.GRAY + "[" + TextFormatting.RED + String.format("%.1f", hp) + TextFormatting.GRAY + "]" + TextFormatting.RESET;
    if (hp > 2F) return TextFormatting.GRAY + "[" + TextFormatting.DARK_RED + String.format("%.1f", hp) + TextFormatting.GRAY + "]" + TextFormatting.RESET;
    return TextFormatting.GRAY + "[" + TextFormatting.DARK_GRAY + String.format("%.1f", hp) + TextFormatting.GRAY + "]" + TextFormatting.RESET;
  }

  public static String above_below(Entity in) {
    return above_below(getLocalPlayer().posY, in.posY);
  }

  public static String above_below(double pos1, double pos2) {
    if (pos1 > pos2) return TextFormatting.GOLD + "++" + TextFormatting.RESET;
    if (pos1 < pos2) return TextFormatting.DARK_GRAY + "--" + TextFormatting.RESET;
    return TextFormatting.GRAY + "==" + TextFormatting.RESET;
  }

  public static String getDistanceColor(Entity in) {
    double distance = getLocalPlayer().getDistance(in);
    if (distance > 30D) return TextFormatting.DARK_AQUA + String.format("%.0fm", distance) + TextFormatting.RESET;
    if (distance > 10D) return TextFormatting.AQUA + String.format("%.0fm", distance) + TextFormatting.RESET;
    return TextFormatting.WHITE + String.format("%.1fm", distance) + TextFormatting.RESET;
  }

  public enum GearLevel {
    NAKED,
    POOR,
    BASIC,
    GEARED,
    PERFECT,
    HACKED
  }

  public static ITextComponent getGearColor(EntityPlayer player) {
    switch(getGearLevel(player.getEquipmentAndArmor())) {
      case HACKED:
        return getFormattedText(player.getName(), TextFormatting.DARK_PURPLE);
      case PERFECT:
        return getFormattedText(player.getName(), TextFormatting.DARK_RED);
      case GEARED:
        return getFormattedText(player.getName(), TextFormatting.RED);
      case BASIC:
        return getFormattedText(player.getName(), TextFormatting.YELLOW);
      case POOR:
        return getFormattedText(player.getName(), TextFormatting.GREEN);
      case NAKED:
        return getFormattedText(player.getName(), TextFormatting.DARK_GRAY);
      default:
        return getFormattedText(player.getName(), TextFormatting.GRAY);
    }
  }
  public static ITextComponent getGearColorFleyr(EntityPlayer player) {
    int point = getGearLevelFleyr(player.getEquipmentAndArmor());
        if(point>100) return getFormattedText(player.getName(), TextFormatting.DARK_PURPLE);
        else if (point>80) return getFormattedText(player.getName(), TextFormatting.DARK_RED);
        else if (point>60) return getFormattedText(player.getName(), TextFormatting.RED);
        else if (point>40) return getFormattedText(player.getName(), TextFormatting.YELLOW);
        else if (point>20) return getFormattedText(player.getName(), TextFormatting.GREEN);
        else if (point>10) return getFormattedText(player.getName(), TextFormatting.DARK_GRAY);
        else if (point>0) return getFormattedText(player.getName(), TextFormatting.GRAY);
        return getFormattedText(player.getName(), TextFormatting.GRAY);
    }
  

  public static GearLevel getGearLevel(Iterable<ItemStack> items) {
  // TODO this is kinda shit and really subjective
    int basic = 0, gear = 0, enchanted = 0, perfect = 0;
    for (ItemStack i : items) {
      if (i == ItemStack.EMPTY) continue;
      if (i.getItem() instanceof ItemBlock || i.getMaxDamage() < 120) {
        continue;
      }
      if (i.getMaxDamage() < 300) {
        basic++;
        continue;
      }
      if (i.isItemEnchanted()) {
        int buf = 0;
        for (EntityEnchantment ench : EnchantmentUtils.getEnchantments(i.getEnchantmentTagList())) {
          if (ench.getLevel() > 10) {
            return GearLevel.HACKED;
          }
          if (ench.getEnchantment().getMaxLevel() == ench.getLevel()) {
            buf++;
          }
        }
        if (buf > 3) perfect++;
        else enchanted++;
      } else {
        gear++;
      }
    }
    if (perfect > 3) return GearLevel.PERFECT;
    if (enchanted+perfect > 3) return GearLevel.GEARED;
    if (gear+enchanted+perfect > 3) return GearLevel.BASIC;
    if (basic+gear+enchanted+perfect > 3) return GearLevel.POOR;
    return GearLevel.NAKED;
  }

  public static int getGearLevelFleyr(Iterable<ItemStack> items) {
    // TODO this is kinda shit and really subjective
      int point=-1;
      for (ItemStack i : items) {
        if (i == ItemStack.EMPTY) continue;
/*        if (i.getItem() instanceof ItemBlock || i.getMaxDamage() < 120) {
          continue;
        }
        if (i.getMaxDamage() < 300) {
          basic++;
          continue;
        }
        if (i.isItemEnchanted()) {
          int buf = 0;
          for (EntityEnchantment ench : EnchantmentUtils.getEnchantments(i.getEnchantmentTagList())) {
            if (ench.getLevel() > 10) {
              return GearLevel.HACKED;
            }
            if (ench.getEnchantment().getMaxLevel() == ench.getLevel()) {
              buf++;
            }
          }
          if (buf > 3) perfect++;
          else enchanted++;
        } else {
          gear++;
        }*/
           if(i.getItem().equals(Items.LEATHER_HELMET)) point=point+1;
      else if(i.getItem().equals(Items.LEATHER_CHESTPLATE)) point=point+3;
      else if(i.getItem().equals(Items.LEATHER_LEGGINGS)) point=point+2;
      else if(i.getItem().equals(Items.LEATHER_BOOTS)) point=point+1;
      else if(i.getItem().equals(Items.GOLDEN_HELMET)) point=point+2;
      else if(i.getItem().equals(Items.GOLDEN_CHESTPLATE)) point=point+5;
      else if(i.getItem().equals(Items.GOLDEN_LEGGINGS)) point=point+3;
      else if(i.getItem().equals(Items.GOLDEN_BOOTS)) point=point+1;      
      else if(i.getItem().equals(Items.CHAINMAIL_HELMET)) point=point+2;
      else if(i.getItem().equals(Items.CHAINMAIL_CHESTPLATE)) point=point+5;
      else if(i.getItem().equals(Items.CHAINMAIL_LEGGINGS)) point=point+4;
      else if(i.getItem().equals(Items.CHAINMAIL_BOOTS)) point=point+1;
      else if(i.getItem().equals(Items.IRON_HELMET)) point=point+2;
      else if(i.getItem().equals(Items.IRON_CHESTPLATE)) point=point+6;
      else if(i.getItem().equals(Items.IRON_LEGGINGS)) point=point+5;
      else if(i.getItem().equals(Items.IRON_BOOTS)) point=point+2;
      else if(i.getItem().equals(Items.DIAMOND_HELMET)) point=point+3;
      else if(i.getItem().equals(Items.DIAMOND_CHESTPLATE)) point=point+8;
      else if(i.getItem().equals(Items.ELYTRA)) point=point+8;
      else if(i.getItem().equals(Items.DIAMOND_LEGGINGS)) point=point+6;
      else if(i.getItem().equals(Items.DIAMOND_BOOTS)) point=point+3;
      point+=getProtBonusPoints(items, point);

    }
      LOGGER.warn(point);
      return point;
    }

    public static int getProtBonusPoints(Iterable<ItemStack> items, int point) {
      for (ItemStack i : items) {
      if(i.isItemEnchanted()){
        for (EntityEnchantment ench : EnchantmentUtils.getEnchantments(i.getEnchantmentTagList())) {
        if(ench.getShortName().equals("p")){
          int level = ench.getLevel();
          point += ((4*level)*point)/100;
        }
        else if(ench.getShortName().equals("bp")){
          int level = ench.getLevel();
          point += ((8*level)*point)/100;
        }


      }
    }
  }
      return point;
    }





  private static ITextComponent getFormattedText(String text, TextFormatting color) {
    return new TextComponentString(text.replaceAll("\r", "")).setStyle(new Style().setColor(color));
  }

}
