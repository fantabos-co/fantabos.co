package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getPlayerController;

import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.entity.LocalPlayerInventory;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class AutoArmor extends ToggleMod {
  
  // private final Setting<Boolean> replace =
  //   getCommandStub()
  //     .builders()
  //     .<Boolean>newSettingBuilder()
  //     .name("replace")
  //     .description("Replace worn pieces with better ones")
  //     .defaultTo(false)
  //     .build();

   private final Setting<Boolean> auto_disable =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("disable")
      .description("Disable mod once armor is equipped")
      .defaultTo(false)
      .build();

  private final Setting<Integer> delay =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("delay")
      .description("Delay in ticks between armor switches")
      .min(0)
      .max(60)
      .defaultTo(5)
      .build();

  private final Setting<Boolean> xp_pause =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("xp-pause")
      .description("Don't re-equip while shooting xp, for AutoMend compatibility")
      .defaultTo(false)
      .build();
  
  public AutoArmor() {
    super(Category.COMBAT, "AutoArmor", false, "Automatically wear armor");
  }

  int cooldown = 0;

  @SubscribeEvent
  public void onLocalPlayerUpdate(LocalPlayerUpdateEvent event) {
    if (cooldown > 0) {
      cooldown--;
      return;
    }
    if (getLocalPlayer() == null) return;
    if (!(LocalPlayerInventory.getOpenContainer() instanceof ContainerPlayer)) {
      return;
    }

    if (xp_pause.get() && getLocalPlayer().getHeldItemMainhand().getItem().equals(Items.EXPERIENCE_BOTTLE) &&
          MC.gameSettings.keyBindUseItem.isKeyDown()) {
      return;
    }

    for (int slot = 0; slot < 4; slot++) {
      ItemStack armor = getLocalPlayer().inventory.armorItemInSlot(slot); // WTF u doing with indexes mojang!?!?!?!
      if (armor.equals(ItemStack.EMPTY)) {
        for (int i = 0; i < 36; i++) {
          ItemStack item = getLocalPlayer().inventory.getStackInSlot(i);
          if (item.getItem() instanceof ItemArmor) {
            if (((ItemArmor) item.getItem()).armorType.ordinal() == slot + 2) { // WTF WHY!!!!!
              getPlayerController().windowClick(0, (i < 9 ? i + 36 : i), // These fucking indexes omg
                        0, ClickType.QUICK_MOVE, getLocalPlayer());
              cooldown = delay.get();
              return;
            }
          }
        }
      }
    }
    if (auto_disable.get())
      this.disable(false);
  }
}
