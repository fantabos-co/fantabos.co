package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;

import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.entity.LocalPlayerInventory;
import com.matt.forgehax.util.entity.LocalPlayerInventory.InvItem;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Enchantments;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class AutoMend extends ToggleMod {

  public enum MendMode {
    OFFHAND,
    DISEQUIP,
    BOTH
  }

  public final Setting<MendMode> mode =
    getCommandStub()
      .builders()
      .<MendMode>newSettingEnumBuilder()
      .name("mode")
      .description("The mending mode [offhand/disequip/both]")
      .defaultTo(MendMode.DISEQUIP)
      .build();

  private final Setting<Integer> delay =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("delay")
      .description("Delay in ticks between inventory operations")
      .min(0)
      .max(60)
      .defaultTo(5)
      .build();

  private final Setting<Boolean> xp_only =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("xp-only")
      .description("Only work if shooting xp")
      .defaultTo(false)
      .build();
  
  public AutoMend() {
    super(
        Category.PLAYER, "AutoMend", false, "Remove fully repaired items, equip in offhand new items to repair");
  }

  private int cooldown = 0;
  
  private boolean isMendable(InvItem item) {
    return item.isItemDamageable()
        && EnchantmentHelper.getEnchantmentLevel(Enchantments.MENDING, item.getItemStack()) > 0;
  }
  
  private boolean isDamaged(InvItem item) {
    return item.getItemStack().isItemDamaged();
  }
  
  @SubscribeEvent
  public void onUpdate(LocalPlayerUpdateEvent event) {
    if (!(LocalPlayerInventory.getOpenContainer() instanceof ContainerPlayer)) {
      return;
    }

    if (xp_only.get() && 
        !(getLocalPlayer().getHeldItemMainhand().getItem().equals(Items.EXPERIENCE_BOTTLE) &&
          MC.gameSettings.keyBindUseItem.isKeyDown())) {
          return;
    }

    if (cooldown > 0) {
      cooldown--;
      return;
    }

    switch(mode.get()) {
      case BOTH:
      case OFFHAND:
        InvItem current = LocalPlayerInventory.getSelected();
      
        Optional.of(LocalPlayerInventory.getOffhand())
            .filter(this::isMendable)
            .filter(item -> !isDamaged(item))
            .ifPresent(
                offhand ->
                    LocalPlayerInventory.getSlotInventory()
                        .stream()
                        .filter(this::isMendable)
                        .filter(this::isDamaged)
                        .filter(inv -> inv.getIndex() != current.getIndex())
                        .max(Comparator.comparingInt(InvItem::getDamage))
                        .ifPresent(
                            inv -> {
                              // pick up
                              LocalPlayerInventory.sendWindowClick(inv, 0, ClickType.PICKUP);
                              // place in offhand
                              LocalPlayerInventory.sendWindowClick(offhand, 0, ClickType.PICKUP);
                              // place shovel back
                              LocalPlayerInventory.sendWindowClick(inv, 0, ClickType.PICKUP);
                              // set cooldown
                              cooldown = delay.get();
                            }));
        if (mode.get() != MendMode.BOTH || cooldown > 0) break;
      case DISEQUIP:
        for (InvItem item : LocalPlayerInventory.getArmorInventory()) {
          if (!item.getItemStack().equals(ItemStack.EMPTY) && isMendable(item) && !isDamaged(item)) {
            LocalPlayerInventory.sendWindowClick(item, 0, ClickType.QUICK_MOVE);
            cooldown = delay.get();
            break;
          }
        }           
    }
  }
}
