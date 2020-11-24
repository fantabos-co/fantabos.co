package com.matt.forgehax.asm.events;

import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Created on 26/10/2020 by Tonio
 *  probably this could be better but damn its late
 */
public class ModelRotationEvent extends Event {

  public ModelRotationEvent(float in) {
    this.val = in;
  }

  protected float val;

  public float get() { return val; };
  public void set(float in) { val = in; };

  public static class Yaw extends ModelRotationEvent {
    public Yaw(float in) { super(in); }
  }
    
  public static class Pitch extends ModelRotationEvent {
    public Pitch(float in) { super(in); }
  }
}

