package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getModManager;

import com.matt.forgehax.asm.events.LocalPlayerUpdateMovementEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class Boost extends ToggleMod {
  
  private final Setting<Float> ground_boost =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("ground")
          .description("Amount while on-ground")
          .min(0f)
          .max(5f)
          .defaultTo(0.02F)
          .build();
  private final Setting<Float> ground_cap =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("cap-ground")
          .description("Max speed on_ground")
          .min(0f)
          .max(5f)
          .defaultTo(0.175F)
          .build();
  private final Setting<Float> air_boost =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("air")
          .description("Amount applied when falling")
          .min(0f)
          .max(5f)
          .defaultTo(0.1F)
          .build();
  private final Setting<Float> air_cap =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("cap-air")
          .description("Max speed mid air")
          .min(0f)
          .max(5f)
          .defaultTo(0.25F)
          .build();
  private final Setting<Float> liquid_boost =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("liquid")
          .description("Amount applied when in liquids")
          .min(0f)
          .max(5f)
          .defaultTo(0.1F)
          .build();
  private final Setting<Float> liquid_cap =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("cap-liquid")
          .description("Max speed in liquids")
          .min(0f)
          .max(5f)
          .defaultTo(0.23F)
          .build();
  
  private final Setting<Boolean> forward_only =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("forward-only")
          .description("Only add speed to forward movement")
          .defaultTo(false)
          .build();
  
  public Boost() {
    super(Category.MOVEMENT, "Boost", false, "Change your speed");
  }
  
  @SubscribeEvent
  public void onLocalPlayerUpdateMovement(LocalPlayerUpdateMovementEvent event) {
    if (getLocalPlayer() == null) return;
    if (getModManager().get("Freecam").get().isEnabled()) return;
    if (MC.gameSettings.keyBindSneak.isKeyDown()) return;
    if (getLocalPlayer().onGround) {
      setPlayerSpeed(ground_boost.get(), false);
      if (ground_cap.get() > 0F)
        capPlayerSpeed(ground_cap.get(), false);
    } else if (getLocalPlayer().isInWater() || getLocalPlayer().isInLava()) {
      setPlayerSpeed(liquid_boost.get(), true);
      if (liquid_cap.get() > 0F)
        capPlayerSpeed(liquid_cap.get(), true);
    } else if (!getLocalPlayer().isElytraFlying() &&
               !getLocalPlayer().capabilities.isFlying && 
               !getLocalPlayer().onGround) {
      setPlayerSpeed(air_boost.get(), true);
      if (air_cap.get() > 0F)
        capPlayerSpeed(air_cap.get(), true);
    }
  }

  private void setPlayerSpeed(float boost, boolean vertical) {
    float yaw = (float)Math.toRadians(getLocalPlayer().rotationYaw);
    if (MC.gameSettings.keyBindForward.isKeyDown() || (getModManager().get("AutoWalk")).get().isEnabled()) {
      getLocalPlayer().motionX -= MathHelper.sin(yaw) * boost;
      getLocalPlayer().motionZ += MathHelper.cos(yaw) * boost;
    } else if (!forward_only.get() && MC.gameSettings.keyBindBack.isKeyDown()) {
      getLocalPlayer().motionX += MathHelper.sin(yaw) * boost;
      getLocalPlayer().motionZ -= MathHelper.cos(yaw) * boost;
    }
    if (!forward_only.get() && MC.gameSettings.keyBindLeft.isKeyDown()) {
      getLocalPlayer().motionX -= MathHelper.sin(yaw - (float) Math.toRadians(90.0F)) * boost;
      getLocalPlayer().motionZ += MathHelper.cos(yaw - (float) Math.toRadians(90.0F)) * boost;
    } else if (!forward_only.get() && MC.gameSettings.keyBindRight.isKeyDown()) {
      getLocalPlayer().motionX -= MathHelper.sin(yaw + (float) Math.toRadians(90.0F)) * boost;
      getLocalPlayer().motionZ += MathHelper.cos(yaw + (float) Math.toRadians(90.0F)) * boost;
    }
    if (vertical && !forward_only.get()) {
      if (MC.gameSettings.keyBindJump.isKeyDown())
        getLocalPlayer().motionY += boost;
      if (MC.gameSettings.keyBindSprint.isKeyDown())
        getLocalPlayer().motionY -= boost;
    }
  }

  private void capPlayerSpeed(float cap, boolean vertical) {
    double speed = Math.sqrt(Math.pow(getLocalPlayer().motionZ, 2) + Math.pow(getLocalPlayer().motionX, 2));
    if (speed > cap) {
      double factor = cap / speed;
      getLocalPlayer().motionX *= factor;
      getLocalPlayer().motionZ *= factor;
    }
    if (vertical && getLocalPlayer().motionY > cap)
      getLocalPlayer().motionY = (getLocalPlayer().motionY / Math.abs(getLocalPlayer().motionY)) * cap;
  }
}
