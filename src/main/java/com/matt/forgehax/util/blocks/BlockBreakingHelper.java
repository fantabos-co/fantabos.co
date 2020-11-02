package com.matt.forgehax.util.blocks;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getWorld;
import static net.minecraft.init.Enchantments.EFFICIENCY;

import com.matt.forgehax.Globals;

import net.minecraft.block.state.IBlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.MobEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemTool;
import net.minecraft.util.math.BlockPos;

public class BlockBreakingHelper implements Globals{
  // I hate this but ew better than nothing

  public static int getBlockBreakingTicks(BlockPos pos, ItemStack tool) {
    IBlockState block = getWorld().getBlockState(pos);
    float hardness = block.getBlockHardness(getWorld(), pos);
    if (hardness <= 0) return 1;

    double speedMult = tool.getDestroySpeed(block);

    float ticks;
    if (speedMult > 0) {
      ticks = hardness * 1.5f * 20f; // 20 ticks per second

      int eff = EnchantmentHelper.getEnchantmentLevel(EFFICIENCY, tool);
      
      if (eff > 0) {
        speedMult += (eff * eff + 1);
      }

      if (getLocalPlayer().isPotionActive(MobEffects.HASTE)) {
        speedMult *= 1 + (0.2 * getLocalPlayer().getActivePotionEffect(MobEffects.HASTE).getAmplifier() + 1);
      }

      if (getLocalPlayer().isPotionActive(MobEffects.MINING_FATIGUE)) {
        speedMult /= Math.pow(3, getLocalPlayer().getActivePotionEffect(MobEffects.MINING_FATIGUE).getAmplifier() + 1);
      }

      ticks /= speedMult;
    } else {
      ticks = hardness * 5f * 20f; // 20 ticks per second
    }

    if (getLocalPlayer().isInWater()) ticks *= 5f;
    if (!getLocalPlayer().onGround) ticks *= 5f;

    return (int) ticks;
  }

  public static boolean canHarvest(IBlockState block, ItemStack tool) {
    if (tool.getItem() instanceof ItemTool) { // lmao I should check that you're using the proper tool
      return true;                            // but you should be using autotool anyway!!!!
    }
    return false;
  }
}