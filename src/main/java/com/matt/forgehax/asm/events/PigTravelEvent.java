package com.matt.forgehax.asm.events;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Created on 28/10/2020 by Tonio
 */
public class PigTravelEvent extends Event {

  private final EntityLivingBase entity;
  private float forward = 1.f;
  private float strafe = 0.f;
  private float jump = 0.f;

  public PigTravelEvent(EntityLivingBase in) {
    this.entity = in;
  }

  public EntityLivingBase getEntity() { return entity; }
  public float getForward() { return forward; }
  public float getStrafe() { return strafe; }
  public float getJump() { return jump; }

  public void setForward(float in) {
    this.forward = in;
  }

  public void setStrafe(float in) {
    this.strafe = in;
  }

  public void setJump(float in) {
    this.jump = in;
  }
}

