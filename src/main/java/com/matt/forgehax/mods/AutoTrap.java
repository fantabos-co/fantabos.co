package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getModManager;
import static com.matt.forgehax.Helper.getWorld;
import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getPlayerController;
import static com.matt.forgehax.Helper.getNetworkManager;

import com.matt.forgehax.Helper;
import com.matt.forgehax.asm.reflection.FastReflection.Fields;
import com.matt.forgehax.mods.managers.PositionRotationManager;
import com.matt.forgehax.mods.managers.PositionRotationManager.RotationState.Local;
import com.matt.forgehax.mods.services.HotbarSelectionService.ResetFunction;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.command.Setting;
import com.matt.forgehax.util.common.PriorityEnum;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.entity.LocalPlayerInventory;
import com.matt.forgehax.util.entity.LocalPlayerUtils;
import com.matt.forgehax.util.entity.LocalPlayerInventory.InvItem;
import com.matt.forgehax.util.BlockHelper;
import com.matt.forgehax.util.PacketHelper;
import com.matt.forgehax.util.SimpleTimer;
import com.matt.forgehax.util.Utils;
import com.matt.forgehax.util.BlockHelper.BlockTraceInfo;
import net.minecraft.block.BlockObsidian;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketEntityAction.Action;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.Comparator;
import java.util.Optional;

@RegisterMod
public class AutoTrap extends ToggleMod implements PositionRotationManager.MovementUpdateListener {

  public AutoTrap() {
    super(Category.COMBAT, "AutoTrap", false, "Trap closest player in Obsidian");
  }

  public final Setting<Integer> blocks_per_tick =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("blocks-per-tick")
          .description("Blocks to place in one burst")
          .min(1)
          .max(9)
          .defaultTo(1)
          .build();

  public final Setting<Integer> delay =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("delay")
          .description("Delay in ticks after each burst")
          .min(0)
          .max(50)
          .defaultTo(1)
          .build();

  public final Setting<Float> range =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("range")
          .description("Trigger only for players this close")
          .min(0F)
          .max(20F)
          .defaultTo(3F)
          .build();
    
  public final Setting<Boolean> legit =
    getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("legit")
        .description("Don't place blocks you could not see")
        .defaultTo(false)
        .build();
  
  public final Setting<Boolean> centered_only =
    getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("centered-only")
        .description("Don't try to trap players who are not centered")
        .defaultTo(true)
        .build();

  public final Setting<Integer> auto_disable =
    getCommandStub()
        .builders()
        .<Integer>newSettingBuilder()
        .name("timeout")
        .description("Disable after this many ms of not placing blocks")
        .min(0)
        .max(100000)
        .defaultTo(3000)
        .build();

  @Override
  protected void onEnabled() {
    PositionRotationManager.getManager().register(this, PriorityEnum.HIGH);
    phase = 0;
    timer.start();
  }
  
  @Override
  protected void onDisabled() {
    PositionRotationManager.getManager().unregister(this);
  }
  
  private int placed = 0;
  private int tickCount = 0;
  private boolean warned = false;
  private int phase = 0;
  private SimpleTimer timer = new SimpleTimer();

  private static boolean isCentered(Entity e) {
    BlockPos round = new BlockPos(e.getPositionVector());
    if (Math.abs(e.posX - (round.getX() + 0.5)) <= 0.2D && Math.abs(e.posZ - (round.getZ() + 0.5)) <= 0.2D)
      return true;
    return false;
  }

  @Override
  public void onLocalPlayerMovementUpdate(Local state) {
    if (placed > 0 && tickCount < delay.get()) {
      ++tickCount;
	    return;
    }
    tickCount = 0;

    if (MC.player == null || getModManager().get("Freecam").get().isEnabled()) return;

    if (timer.hasTimeElapsed(auto_disable.get())) {
      Helper.printWarning("AutoTrap timed out, disabling");
      this.disable(false);
      return;
    }

    // Search for target player
    EntityPlayer target_player = getWorld().playerEntities.stream()
                            .filter(p -> !getLocalPlayer().equals(p))
                            .filter(p -> !centered_only.get() || isCentered(p)) // Can't trap him properly if he's not centered
                            .min(Comparator.comparing(p -> getLocalPlayer().getDistance(p)))
                            .orElse(null);
    if (target_player == null || getLocalPlayer().getDistance(target_player) > range.get()) return;

    // Search for Obsidian
    InvItem items = LocalPlayerInventory.getHotbarInventory()
      .stream()
      .filter(InvItem::nonNull)
      .filter(held_item -> held_item.getItem() instanceof ItemBlock &&
                           ((ItemBlock) held_item.getItem()).getBlock() instanceof BlockObsidian)
      .max(Comparator.comparingInt(LocalPlayerInventory::getHotbarDistance))
      .orElse(InvItem.EMPTY);
    
    if (items == null || items.equals(InvItem.EMPTY)) {
      if (!warned) {
        Helper.printError("Out of Obsidian");
        warned = true;
      }
      return;
    }
    warned = false;
    ResetFunction func = LocalPlayerInventory.setSelected(items);

    Vec3d offsets[] = Offsets.FULL;
    int size = 18;

    int start = phase;
    int placed = 0;
    boolean once = false;
    while (placed < blocks_per_tick.get()) {
      BlockPos target = new BlockPos(target_player.getPositionVector().add(offsets[phase]));

      if (once && phase == start) break; // stop for this tick once a full cycle has been done
      once = true;

      phase++;
      if (phase >= size) phase = 0;

      // check if block is already placed
      if (!BlockHelper.isBlockReplaceable(target)) {
        continue;
      }

      // Check placeable block
      final Vec3d eyes = EntityUtils.getEyePos(getLocalPlayer());
      final Vec3d dir = LocalPlayerUtils.getViewAngles().getDirectionVector();

      // check if we have a block (adjacent or not) to blockpos to click at
      BlockTraceInfo trace =
      Optional.ofNullable(BlockHelper.getPlaceableBlockSideTrace(eyes, dir, target, legit.get()))
          .filter(tr -> tr.isPlaceable(items))
          .orElse(null);

      if (trace == null) {
        continue;
      }

      // Actually place the block

      Vec3d hit = trace.getHitVec();
      state.setServerAngles(Utils.getLookAtAngles(hit));
      if (legit.get()) {
        state.setClientAngles(Utils.getLookAtAngles(hit));
      }
      
      final BlockTraceInfo tr = trace;

      boolean sneak = tr.isSneakRequired() && !LocalPlayerUtils.isSneaking();
      if (sneak) {
        // send start sneaking packet
        PacketHelper.ignoreAndSend(
            new CPacketEntityAction(getLocalPlayer(), Action.START_SNEAKING));

        LocalPlayerUtils.setSneaking(true);
        LocalPlayerUtils.setSneakingSuppression(true);
      }

      getPlayerController()
          .processRightClickBlock(
              getLocalPlayer(),
              getWorld(),
              tr.getPos(),
              tr.getOppositeSide(),
              hit,
              EnumHand.MAIN_HAND);

      getNetworkManager().sendPacket(new CPacketAnimation(EnumHand.MAIN_HAND));

      if (sneak) {
        LocalPlayerUtils.setSneaking(false);
        LocalPlayerUtils.setSneakingSuppression(false);

        getNetworkManager()
            .sendPacket(new CPacketEntityAction(getLocalPlayer(), Action.STOP_SNEAKING));
      }
      placed++;
    }
    
    Fields.Minecraft_rightClickDelayTimer.set(MC, 4);
    func.revert();
    tickCount = 0;
    timer.start();
  }

  private static class Offsets {
    private static final Vec3d[] FULL = {
      // Floor center
      new Vec3d(0, -1, 0),
      // Floor
      new Vec3d(1, -1, 0),
      new Vec3d(-1, -1, 0),
      new Vec3d(0, -1, 1),
      new Vec3d(0, -1, -1),
      // Layer 1
      new Vec3d(1, 0, 0),
      new Vec3d(-1, 0, 0),
      new Vec3d(0, 0, 1),
      new Vec3d(0, 0, -1),
      // Layer 2
      new Vec3d(1, 1, 0),
      new Vec3d(-1, 1, 0),
      new Vec3d(0, 1, 1),
      new Vec3d(0, 1, -1),
      // Ceiling
      new Vec3d(1, 2, 0),
      new Vec3d(-1, 2, 0),
      new Vec3d(0, 2, 1),
      new Vec3d(0, 2, -1),
      // Ceiling center
      new Vec3d(0, 2, 0)
    };
  }
}
