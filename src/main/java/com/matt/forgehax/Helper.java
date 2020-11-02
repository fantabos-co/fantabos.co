package com.matt.forgehax;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.matt.forgehax.mods.BetterChat;
import com.matt.forgehax.util.FileManager;
import com.matt.forgehax.util.command.CommandGlobal;
import com.matt.forgehax.util.mod.loader.ModManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.NetworkManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.World;
import net.minecraftforge.fml.client.FMLClientHandler;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Optional;
import java.util.Scanner;

/**
 * Created on 4/25/2017 by fr1kin
 */
public class Helper implements Globals {

  @Nullable
  public static GuiScreen getCurrentScreen() {
    return getMinecraft().currentScreen;
  }

  public static CommandGlobal getGlobalCommand() {
    return CommandGlobal.getInstance();
  }
  
  public static Minecraft getMinecraft() {
    return MC;
  }
  
  public static ModManager getModManager() {
    return ModManager.getInstance();
  }
  
  public static FileManager getFileManager() {
    return FileManager.getInstance();
  }
  
  public static Logger getLog() {
    return LOGGER;
  }

  public static Entity getRenderEntity() {
    return MC.getRenderViewEntity();
  }

  public static EntityPlayerSP getLocalPlayer() {
    return MC.player;
  }
  
  @Nullable
  public static Entity getRidingEntity() {
    if (getLocalPlayer() != null) {
      return getLocalPlayer().getRidingEntity();
    } else {
      return null;
    }
  }
  
  public static Optional<Entity> getOptionalRidingEntity() {
    return Optional.ofNullable(getRidingEntity());
  }
  
  // Returns the riding entity if present, otherwise the local player
  @Nullable
  public static Entity getRidingOrPlayer() {
    return getRidingEntity() != null ? getRidingEntity() : getLocalPlayer();
  }
  
  @Nullable
  public static WorldClient getWorld() {
    return MC.world;
  }
  
  public static World getWorld(Entity entity) {
    return entity.getEntityWorld();
  }
  
  public static World getWorld(TileEntity tileEntity) {
    return tileEntity.getWorld();
  }
  
  @Nullable
  public static NetworkManager getNetworkManager() {
    return FMLClientHandler.instance().getClientToServerNetworkManager();
  }
  
  public static PlayerControllerMP getPlayerController() {
    return MC.playerController;
  }
  
  public static void printMessageNaked(
      String startWith, String message, Style firstStyle, Style secondStyle) {
    if (!Strings.isNullOrEmpty(message)) {
      if (message.contains("\n")) {
        Scanner scanner = new Scanner(message);
        scanner.useDelimiter("\n");
        Style s1 = firstStyle;
        Style s2 = secondStyle;
        while (scanner.hasNext()) {
          printMessageNaked(startWith, scanner.next(), s1, s2);
          // alternate between colors each newline
          Style cpy = s1;
          s1 = s2;
          s2 = cpy;
        }
      } else {
        ITextComponent out = timestamp()
          .appendSibling(
            new TextComponentString(startWith + message.replaceAll("\r", "")).setStyle(firstStyle)
            );
        outputMessage(out);
      }
    }
  }

  // Check if timestamps should be added and returns a formatted timestamp string or an empty string
  // Making this in outputMessage() would have added a timestamp for every line, I want
  //      one for every command or warning: just one for multiline cmds!
  // I don't really like this but it should not matter much anyway
  public static ITextComponent timestamp() {
    String timestamp = "";
    ITextComponent out = new TextComponentString("");
    try {
      if (getModManager().get(BetterChat.class).get().isEnabled() &&
          getModManager().get(BetterChat.class).get().timestamps.get()) {
        timestamp = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime()) + " \u23d0 ";
        out = getFormattedText(timestamp, TextFormatting.DARK_GRAY, false, false);
      }
    } catch (Exception e) {
      // ignore, default to no timestamps, maybe mod not loaded? Maybe this whole try is unnecessary?
      LOGGER.warn("Could not get BetterChat! Make better code!");
    }
    return out;
  }

  public static void outputMessage(ITextComponent text) {
    if (getLocalPlayer() != null)
      getLocalPlayer().sendMessage(text);
  }
  
  public static void printMessageNaked(String append, String message, Style style) {
    printMessageNaked(append, message, style, style);
  }
  
  public static void printMessageNaked(String append, String message) {
    printMessageNaked(
        append,
        message,
        new Style().setColor(TextFormatting.GRAY),
        new Style().setColor(TextFormatting.DARK_GRAY));
  }
  
  public static void printMessageNaked(String message) {
    printMessageNaked("", message);
  }
  
  private static HoverEvent defaultHover() {
    return new HoverEvent(HoverEvent.Action.SHOW_TEXT,
          getFormattedText("ForgeHax v2.10 - tonio's private fork", TextFormatting.GRAY, true, false));
  }

  private static ClickEvent defaultClick() {
    return new ClickEvent(ClickEvent.Action.RUN_COMMAND, ".help");
  }

  // Will append '[FH] ' in front
  public static void printMessage(String message) {
    if (!Strings.isNullOrEmpty(message)) {
      printMessageNaked("[FH] " + message);
    }
  }
  
  public static void printMessage(String format, Object... args) {
    printMessage(String.format(format, args));
  }

  public static ITextComponent getFormattedText(String text, TextFormatting color,
      boolean bold, boolean italic, ClickEvent onClick, HoverEvent onHover) {
        return new TextComponentString(text.replaceAll("\r", ""))
        .setStyle(new Style()
            .setColor(color)
            .setBold(bold)
            .setItalic(italic)
            .setClickEvent(onClick)
            .setHoverEvent(onHover)
        );
  }
  
  public static ITextComponent getFormattedText(String text, TextFormatting color,
      boolean bold, boolean italic) {
    return new TextComponentString(text.replaceAll("\r", ""))
        .setStyle(new Style()
            .setColor(color)
            .setBold(bold)
            .setItalic(italic)
        );
  }

  public static void printLog(ITextComponent text) {
    outputMessage(timestamp()
      .appendSibling(
        getFormattedText("[FH] ", TextFormatting.DARK_GRAY, true, false)
          .appendSibling(text)
      )
    );
  }

  public static void printLog(String format, Object... args) {
    printLog(getFormattedText(String.format(format, args).trim(),
                    TextFormatting.GRAY, false, false));
  }
  
  public static void printInform(ITextComponent text) {
    outputMessage(timestamp()
      .appendSibling(
        getFormattedText("[FH] ", TextFormatting.DARK_AQUA, true, false)
          .appendSibling(text)
      )
    );
  }

  public static void printInform(String format, Object... args) {
    printInform(getFormattedText(String.format(format, args).trim(),
                    TextFormatting.GRAY, false, false));
  }
  
  public static void printWarning(ITextComponent text) {
    outputMessage(timestamp()
      .appendSibling(
        getFormattedText("[FH] ", TextFormatting.GOLD, true, false)
          .appendSibling(text)
      )
    );
  }

  public static void printWarning(String format, Object... args) {
    printWarning(getFormattedText(" " + String.format(format, args).trim(),
      TextFormatting.GRAY, false, false));
  }
  
  public static void printError(ITextComponent text) {
    outputMessage(timestamp()
      .appendSibling(
        getFormattedText("[FH] ", TextFormatting.DARK_RED, true, false)
          .appendSibling(text)
      )
    );
  }

  public static void printError(String format, Object... args) {
    printError(getFormattedText(" " + String.format(format, args).trim(),
              TextFormatting.GRAY, false, false));
  }
  
  public static void printStackTrace(Throwable t) {
    getLog().error(Throwables.getStackTraceAsString(t));
  }
  
  public static void handleThrowable(Throwable t) {
    getLog().error(String.format("[%s] %s",
        t.getClass().getSimpleName(),
        Strings.nullToEmpty(t.getMessage())));
    
    if (t.getCause() != null) {
      handleThrowable(t.getCause());
    }
    printStackTrace(t);
  }
  
  public static void reloadChunks() {
    // credits to 0x22
    if (getWorld() != null && getLocalPlayer() != null) {
      MC.addScheduledTask(
          () -> {
            int x = (int) getLocalPlayer().posX;
            int y = (int) getLocalPlayer().posY;
            int z = (int) getLocalPlayer().posZ;
            
            int distance = MC.gameSettings.renderDistanceChunks * 16;
            
            MC.renderGlobal.markBlockRangeForRenderUpdate(
                x - distance, y - distance, z - distance, x + distance, y + distance, z + distance);
          });
    }
  }
  
  public static void reloadChunksHard() {
    MC.addScheduledTask(
        () -> {
          if (getWorld() != null && getLocalPlayer() != null) {
            MC.renderGlobal.loadRenderers();
          }
        });
  }

  public static int getPlayerDirection() {
    return (MathHelper.floor((double) (Minecraft.getMinecraft().player.rotationYaw * 8.0F / 360.0F) + 0.5D) & 7);
  }
}
