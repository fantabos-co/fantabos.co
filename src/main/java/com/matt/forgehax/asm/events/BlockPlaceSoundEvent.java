package com.matt.forgehax.asm.events;

import net.minecraft.block.SoundType;
import net.minecraftforge.fml.common.eventhandler.Event;

/**
 * Created on 29/07/2020 by Tonio
 */
public class BlockPlaceSoundEvent extends Event {
  
  private SoundType sound;
  
  public BlockPlaceSoundEvent(SoundType sound) {
    this.sound = sound;
  }
  
  public SoundType getSound() { return this.sound; }
  
  public void setSound(SoundType sound) {
    this.sound = sound;
  }
}
