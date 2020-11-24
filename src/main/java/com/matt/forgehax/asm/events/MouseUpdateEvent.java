package com.matt.forgehax.asm.events;

import net.minecraft.util.MouseHelper;
import net.minecraftforge.fml.common.eventhandler.Event;

public class MouseUpdateEvent extends Event {

  private final MouseHelper in;

  public MouseUpdateEvent(MouseHelper in) {
    this.in = in;
  }

  public MouseHelper getInput() { return this.in; }

  public void copyTo(MouseHelper to) {
    to.deltaX = in.deltaX;
    to.deltaY = in.deltaY;
  }

  public void clearInput() {
    in.deltaX = 0;
    in.deltaY = 0;
  }
}
