package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getWorld;

import java.util.HashMap;

import com.matt.forgehax.Helper;
import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.util.SimpleTimer;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityEnderPearl;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.network.play.server.SPacketBlockBreakAnim;
import net.minecraft.network.play.server.SPacketEntityStatus;
import net.minecraft.network.play.server.SPacketSoundEffect;
import net.minecraft.network.play.server.SPacketUpdateHealth;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@RegisterMod
public class CombatLog extends ToggleMod {
  
  private final Setting<Boolean> damage =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("damage")
      .description("log damage received")
      .defaultTo(true)
      .build();
  private final Setting<Boolean> ender_pearls =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("ender-pearls")
      .description("log when an ender pearl is spawned")
      .defaultTo(true)
      .build();
  private final Setting<Boolean> potion =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("potion")
      .description("warn when player drinks a potion")
      .defaultTo(true)
      .build();
  private final Setting<Boolean> mining =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("mining")
      .description("warn when a nearby block is being mined")
      .defaultTo(true)
      .build();
  private final Setting<Boolean> totem_use =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("totems")
      .description("Keep track of totems used by players")
      .defaultTo(true)
      .build();
  
  public CombatLog() {
    super(Category.COMBAT, "CombatLog", false, "log combat events in chat");
  }
  
  private BlockPos last_mined = null;
  private SimpleTimer timer = new SimpleTimer();

  @Override
  protected void onEnabled() {
    timer.start();
  }

  @Override
  public void onLoad() {
    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("popped")
        .description("get popped totems for player")
        .processor(
            data -> {
              data.requiredArguments(1);
              final String name = data.getArgumentAsString(0);
              if (counter.containsKey(name)) {
                Helper.printInform("%s used %d totems", name, counter.get(name));
              } else {
                Helper.printInform("No totem use records for %s", name);
              }
            })
        .build();
      
    getCommandStub()
        .builders()
        .newCommandBuilder()
        .name("flush")
        .description("clear totem pop data")
        .processor(
            data -> {
              counter.clear();
            })
        .build();
  }

  private HashMap<String, Integer> counter = new HashMap<String, Integer>();

  @SubscribeEvent
  public void onPacketIncoming(PacketEvent.Incoming.Pre event) {
    if (getLocalPlayer() == null) return;
    if (damage.get() && event.getPacket() instanceof SPacketUpdateHealth) {
      SPacketUpdateHealth packet = event.getPacket();
      if (packet.getHealth() < getLocalPlayer().getHealth())
        Helper.printInform("Received %.1f damage", getLocalPlayer().getHealth() - packet.getHealth());
    }
    else if (potion.get() && event.getPacket() instanceof SPacketSoundEffect) {
      SPacketSoundEffect packet = event.getPacket();
      if (packet.getSound().equals(SoundEvents.ENTITY_GENERIC_DRINK)) {
        Helper.printInform("Potion drank");
      }
    }
    else if (mining.get() && event.getPacket() instanceof SPacketBlockBreakAnim) {
      SPacketBlockBreakAnim packet = event.getPacket();
      if ((!packet.getPosition().equals(last_mined) || timer.hasTimeElapsed(3000)) && // hardcoded obsidian mine time approx
          getLocalPlayer().getDistanceSq(packet.getPosition()) < 2d) {
        Helper.printWarning("Block being mined nearby");
        timer.start();
        last_mined = packet.getPosition();
      }
    }
    else if (totem_use.get() && event.getPacket() instanceof SPacketEntityStatus) {
      SPacketEntityStatus packet = (SPacketEntityStatus) event.getPacket();
      if (packet.getOpCode() == 35) {
        Entity e = packet.getEntity(getWorld());
        if (e != null) {
          if (counter.get(e.getName()) == null) {
            counter.put(e.getName(), 1);
          } else {
            counter.put(e.getName(), counter.get(e.getName()) + 1);
          }
          Helper.printInform("%s used %d totems", e.getName(), counter.get(e.getName()));
        }
      }
    }
  }

  @SubscribeEvent
  public void onTick(TickEvent.ClientTickEvent event) {
    if (!totem_use.get() || getWorld() == null) return;
    for (EntityPlayer player : getWorld().playerEntities) {
      if (player != null && (player.isDead || player.getHealth() <= 0F) && counter.containsKey(player.getName())) {
        Helper.printWarning("%s died after using %d totems", player.getName(), counter.get(player.getName()));
        counter.remove(player.getName());
      }
    }
  }

  @SubscribeEvent
  public void onEntityJoinWorld(EntityJoinWorldEvent event) {
    if (ender_pearls.get() && event.getEntity() instanceof EntityEnderPearl)
      Helper.printError("Ender pearl thrown");
  }
}
