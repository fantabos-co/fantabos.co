package com.matt.forgehax.mods;

import com.matt.forgehax.asm.ForgeHaxHooks;
import com.matt.forgehax.asm.events.DrawPingEvent;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.draw.SurfaceHelper;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.TextTable.Alignment;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;

/**
 * Created by Babbaj on 9/2/2017.
 */
@RegisterMod
public class ExtraTab extends ToggleMod {

  private final Setting<Boolean> smart =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("smart")
      .description("Don't increase tablist when at 60 players or less")
      .defaultTo(true)
      .build();

  private final Setting<Boolean> number_ping =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("number-ping")
      .description("Show numbers instead of ping bars")
      .defaultTo(false)
      .build();

  private final Setting<Integer> number_offset =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("offset-number")
      .description("Offset of number from corner")
      .min(0)
      .defaultTo(2)
      .build();
      
  private final Setting<Double> number_scale =
    getCommandStub()
      .builders()
      .<Double>newSettingBuilder()
      .name("scale-number")
      .description("Scale of ping numbers")
      .min(0D)
      .defaultTo(0.5D)
      .build();

  private final Setting<Boolean> number_color =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("color-number")
      .description("Color ping numbers")
      .defaultTo(true)
      .build();

  public ExtraTab() {
    super(Category.MISC, "BetterTab", false, "Make tablist better sized and display ping numbers");
  }
  
  @Override
  public void onEnabled() {
    ForgeHaxHooks.doIncreaseTabListSize = true;
  }
  
  @Override
  public void onDisabled() {
    ForgeHaxHooks.doIncreaseTabListSize = false;
  }

  @SubscribeEvent
  public void onTick(PlayerTickEvent event) {
    if (smart.get() && MC.getConnection() != null && MC.getConnection().getPlayerInfoMap() != null) {
      ForgeHaxHooks.doIncreaseTabListSize =
        (MC.getConnection().getPlayerInfoMap().size() > 60);
    }
  }

  @SubscribeEvent
  public void onPingDrawn(DrawPingEvent event) {
    if (number_ping.get()) {
      event.setCanceled(true);
      int ping = event.getPlayer().getResponseTime();
      String latency = (ping == 0 ? // So I can obfuscate 0 ping
            TextFormatting.DARK_GRAY.toString() + TextFormatting.OBFUSCATED + ping + TextFormatting.RESET :
            String.format("%d", ping));
      SurfaceHelper.drawTextAlign(latency, event.getX1() + event.getX2() - number_offset.get(),
                                  event.getY() + number_offset.get(), getColorPing(ping),
                                  number_scale.get(), true, Alignment.RIGHT.ordinal());
    }
  }

  private int getColorPing(int ping) {
    if (!number_color.get()) return Colors.GRAY.toBuffer();
    if (ping > 1000) return Colors.DARK_GRAY.toBuffer();
    if (ping > 500) return Colors.DARK_RED.toBuffer();
    if (ping > 300) return Colors.RED.toBuffer();
    if (ping > 180) return Colors.GOLD.toBuffer();
    if (ping > 100) return Colors.YELLOW.toBuffer();
    if (ping > 70) return Colors.GREEN.toBuffer();
    if (ping > 40) return Colors.DARK_GREEN.toBuffer();
    return Colors.DARK_AQUA.toBuffer();
  }
}
