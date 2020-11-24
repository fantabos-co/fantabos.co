package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;

import com.matt.forgehax.asm.events.AddCollisionBoxToListEvent;
import com.matt.forgehax.asm.events.SneakCheckEvent;
import com.matt.forgehax.events.LocalPlayerUpdateEvent;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;

import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Created on 9/4/2016 by fr1kin
 * Updated by IronException and Tonio_Cartonio
 */
@RegisterMod
public class SafeWalkMod extends ToggleMod {
  
  public SafeWalkMod() {
    super(Category.MOVEMENT, "SafeWalk", false, "Prevents you from falling off blocks");
  }
  
  private final Setting<Boolean> collisions =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("collisions")
      .description("Give air collision boxes")
      .defaultTo(false)
      .build();

  private final Setting<Boolean> sneak_lock =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("sneak")
      .description("Block you as if you were sneaking")
      .defaultTo(true)
      .build();

  private final Setting<Boolean> ground_only =
    getCommandStub()
      .builders()
      .<Boolean>newSettingBuilder()
      .name("ground-only")
      .description("Don't safewalk if midair")
      .defaultTo(false)
      .build();

  private final Setting<Float> min_height =
    getCommandStub()
      .builders()
      .<Float>newSettingBuilder()
      .name("min-height")
      .description("Minimum height above ground for safewalk to trigger")
      .min(0f)
      .max(100f)
      .defaultTo(2f)
      .build();

  private final Setting<Double> offset =
    getCommandStub()
      .builders()
      .<Double>newSettingBuilder()
      .name("offset")
      .description("Set this to very small numbers to \"cross gaps\", may allow you to fall")
      .defaultTo(0d)
      .build();

  @SubscribeEvent
  public void onAddCollisionBox(AddCollisionBoxToListEvent event) {
    if (!collisions.get()) {
      return;
    }
    
    if (getLocalPlayer() != null &&
        (EntityUtils.isDrivenByPlayer(event.getEntity())
            || event.getEntity() == getLocalPlayer())) {
      
      AxisAlignedBB axisalignedbb = new AxisAlignedBB(event.getPos()).shrink(0.3D);
      if (event.getEntityBox().intersects(axisalignedbb)) {
        if (isAbovePlayer(event.getPos()) &&
            !hasCollisionBox(event.getPos()) &&
            !isAboveBlock(event.getPos(), min_height.getAsInteger())) {
          
          event.getCollidingBoxes().add(axisalignedbb);
        }
      }
    }
  }
 
  private boolean isAbovePlayer(BlockPos pos) {
    return pos.getY() >= getLocalPlayer().posY;
  }
  
  private boolean isAboveBlock(BlockPos pos, int minHeight) {
    for (int i = 0; i < minHeight; i++) {
      if (hasCollisionBox(pos.down(i))) {
        return true;
      }
    }
    return false;
  }
  
  private boolean hasCollisionBox(BlockPos pos) {
    return MC.world.getBlockState(pos).getCollisionBoundingBox(MC.world, pos) != null;
  }

  @SubscribeEvent
  public void onSneakCheck(SneakCheckEvent event) {
    if (!sneak_lock.get()) return;
    if (!event.getEntity().equals(getLocalPlayer())) return;
    if (shouldEvenSneak(getLocalPlayer())) {
      if (!ground_only.get() || event.getOnGround()) {
        event.setCanceled(true); // DO SNEAK!
        getLocalPlayer().stepHeight = Math.min(min_height.get(), 0.6f); // default step
      }
    }
  }

  private boolean shouldEvenSneak(final EntityPlayerSP entity) {
    // was this.onGround in vanilla
    return noCollision(entity, entity.motionX, entity.motionZ) && !(entity.movementInput.jump);
  }

  private boolean noCollision(final Entity entity, final double x, final double z) {
    return entity.world.getCollisionBoxes(entity,
        getBoundingBox(entity, x, min_height.get() + offset.get(), z)).isEmpty() ||
           entity.world.getCollisionBoxes(entity,
        getBoundingBox(entity, 0d, min_height.get() + offset.get(), z)).isEmpty() ||
           entity.world.getCollisionBoxes(entity,
        getBoundingBox(entity, x, min_height.get() + offset.get(), 0d)).isEmpty();
  }

  private AxisAlignedBB getBoundingBox(final Entity entity, final double x, final double y, final double z) {
    return entity.getEntityBoundingBox().expand(offset.get(), -y, offset.get()).offset(x, 0, z);
  }
}
