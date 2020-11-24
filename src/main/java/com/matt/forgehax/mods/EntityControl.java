package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getRidingEntity;

import com.matt.forgehax.asm.ForgeHaxHooks;
import com.matt.forgehax.asm.events.PigTravelEvent;
import com.matt.forgehax.asm.reflection.FastReflection;
import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.passive.AbstractHorse;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class EntityControl extends ToggleMod {

    private final Setting<Boolean> forceControl =
      getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("forceSteer")
        .description("Force llamas, horses and pigs to steer with you")
        .defaultTo(true)
        .changed(cb -> {
            if (isEnabled())
                ForgeHaxHooks.forceControlEntity = cb.getTo();
        })
        .build();

    public final Setting<Boolean> max_jump = 
      getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("jump")
        .description("Make jumps with horses always best possible value")
        .defaultTo(true)
        .build();

    public final Setting<Boolean> set_attributes = 
      getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("attributes")
        .description("Change entity attributes")
        .defaultTo(true)
        .changed(cb -> { if (isEnabled() && cb.getFrom()) resetStats(); })
        .build();

    public final Setting<Double> speed =
      getCommandStub()
        .builders()
        .<Double>newSettingBuilder()
        .name("speed")
        .description("Sets the speed for ridable entities. Default: 0.3375")
        .defaultTo(0.3375D)
        .min(0.01D)
        .max(10.0D)
        .build();

    private final Setting<Double> multiplier =
      getCommandStub()
        .builders()
        .<Double>newSettingBuilder()
        .name("multiplier")
        .description("Speed multiplier while sprinting")
        .min(0D)
        .max(5D)
        .defaultTo(1.0D)
        .build();

    private final Setting<Double> jumpHeight =
      getCommandStub()
        .builders()
        .<Double>newSettingBuilder()
        .name("jumpHeight")
        .description("Modified horse jump height attribute. Default: 1")
        .min(0D)
        .max(10D)
        .defaultTo(1.0D)
        .build();

    public EntityControl() {
      super(Category.MOVEMENT, "EntityControl", false, "Allows you to manipulate the behavior of whatever entity you're riding");
    }

    @Override
    protected void onEnabled() {
      ForgeHaxHooks.forceControlEntity = forceControl.getAsBoolean();
    }

    @Override
    protected void onDisabled() {
      ForgeHaxHooks.forceControlEntity = false;
      if (getRidingEntity() instanceof AbstractHorse) {
        resetStats();
      }
    }

    @SubscribeEvent
    public void onPigTravel(PigTravelEvent event) {
      event.setForward(getLocalPlayer().movementInput.moveForward);
      event.setStrafe(getLocalPlayer().movementInput.moveStrafe);
      event.setJump(getLocalPlayer().movementInput.jump
          ? 1.f / (float) ((speed.get() / speed.getDefault()) * jumpHeight.get()) // pig would move up relative to its speed otherwise
          : 0.f);
      if (set_attributes.get()) {
        double s = (getLocalPlayer().isSprinting() ? speed.get() * multiplier.get() : speed.get());
        event.getEntity().setAIMoveSpeed((float) s);
      }
    }

    @SubscribeEvent
    public void onLocalPlayerUpdate(LocalPlayerUpdateEvent event) {
      if (!max_jump.get()) return;
      FastReflection.Fields.EntityPlayerSP_horseJumpPower.set(getLocalPlayer(), 1.F);
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingEvent.LivingUpdateEvent event) {
      if (!set_attributes.getAsBoolean()) return;
      if (EntityUtils.isDrivenByPlayer(event.getEntity())
          && getRidingEntity() instanceof AbstractHorse) {
        
        double newSpeed = speed.getAsDouble();
        if (getLocalPlayer().isSprinting()) {
          newSpeed *= multiplier.getAsDouble();
        }
        applyStats(jumpHeight.getAsDouble(), newSpeed);
      }
    }

    private void resetStats() { applyStats(jumpHeight.getDefault(), speed.getDefault()); }
    
    private void applyStats(double newJump, double newSpeed) {
      final IAttribute jump_strength =
          FastReflection.Fields.AbstractHorse_JUMP_STRENGTH.get(getRidingEntity());
      final IAttribute movement_speed =
          FastReflection.Fields.SharedMonsterAttributes_MOVEMENT_SPEED.get(getRidingEntity());

      if(getRidingEntity() == null) return;

      ((EntityLivingBase) getRidingEntity())
          .getEntityAttribute(jump_strength)
          .setBaseValue(newJump);
      ((EntityLivingBase) getRidingEntity())
          .getEntityAttribute(movement_speed)
          .setBaseValue(newSpeed);
    }
}
