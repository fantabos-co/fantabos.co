package com.matt.forgehax.mods.services;

import com.matt.forgehax.gui.PromptGui;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import java.util.List;

import com.mojang.realmsclient.gui.ChatFormatting;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import static com.matt.forgehax.Helper.*;
import static com.matt.forgehax.Helper.printInform;

/**
 * Created by Babbaj on 4/10/2018.
 * Updated by Tonio and Overfloyd, made standalone and moved out
 */
@RegisterMod
public class MainMenuGuiService extends ServiceMod {


  public MainMenuGuiService() {
    super("MainMenuGuiService", "Replace \"Realms\" button");
  }

  @Override
  public void onLoad() {

  }

  @SubscribeEvent
  public void onGui(GuiScreenEvent.InitGuiEvent.Post event) {
    if (event.getGui() instanceof GuiMainMenu) {
      List<GuiButton> menu = event.getButtonList();
      for (GuiButton b : menu) {
        if (b.id == 14) {
          b.id = 666;
          b.displayString = "ForgeHax Prompt";
        }
      }
    }
  }

  @SubscribeEvent
  public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent event) {
    if (event.getButton().id == 666) {
      MC.displayGuiScreen(new PromptGui(""));
    }
  }
}
