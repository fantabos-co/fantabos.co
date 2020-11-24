package com.matt.forgehax.mods;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.asm.reflection.FastReflection;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import com.matt.forgehax.util.PacketHelper;
import com.matt.forgehax.util.command.Setting;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.network.play.client.CPacketCloseWindow;
import net.minecraft.network.play.client.CPacketUpdateSign;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;

/**
 * Created on 6/27/2020 by Fleyr and Tonio
 */
@RegisterMod
public class AutoSign extends ToggleMod {

  public final Setting<String> l1 =
    getCommandStub()
      .builders()
      .<String>newSettingBuilder()
      .name("l1")
      .description("1st line of text")
      .defaultTo("> Using closed   ")
      .build();

  public final Setting<String> l2 =
    getCommandStub()
      .builders()
      .<String>newSettingBuilder()
      .name("l2")
      .description("2nd line of text")
      .defaultTo("   source clients")
      .build();

  public final Setting<String> l3 =
    getCommandStub()
      .builders()
      .<String>newSettingBuilder()
      .name("l3")
      .description("3rd line of text")
      .defaultTo("   ForgeHax")
      .build();
    
  public final Setting<String> l4 =
    getCommandStub()
      .builders()
      .<String>newSettingBuilder()
      .name("l4")
      .description("4th line of text")
      .defaultTo("   on Top!")
      .build();

  public final Setting<Integer> delay =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("delay")
      .description("Delay in ms for setting sign text")
      .min(0)
      .max(10000)
      .defaultTo(3000)
      .build();
  
  public AutoSign() {
    super(Category.WORLD, "AutoSign", false, "Immediately close sign screen, replace all sign packets text");
  }

  private Map<TileEntitySign, Long> signCache = new ConcurrentHashMap<TileEntitySign, Long>();

  @Override
  protected void onEnabled() {
    signCache.clear();
  }

  @SubscribeEvent
  public void onGuiScreen(GuiScreenEvent event) {
    if (event.getGui() != null && event.getGui() instanceof GuiEditSign) {
      GuiEditSign gui = (GuiEditSign) event.getGui();
      TileEntitySign sign = FastReflection.Fields.GuiEditSign_tileSign.get(gui);
      signCache.put(sign, System.currentTimeMillis());
      MC.player.closeScreen();
    }
  }

  @SubscribeEvent
  public void onSignUpdate(PacketEvent.Outgoing.Pre event) {
    if (event.getPacket() instanceof CPacketCloseWindow ||
        event.getPacket() instanceof CPacketUpdateSign) {
      if (!PacketHelper.isIgnored(event.getPacket())) {
        event.setCanceled(true);
      }
    }
  }

  @SubscribeEvent
  public void onClientTick(TickEvent.ClientTickEvent event) {
    long now = System.currentTimeMillis();
    ITextComponent[] text = {
      new TextComponentString(l1.get()),
      new TextComponentString(l2.get()),
      new TextComponentString(l3.get()),
      new TextComponentString(l4.get()),
    };
    for (TileEntitySign s : signCache.keySet()) {
      if (now > signCache.get(s) + delay.get()) {
        PacketHelper.ignoreAndSend(new CPacketUpdateSign(s.getPos(), text));
        signCache.remove(s);
      }
    }
  }
}