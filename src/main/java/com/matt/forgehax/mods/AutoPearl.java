package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getWorld;
import static com.matt.forgehax.Helper.getPlayerController;

import com.matt.forgehax.Helper;
import com.matt.forgehax.mods.services.HotbarSelectionService.ResetFunction;
import com.matt.forgehax.util.entity.LocalPlayerInventory;
import com.matt.forgehax.util.entity.LocalPlayerInventory.InvItem;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import org.lwjgl.input.Keyboard;
import java.util.Comparator;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.item.ItemEnderPearl;
import net.minecraft.util.EnumHand;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;

//Coded by Fleyr (Thanks Tonio <3) on 7th August 2020

@RegisterMod
public class AutoPearl extends ToggleMod {
  private final KeyBinding bindMacro = new KeyBinding("ThrowPearl", Keyboard.KEY_NONE, "ForgeHax");
  
    /*  public final Setting<Integer> delay =
          getCommandStub()
              .builders()
              .<Integer>newSettingBuilder()
              .name("delay")
              .description("Delay in ticks beore reverting to the prev slot")
              .defaultTo(5)
              .min(0)
              .max(60)
              .build();
    */

  public AutoPearl() {
    super(Category.COMBAT, "AutoPearl", false, "Automatically throw an ender pearl and switch to the old item slot");
    ClientRegistry.registerKeyBinding(this.bindMacro);
  }

  @SubscribeEvent
  public void onKeyPressed(InputEvent.KeyInputEvent event){
    if (bindMacro.isPressed() && getLocalPlayer() != null) {
      InvItem items_stack = LocalPlayerInventory.getHotbarInventory()
      .stream()
      .filter(InvItem::nonNull)
      .filter(held_item ->(held_item.getItem()) instanceof ItemEnderPearl)
      .max(Comparator.comparingInt(LocalPlayerInventory::getHotbarDistance))
      .orElse(InvItem.EMPTY);
    
      if (items_stack == null || items_stack.equals(InvItem.EMPTY)) {
        Helper.printError("Out of Ender pearls");
        return;
      }
      ResetFunction func = LocalPlayerInventory.setSelected(items_stack);
      getPlayerController().processRightClick(getLocalPlayer(), getWorld(), EnumHand.MAIN_HAND);
      func.revert();
    }
  }  
}
