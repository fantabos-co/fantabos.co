package com.matt.forgehax.events;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.eventhandler.Event;

public class BlockUpdateEvent extends Event {
  private World world;
  private BlockPos pos;
  private IBlockState oldState;
  private IBlockState newState;
  private int flags;
  
  public BlockUpdateEvent(World worldIn, BlockPos pos, IBlockState oldState, IBlockState newState, int flags) {
    this.world = worldIn;
    this.pos = pos;
    this.oldState = oldState;
    this.newState = newState;
    this.flags = flags;
  }
  
  public World getWorld() { return this.world; }
  public BlockPos getPos() { return this.pos; }
  public IBlockState getOldState() { return this.oldState; }
  public IBlockState getNewState() { return this.newState; }
  public int getFlags() { return this.flags; }
}
