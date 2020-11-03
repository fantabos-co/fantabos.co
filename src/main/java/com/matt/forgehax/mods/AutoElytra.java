package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getPlayerController;
import static com.matt.forgehax.Helper.getNetworkManager;

import com.matt.forgehax.asm.events.PacketEvent;

import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.SimpleTimer;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.init.Items;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketEntityAction.Action;
import net.minecraft.network.play.server.SPacketEntityMetadata;
import net.minecraft.network.play.server.SPacketSetSlot;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Created on 14/07/2020 by tonio coz Constantiam has not that good anti-elytra
 * 
 *	Phobos found this who knows how and right after they included it, the 1st major leak
 *		happened and literally everyone had it. It's patched anyway so enjoy.
 *		If you check phobos src you can see most of my weird SubscribeEvents,
 *		and most importantly the fucking struggle with the inventory indexes.
 *
 */

@RegisterMod
public class AutoElytra extends ToggleMod {

  private final Setting<Boolean> no_disequip =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("no-disequip")
      .description("Prevent elytra from being disequipped")
      .defaultTo(false)
      .build();

  private final Setting<Boolean> disequip =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("disequip")
      .description("Remove chest on enable")
      .defaultTo(false)
      .build();

  private final Setting<Boolean> reopen =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("reopen")
      .description("Open elytra automatically")
      .defaultTo(true)
      .build();

  private final Setting<Integer> reopen_interval =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("open-cd")
      .description("Cooldown in ms of elytra reopen packets")
      .min(0)
      .max(3000)
      .defaultTo(500)
      .build();

  private final Setting<Boolean> reequip =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("reequip")
      .description("Reequip elytras automatically")
      .defaultTo(false)
      .build();
    
  private final Setting<Integer> cooldown_reequip =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("equip-cd")
      .description("Cooldown in ms of elytra reequip actions")
      .min(0)
      .max(5000)
      .defaultTo(20)
      .build();

  private final Setting<Boolean> cancel_ghost =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("ghost-cancel")
      .description("Prevent elytras appearing in inventory")
      .defaultTo(false)
      .build();

  private final Setting<Boolean> discreet =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("discreet")
      .description("Only reequip when midair and not wearing an elytra")
      .defaultTo(true)
      .build();

  private final Setting<Boolean> cancel_close =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("cancel-close")
      .description("Cancel Elytra Close packets")
      .defaultTo(false)
      .build();
  
  public AutoElytra() {
    super(Category.EXPLOIT, "AutoElytra", false, "Automatically re-equips and/or deploys elytra");
  }

  @Override
  protected void onEnabled() {
    if (disequip.get() && !getLocalPlayer().inventory.getStackInSlot(6).equals(ItemStack.EMPTY))
      getPlayerController().windowClick(0, 6, // ChestPlate
        0, ClickType.QUICK_MOVE, getLocalPlayer());
  }

  @Override
  protected void onDisabled() {
    if (disequip.get() && !getLocalPlayer().inventory.getStackInSlot(6).equals(ItemStack.EMPTY))
      getPlayerController().windowClick(0, 6, // ChestPlate
        0, ClickType.QUICK_MOVE, getLocalPlayer());
  }

  int cooldown = 0;
  private SimpleTimer timer_takeoff = new SimpleTimer();
  private SimpleTimer timer_reequip = new SimpleTimer();

  @SubscribeEvent
  public void onIncomingPacket(PacketEvent.Incoming.Pre event) {
    if (getLocalPlayer() == null) return;
    if (event.getPacket() instanceof SPacketSetSlot) {
      SPacketSetSlot packet = (SPacketSetSlot) event.getPacket();
      if (no_disequip.get() && packet.getSlot() == 6) {
        event.setCanceled(true);
      }
      if (packet.getStack().getItem().equals(Items.ELYTRA)) {
        if (cancel_ghost.get())
          event.setCanceled(true);
        getLocalPlayer().getItemStackFromSlot(EntityEquipmentSlot.CHEST) // update elytra damage
                        .setItemDamage(packet.getStack().getItemDamage());;
      }
    }
    if (cancel_close.get() && getLocalPlayer().isElytraFlying() &&
        event.getPacket() instanceof SPacketEntityMetadata) {
      SPacketEntityMetadata MetadataPacket = event.getPacket();
      if (MetadataPacket.getEntityId() == getLocalPlayer().getEntityId()) {
        event.setCanceled(true);
      }
    }
  }

  @SubscribeEvent
  public void onTick(TickEvent.ClientTickEvent event) {
    if (getLocalPlayer() == null ||
        !timer_reequip.hasTimeElapsed(cooldown_reequip.get()) || 
        MC.currentScreen instanceof GuiInventory ||
        (getLocalPlayer().fallDistance <= 0 && discreet.get()))
      return;
    for (int i = 0; i < 36; i++) {
      ItemStack item = getLocalPlayer().inventory.getStackInSlot(i);
      if (item.getItem().equals(Items.ELYTRA)) {
        getPlayerController().windowClick(0, (i < 9 ? i + 36 : i), // These fucking indexes omg
                  0, ClickType.QUICK_MOVE, getLocalPlayer());
        timer_reequip.start();
        return;
      }
    }
  }

  @SubscribeEvent
  public void onUpdate(LocalPlayerUpdateEvent event) {
    if (reopen.get() && timer_takeoff.hasTimeElapsed(reopen_interval.get()) &&
        !getLocalPlayer().isElytraFlying() && getLocalPlayer().fallDistance > 0F) {
      getNetworkManager()
        .sendPacket(new CPacketEntityAction(getLocalPlayer(), Action.START_FALL_FLYING));
      timer_takeoff.start();
    }
  }
}
