package com.matt.forgehax.asm.events;

import net.minecraft.entity.Entity;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class SneakCheckEvent extends Event {

  // the parameters
  final Entity entity;
  final boolean isSneaking;
  final boolean onGround;

  // TODO more info could be added to the event?
  public SneakCheckEvent(Entity in, boolean isSneaking, boolean onGround) {
    this.entity = in;
    this.isSneaking = isSneaking;
    this.onGround = onGround;
  }


  public Entity getEntity() { return entity; }
  public boolean getSneaking() { return isSneaking; }
  public boolean getOnGround() { return onGround; }
}
