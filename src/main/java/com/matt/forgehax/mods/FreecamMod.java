package com.matt.forgehax.mods;

import com.matt.forgehax.asm.ForgeHaxHooks;
import com.matt.forgehax.asm.events.MouseUpdateEvent;
import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.asm.events.UpdateInputFromOptionsEvent;
import com.matt.forgehax.asm.reflection.FastReflection;
import com.matt.forgehax.util.PacketHelper;
import com.matt.forgehax.util.Switch.Handle;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.entity.LocalPlayerUtils;
import com.matt.forgehax.util.key.Bindings;
import com.matt.forgehax.util.math.Angle;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import org.lwjgl.input.Keyboard;

import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.MoverType;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketInput;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketUseEntity;
import net.minecraft.network.play.client.CPacketVehicleMove;
import net.minecraft.network.play.server.SPacketPlayerPosLook;
import net.minecraft.util.MouseHelper;
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.client.registry.ClientRegistry;

import java.util.Objects;

import static com.matt.forgehax.Helper.*;

/**
 * Created on 9/3/2016 by fr1kin
 */
@RegisterMod
public class FreecamMod extends ToggleMod {

  private final KeyBinding bindMacro = new KeyBinding("Freecam Control", Keyboard.KEY_NONE, "ForgeHax");

  private enum CameraMode {
    PLAYER,
    VIEWENTITY
  }

  private enum LookType {
    STATIC,
    CAMERA,
    CURSOR
  }

  private final Setting<Double> speed =
    getCommandStub()
      .builders()
      .<Double>newSettingBuilder()
      .name("speed")
      .description("Movement speed")
      .min(0D)
      .max(2D)
      .defaultTo(0.05D)
      .build();

  private final Setting<CameraMode> mode =
    getCommandStub()
      .builders()
      .<CameraMode>newSettingEnumBuilder()
      .name("mode")
      .description("Camera mode to use [player/viewentity]")
      .defaultTo(CameraMode.VIEWENTITY)
      .build();

  private final Setting<Boolean> toggle_culling =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("toggle-culling")
      .description("Automatically toggle NoCaveCulling")
      .defaultTo(false)
      .build();

  private final Setting<Boolean> cancel_packets =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("cancel-packets")
      .description("Cancel movement packets in Player mode")
      .defaultTo(true)
      .build();

  private final Setting<Boolean> cancelMotion =
    getCommandStub().builders().<Boolean>newSettingBuilder()
      .name("cancel-motion")
      .description("Stops when you're not pressing any key")
      .defaultTo(true)
      .build();

  private final Setting<LookType> lookType =
    getCommandStub()
      .builders()
      .<LookType>newSettingEnumBuilder()
      .name("look-type")
      .description("Where your player is looking at [static/camera/cursor]")
      .defaultTo(LookType.STATIC)
      .build();

  private final Setting<Boolean> hideHands =
    getCommandStub().builders().<Boolean>newSettingBuilder()
      .name("hide-hands")
      .description("Hides your hands while in freecam")
      .defaultTo(true)
      .build();

  private final Handle flying = LocalPlayerUtils.getFlySwitch().createHandle(getModName());

  private Vec3d pos = Vec3d.ZERO;
  private Angle angle = Angle.ZERO;
  private float flyingSpeed;

  private boolean isRidingEntity;
  private Entity ridingEntity;

  private CameraMode used_mode; // don't allow mode changes while freecam is active

  private SelfPlayer originalPlayer;
  private CameraEntity camera;

  private static FreecamMod INSTANCE = null;

  public FreecamMod() {
    super(Category.PLAYER, "Freecam", false, "Freecam mode");
    ClientRegistry.registerKeyBinding(this.bindMacro);
    INSTANCE = this;
  }

  public static boolean shouldIgnoreInput() {
    if (INSTANCE == null || !INSTANCE.isEnabled()) return false;
    return (INSTANCE.mode.get().equals(CameraMode.VIEWENTITY) ? !INSTANCE.bindMacro.isPressed() : true);
  }

  @Override
  public void onEnabled() {
    used_mode = mode.get();

    if (getLocalPlayer() == null || getWorld() == null) {
      this.disable(true);
      return;
    }

    if (toggle_culling.get()) {
      getModManager().get(NoCaveCulling.class).get().enable(false);
    }

    switch (used_mode) {
      case VIEWENTITY:
        camera = new CameraEntity();
        camera.activate();
        break;
      case PLAYER:
        originalPlayer = new SelfPlayer();
        originalPlayer.activate();
        break;
    }
  }

  @Override
  public void onDisabled() {
    if (getLocalPlayer() == null || getWorld() == null) {
      return;
    }

    if (toggle_culling.get()) {
      getModManager().get(NoCaveCulling.class).get().disable(false);
    }

    switch (used_mode) {
      case VIEWENTITY:
        if (camera != null)
          camera.deactivate();
        break;
      case PLAYER:
        if (originalPlayer != null)
          originalPlayer.deactivate();
        break;
    }
  }

  @SubscribeEvent
  public void onTick(TickEvent.ClientTickEvent event) {
    if (getLocalPlayer() == null) {
      return;
    }

    switch (used_mode) {
      case PLAYER:
        if (originalPlayer == null) {
          this.disable(true);
          return;
        }
        originalPlayer.tick();
        break;
      case VIEWENTITY:
        if (camera == null) {
          this.disable(true);
          return;
        }
        camera.tick();
        break;
    }

    if (!bindMacro.isKeyDown()) {
      Entity rotate_me = (used_mode == CameraMode.PLAYER ? originalPlayer : getLocalPlayer());
      boolean send_packet = (used_mode == CameraMode.PLAYER ? getLocalPlayer().ticksExisted % 20 == 0 : false);
      switch (lookType.get()) {
        case STATIC:
          break;
        case CURSOR:
          // MC.objectMouseOver.hitVec is always set
          LocalPlayerUtils.lookAt(rotate_me, MC.objectMouseOver.hitVec, send_packet);
          break;
        case CAMERA:
          LocalPlayerUtils.lookAt(rotate_me, getRenderEntity().getPositionEyes(1f), send_packet);
          break;
      }
    }
  }

  @SubscribeEvent
  public void onRenderHands(RenderHandEvent event) {
    if (hideHands.get()) event.setCanceled(true);
  }

  @SubscribeEvent
  public void onUpdateInput(UpdateInputFromOptionsEvent event) {
    if (used_mode == CameraMode.VIEWENTITY && !this.bindMacro.isKeyDown()) {
      // event.copyTo(camera.movementInput);
      event.clearInput();
    }
  }

  @SubscribeEvent
  public void onUpdateMouse(MouseUpdateEvent event) {
    if (used_mode == CameraMode.VIEWENTITY && !this.bindMacro.isKeyDown()) {
      event.copyTo(camera.mouseHelper);
      float f = MC.gameSettings.mouseSensitivity * 0.6F + 0.2F; // this shit is straight outta minecraft, don't ask me
      float f1 = f * f * f * 8.0F;
      float f2 = (float) camera.mouseHelper.deltaX * f1;
      float f3 = (float) camera.mouseHelper.deltaY * f1;
      int i = 1;

      if (MC.gameSettings.invertMouse) {
        i = -1;
      }

      camera.turn(f2, f3 * i);
      event.clearInput();
    }
  }

  @SubscribeEvent
  public void onPacketSend(PacketEvent.Outgoing.Pre event) {
    if (getLocalPlayer() == null || getWorld() == null) return;

    if (event.getPacket() instanceof CPacketUseEntity) {
      if (((CPacketUseEntity) event.getPacket()).getEntityFromWorld(getWorld()) == getLocalPlayer()) {
        event.setCanceled(true); // Disallow interacting with yourself! May crash or get you kicked
      }
    }

    if (!cancel_packets.get() || used_mode != CameraMode.PLAYER || PacketHelper.isIgnored(event.getPacket())) {
      return;
    }

    if (event.getPacket() instanceof CPacketPlayer
      || event.getPacket() instanceof CPacketInput
      || event.getPacket() instanceof CPacketVehicleMove) {
      event.setCanceled(true);
    }

    if (event.getPacket() instanceof CPacketEntityAction) {
      CPacketEntityAction packet = event.getPacket();

      if (packet.getAction() == CPacketEntityAction.Action.START_SNEAKING
        || packet.getAction() == CPacketEntityAction.Action.STOP_SNEAKING) {
        event.setCanceled(true);
      }
    }
  }

  @SubscribeEvent
  public void onPacketReceived(PacketEvent.Incoming.Pre event) {
    if (getLocalPlayer() == null || used_mode != CameraMode.PLAYER ||
        getCurrentScreen() instanceof GuiDownloadTerrain) {
      return;
    }

    if (event.getPacket() instanceof SPacketPlayerPosLook) {
      SPacketPlayerPosLook packet = event.getPacket();
      pos = new Vec3d(packet.getX(), packet.getY(), packet.getZ());
      angle = Angle.degrees(packet.getPitch(), packet.getYaw());
      event.setCanceled(true);
    }
  }

  @SubscribeEvent
  public void onWorldLoad(WorldEvent.Load event) {
    this.disable(true);
  }

  @SubscribeEvent
  public void onEntityRender(RenderLivingEvent.Pre<?> event) {
    if (getLocalPlayer() != null
        && getLocalPlayer().equals(event.getEntity())
        && mode.get() == CameraMode.PLAYER) {
      event.setCanceled(true);
    }
  }


  // Why, no, do draw the nametag on yourself when in freecam!
  // @SubscribeEvent
  // public void onRenderTag(RenderLivingEvent.Specials.Pre<?> event) {
  //   if (getLocalPlayer() != null
  //       && getLocalPlayer().equals(event.getEntity())
  //       && mode.get() == CameraMode.PLAYER) {
  //     event.setCanceled(true);
  //   }
  // }

  private class SelfPlayer extends EntityOtherPlayerMP {

    public SelfPlayer() {
      super(Objects.requireNonNull(getWorld()), getLocalPlayer().getGameProfile());
    }

    private void activate() {
      angle = LocalPlayerUtils.getViewAngles();
      flyingSpeed = getLocalPlayer().capabilities.getFlySpeed();
      this.copyLocationAndAnglesFrom(getLocalPlayer());
      this.rotationYawHead = getLocalPlayer().rotationYawHead;
      this.inventory = getLocalPlayer().inventory;
      this.inventoryContainer = getLocalPlayer().inventoryContainer;

      getWorld().addEntityToWorld(-100, this);

      if (isRidingEntity = getLocalPlayer().isRiding()) {
        ridingEntity = getLocalPlayer().getRidingEntity();
        getLocalPlayer().dismountRidingEntity();

        this.startRiding(ridingEntity, true);
      }
      pos = getLocalPlayer().getPositionVector();
    }

    private void deactivate() {
      getLocalPlayer().noClip = false;
      getLocalPlayer().setPositionAndRotation(pos.x, pos.y, pos.z, angle.getYaw(), angle.getPitch());
      flying.disable();
      getLocalPlayer().setVelocity(0,0,0);
      if (isRidingEntity) {
        getLocalPlayer().startRiding(ridingEntity, true);
        ridingEntity = null;
      }
      getLocalPlayer().capabilities.setFlySpeed(flyingSpeed);
      getWorld().removeEntityFromWorld(this.getEntityId());
    }

    private void tick() {
      flying.enable();
      getLocalPlayer().capabilities.setFlySpeed(speed.getAsFloat());
      getLocalPlayer().noClip = true;
      getLocalPlayer().onGround = false;
      getLocalPlayer().fallDistance = 0;

      if (!Bindings.forward.isPressed()
          && !Bindings.back.isPressed()
          && !Bindings.left.isPressed()
          && !Bindings.right.isPressed()
          && !Bindings.jump.isPressed()
          && !Bindings.sneak.isPressed()
          && cancelMotion.get()) {
        getLocalPlayer().setVelocity(0, 0, 0);
      }
    }
  }

  private class CameraEntity extends EntityPlayerSP {
    private final MouseHelper mouseHelper;

    public CameraEntity() {
      super(MC, Objects.requireNonNull(getWorld()), getLocalPlayer().connection,
            getLocalPlayer().getStatFileWriter(), getLocalPlayer().getRecipeBook());

      this.movementInput = new MovementInput();
      this.mouseHelper = new MouseHelper();

      this.copyLocationAndAnglesFrom(getLocalPlayer());

      this.prevRotationPitch = getLocalPlayer().rotationPitch;
      this.prevRotationYaw = getLocalPlayer().rotationYaw;
      this.rotationPitch = getLocalPlayer().rotationPitch;
      this.rotationYaw = getLocalPlayer().rotationYaw;
      this.prevRotationYawHead = getLocalPlayer().prevRotationYawHead;
      this.rotationYawHead = getLocalPlayer().rotationYawHead;
      this.setRenderYawOffset(this.rotationYaw);
      this.setRotationYawHead(this.rotationYaw);

      this.prevPosX = this.lastTickPosX = this.posX;
      this.prevPosY = this.lastTickPosY = this.posY;
      this.prevPosZ = this.lastTickPosZ = this.posZ;

      this.inventory = getLocalPlayer().inventory;
      this.inventoryContainer = getLocalPlayer().inventoryContainer;
      this.noClip = true;
    }

    private void activate() {
      MC.setRenderViewEntity(this);
      MC.getRenderManager().renderViewEntity = this;
      ForgeHaxHooks.allowDifferentUserForFreecam = true;
      ForgeHaxHooks.allowMovementInFreecam = true;
    }

    private void deactivate() {
      MC.setRenderViewEntity(getLocalPlayer());
      MC.getRenderManager().renderViewEntity = getLocalPlayer();
      ForgeHaxHooks.allowDifferentUserForFreecam = false;
      ForgeHaxHooks.allowMovementInFreecam = false;
    }

    private void tick() {
      if (!bindMacro.isKeyDown()) { // Baritone messes with the movementInput, so it's better to do this ourselves
        float forward = 0f, strafe = 0f, up = 0f;
        if (Bindings.forward.isPressed()) forward++;
        if (Bindings.back.isPressed()) forward--;
        if (Bindings.left.isPressed()) strafe++;
        if (Bindings.right.isPressed()) strafe--;
        if (Bindings.jump.isPressed()) up++;
        if (Bindings.sneak.isPressed()) up--;
        this.handleMotion(forward, strafe, up, speed.get());
      } else this.handleMotion(0, 0, 0, 0);
    }

    @Override
    public boolean isSpectator() { return true; }
    @Override
    public boolean isUser() { return true; } // so this doesn't get overruled by patch I hope
    @Override
    public boolean isCurrentViewEntity() { return true; } // so this doesn't get overruled by patch I hope
    // @Override
    // public void onUpdate() { } // don't do anything

    private void handleMotion(float forward, float strafe, float up, double scale) {
      this.prevPosX = this.lastTickPosX = this.posX;
      this.prevPosY = this.lastTickPosY = this.posY;
      this.prevPosZ = this.lastTickPosZ = this.posZ;

      double xFactor = Math.sin(this.rotationYaw * Math.PI / 180D);
      double zFactor = Math.cos(this.rotationYaw * Math.PI / 180D);

      this.motionX = (strafe * zFactor - forward * xFactor) * scale;
      this.motionY = (double) up * scale;
      this.motionZ = (forward * zFactor + strafe * xFactor) * scale;

      this.move(MoverType.SELF, this.motionX, this.motionY, this.motionZ);

      this.chunkCoordX = (int) Math.floor(this.posX) >> 4;
      this.chunkCoordY = (int) Math.floor(this.posY) >> 4;
      this.chunkCoordZ = (int) Math.floor(this.posZ) >> 4;
    }
  }
}
