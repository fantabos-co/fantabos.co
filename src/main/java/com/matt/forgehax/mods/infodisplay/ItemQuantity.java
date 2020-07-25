package com.matt.forgehax.mods.infodisplay;

import java.util.stream.IntStream;

import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;

@RegisterMod
public class ItemQuantity extends ToggleMod {

  public ItemQuantity() {
    super(Category.GUI, "ItemQuantity", true, "Shows how many more items you have in inventory");
  }

  public final Setting<Boolean> mainhand =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("mainhand")
          .description("Show the total amount of items in main hand")
          .defaultTo(true)
          .build();

  public final Setting<Boolean> offhand =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("offhand")
          .description("Show the total amount of items in off hand")
          .defaultTo(true)
          .build();

  @Override
  public boolean isInfoDisplayElement() {
    return true;
  }

  public String getInfoDisplayText() {
    StringBuilder builderQuantity = new StringBuilder("Quantity: ");
    ItemStack itemStackM = MC.player.getHeldItemMainhand();
    ItemStack itemStackO = MC.player.getHeldItemOffhand();

    if (mainhand.get()) {
      final long mainHandCount =
        IntStream.rangeClosed(9, 45) // include offhand slot
          .mapToObj(i -> MC.player.inventoryContainer.getSlot(i).getStack())
          .filter(stack -> stack.getItem() == itemStackM.getItem())
          .mapToInt(stack -> stack.getCount())
          .sum();

      builderQuantity.append(String.format("%d", mainHandCount));
    }

    if (offhand.get()) {
      final long offHandCount =
        IntStream.rangeClosed(9, 45) // include offhand slot
          .mapToObj(i -> MC.player.inventoryContainer.getSlot(i).getStack())
          .filter(stack -> stack.getItem() == itemStackO.getItem())
          .mapToInt(stack -> stack.getCount())
          .sum();

      builderQuantity.append(String.format(TextFormatting.GRAY + " [%d]" + TextFormatting.RESET, offHandCount));
    }

    return builderQuantity.toString();
  }
}
