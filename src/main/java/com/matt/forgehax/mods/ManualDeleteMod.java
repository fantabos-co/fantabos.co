package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getWorld;

import com.matt.forgehax.Helper;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.math.VectorUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Mouse;

/**
 * Kami BLUE added the INFO mode, I like that and am adding it too to my fork
 */

@RegisterMod
public class ManualDeleteMod extends ToggleMod {

  public enum Mode {
    DELETE,
    INFO
  }

    public final Setting<Mode> mode =
      getCommandStub()
          .builders()
          .<Mode>newSettingEnumBuilder()
          .name("mode")
          .description("The action mode [info/delete]")
          .defaultTo(Mode.INFO)
          .build();
    public final Setting<Boolean> distance =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("distance")
          .description("Measure distance if no entity is targeted")
          .defaultTo(true)
          .build();
    public final Setting<Boolean> blockpos =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("blockpos")
          .description("Print the BlockPos together with distance")
          .defaultTo(true)
          .build();
  
  public ManualDeleteMod() {
    super(Category.WORLD, "EntityTool", false,
        "Get info or delete entities clientside");
  }
  
  @SubscribeEvent
  public void onInput(MouseEvent event) {
    if (getWorld() == null || getLocalPlayer() == null) {
      return;
    }
    
    if (event.getButton() == 2 && Mouse.getEventButtonState()) { // on middle click
      RayTraceResult aim = MC.objectMouseOver;
      if (aim != null && aim.typeOfHit == RayTraceResult.Type.ENTITY && aim.entityHit != null) {
        switch(mode.get()) {
          case DELETE:
            MC.world.removeEntity(aim.entityHit);
            break;
          case INFO:
            NBTTagCompound tag = new NBTTagCompound();
            aim.entityHit.writeToNBT(tag);
            Helper.printInform(tag.toString().trim());
            break;
        }
      } else if (distance.get() || blockpos.get()) {
        aim = MC.player.rayTrace(999, 0);
        if (aim.typeOfHit == RayTraceResult.Type.BLOCK) {
          Vec3d target = new Vec3d(aim.getBlockPos().getX(), aim.getBlockPos().getY(), aim.getBlockPos().getZ());
          if (blockpos.get()) Helper.printInform("%s", aim.getBlockPos().toString());
          if (distance.get()) Helper.printInform("%.1fm", VectorUtils.distance(getLocalPlayer().getPositionVector(), target));
        }
      }
    }
  }
}
