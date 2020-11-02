package com.matt.forgehax.util.entity;

import static com.matt.forgehax.Helper.getLocalPlayer;

import com.matt.forgehax.Globals;
import com.matt.forgehax.util.entity.EnchantmentUtils.EntityEnchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
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

  private static ITextComponent getFormattedText(String text, TextFormatting color) {
    return new TextComponentString(text.replaceAll("\r", "")).setStyle(new Style().setColor(color));
  }

}
