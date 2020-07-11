package com.matt.forgehax.mods.services;

import static com.matt.forgehax.Helper.getWorld;
import static com.matt.forgehax.Helper.getLocalPlayer;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.mod.ServiceMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.entity.Entity;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;


@RegisterMod
public class HoleService extends ServiceMod {

  public final Setting<Boolean> enabled =
  getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("enabled")
      .description("Actively look for holes around player")
      .defaultTo(true)
      .build();

  public final Setting<Double> radius =
  getCommandStub()
      .builders()
      .<Double>newSettingBuilder()
      .name("radius")
      .description("How far to look for holes")
      .min(0D)
      .defaultTo(20D)
      .build();
  
  public HoleService() {
    super("HoleService");
  }

  public static Queue<BlockPos> holes = new ConcurrentLinkedQueue<BlockPos>();

  @SubscribeEvent(priority = EventPriority.LOW)
  public void onClientTick(TickEvent.ClientTickEvent event) {
    if (getLocalPlayer() == null || getWorld() == null) return;
    holes.clear();

    int maxX = (int) Math.round(getLocalPlayer().posX + (radius.get()/2));
    int maxY = (int) Math.round(getLocalPlayer().posY + (radius.get()/2));
    int maxZ = (int) Math.round(getLocalPlayer().posZ + (radius.get()/2));

    for (int x = (int) Math.round(getLocalPlayer().posX - (radius.get()/2)); x < maxX; x++) {
      for (int y = (int) Math.round(getLocalPlayer().posY - (radius.get()/2)); y < maxY; y++) {
        for (int z = (int) Math.round(getLocalPlayer().posZ - (radius.get()/2)); z < maxZ; z++) {
          BlockPos pos = new BlockPos(x, y, z);
          if (isHole(pos)) {
            holes.add(pos);
          }
        }
      }
    }
  }

  public static boolean isHole(BlockPos pos) {
    if (!getWorld().getBlockState(pos).getBlock().equals(Blocks.AIR) ||
        !getWorld().getBlockState(pos.offset(EnumFacing.UP)).getBlock().equals(Blocks.AIR))
            return false;
    for (EnumFacing off : Offsets.HOLE) {
      if (!getWorld().getBlockState(pos.offset(off)).getBlock().equals(Blocks.BEDROCK) &&
          !getWorld().getBlockState(pos.offset(off)).getBlock().equals(Blocks.OBSIDIAN)) {
              return false;
      }
    }
    return true;
  }

  public static boolean isAboveHole(BlockPos hole, Entity entity) {
    return isAboveHole(hole, entity, 0.2D);
  }

  public static boolean isAboveHole(BlockPos hole, Entity entity, double distance) {
    return isAboveHole(hole, entity.getPositionVector(), distance);
  }

  public static boolean isAboveHole(BlockPos hole, Vec3d player, double distance) {
    double xp = player.x;
    double zp = player.z;
    double xh = hole.getX() + 0.5;
    double zh = hole.getZ() + 0.5;
    if (Math.abs(xp - xh) < 0.2 && Math.abs(zp - zh) < distance)
      return true;
    return false;
  }

  public static class Offsets {
    private static final EnumFacing[] HOLE = {
      EnumFacing.DOWN, EnumFacing.SOUTH, EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.WEST
    };
  }
}
