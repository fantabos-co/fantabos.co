package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getModManager;
import static com.matt.forgehax.Helper.getLocalPlayer;

import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import java.util.OptionalInt;
import java.util.stream.IntStream;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class AutoTotemMod extends ToggleMod {
  
  private final int OFFHAND_SLOT = 45;
  
  public AutoTotemMod() {
    super(Category.COMBAT, "AutoTotem", false, "Automatically move totems to off-hand");
  }

  private final Setting<Boolean> allowGui =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("allow-gui")
          .description("Lets AutoTotem work in inventory")
          .defaultTo(false)
          .build();

  private final Setting<Boolean> force_equip =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("force-equip")
          .description("Force equip a totem even with full offhand when below threshold hp")
          .defaultTo(false)
          .build();

  private final Setting<Integer> force_threshold =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("threshold")
          .description("Replace offhand item when below this many hp")
          .defaultTo(20)
          .min(0)
          .max(20)
          .build();

  @Override
  public String getDisplayText() {
    final long totemCount =
        IntStream.rangeClosed(9, 45) // include offhand slot
            .mapToObj(i -> MC.player.inventoryContainer.getSlot(i).getStack().getItem())
            .filter(stack -> stack == Items.TOTEM_OF_UNDYING)
            .count();
    return (super.getDisplayText() + " [" + TextFormatting.YELLOW + totemCount + TextFormatting.RESET + "]");
  }
  
  @SubscribeEvent
  public void onPlayerUpdate(LocalPlayerUpdateEvent event) {
    if (getOffhand().getItem().equals(Items.TOTEM_OF_UNDYING)) return; // nothing to do!
    
    if (MC.currentScreen instanceof GuiInventory && !allowGui.getAsBoolean()) {
      return; // if in inventory
    }

    if (!getOffhand().equals(ItemStack.EMPTY)) {
      if (!(force_equip.get() && getLocalPlayer().getHealth() <= force_threshold.get())) {
        return;
      } else {
        MC.playerController.windowClick(0, OFFHAND_SLOT, 0, ClickType.QUICK_MOVE, getLocalPlayer());
      }
    }
    
    findItem(Items.TOTEM_OF_UNDYING)
        .ifPresent(
            slot -> {
              invPickup(slot);
              invPickup(OFFHAND_SLOT);
              if (getModManager().get(MatrixNotifications.class).get().isEnabled()) {
                getModManager().get(MatrixNotifications.class).get().send_notify("Equipped new Totem");
              }
            });
  }
  
  private void invPickup(final int slot) {
    MC.playerController.windowClick(0, slot, 0, ClickType.PICKUP, MC.player);
  }
  
  private OptionalInt findItem(final Item ofType) {
    for (int i = 9; i <= 44; i++) {
      if (MC.player.inventoryContainer.getSlot(i).getStack().getItem() == ofType) {
        return OptionalInt.of(i);
      }
    }
    return OptionalInt.empty();
  }
  
  private ItemStack getOffhand() {
    return MC.player.getItemStackFromSlot(EntityEquipmentSlot.OFFHAND);
  }
}
