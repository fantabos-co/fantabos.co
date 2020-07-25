package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getNetworkManager;

import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.util.PacketHelper;
import com.matt.forgehax.util.SimpleTimer;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.network.play.client.CPacketKeepAlive;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@RegisterMod
public class PingControl extends ToggleMod {

  private final Setting<Integer> amount =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("amount")
      .description("Number of ms to add to ping")
      .min(0)
      .defaultTo(300)
      .build();
  
  public PingControl() {
    super(Category.EXPLOIT, "PingControl", false, "Changes your ping on server");
  }

  private CPacketKeepAlive packet = null;
  private SimpleTimer timer = new SimpleTimer();

  @Override
  public String getDisplayText() {
    if (packet != null)
      return (getModName() + " [" + TextFormatting.RED + "..." + TextFormatting.RESET + "]");
    return super.getDisplayText();
  }

  @Override
  protected void onDisabled() {
    if (packet != null) {
      PacketHelper.ignoreAndSend(packet);
      packet = null;
    }
  }
  
  @SubscribeEvent()
  public void onOutgoingPacket(PacketEvent.Outgoing.Pre event) {
    if (event.getPacket() instanceof CPacketKeepAlive &&
        !PacketHelper.isIgnored(event.getPacket())) {
      event.setCanceled(true);
      packet = event.getPacket();
      timer.start();
    }
  }

  @SubscribeEvent
  public void onTick(TickEvent.ClientTickEvent event) {
    if (getNetworkManager() == null) return;
    if (packet != null && timer.hasTimeElapsed(amount.get())) {
      PacketHelper.ignoreAndSend(packet);
      packet = null;
    }
  }
}