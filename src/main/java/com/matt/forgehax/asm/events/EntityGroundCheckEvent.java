package com.matt.forgehax.asm.events;

import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class EntityGroundCheckEvent extends Event {
  
  private final EntityLivingBase entityLivingBase;
  
  public EntityGroundCheckEvent(EntityLivingBase entityLivingBase) {
    this.entityLivingBase = entityLivingBase;
  }
  
  public EntityLivingBase getEntityLivingBase() {
    return entityLivingBase;
  }
}
