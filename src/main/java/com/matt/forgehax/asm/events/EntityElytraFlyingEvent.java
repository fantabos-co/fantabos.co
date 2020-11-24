package com.matt.forgehax.asm.events;

import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class EntityElytraFlyingEvent extends Event {
    
  private final Entity entity;
    
  public EntityElytraFlyingEvent(Entity in) {
    this.entity = in;
  }
  
  public Entity getEntity() {
    return entity;
  }
}