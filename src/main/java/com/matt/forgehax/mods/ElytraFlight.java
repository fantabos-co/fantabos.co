package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getNetworkManager;
import static com.matt.forgehax.Helper.getModManager;

import com.matt.forgehax.asm.ForgeHaxHooks;
import com.matt.forgehax.asm.events.LocalPlayerUpdateMovementEvent;
import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.asm.events.EntityElytraFlyingEvent;
import com.matt.forgehax.asm.reflection.FastReflection;
import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.Switch.Handle;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.entity.LocalPlayerUtils;
import com.matt.forgehax.util.key.Bindings;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketEntityAction.Action;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.server.SPacketEntityMetadata;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class ElytraFlight extends ToggleMod {
  
  public enum FlyMode {
    FLIGHT,
    CONTROL,
    BOOST,
    SUPERIOR
  }

  public enum TakeoffMode {
    NONE,
    BASIC,
    HOLD
  }

  public enum SpacebarMode {
    ASCEND,
    TOGGLE
  }

  public final Setting<FlyMode> mode =
    getCommandStub()
        .builders()
        .<FlyMode>newSettingEnumBuilder()
        .name("mode")
        .description("The fly mode [flight/control/superior/boost]")
        .defaultTo(FlyMode.SUPERIOR)
        .build();

  public final Setting<TakeoffMode> takeoff =
      getCommandStub()
          .builders()
          .<TakeoffMode>newSettingEnumBuilder()
          .name("takeoff")
          .description("Make takeoff easier [none/basic/hold]")
          .defaultTo(TakeoffMode.BASIC)
          .build();

  public final Setting<SpacebarMode> spacebar_mode =
      getCommandStub()
          .builders()
          .<SpacebarMode>newSettingEnumBuilder()
          .name("spacebar-mode")
          .description("Either fly up like sneak does or toggle vanilla fly [ascend/toggle]")
          .defaultTo(SpacebarMode.TOGGLE)
          .build();

  public final Setting<Boolean> smart_boost =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("smart-boost")
          .description("Stop boosting when looking up to ascend")
          .defaultTo(false)
          .build();

  public final Setting<Double> hold_fall_activation =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("hold-threshold")
          .description("Fall at which to trigger hold takeoff")
          .min(0D)
          .max(1D)
          .defaultTo(0.1D)
          .build();

  public final Setting<Double> speed_flight =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("speed-flight")
          .description("Speed in Flight mode")
          .min(0D)
          .max(1D)
          .defaultTo(0.0825D)
          .build();

  public final Setting<Float> boost =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("boost")
          .description("Acceleration amount in control and superior mode")
          .min(0f)
          .max(2f)
          .defaultTo(0.2F)
          .build();

  public final Setting<Double> maxboost =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("maxboost")
          .description("Max speed in superior mode")
          .min(0D)
          .max(3D)
          .defaultTo(1.75D)
          .build();

   public final Setting<Boolean> glide =
       getCommandStub()
           .builders()
           .<Boolean>newSettingBuilder()
           .name("slow-drift")
           .description("Decrease your altitude every packet to trick anticheat")
           .defaultTo(true)
           .build();

  public final Setting<Double> glide_amount =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("drift-amount")
          .description("Amount to descend at every packet sent")
          .min(0D)
          .max(1D)
          .defaultTo(0.0001D)
          .build();

  public final Setting<Double> vertical_speed =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("vertical-speed")
          .description("Speed applied vertically by sneak and jump")
          .min(0D)
          .max(2D)
          .defaultTo(0.05D)
          .build();

   public final Setting<Boolean> spam_open =
       getCommandStub()
           .builders()
           .<Boolean>newSettingBuilder()
           .name("open-spam")
           .description("Spams a ton of elytra open packets. Allows Creative Flight and infinite durability")
           .defaultTo(false)
           .build();

   public final Setting<Boolean> idle_jitter =
       getCommandStub()
           .builders()
           .<Boolean>newSettingBuilder()
           .name("idle-jitter")
           .description("Never stand still, like Atom")
           .defaultTo(false)
           .build();

  public final Setting<Double> jitter_amount =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("amount-jitter")
          .description("Movement to apply while stationary")
          .min(0D)
          .max(1D)
          .defaultTo(0.05D)
          .build();

   public final Setting<Boolean> hide_pitch =
       getCommandStub()
           .builders()
           .<Boolean>newSettingBuilder()
           .name("hide-pitch")
           .description("Hide your true pitch")
           .defaultTo(true)
           .build();

   public final Setting<Boolean> hide_yaw =
       getCommandStub()
           .builders()
           .<Boolean>newSettingBuilder()
           .name("hide-yaw")
           .description("Hides your yaw while stationary")
           .defaultTo(false)
           .build();

   public final Setting<Boolean> no_rotation =
       getCommandStub()
           .builders()
           .<Boolean>newSettingBuilder()
           .name("hide-rotation")
           .description("Set rotating to false in your packets")
           .defaultTo(false)
           .build();

  public final Setting<Boolean> no_vanilla_motion =
     getCommandStub()
         .builders()
         .<Boolean>newSettingBuilder()
         .name("no-drift")
         .description("Cancel vanilla elytra drift")
         .defaultTo(true)
         .build();

  public final Setting<Boolean> cancel_motion =
     getCommandStub()
         .builders()
         .<Boolean>newSettingBuilder()
         .name("cancel-motion")
         .description("Cancel")
         .defaultTo(true)
         .build();

   public final Setting<Boolean> silent =
       getCommandStub()
           .builders()
           .<Boolean>newSettingBuilder()
           .name("silent")
           .description("Prevent elytra speed sound from updating")
           .defaultTo(false)
           .changed(cb -> {
             if (this.isEnabled())
              ForgeHaxHooks.preventElytraSoundUpdate = cb.getTo();
           })
           .build();

  private final Handle flying = LocalPlayerUtils.getFlySwitch().createHandle(getModName());

  private double jitter_x = 1;
  private double jitter_z = 1;
  private boolean jitter_axis = false;
  private int jitter_counter = 0;

  private float last_pitch = 0F;
  private float last_yaw = 0F;

  private boolean idle = false; // Spoof angles for 1 more packet after not idle anymore
  private boolean sent_packet = false;

  public ElytraFlight() {
    super(Category.MOVEMENT, "ElytraFlight", false, "Elytra Flight");
  }

  @Override
  public String getDisplayText() {  
    if (mode.get() == FlyMode.FLIGHT)
      return (getModName() + " [" + TextFormatting.LIGHT_PURPLE + mode.get() + TextFormatting.RESET + "]");
    if (getLocalPlayer().isElytraFlying() && isModEngaged())
      return (getModName() + " [" + TextFormatting.LIGHT_PURPLE + mode.get() + TextFormatting.RESET + "]");
    else
      return getModName();
  }
  
  @Override
  protected void onEnabled() {
    ForgeHaxHooks.preventElytraSoundUpdate = silent.get();
    sent_packet = false;
    if (getLocalPlayer() == null) return;

    if (takeoff.get().equals(TakeoffMode.BASIC)) {
      MC.addScheduledTask(
          () -> {
            if (getLocalPlayer() != null && !getLocalPlayer().isElytraFlying()) {
              getNetworkManager()
                  .sendPacket(new CPacketEntityAction(getLocalPlayer(), Action.START_FALL_FLYING));
            }
          });
    }
  }

  @Override
  public void onDisabled() {
    ForgeHaxHooks.preventElytraSoundUpdate = false;
    if (getLocalPlayer() == null) return;
    if (mode.get() == FlyMode.FLIGHT) {
      flying.disable();
      getNetworkManager()
          .sendPacket(new CPacketEntityAction(getLocalPlayer(), Action.START_FALL_FLYING));
    }
  }

  private boolean isModEngaged() {
    boolean val = true; // by default always force elytra motion

    if (spacebar_mode.get() == SpacebarMode.TOGGLE && Bindings.jump.isPressed())
      val = false; // If spacebar mode is toggle, player can disable motion setting pressing spacebar
    if (smart_boost.get() && getLocalPlayer().rotationPitch < 0.5F)
      val = false; // If smart boost is enabled, player can disable motion setting by looking up
    if (mode.get() == FlyMode.BOOST)
      val = !val; // If in boost mode, only apply motion change while player is pressing space or looking up

    return val;
  }

  @SubscribeEvent
  public void onPacketInbound(PacketEvent.Incoming.Pre event) {
    if (getLocalPlayer() == null) return;
    if (spam_open.get() && (getLocalPlayer().isElytraFlying() || mode.get() == FlyMode.FLIGHT)
        && event.getPacket() instanceof SPacketEntityMetadata) {
      SPacketEntityMetadata MetadataPacket = event.getPacket();
      if (MetadataPacket.getEntityId() == getLocalPlayer().getEntityId()) {
        event.setCanceled(true);
      }
    }
  }
  
  @SubscribeEvent
  public void onOutgoingPacketSent(PacketEvent.Outgoing.Pre event) {
    if (getLocalPlayer() == null || !getLocalPlayer().isElytraFlying() || !isModEngaged()) return;
    if (event.getPacket() instanceof CPacketPlayer) {
      CPacketPlayer packet = event.getPacket();
      if (glide.get() && (packet instanceof CPacketPlayer.Position || packet instanceof CPacketPlayer.PositionRotation)) {
        getLocalPlayer().posY -= glide_amount.get();
        FastReflection.Fields.CPacketPlayer_y.set(packet, getLocalPlayer().posY);
      }
      if ((packet instanceof CPacketPlayer.Rotation || packet instanceof CPacketPlayer.PositionRotation)) {
        if (hide_pitch.get() && FastReflection.Fields.CPacketPlayer_pitch.get(packet) < 0.5F) {
          FastReflection.Fields.CPacketPlayer_pitch.set(packet, 0.5F);
        }
        if (no_rotation.get()) FastReflection.Fields.CPacketPlayer_rotating.set(packet, false);
        if (hide_yaw.get()) FastReflection.Fields.CPacketPlayer_yaw.set(packet, last_yaw);
      }
    }
  }

  @SubscribeEvent
  public void onPlayerElytraFlying(EntityElytraFlyingEvent event) {
    if (mode.get() == FlyMode.FLIGHT) return;
    if (event.getEntity().equals(getLocalPlayer()) && isModEngaged()) {
      if (no_vanilla_motion.get())
        event.setCanceled(true);
  
      if (mode.get() == FlyMode.SUPERIOR) {
        getLocalPlayer().motionY = 0;
      }

      if (!FreecamMod.shouldIgnoreInput()) {
        if (Bindings.sneak.isPressed())
          getLocalPlayer().motionY = -vertical_speed.get();
        else if (spacebar_mode.get() == SpacebarMode.ASCEND && Bindings.jump.isPressed())
          getLocalPlayer().motionY = vertical_speed.get();

        boost(!(mode.get() == FlyMode.SUPERIOR));
        capPlayerMotion();
      }
    }
  }

  @SubscribeEvent
  public void onLocalPlayerUpdate(LocalPlayerUpdateEvent event) {
    if (getModManager().get(FreecamMod.class).get().isEnabled()) return;
    if (getLocalPlayer() == null) return;

    // This allows creative flight and infinite durability on some servers
    if (spam_open.get()) { // Don't spam while trying to go up
      getNetworkManager()
        .sendPacket(new CPacketEntityAction(getLocalPlayer(), Action.START_FALL_FLYING));
    }

    // This happens when you are in flight mode and switch to any other mode. Put player in elytra flight
    if (getLocalPlayer().capabilities.isFlying && !mode.get().equals(FlyMode.FLIGHT)) {
      flying.disable();
      if (!spam_open.get())
        getNetworkManager()
          .sendPacket(new CPacketEntityAction(getLocalPlayer(), Action.START_FALL_FLYING));
    }

    // if we're in Flight mode, set player flying
    if (mode.get() == FlyMode.FLIGHT) {
      // Enable our flight as soon as the player starts flying his elytra or always if in packet mode
      if (spam_open.get() || getLocalPlayer().isElytraFlying()) {
        flying.enable();
      }
      getLocalPlayer().capabilities.setFlySpeed(speed_flight.getAsFloat());
    }
  }

  @SubscribeEvent
  public void onLocalPlayerUpdateMovement(LocalPlayerUpdateMovementEvent event) {
    if (getLocalPlayer() == null) return;

    // Takeoff
    if (!getLocalPlayer().isElytraFlying() && !getLocalPlayer().capabilities.isFlying) {
      if (takeoff.get().equals(TakeoffMode.HOLD) && getLocalPlayer().fallDistance >= hold_fall_activation.get()) {
        getLocalPlayer().setVelocity(0, 0, 0);
        if (!sent_packet && !spam_open.get()) {
          getNetworkManager()
                  .sendPacket(new CPacketEntityAction(getLocalPlayer(), Action.START_FALL_FLYING));
          sent_packet = true;
        }
      }
      
      return;
    }
    sent_packet = false;

    // Do Idle Jitter or cancel player motion or save last player rotation
    if (isPlayerIdle()) {
      if (idle_jitter.get()) {
        do_jitter();
      } else if (cancel_motion.get()) {
        getLocalPlayer().setVelocity(0, 0, 0);
      }
      idle = true;
    } else {
      if (idle) idle = false; // Spoof angles 1 more time
      else {
        last_pitch = getLocalPlayer().rotationPitch;
        last_yaw = getLocalPlayer().rotationYaw;
      }
    }
  }

  private void boost(boolean vertical) {
    float yaw = (float)Math.toRadians(getLocalPlayer().rotationYaw);
    float v_factor = (vertical ? Math.abs(getLocalPlayer().rotationPitch / 90.0F) : 0.f);
    float v_dir = (getLocalPlayer().rotationPitch > 0.f ? -1.f : 1.f);

    if (Bindings.forward.isPressed()) {
      getLocalPlayer().motionX -= (MathHelper.sin(yaw) * boost.get()) * (1.f - v_factor);
      getLocalPlayer().motionZ += (MathHelper.cos(yaw) * boost.get()) * (1.f - v_factor);
      if (vertical) {
        getLocalPlayer().motionY += v_dir * v_factor * boost.get();
      }
    } else if (Bindings.back.isPressed()) {
      getLocalPlayer().motionX += (MathHelper.sin(yaw) * boost.get()) * (1.f - v_factor);
      getLocalPlayer().motionZ -= (MathHelper.cos(yaw) * boost.get()) * (1.f - v_factor);
      if (vertical) {
        getLocalPlayer().motionY -= v_dir * v_factor * boost.get();
      }
    }
    if (Bindings.left.isPressed()) {
      getLocalPlayer().motionX -= (MathHelper.sin(yaw - (float) Math.toRadians(90.0F)) * boost.get()) * (1.f - v_factor);
      getLocalPlayer().motionZ += (MathHelper.cos(yaw - (float) Math.toRadians(90.0F)) * boost.get()) * (1.f - v_factor);
    } else if (Bindings.right.isPressed()) {
      getLocalPlayer().motionX -= (MathHelper.sin(yaw + (float) Math.toRadians(90.0F)) * boost.get()) * (1.f - v_factor);
      getLocalPlayer().motionZ += (MathHelper.cos(yaw + (float) Math.toRadians(90.0F)) * boost.get()) * (1.f - v_factor);
    }
  }

  private void capPlayerMotion() {
    double speed = Math.sqrt(Math.pow(getLocalPlayer().motionX, 2) + Math.pow(getLocalPlayer().motionY, 2) + Math.pow(getLocalPlayer().motionZ, 2));
    if (speed > maxboost.get()) {
      double factor = maxboost.get() / speed;
      getLocalPlayer().motionX *= factor;
      getLocalPlayer().motionY *= factor;
      getLocalPlayer().motionZ *= factor;
    }
  }

  private static boolean isPlayerIdle() {
    if (!Bindings.jump.isPressed() && !Bindings.sneak.isPressed() && 
        !Bindings.forward.isPressed() && !Bindings.back.isPressed() &&
        !Bindings.left.isPressed() && !Bindings.right.isPressed()) return true;
    return false;
  }

  private void do_jitter() {
    if (jitter_axis) getLocalPlayer().setVelocity(jitter_x * jitter_amount.get(), 0, 0);
    else getLocalPlayer().setVelocity(0, 0, jitter_z * jitter_amount.get());
    jitter_counter++;
    if (jitter_counter > 5) {
      jitter_counter = 0;
      if (jitter_axis) jitter_x = -jitter_x;
      else jitter_z = -jitter_z;
      jitter_axis = !jitter_axis;
    }
  }
}
