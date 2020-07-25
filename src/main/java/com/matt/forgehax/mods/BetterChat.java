package com.matt.forgehax.mods;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.matt.forgehax.asm.ForgeHaxHooks;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@RegisterMod
public class BetterChat extends ToggleMod {

  public final Setting<Boolean> timestamps =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("timestamps")
      .description("Add timestamps to chat")
      .defaultTo(true)
      .build();

  public final Setting<Boolean> hide_background =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("background-hide")
      .description("Hide chat background")
      .defaultTo(true)
      .build();

  public final Setting<Boolean> infinite =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("infinite")
      .description("Make chat history infinite. To disable, set this false and turn on mod")
      .defaultTo(true)
      .build();

  public final Setting<Boolean> autoscale =
      getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("autoscale")
        .description("Scale your chat back to full size when open")
        .defaultTo(true)
        .build();
  
  public BetterChat() {
    super(Category.CHAT, "BetterChat", false, "Improve in-game chat");
  }

  private float scale;

  @Override
  protected void onEnabled() {
    scale = MC.gameSettings.chatScale;
  }

  @Override
  protected void onDisabled() {
    ForgeHaxHooks.doHideChatBackground = false;
    // Don't disable the Chat size hook to prevent accidental history wipes
  }

  @SubscribeEvent(priority = EventPriority.LOW)
  public void onChat(ClientChatReceivedEvent event) {
    if (!timestamps.get()) return;
    String timeStamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());
    event.setMessage(new TextComponentString(TextFormatting.DARK_GRAY + timeStamp + " \u23d0 " + TextFormatting.RESET)
                          .appendSibling(event.getMessage()));
  }

  @SubscribeEvent // Doing this on tick is overkill but flipping booleans should not be that bad and allows 1 mod
  public void onTick(TickEvent.ClientTickEvent event) {                             // for multiple features
    if (MC.currentScreen instanceof GuiChat) {
      ForgeHaxHooks.doHideChatBackground = false;
      if (autoscale.get())
        MC.gameSettings.chatScale = 1f;
    } else {
      ForgeHaxHooks.doHideChatBackground = hide_background.get();
      if (autoscale.get()) {
        if (MC.gameSettings.chatScale != 1f) scale = MC.gameSettings.chatScale;
        else MC.gameSettings.chatScale = scale;
      }
    }
    ForgeHaxHooks.doPreventChatSizeLimit = infinite.get();
  }
} 
