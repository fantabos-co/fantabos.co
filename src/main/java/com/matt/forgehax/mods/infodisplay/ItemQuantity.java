package com.matt.forgehax.mods.infodisplay;

import java.util.stream.IntStream;

import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TextFormatting;

import static com.matt.forgehax.Helper.getLocalPlayer;

@RegisterMod
public class ItemQuantity extends ToggleMod {

  public final Setting<Boolean> mainHand =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("mainhand")
          .description("Show the total amount of items in main hand.")
          .defaultTo(true)
          .build();

  public final Setting<Boolean> offHand =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("offhand")
          .description("Show the total amount of items in off hand.")
          .defaultTo(true)
          .build();

    public ItemQuantity() {
      super(Category.GUI, "ItemQuantity", true, "Shows how many more items you have in inventory.");
    }

  @Override
  public boolean isInfoDisplayElement() {
    return true;
  }

  public String getInfoDisplayText() {
    StringBuilder builderQuantity = new StringBuilder("Quantity:");
    ItemStack itemStackM = getLocalPlayer().getHeldItemMainhand();
    ItemStack itemStackO = getLocalPlayer().getHeldItemOffhand();

      if (!mainHand.get() && !offHand.get()) {
          builderQuantity.append(" [Disabled]");
      } else {
        if (mainHand.get()) {
          builderQuantity.append(String.format(" %d", amountCount(itemStackM)));
        }

        if (offHand.get()) {
          builderQuantity.append(String.format(TextFormatting.GRAY + " [%d]" + TextFormatting.RESET, amountCount(itemStackO)));
        }
      }

    return builderQuantity.toString();
  }

  int amountCount(ItemStack hand) {
    return IntStream.rangeClosed(9, 45) // include offhand slot
      .mapToObj(i -> getLocalPlayer().inventoryContainer.getSlot(i).getStack())
      .filter(stack -> stack.getItem() == hand.getItem())
      .mapToInt(ItemStack::getCount)
      .sum();
  }
}
