package com.matt.forgehax.util.irc;

import com.matt.forgehax.gui.ClickGui;
import com.matt.forgehax.gui.PromptGui;

import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiScreenServerList;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.util.text.TextFormatting;

public class FormatConverter {
  public static String replaceColorCodes(String msgIn) {
    return msgIn.replace("\u000315", TextFormatting.GRAY.toString())
                .replace("\u000314", TextFormatting.DARK_GRAY.toString())
                .replace("\u000313", TextFormatting.LIGHT_PURPLE.toString())
                .replace("\u000312", TextFormatting.BLUE.toString())
                .replace("\u000311", TextFormatting.AQUA.toString())
                .replace("\u000310", TextFormatting.DARK_AQUA.toString())
                .replace("\u00039", TextFormatting.GREEN.toString())
                .replace("\u00038", TextFormatting.YELLOW.toString())
                .replace("\u00037", TextFormatting.GOLD.toString())
                .replace("\u00036", TextFormatting.DARK_PURPLE.toString())
                .replace("\u00035", TextFormatting.DARK_RED.toString())
                .replace("\u00034", TextFormatting.RED.toString())
                .replace("\u00033", TextFormatting.DARK_GREEN.toString())
                .replace("\u00032", TextFormatting.DARK_BLUE.toString())
                .replace("\u00031", TextFormatting.BLACK.toString()) // Fucking js bot adding useless characters
                .replace("\u000309", TextFormatting.GREEN.toString())
                .replace("\u000308", TextFormatting.YELLOW.toString())
                .replace("\u000307", TextFormatting.GOLD.toString())
                .replace("\u000306", TextFormatting.DARK_PURPLE.toString())
                .replace("\u000305", TextFormatting.DARK_RED.toString())
                .replace("\u000304", TextFormatting.RED.toString())
                .replace("\u000303", TextFormatting.DARK_GREEN.toString())
                .replace("\u000302", TextFormatting.DARK_BLUE.toString())
                .replace("\u000301", TextFormatting.BLACK.toString())
                .replace("\u000300", TextFormatting.RESET.toString())
                .replace("\u00030", TextFormatting.RESET.toString())
                .replace("\u0003", TextFormatting.RESET.toString())
                .replace("\u000F", TextFormatting.RESET.toString())
                .replace("\u0002", TextFormatting.BOLD.toString())
                .replace("\u001D", TextFormatting.ITALIC.toString())
                .replace("\u001F", TextFormatting.UNDERLINE.toString());
                // .replace("\u001E", TextFormatting.STRIKETHROUGH.toString()); // unsupported
  }

  public static String convertToIRCColor(String msgIn) {
    return msgIn.replace(TextFormatting.GRAY.toString(), "\u000315")
                .replace(TextFormatting.DARK_GRAY.toString(), "\u000314")
                .replace(TextFormatting.LIGHT_PURPLE.toString(), "\u000313")
                .replace(TextFormatting.BLUE.toString(), "\u000312")
                .replace(TextFormatting.AQUA.toString(), "\u000311")
                .replace(TextFormatting.DARK_AQUA.toString(), "\u000310")
                .replace(TextFormatting.GREEN.toString(), "\u00039")
                .replace(TextFormatting.YELLOW.toString(), "\u00038")
                .replace(TextFormatting.GOLD.toString(), "\u00037")
                .replace(TextFormatting.DARK_PURPLE.toString(), "\u00036")
                .replace(TextFormatting.DARK_RED.toString(), "\u00035")
                .replace(TextFormatting.RED.toString(), "\u00034")
                .replace(TextFormatting.DARK_GREEN.toString(), "\u00033")
                .replace(TextFormatting.DARK_BLUE.toString(), "\u00032")
                .replace(TextFormatting.BLACK.toString(), "\u00031")
                .replace(TextFormatting.RESET.toString(), "\u000F")
                .replace(TextFormatting.BOLD.toString(), "\u0002")
                .replace(TextFormatting.ITALIC.toString(), "\u001D")
                .replace(TextFormatting.UNDERLINE.toString(), "\u001F");
                // .replace(TextFormatting.STRIKETHROUGH.toString(), "\u001E"); // unsupported
  }

  public static String replaceUserFriendlyCodes(String msgIn) {
    return msgIn.replace("&7", TextFormatting.GRAY.toString())
                .replace("&8", TextFormatting.DARK_GRAY.toString())
                .replace("&d", TextFormatting.LIGHT_PURPLE.toString())
                .replace("&9", TextFormatting.BLUE.toString())
                .replace("&b", TextFormatting.AQUA.toString())
                .replace("&3", TextFormatting.DARK_AQUA.toString())
                .replace("&a", TextFormatting.GREEN.toString())
                .replace("&e", TextFormatting.YELLOW.toString())
                .replace("&6", TextFormatting.GOLD.toString())
                .replace("&5", TextFormatting.DARK_PURPLE.toString())
                .replace("&4", TextFormatting.DARK_RED.toString())
                .replace("&c", TextFormatting.RED.toString())
                .replace("&2", TextFormatting.DARK_GREEN.toString())
                .replace("&1", TextFormatting.DARK_BLUE.toString())
                .replace("&0", TextFormatting.BLACK.toString())
                .replace("&f", TextFormatting.RESET.toString())
                .replace("&r", TextFormatting.RESET.toString())
                .replace("&l", TextFormatting.BOLD.toString())
                .replace("&o", TextFormatting.ITALIC.toString())
                .replace("&n", TextFormatting.UNDERLINE.toString());
                // .replace("&m", TextFormatting.STRIKETHROUGH.toString()); // unsupported
  }

  // This sure doesn't belong here but fuck it it's irrelevant anyway
  public static String getStateFromLastScreen(GuiScreen in) {
    if (in == null) return "Disabled while in-game";
    if (in instanceof ClickGui) return "Disabled from GUI";
    if (in instanceof GuiChat) return "Disabled from chat";
    if (in instanceof PromptGui) return "Disabled from command prompt";
    if (in instanceof GuiMainMenu) return "Quit Minecraft";
    if (in instanceof GuiScreenServerList) return "Disabled while browsing servers?";
    if (in instanceof GuiDisconnected) return "Disabled after being disconnected";
    if (in instanceof GuiConnecting) return "Disabled while connecting";
    if (in instanceof GuiIngameMenu) return "Rage quit! Not even back to main menu!";
    if (in instanceof GuiInventory) return "Items too strong, mod disabled";
    if (in instanceof GuiMultiplayer) return "Could not click ESC 1 more time to go from Server Selector to Main Menu and close Minecraft properly";
    return String.format("Disconnected in an unknown screen (%s), woot!", in.getClass().getSimpleName());
  }
}
