package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getRidingEntity;
import static com.matt.forgehax.Helper.getNetworkManager;
import static com.matt.forgehax.Helper.getWorld;

import com.matt.forgehax.asm.events.PacketEvent;

import com.matt.forgehax.asm.ForgeHaxHooks;
import com.matt.forgehax.asm.events.RenderBoatEvent;
import com.matt.forgehax.asm.reflection.FastReflection;
import com.matt.forgehax.events.EntityRemovedEvent;
import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.events.RenderEvent;
import com.matt.forgehax.util.color.Colors;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.draw.RenderUtils;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.key.Bindings;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.entity.item.EntityBoat;
import net.minecraft.item.ItemBoat;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovementInput;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketInput;
import net.minecraft.network.play.client.CPacketUseEntity.Action;
import net.minecraft.network.play.client.CPacketConfirmTeleport;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketSteerBoat;
import net.minecraft.network.play.client.CPacketPlayerTryUseItem;
import net.minecraft.network.play.server.SPacketMoveVehicle;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

@RegisterMod
public class BoatFly extends ToggleMod {

  public final Setting<Float> opacity =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("opacity")
          .description("Set boat model opacity")
          .min(0F)
          .max(1F)
          .defaultTo(1F)
          .build();
  public final Setting<Double> speed =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("speed")
          .description("how fast to move")
          .min(0D)
          .max(5D)
          .defaultTo(2.5D)
          .build();
  public final Setting<Double> speedFall =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("FallSpeed")
          .description("how slowly to fall")
          .min(0D)
          .max(2D)
          .defaultTo(0D)
          .build();
  public final Setting<Double> speedUp =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("UpSpeed")
          .description("how fast to ascend")
          .min(0.01D)
          .max(2D)
          .defaultTo(1D)
          .build();
  public final Setting<Double> speedDown =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("DownSpeed")
          .description("how fast to descend")
          .min(0.01D)
          .max(2D)
          .defaultTo(1D)
          .build();
  public final Setting<Boolean> sprint =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("half-speed")
          .description("Halve boat speed if not sprinting")
          .defaultTo(true)
          .build();
  public final Setting<Boolean> cancel_rubber =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("posLock")
          .description("Ignore server position")
          .defaultTo(true)
          .build();
  public final Setting<Boolean> force_boat_pos =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("keep-boat")
          .description("Keep the boat sticking to you")
          .defaultTo(true)
          .build();
  public final Setting<Boolean> remount =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("remount")
          .description("Automatically remount boat")
          .defaultTo(true)
          .build();
  public final Setting<Boolean> spoof =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("spoof")
          .description("Cancel some circumstantial packets")
          .defaultTo(false)
          .build();
  public final Setting<Boolean> place_spoof =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("place-spoof")
          .description("Cancel some boat placing packets")
          .defaultTo(true)
          .build();
  public final Setting<Boolean> force_on_ground =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("ground")
          .description("Sets your movement as always on-ground")
          .defaultTo(true)
          .build();
  
  public final Setting<Boolean> setYaw =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("SetYaw")
          .description("set the boat yaw")
          .defaultTo(true)
          .build();
  public final Setting<Boolean> noClamp =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("NoClamp")
          .description("clamp view angles")
          .defaultTo(true)
          .build();
  public final Setting<Boolean> noGravity =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("NoGravity")
          .description("disable boat gravity")
          .defaultTo(false)
          .build();

  public final Setting<Boolean> entity_fly =
       getCommandStub()
           .builders()
           .<Boolean>newSettingBuilder()
           .name("entity-fly")
           .description("Fly with any entity as if it was a boat")
           .defaultTo(false)
           .build();
    
  
  public BoatFly() {
    super(Category.MOVEMENT, "BoatFly", false, "Boathax");
  }

  private boolean isRiding() {
    return (getLocalPlayer() != null 
            && (getLocalPlayer().getRidingEntity() != null)
            && (getLocalPlayer().getRidingEntity() instanceof EntityBoat || entity_fly.get()));
  }

  private boolean wasRiding() {
    return isRiding() || (last_boat != null 
            && (last_boat instanceof EntityBoat || entity_fly.get()));
  }

  @Override
  public String getDisplayText() {
    if (getStatus() == EntityBoat.Status.IN_AIR)
      return (super.getDisplayText() + " [" + TextFormatting.DARK_PURPLE + "F" + TextFormatting.RESET + "]");
    if (getStatus() == EntityBoat.Status.ON_LAND)
      return (super.getDisplayText() + " [" + TextFormatting.DARK_GREEN + "G" + TextFormatting.RESET + "]");
    return super.getDisplayText();
  }

  private Entity last_boat = null;

  @SubscribeEvent
  public void onWorldUnload(WorldEvent.Unload event) {
    if (getLocalPlayer() != null)
      getLocalPlayer().dismountRidingEntity();
    last_boat = null;
  }

  @SubscribeEvent
  public void onEntityRemoved(EntityRemovedEvent event) {
    if (event.getEntity().equals(last_boat))
      last_boat = null;
  }

  @SubscribeEvent
  public void onPacketInbound(PacketEvent.Incoming.Pre event) {
    if (getLocalPlayer() == null) return;
    if (event.getPacket() instanceof SPacketMoveVehicle) {
      if (force_boat_pos.get() && isRiding())
        event.setCanceled(true);
    } else if (event.getPacket() instanceof SPacketPlayerPosLook) {
      if (wasRiding() && !Bindings.sneak.isPressed()) {
        SPacketPlayerPosLook packet = event.getPacket();
        if (cancel_rubber.get()) {
          event.setCanceled(true);
          if (!spoof.get()) {
            getNetworkManager().sendPacket(
                new CPacketConfirmTeleport(packet.getTeleportId()));
          }
        }
        if (remount.get()) {
          Vec3d pos = new Vec3d(last_boat.posX, last_boat.posY, last_boat.posZ);
          getNetworkManager().sendPacket(new CPacketUseEntity(last_boat, EnumHand.MAIN_HAND, pos));
          getNetworkManager().sendPacket(new CPacketUseEntity(last_boat, EnumHand.MAIN_HAND));
        }
      }
    }
  }

  @SubscribeEvent
  public void onOutgoingPacketSent(PacketEvent.Outgoing.Pre event) {
    if (getLocalPlayer() == null) return;
    if (spoof.get() && event.getPacket() instanceof CPacketUseEntity &&
        ((CPacketUseEntity) event.getPacket()).getAction() == Action.INTERACT_AT) {
      event.setCanceled(true);
    }
    if (spoof.get() && wasRiding() && (
        event.getPacket() instanceof CPacketSteerBoat ||
        event.getPacket() instanceof CPacketPlayer ||
        event.getPacket() instanceof CPacketInput ||
        event.getPacket() instanceof CPacketEntityAction ||
        event.getPacket() instanceof CPacketConfirmTeleport
        )) {
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public void onPlaceBoat(PlayerInteractEvent.RightClickItem event) {
    if (place_spoof.get() && event.getItemStack().getItem() instanceof ItemBoat) {
      event.setCanceled(true);
      getNetworkManager().sendPacket(new CPacketPlayerTryUseItem(EnumHand.MAIN_HAND));
    }
  }

  @SubscribeEvent // disable gravity
  public void onLocalPlayerUpdate(LocalPlayerUpdateEvent event) {
    ForgeHaxHooks.isNoBoatGravityActivated =
        getRidingEntity() instanceof EntityBoat; // disable gravity if in boat
  }
  
  @Override
  public void onDisabled() {
    // ForgeHaxHooks.isNoClampingActivated = false; // disable view clamping
    ForgeHaxHooks.isNoBoatGravityActivated = false; // disable gravity
    ForgeHaxHooks.isBoatSetYawActivated = false;
    // ForgeHaxHooks.isNotRowingBoatActivated = false; // items always usable - can not be disabled
    last_boat = null;
  }
  
  @Override
  public void onLoad() {
    ForgeHaxHooks.isNoClampingActivated = noClamp.getAsBoolean();
  }
  
  @SubscribeEvent
  public void onRenderBoat(RenderBoatEvent event) {
    if (EntityUtils.isDrivenByPlayer(event.getBoat())) {
      if (opacity.get() < 1F) {
        event.setOpacity(opacity.get());
      }
      if (setYaw.getAsBoolean()) {
        float yaw = getLocalPlayer().rotationYaw;
        event.getBoat().rotationYaw = yaw;
        event.setYaw(yaw);
      }
    }
  }
  
  @SubscribeEvent
  public void onClientTick(TickEvent.ClientTickEvent event) {
    if (getLocalPlayer() == null || FreecamMod.shouldIgnoreInput()) return;
    // if (getModManager().get(FreecamMod.class).get().isEnabled()) return;
    // check if the player is really riding a entity
    if (MC.player.getRidingEntity() != null) {
      if (getLocalPlayer().getRidingEntity() instanceof EntityBoat
          || entity_fly.get()) {
        last_boat = MC.player.getRidingEntity();
        ForgeHaxHooks.isNoClampingActivated = noClamp.getAsBoolean();
        ForgeHaxHooks.isBoatSetYawActivated = setYaw.getAsBoolean();
        ForgeHaxHooks.isNoBoatGravityActivated = noGravity.getAsBoolean();
  
        MC.player.getRidingEntity().motionY = 0f;
        
        if (MC.gameSettings.keyBindJump.isKeyDown()) {
          // trick the riding entity to think its onground
          MC.player.getRidingEntity().onGround = force_on_ground.get();
          // teleport up
          MC.player.getRidingEntity().motionY += MC.gameSettings.keyBindSprint.isKeyDown() ? 5*speedUp.get() : speedUp.get();
        } else if (MC.gameSettings.keyBindSprint.isKeyDown()) {
          MC.player.getRidingEntity().motionY -= speedDown.get();
        } else if (speedFall.get() != 0D) {
          MC.player.getRidingEntity().motionY -= speedFall.getAsDouble();
        } else if (MC.gameSettings.keyBindSneak.isKeyDown()) {
          getLocalPlayer().dismountRidingEntity();
          last_boat = null;
          return;
        } 
  
        /*if ((MC.player.posY <= maintainY.getAsDouble()-5D) && (MC.player.posY > maintainY.getAsDouble()-10D) && maintainY.getAsDouble() != 0D)
        MC.player.getRidingEntity().setPositionAndUpdate(MC.player.posX, maintainY.getAsDouble(), MC.player.posZ );*/
      
        setMoveSpeedEntity(speed.getAsDouble());
      } else {
        last_boat = null;
      }
    } else {
      if (MC.gameSettings.keyBindSneak.isKeyDown()) {
        getLocalPlayer().dismountRidingEntity();
        last_boat = null;
      } else if ((force_boat_pos.get()) && last_boat != null) {
        getLocalPlayer().startRiding(last_boat, true);
      }
    }
  }
  
  public void setMoveSpeedEntity(double speed) {
    if (MC.player != null && MC.player.getRidingEntity() != null) {
      MovementInput movementInput = MC.player.movementInput;
      double forward = movementInput.moveForward;
      double strafe = movementInput.moveStrafe;
      float yaw = MC.player.rotationYaw;
      if (sprint.get() && !getLocalPlayer().isSprinting())
        speed /= 2.D;
      
      if ((forward == 0.0D) && (strafe == 0.0D)) {
        MC.player.getRidingEntity().motionX = (0.0D);
        MC.player.getRidingEntity().motionZ = (0.0D);
      } else {
        if (forward != 0.0D) {
          if (strafe > 0.0D) {
            yaw += (forward > 0.0D ? -45 : 45);
          } else if (strafe < 0.0D) {
            yaw += (forward > 0.0D ? 45 : -45);
          }
          
          strafe = 0.0D;
          
          if (forward > 0.0D) {
            forward = 1.0D;
          } else if (forward < 0.0D) {
            forward = -1.0D;
          }
        }
        MC.player.getRidingEntity().motionX =
            (forward * speed * Math.cos(Math.toRadians(yaw + 90.0F))
                + strafe * speed * Math.sin(Math.toRadians(yaw + 90.0F)));
        MC.player.getRidingEntity().motionZ =
            (forward * speed * Math.sin(Math.toRadians(yaw + 90.0F))
                - strafe * speed * Math.cos(Math.toRadians(yaw + 90.0F)));
      }
    }
  }

  private EntityBoat.Status getStatus() {
    if (getLocalPlayer() == null || getLocalPlayer().getRidingEntity() == null
        || !(getLocalPlayer().getRidingEntity() instanceof EntityBoat)) return null;
    return FastReflection.Fields.EntityBoat_status.get(getLocalPlayer().getRidingEntity());
  }

  private boolean isInAir() {
    if (getStatus() == EntityBoat.Status.IN_AIR )
      return true;
    return false;
  }
}
