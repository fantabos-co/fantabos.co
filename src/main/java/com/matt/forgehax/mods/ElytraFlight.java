package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getNetworkManager;
import static com.matt.forgehax.Helper.getModManager;

import com.matt.forgehax.asm.events.PacketEvent;

import com.matt.forgehax.asm.events.LocalPlayerUpdateMovementEvent;
import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.Switch.Handle;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.entity.LocalPlayerUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.util.math.MathHelper;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketEntityAction.Action;
import net.minecraft.network.play.server.SPacketEntityMetadata;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Since KamiBlue added a packet mode I may
 *    as well push mine too. The gist is public now anyway.
 *  I'm also throwing in a super basic Boost mode, the good
 *    one will remain undisclosed
 */

@RegisterMod
public class ElytraFlight extends ToggleMod {
  
  public enum FlyMode {
    FLIGHT,
    PACKET,
    BOOST
  }

  public final Setting<Boolean> takeoff =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("takeoff")
          .description("Send Elytra Open packet on enable")
          .defaultTo(true)
          .build();

  public final Setting<Double> speed =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("speed")
          .description("Flight speed in packet mode")
          .defaultTo(0.0825D)
          .build();

  public final Setting<Float> boost =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("boost")
          .description("Acceleration amount in boost mode")
          .defaultTo(0.2F)
          .build();

  public final Setting<Double> maxboost =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("maxboost")
          .description("Max speed in boost mode")
          .defaultTo(1.75D)
          .build();

  public final Setting<Double> down_speed =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("down-speed")
          .description("Downward speed when pressing Sneak")
          .defaultTo(0.1D)
          .build();
  
  public final Setting<Double> up_speed =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("up-speed")
          .description("Upward speed when pressing Jump")
          .defaultTo(0.1D)
          .build();

  public final Setting<FlyMode> mode =
      getCommandStub()
          .builders()
          .<FlyMode>newSettingEnumBuilder()
          .name("mode")
          .description("The fly mode [flight/packet/boost]")
          .defaultTo(FlyMode.PACKET)
          .build();
  
  private final Handle flying = LocalPlayerUtils.getFlySwitch().createHandle(getModName());

  public ElytraFlight() {
    super(Category.MOVEMENT, "ElytraFlight", false, "Elytra Flight");
  }

  @Override
  public String getDisplayText() {
    return (getModName() + " [" + mode.get() + "]");
  }
  
  @Override
  protected void onEnabled() {
    if (getLocalPlayer() == null) return;
    switch(mode.get()) {
      case BOOST:
      case FLIGHT:
        if (takeoff.get()) {
          MC.addScheduledTask(
              () -> {
                if (getLocalPlayer() != null && !getLocalPlayer().isElytraFlying()) {
                  getNetworkManager()
                      .sendPacket(new CPacketEntityAction(getLocalPlayer(), Action.START_FALL_FLYING));
                }
              });
        }
        break;
      case PACKET:
        MC.player.capabilities.isFlying = true;
        MC.player.capabilities.allowFlying = true;
        getLocalPlayer().capabilities.setFlySpeed(speed.getAsFloat());
        break;
    }
  }

  @Override
  public void onDisabled() {
    if (getLocalPlayer() == null) return;
    switch(mode.get()) {
      case FLIGHT:
        flying.disable();
        getNetworkManager()
            .sendPacket(new CPacketEntityAction(getLocalPlayer(), Action.START_FALL_FLYING));
        break;
      case PACKET:
        MC.player.capabilities.isFlying = false;
        MC.player.capabilities.allowFlying = false;
        getNetworkManager()
            .sendPacket(new CPacketEntityAction(getLocalPlayer(), Action.START_FALL_FLYING));
        break;
      default: break;
    }
  }

  @SubscribeEvent
  public void onPacketInbound(PacketEvent.Incoming.Pre event) {
    if (getLocalPlayer() == null) return;
    switch(mode.get()) {
      case PACKET:
        if (event.getPacket() instanceof SPacketEntityMetadata) {
          SPacketEntityMetadata MetadataPacket = event.getPacket();
          if (MetadataPacket.getEntityId() == getLocalPlayer().getEntityId()) {
            event.setCanceled(true);
          }
        }
        break;
      default: break;
    }
  }
  
  @SubscribeEvent
  public void onLocalPlayerUpdate(LocalPlayerUpdateEvent event) {
    if (getModManager().get(FreecamMod.class).get().isEnabled()) return;
    if (getLocalPlayer() == null) return;
    switch(mode.get()) {
      case FLIGHT:
        // Enable our flight as soon as the player starts flying his elytra.
        if (getLocalPlayer().isElytraFlying()) {
          flying.enable();
        }
        getLocalPlayer().capabilities.setFlySpeed(speed.getAsFloat());
        break;
      case PACKET:
        getNetworkManager()
            .sendPacket(new CPacketEntityAction(getLocalPlayer(), Action.START_FALL_FLYING));
        MC.player.capabilities.isFlying = true;
        MC.player.capabilities.allowFlying = true;
        getLocalPlayer().capabilities.setFlySpeed(speed.getAsFloat());
        break;
      default: break;
    }
  }
  @SubscribeEvent
  public void onLocalPlayerUpdateMovement(LocalPlayerUpdateMovementEvent event) {
    if (getLocalPlayer() == null) return;
    if (getModManager().get(FreecamMod.class).get().isEnabled()) return;
    switch(mode.get()) {
      case BOOST:
        if (!getLocalPlayer().isElytraFlying()) return;
      
        if (MC.gameSettings.keyBindSneak.isKeyDown()) {
          getLocalPlayer().motionY = -down_speed.get();
        } else if (MC.gameSettings.keyBindJump.isKeyDown()) {
          getLocalPlayer().motionY = up_speed.get();
        } else if (isPlayerIdle()) {
          getLocalPlayer().setVelocity(0D, 0D, 0D);
        } else {
          getLocalPlayer().motionY = 0;
        }
      
        float yaw = (float)Math.toRadians(getLocalPlayer().rotationYaw);
        if (MC.gameSettings.keyBindForward.isKeyDown() || (getModManager().get("AutoWalk")).get().isEnabled()) {
          getLocalPlayer().motionX -= MathHelper.sin(yaw) * boost.get();
          getLocalPlayer().motionZ += MathHelper.cos(yaw) * boost.get();
        } else if (MC.gameSettings.keyBindBack.isKeyDown()) {
          getLocalPlayer().motionX += MathHelper.sin(yaw) * boost.get();
          getLocalPlayer().motionZ -= MathHelper.cos(yaw) * boost.get();
        }
        if (MC.gameSettings.keyBindLeft.isKeyDown()) {
          getLocalPlayer().motionX -= MathHelper.sin(yaw - (float) Math.toRadians(90.0F)) * boost.get();
          getLocalPlayer().motionZ += MathHelper.cos(yaw - (float) Math.toRadians(90.0F)) * boost.get();
        } else if (MC.gameSettings.keyBindRight.isKeyDown()) {
          getLocalPlayer().motionX -= MathHelper.sin(yaw + (float) Math.toRadians(90.0F)) * boost.get();
          getLocalPlayer().motionZ += MathHelper.cos(yaw + (float) Math.toRadians(90.0F)) * boost.get();
        }
        double speed = Math.sqrt(Math.pow(getLocalPlayer().motionZ, 2) + Math.pow(getLocalPlayer().motionX, 2));
        if (speed > maxboost.get()) {
          double factor = maxboost.get() / speed;
          getLocalPlayer().motionX *= factor;
          getLocalPlayer().motionZ *= factor;
        } 
        break;
      default: break;
    }
  }
  private static boolean isPlayerIdle() {
    if (!MC.gameSettings.keyBindJump.isKeyDown() && !MC.gameSettings.keyBindSneak.isKeyDown() && 
      !MC.gameSettings.keyBindForward.isKeyDown() && !MC.gameSettings.keyBindBack.isKeyDown() &&
      !MC.gameSettings.keyBindLeft.isKeyDown() && !MC.gameSettings.keyBindRight.isKeyDown() &&
      !(getModManager().get("AutoWalk")).get().isEnabled()) return true;
    return false;
  }
}
