package com.matt.forgehax.asm.events;

import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.Cancelable;
import net.minecraftforge.fml.common.eventhandler.Event;

@Cancelable
public class PlayerDamageBlockEvent extends Event {
  
  private final PlayerControllerMP playerController;
  private final BlockPos pos;
  private final EnumFacing side;
  private boolean fake = false; // Fake mine events can be generated to trigger autotool
  
  public PlayerDamageBlockEvent(
    PlayerControllerMP playerController, BlockPos pos, EnumFacing side) {
    this.playerController = playerController;
    this.pos = pos;
    this.side = side;
  }
  
  public PlayerControllerMP getPlayerController() {
    return playerController;
  }
  
  public BlockPos getPos() {
    return pos;
  }
  
  public EnumFacing getSide() {
    return side;
  }

  public boolean isFake() {
    return this.fake;
  }

  public void setFake(boolean state) {
    this.fake = state;
  }
}
