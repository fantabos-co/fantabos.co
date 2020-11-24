package com.matt.forgehax.asm.events;

import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInputFromOptions;
import net.minecraftforge.fml.common.eventhandler.Event;

public class UpdateInputFromOptionsEvent extends Event {

  private final MovementInputFromOptions in;

  public UpdateInputFromOptionsEvent(MovementInputFromOptions in) {
    this.in = in;
  }

  public MovementInputFromOptions getInput() { return this.in; }

  public void copyTo(MovementInput to) {
    to.moveStrafe = in.moveStrafe;
    to.moveForward = in.moveForward;
    to.forwardKeyDown = in.forwardKeyDown;
    to.backKeyDown = in.backKeyDown;
    to.leftKeyDown = in.leftKeyDown;
    to.rightKeyDown = in.rightKeyDown;
    to.jump = in.jump;
    to.sneak = in.sneak;
  }

  public void clearInput() {
    in.moveStrafe = 0f;
    in.moveForward = 0f;
    in.forwardKeyDown = false;
    in.backKeyDown = false;
    in.leftKeyDown = false;
    in.rightKeyDown = false;
    in.jump = false;
    in.sneak = false;
  }
    
}
