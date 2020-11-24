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
import com.matt.forgehax.util.Utils;
import com.matt.forgehax.util.BlockHelper.BlockTraceInfo;

import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockObsidian;
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
public class Surround extends ToggleMod implements PositionRotationManager.MovementUpdateListener {

  public enum Mode {
    FULL,
    SURROUND,
    HUT
  }

  public Surround() {
    super(Category.COMBAT, "Surround", false, "Place obsidian around your feet");
  }

  public final Setting<Mode> mode =
      getCommandStub()
          .builders()
          .<Mode>newSettingEnumBuilder()
          .name("mode")
          .description("Preset [surround/full/hut]")
          .defaultTo(Mode.SURROUND)
          .build();

  public final Setting<Boolean> sneak_only =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("sneak-only")
          .description("Activate only if sneaking")
          .defaultTo(true)
          .build();

  public final Setting<Boolean> ender_chest =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("ender-chest")
          .description("Place ender chests if out of obsidian")
          .defaultTo(true)
          .build();

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
          .defaultTo(0)
          .build();

  public final Setting<Boolean> auto_center =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("center")
          .description("Automatically center the player")
          .defaultTo(true)
          .build();
    
  public final Setting<Boolean> legit =
    getCommandStub()
        .builders()
        .<Boolean>newSettingBuilder()
        .name("legit")
        .description("Don't place blocks you could not see")
        .defaultTo(false)
        .build();

  @Override
  protected void onEnabled() {
    PositionRotationManager.getManager().register(this, PriorityEnum.HIGHEST);
    phase = 0;
  }
  
  @Override
  protected void onDisabled() {
    PositionRotationManager.getManager().unregister(this);
  }
  
  private int placed = 0;
  private int tickCount = 0;
  private boolean warned = false;
  private int phase = 0;

  @Override
  public void onLocalPlayerMovementUpdate(Local state) {
    if (placed > 0 && tickCount < delay.get()) {
      ++tickCount;
	    return;
    }
    tickCount = 0;

    if (sneak_only.get() && !MC.gameSettings.keyBindSneak.isKeyDown()) return;

    if (MC.player == null || getModManager().get("Freecam").get().isEnabled()) return;

    // Search for Obsidian
    InvItem items_stack = LocalPlayerInventory.getHotbarInventory()
      .stream()
      .filter(InvItem::nonNull)
      .filter(held_item -> held_item.getItem() instanceof ItemBlock &&
                           ((ItemBlock) held_item.getItem()).getBlock() instanceof BlockObsidian)
      .max(Comparator.comparingInt(LocalPlayerInventory::getHotbarDistance))
      .orElse(InvItem.EMPTY);
    
    if (items_stack == null || items_stack.equals(InvItem.EMPTY)) {
      if (!warned) {
        Helper.printError("Out of Obsidian");
        warned = true;
      }
      if (ender_chest.get()) {
        items_stack = LocalPlayerInventory.getHotbarInventory()
            .stream()
            .filter(InvItem::nonNull)
            .filter(held_item -> held_item.getItem() instanceof ItemBlock &&
                                 ((ItemBlock) held_item.getItem()).getBlock() instanceof BlockEnderChest)
            .max(Comparator.comparingInt(LocalPlayerInventory::getHotbarDistance))
            .orElse(InvItem.EMPTY);
        
        if (items_stack == null || items_stack.equals(InvItem.EMPTY)) return;
      } else {
        return;
      }
    } else {
      warned = false;
    }
    
    final InvItem items = items_stack;
    ResetFunction func = LocalPlayerInventory.setSelected(items);

    if (auto_center.get() && getLocalPlayer().onGround) {
      BlockPos round = new BlockPos(getLocalPlayer().getPositionVector());
      getLocalPlayer().motionX = ((round.getX() + 0.5D) - getLocalPlayer().posX) / 5;
      getLocalPlayer().motionZ = ((round.getZ() + 0.5D) - getLocalPlayer().posZ) / 5;
    }

    Vec3d offsets[];
    int size;
    switch (mode.get()) {
      case FULL:
        offsets = Offsets.FULL;
        size = 18;
        break;
      case SURROUND:
        offsets = Offsets.SURROUND;
        size = 9;
        break;
      case HUT:
        offsets = Offsets.HUT;
        size = 58;
        break;
      default: return;
    }
    if (phase >= size) phase = 0;

    int start = phase;
    int placed = 0;
    boolean once = false;

    while (placed < blocks_per_tick.get()) {
      BlockPos target = new BlockPos(getLocalPlayer().getPositionVector().add(offsets[phase]));
      
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
  }

  private static class Offsets {
    private static final Vec3d[] SURROUND = {
      new Vec3d(0, -1, 0),
      new Vec3d(1, -1, 0),
      new Vec3d(-1, -1, 0),
      new Vec3d(0, -1, 1),
      new Vec3d(0, -1, -1),
      new Vec3d(1, 0, 0),
      new Vec3d(-1, 0, 0),
      new Vec3d(0, 0, 1),
      new Vec3d(0, 0, -1)
    };
    private static final Vec3d[] FULL = {
      new Vec3d(0, -1, 0),
      new Vec3d(1, -1, 0),
      new Vec3d(-1, -1, 0),
      new Vec3d(0, -1, 1),
      new Vec3d(0, -1, -1),
      new Vec3d(1, 0, 0),
      new Vec3d(-1, 0, 0),
      new Vec3d(0, 0, 1),
      new Vec3d(0, 0, -1),
      new Vec3d(1, 1, 0),
      new Vec3d(-1, 1, 0),
      new Vec3d(0, 1, 1),
      new Vec3d(0, 1, -1),
      new Vec3d(1, 2, 0),
      new Vec3d(-1, 2, 0),
      new Vec3d(0, 2, 1),
      new Vec3d(0, 2, -1),
      new Vec3d(0, 2, 0)
    };
    private static final Vec3d[] HUT = {
      // Center
      new Vec3d(0, -1, 0),
      // Floor
      new Vec3d(1, -1, 0),
      new Vec3d(-1, -1, 0),
      new Vec3d(0, -1, 1),
      new Vec3d(0, -1, -1),
      new Vec3d(1, -1, 1),
      new Vec3d(-1, -1, 1),
      new Vec3d(-1, -1, -1),
      new Vec3d(1, -1, -1),
      // Walls Help
      new Vec3d(2, -1, 0),
      new Vec3d(-2, -1, 0),
      new Vec3d(0, -1, 2),
      new Vec3d(0, -1, -2),
      // Walls
      new Vec3d(2, 0, 0),
      new Vec3d(2, 0, 1),
      new Vec3d(2, 0, -1),
      new Vec3d(-2, 0, 0),
      new Vec3d(-2, 0, 1),
      new Vec3d(-2, 0, -1),
      new Vec3d(0, 0, 2),
      new Vec3d(1, 0, 2),
      new Vec3d(-1, 0, 2),
      new Vec3d(0, 0, -2),
      new Vec3d(1, 0, -2),
      new Vec3d(-1, 0, -2),
      // Layer 2
      new Vec3d(2, 1, 1),
      new Vec3d(2, 1, -1),
      new Vec3d(-2, 1, 1),
      new Vec3d(-2, 1, -1),
      new Vec3d(1, 1, 2),
      new Vec3d(-1, 1,2),
      new Vec3d(1, 1, -2),
      new Vec3d(-1, 1, -2),
      // layer 3
      new Vec3d(2, 2, 1),
      new Vec3d(2, 2, 0),
      new Vec3d(2, 2, -1),
      new Vec3d(-2, 2, 1),
      new Vec3d(-2, 2, 0),
      new Vec3d(-2, 2, -1),
      new Vec3d(1, 2, 2),
      new Vec3d(0, 2, 2),
      new Vec3d(-1, 2, 2),
      new Vec3d(1, 2, -2),
      new Vec3d(0, 2, -2),
      new Vec3d(-1, 2, -2),
      // Ceiling help
      new Vec3d(2, 3, 0),
      new Vec3d(-2, 3, 0),
      new Vec3d(0, 3, 2),
      new Vec3d(0, 3, -2),
      // Ceiling
      new Vec3d(1, 3, 0),
      new Vec3d(-1, 3, 0),
      new Vec3d(0, 3, 1),
      new Vec3d(0, 3, -1),
      new Vec3d(1, 3, 1),
      new Vec3d(-1, 3, 1),
      new Vec3d(-1, 3, -1),
      new Vec3d(1, 3, -1),
      // Center
      new Vec3d(0, 3, 0)
    };
  }
}
