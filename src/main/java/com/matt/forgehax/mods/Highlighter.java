package com.matt.forgehax.mods;

// import com.mojang.blaze3d.systems.RenderSystem;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.color.Color;
import com.matt.forgehax.util.entity.EnchantmentUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.client.event.GuiContainerEvent;
import net.minecraftforge.fml.client.config.GuiUtils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.util.function.Predicate;

@RegisterMod
public class Highlighter extends ToggleMod {
    private final Setting<String> find =
    getCommandStub()
      .builders()
      .<String>newSettingBuilder()
      .name("find")
      .description("Match items with this name")
      .defaultTo("mending")
      .build();

  public Highlighter() {
    super(Category.MISC, "Highlighter", false, "Highlight container contents");
  }

  private boolean isEnchanted(ItemStack stack) {
    return Items.ENCHANTED_BOOK.equals(stack.getItem()) || stack.isItemEnchanted();
  }

  private NBTTagList getEnchantmentNBT(ItemStack stack) {
    return Items.ENCHANTED_BOOK.equals(stack.getItem())
            ? stack.getTagCompound().getTagList("StoredEnchantments", 10)  // EnchantedBookItem.getEnchantments(stack)
            : stack.getEnchantmentTagList();
  }

  private boolean shouldHighlight(ItemStack stack, Predicate<String> matcher) {
    if (stack.isEmpty()) {
      return false;
    } else if (matcher.test(stack.getDisplayName())) {
      return true;
    } else if (isEnchanted(stack) &&
        EnchantmentUtils.getEnchantments(getEnchantmentNBT(stack)).stream()
            .map(en -> en.toString()) // wtf using getEnchantment().getName() gives super weird names
            .anyMatch(matcher)) {
      return true;
    }
    return false; // default case
  }

  @SubscribeEvent
  public void onGuiContainerDrawEvent(GuiContainerEvent.DrawForeground event) {
    GlStateManager.pushMatrix();
    GlStateManager.enableDepth();
    // TODO don't mess with durability bars

    final String matching = find.get().toLowerCase();
    for (Slot slot : event.getGuiContainer().inventorySlots.inventorySlots) {
      ItemStack stack = slot.getStack();
      if (shouldHighlight(stack, str -> str.toLowerCase().contains(matching))) {
        GuiUtils.drawGradientRect(0,
            slot.xPos, slot.yPos,
            slot.xPos + 16, slot.yPos + 16,
            Color.of(218,165,32, 200).toBuffer(),
            Color.of(189,183,107, 200).toBuffer());
      }
    }

    GlStateManager.popMatrix();
  }
}