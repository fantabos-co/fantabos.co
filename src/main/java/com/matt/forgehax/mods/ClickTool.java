package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getWorld;

import com.matt.forgehax.Helper;
import com.matt.forgehax.mods.managers.FriendManager;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.math.VectorUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import org.lwjgl.input.Mouse;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;

@RegisterMod
public class ClickTool extends ToggleMod {

  private enum Mode {
    DELETE,
    INFO,
    DISTANCE,
    FRIEND,
    SETBLOCK,
    TELEPORT,
    MOUNT
  }

  public final Setting<Mode> mode =
    getCommandStub()
      .builders()
      .<Mode>newSettingEnumBuilder()
      .name("mode")
      .description("The action mode (use -? to list them)")
      .defaultTo(Mode.INFO)
      .build();

  public final Setting<Integer> set_to =
    getCommandStub()
      .builders()
      .<Integer>newSettingBuilder()
      .name("set-to")
      .description("The block to set at click position in SETBLOCK mode (block id)")
      .defaultTo(Block.getIdFromBlock(Blocks.BEDROCK))
      .build();

  public final Setting<Boolean> clipboard =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("clipboard")
      .description("If in info mode, copy NBT data to clipboard")
      .defaultTo(true)
      .build();

  public ClickTool() {
    super(Category.WORLD, "ClickTool", false, "Do action on middle click");
  }
  
  @SubscribeEvent
  public void onInput(MouseEvent event) {
    if (getWorld() == null || getLocalPlayer() == null) {
      return;
    }
    
    if (event.getButton() == 2 && Mouse.getEventButtonState()) { // on middle click
      RayTraceResult aim;
      NBTTagCompound tag = new NBTTagCompound();
      switch(mode.get()) {
        case MOUNT:
          aim = MC.objectMouseOver;
          if (aim != null && aim.typeOfHit == RayTraceResult.Type.ENTITY &&
              aim.entityHit != null) {
            getLocalPlayer().startRiding(aim.entityHit, true);
          } else {
            getLocalPlayer().dismountRidingEntity();
          }
          return;
        case FRIEND:
          aim = MC.objectMouseOver;
          if (aim != null && aim.typeOfHit == RayTraceResult.Type.ENTITY &&
              aim.entityHit != null && aim.entityHit instanceof EntityPlayer) {
            FriendManager.addFriend(aim.entityHit.getName());
          }
          return;
        case DELETE:
          aim = MC.objectMouseOver;
          if (aim != null && aim.typeOfHit == RayTraceResult.Type.ENTITY && aim.entityHit != null) {
            MC.world.removeEntity(aim.entityHit);
            Helper.printWarning("Removed %s from client world", aim.entityHit.getName());
          }
          return;
        case DISTANCE:
          aim = MC.player.rayTrace(999, 0);
          if (aim != null && aim.typeOfHit != RayTraceResult.Type.MISS) {
            Vec3d target = new Vec3d(aim.getBlockPos().getX(), aim.getBlockPos().getY(), aim.getBlockPos().getZ());
            Helper.printLog("[ %d %d %d ]", aim.getBlockPos().getX(), aim.getBlockPos().getY(), aim.getBlockPos().getZ());
            Helper.printLog("%s : %.1fm", getWorld().getBlockState(aim.getBlockPos()).getBlock().getLocalizedName(),
                                                  VectorUtils.distance(getLocalPlayer().getPositionVector(), target));
          }
          return;
        case TELEPORT:
          aim = MC.player.rayTrace(999, 0);
          if (aim != null && aim.typeOfHit != RayTraceResult.Type.MISS) {
            getLocalPlayer().setPosition(aim.getBlockPos().getX(), aim.getBlockPos().getY(), aim.getBlockPos().getZ());
          }
          return;
        case SETBLOCK:
          aim = MC.player.rayTrace(999, 0);
          if (aim != null && aim.typeOfHit == RayTraceResult.Type.BLOCK) {
            getWorld().setBlockState(aim.getBlockPos(), Block.getStateById(set_to.get()));
            Helper.printLog("[ %d %d %d ] %s -> %s", aim.getBlockPos().getX(), aim.getBlockPos().getY(), aim.getBlockPos().getZ(),
               getWorld().getBlockState(aim.getBlockPos()).getBlock().getLocalizedName(), Block.getBlockById(set_to.get()).getLocalizedName());
          }
          return;
        case INFO:
          aim = MC.objectMouseOver;
          if (aim == null) aim = MC.player.rayTrace(999, 0);
          if (aim == null || aim.typeOfHit == RayTraceResult.Type.MISS) { // Show NBT for held item if trace is still null
            getLocalPlayer().getHeldItemMainhand().writeToNBT(tag);
            Helper.printInform("Held Item : " + getLocalPlayer().getHeldItemMainhand().getDisplayName());
          } else if (aim.typeOfHit == RayTraceResult.Type.BLOCK) {
            if (getWorld().getBlockState(aim.getBlockPos()).getBlock().hasTileEntity()) {
              Helper.printLog(getWorld().getBlockState(aim.getBlockPos()).getBlock().getLocalizedName());
              getWorld().getTileEntity(aim.getBlockPos()).writeToNBT(tag);
            } else { // If there is nothing to display for the target block, show its distance
              Vec3d target = new Vec3d(aim.getBlockPos().getX(), aim.getBlockPos().getY(), aim.getBlockPos().getZ());
              Helper.printLog("[ %d %d %d ]", aim.getBlockPos().getX(), aim.getBlockPos().getY(), aim.getBlockPos().getZ());
              Helper.printLog("%s : %.1fm", getWorld().getBlockState(aim.getBlockPos()).getBlock().getLocalizedName(),
                                                    VectorUtils.distance(getLocalPlayer().getPositionVector(), target));
            }
          } else if (aim.typeOfHit == RayTraceResult.Type.ENTITY && aim.entityHit != null) {
            Helper.printInform(aim.entityHit.getName());
            aim.entityHit.writeToNBT(tag);
          }
          break;
      }
      printNBTCompound(tag);
      LOGGER.info(tag.toString().trim());
      if (clipboard.get() && mode.get() == Mode.INFO) {
        setClipboardString(tag.toString().trim());
      }
    }
  }

  private void printNBTCompound(NBTTagCompound in) { printNBTCompound(in, 0);}
  private void printNBTCompound(NBTTagCompound in, int depth) {
    for (String key : in.getKeySet()) {
      NBTBase tag = in.getTag(key);
      if (tag instanceof NBTTagCompound) {
        Helper.printLog(arrow(depth) + " " + key + " >");
        printNBTCompound(((NBTTagCompound) tag), depth + 1);
      } else if (tag instanceof NBTTagList) {
        Helper.printLog(arrow(depth) + " " + key + " >");
        NBTTagList list = (NBTTagList) tag;
        list.forEach(t -> {
                    if (t instanceof NBTTagCompound)
                      printNBTCompound(((NBTTagCompound) t), depth + 1);
                    else
                      Helper.printLog(arrow(depth+1) + t.toString());
              });
      } else {
        Helper.printLog(arrow(depth) + " %s : %s", key, tag.toString());
      }
    }
  }

  private static String arrow(int depth) {
    String out = "> ";
    for (int i=0; i<depth; i++) out += "> ";
    return out;
  }

  private static void setClipboardString(String stringIn) {
    StringSelection selection = new StringSelection(stringIn);
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
  }
}
