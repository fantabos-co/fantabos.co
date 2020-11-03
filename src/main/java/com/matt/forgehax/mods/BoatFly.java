package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getRidingEntity;
import static com.matt.forgehax.Helper.getNetworkManager;
import static com.matt.forgehax.Helper.getModManager;
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

/*	No-Lifed on by Tonio_Cartonio during the 1st covid wave (~March 2020)
 *		This is a shittier but working version, skid and improve
 *		freely. I will not provide settings. I will *maybe* provide
 *		some explaination if you DM me.
 *
 *	Thanks to Fleyr and Number1Princess for helping debug this shit
 *		very early on. Early adopters best adopters.
 *
 *	I would also like to thank Phobos for finding, skidding and then
 *		leaking to literally everybody my Constantiam elytra bypass.
 *		Hope u guys enjoyed!
 *
 *	Always thanks to supreme lord of the boat Popstonia himself,
 *		this would never have been possible without dank boat vids
 */

@RegisterMod
public class BoatFly extends ToggleMod {

  public final Setting<Double> speed =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("speed")
          .description("how fast to move")
          .min(0D)
          .max(3D)
          .defaultTo(1D)
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
  public final Setting<Boolean> cancel_rubber =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("posLock")
          .description("Ignore server position")
          .defaultTo(false)
          .build();
  public final Setting<Boolean> force_boat_pos =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("keep-boat")
          .description("Keep the boat sticking to you")
          .defaultTo(false)
          .build();
  public final Setting<Boolean> remount =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("remount")
          .description("Automatically remount boat")
          .defaultTo(false)
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
          .defaultTo(false)
          .build();
  public final Setting<Boolean> force_on_ground =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("ground")
          .description("Sets your movement as always on-ground")
          .defaultTo(false)
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
  
  public BoatFly() {
    super(Category.MOVEMENT, "BoatFly", false, "Boathax");
  }

  private Entity last_boat = null;

  @SubscribeEvent
  public void onWorldUnload(WorldEvent.Unload event) {
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
    if (force_boat_pos.get() && event.getPacket() instanceof SPacketMoveVehicle &&
          getLocalPlayer().getRidingEntity() != null) {
      event.setCanceled(true);
    }
    if (event.getPacket() instanceof SPacketPlayerPosLook) {
      SPacketPlayerPosLook packet = event.getPacket();
      if (cancel_rubber.get() && last_boat != null && !MC.gameSettings.keyBindSneak.isKeyDown()) {
        event.setCanceled(true);
        if (!spoof.get()) {
          getNetworkManager().sendPacket(
              new CPacketConfirmTeleport(packet.getTeleportId()));
        }
      }
      if (remount.get() && last_boat != null && !MC.gameSettings.keyBindSneak.isKeyDown()) {
        Vec3d pos = new Vec3d(last_boat.posX, last_boat.posY, last_boat.posZ);
        getNetworkManager().sendPacket(new CPacketUseEntity(last_boat, EnumHand.MAIN_HAND, pos));
        getNetworkManager().sendPacket(new CPacketUseEntity(last_boat, EnumHand.MAIN_HAND));
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
    if (last_boat == null && getLocalPlayer().getRidingEntity() == null) return;
    if (spoof.get() && (
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
  public void onClientTick(TickEvent.ClientTickEvent event) {
    if (getLocalPlayer() == null) return;
    // if (getModManager().get(FreecamMod.class).get().isEnabled()) return;

    // check if the player is really riding a entity
    if (MC.player.getRidingEntity() != null) {
      last_boat = MC.player.getRidingEntity();

      ForgeHaxHooks.isNoClampingActivated = noClamp.getAsBoolean();
      ForgeHaxHooks.isBoatSetYawActivated = setYaw.getAsBoolean();
      ForgeHaxHooks.isNoBoatGravityActivated = noGravity.getAsBoolean();

      MC.player.getRidingEntity().motionY = 0f;
      
      if (MC.gameSettings.keyBindJump.isKeyDown()) {
        MC.player.getRidingEntity().onGround = force_on_ground.get();
        MC.player.getRidingEntity().motionY += speedUp.get();
      } else if (MC.gameSettings.keyBindSprint.isKeyDown()) {
        MC.player.getRidingEntity().motionY -= speedDown.get();
      } else if (MC.gameSettings.keyBindSneak.isKeyDown()) {
        getLocalPlayer().dismountRidingEntity();
        last_boat = null;
        return;
      } 
      setMoveSpeedEntity(speed.getAsDouble());
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
}
