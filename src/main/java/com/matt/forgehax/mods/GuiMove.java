package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;

import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.gui.ClickGui;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiScreenOptionsSounds;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.network.play.client.CPacketCloseWindow;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;

/**
 * Created by Babbaj on 9/5/2017.
 */
@RegisterMod
public class GuiMove extends ToggleMod {

  public final Setting<Double> rotate_speed =
    getCommandStub()
      .builders()
      .<Double>newSettingBuilder()
      .name("speed-rotation")
      .description("Speed at which screen rotates with arrows")
      .min(0D)
      .max(20D)
      .defaultTo(5D)
      .build();

  public final Setting<Boolean> secret_close =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("secret-close")
      .description("Cancel the close-window packet")
      .defaultTo(false)
      .build();
  
  public GuiMove() {
    super(Category.MISC, "GuiMove", false, "move with a gui open, tilt camera with arrows");
  }

  @SubscribeEvent
  public void onPacketSending(PacketEvent.Outgoing.Pre event) {
    if (secret_close.get() && event.getPacket() instanceof CPacketCloseWindow)
      event.setCanceled(true);
  }
  
  @SubscribeEvent
  public void LocalPlayerUpdate(LocalPlayerUpdateEvent event) {
    KeyBinding[] keys = {
        MC.gameSettings.keyBindForward,
        MC.gameSettings.keyBindBack,
        MC.gameSettings.keyBindLeft,
        MC.gameSettings.keyBindRight,
        MC.gameSettings.keyBindJump,
        MC.gameSettings.keyBindSprint
    };
    if (MC.currentScreen instanceof GuiOptions
        || MC.currentScreen instanceof GuiVideoSettings
        || MC.currentScreen instanceof GuiScreenOptionsSounds
        || MC.currentScreen instanceof GuiContainer
        || MC.currentScreen instanceof GuiIngameMenu
        || MC.currentScreen instanceof ClickGui) {
      for (KeyBinding bind : keys) {
        KeyBinding.setKeyBindState(bind.getKeyCode(), Keyboard.isKeyDown(bind.getKeyCode()));
      }

      // this block is ugly, I *may* refactor in the future

      if (Keyboard.isKeyDown(203)) {
        getLocalPlayer().rotationYaw -= rotate_speed.get();
      }
      if (Keyboard.isKeyDown(205)) {
        getLocalPlayer().rotationYaw += rotate_speed.get();
      }
      if (Keyboard.isKeyDown(208)) {
        getLocalPlayer().rotationPitch += rotate_speed.get();
      }
      if (Keyboard.isKeyDown(200)) {
        getLocalPlayer().rotationPitch -= rotate_speed.get();
      }

    } else if (MC.currentScreen == null) {
      for (KeyBinding bind : keys) {
        if (!Keyboard.isKeyDown(bind.getKeyCode())) {
          KeyBinding.setKeyBindState(bind.getKeyCode(), false);
        }
      }
    }
  }
}
