package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getNetworkManager;
import static com.matt.forgehax.Helper.getPlayerController;
import static com.matt.forgehax.Helper.getWorld;

import com.matt.forgehax.asm.reflection.FastReflection.Fields;
import com.matt.forgehax.Helper;
import com.matt.forgehax.asm.events.PacketEvent;
import com.matt.forgehax.mods.managers.PositionRotationManager;
import com.matt.forgehax.mods.managers.PositionRotationManager.RotationState.Local;
import com.matt.forgehax.mods.services.HotbarSelectionService.ResetFunction;
import com.matt.forgehax.util.BlockHelper;
import com.matt.forgehax.util.BlockHelper.BlockTraceInfo;
import com.matt.forgehax.util.PacketHelper;
import com.matt.forgehax.util.Utils;
import com.matt.forgehax.util.common.PriorityEnum;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.entity.LocalPlayerInventory;
import com.matt.forgehax.util.entity.LocalPlayerInventory.InvItem;
import com.matt.forgehax.util.key.Bindings;
import com.matt.forgehax.util.entity.LocalPlayerUtils;
import com.matt.forgehax.util.math.Angle;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.command.Setting;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketPlayer;
import net.minecraft.network.play.client.CPacketEntityAction.Action;
import net.minecraft.network.play.server.SPacketBlockChange;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@RegisterMod
public class Scaffold extends ToggleMod implements PositionRotationManager.MovementUpdateListener {

  public final Setting<Boolean> ascend =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("ascend-server")
          .description("Go up very fast on server actions")
          .defaultTo(false)
          .build();

  public final Setting<Float> up_speed =
      getCommandStub()
          .builders()
          .<Float>newSettingBuilder()
          .name("up-speed")
          .description("Speed at which you ascend")
          .min(0F)
          .max(1F)
          .defaultTo(0.42F)
          .build();

  public final Setting<Integer> cooldown =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("cooldown")
          .description("Ticks to wait idle before placing next block")
          .min(0)
          .max(100)
          .defaultTo(0)
          .build();
  
  public final Setting<Boolean> legit =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("legit")
          .description("Don't place blocks you could not see")
          .defaultTo(false)
          .build();

  public final Setting<Boolean> safe =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("safe")
          .description("Prevent Y from decreasing")
          .defaultTo(false)
          .build();

  public final Setting<Boolean> land_spoof =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("land-spoof")
          .description("Fake landing on the block")
          .defaultTo(false)
          .build();


  public final Setting<Double> jitter_spoof =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("jitter-spoof")
          .description("Change slightly x and z while ascending")
          .min(0D)
          .max(1D)
          .defaultTo(0.D)
          .build();
  
  private static final EnumSet<EnumFacing> NEIGHBORS =
      EnumSet.of(EnumFacing.NORTH, EnumFacing.EAST, EnumFacing.SOUTH, EnumFacing.WEST);
  
  private int tickCount = 0;
  private boolean placing = false;
  private BlockPos last = null;
  private boolean placed = false;
  private boolean warned = false;
  private Angle looking = null;
  private Random r = new Random();
  
  public Scaffold() {
    super(Category.MOVEMENT, "Scaffold", false, "Place blocks under yourself");
  }
  
  @Override
  protected void onEnabled() {
    PositionRotationManager.getManager().register(this, PriorityEnum.HIGHEST);
    last = getLocalPlayer().getPosition();
  }
  
  @Override
  protected void onDisabled() {
    PositionRotationManager.getManager().unregister(this);
  }
  
  @SubscribeEvent
  public void onPacketInbound(PacketEvent.Incoming.Pre event) {
    if (getLocalPlayer() == null) return;
    if (ascend.get() && Bindings.jump.isPressed()) {
      if (event.getPacket() instanceof SPacketBlockChange) {
        SPacketBlockChange packet = event.getPacket();
        if (packet.getBlockPosition().equals(last) && placed
            && !packet.getBlockState().getBlock().equals(Blocks.AIR)) {
          if (land_spoof.get()) {
          // Send that we landed on it
            double js = jitter_spoof.get();
            double r1 = -js + (2 * js) * r.nextDouble();
            double r2 = -js + (2 * js) * r.nextDouble();
            getNetworkManager().sendPacket(
              new CPacketPlayer.Position(getLocalPlayer().posX + r1, last.getY() + 1D, getLocalPlayer().posZ + r2, true));
            getLocalPlayer().setPosition(getLocalPlayer().posX, last.getY() + 1D, getLocalPlayer().posZ);
            getLocalPlayer().onGround = true;
          }
          
          getLocalPlayer().motionY = up_speed.get();
          placed = false;
        }
      }
    }
  }

  @Override
  public void onLocalPlayerMovementUpdate(Local state) {
    if (safe.get() && !Bindings.sneak.isPressed()
        && !Bindings.jump.isPressed()) {
      getLocalPlayer().motionY = Math.max(0.D, getLocalPlayer().motionY);
    }
    if (placing && looking != null) state.setServerAngles(looking);
    if (placing && tickCount < cooldown.get()) {
      ++tickCount;
	  return;
    }
    placing = false;
    tickCount = 0;
    
    if (LocalPlayerUtils.isSneaking()) return;
    
    BlockPos below = new BlockPos(getLocalPlayer()).down();
    
    if (!getWorld().getBlockState(below).getMaterial().isReplaceable()) {
      return;
    }

    InvItem items =
        LocalPlayerInventory.getHotbarInventory()
            .stream()
            .filter(InvItem::nonNull)
            .filter(item -> item.getItem() instanceof ItemBlock)
            .filter(item -> Block.getBlockFromItem(item.getItem()).getDefaultState().isFullBlock())
            .max(Comparator.comparingInt(LocalPlayerInventory::getHotbarDistance))
            .orElse(InvItem.EMPTY);
    
    if (items.isNull()) {
      if (!warned)
        Helper.printError("Out of blocks");
      warned = true;
      return;
    }
    warned = false;
    
    final Vec3d eyes = EntityUtils.getEyePos(getLocalPlayer());
    final Vec3d dir = LocalPlayerUtils.getViewAngles().getDirectionVector();
    
    BlockTraceInfo trace =
        Optional.ofNullable(BlockHelper.getPlaceableBlockSideTrace(eyes, dir, below, legit.get()))
            .filter(tr -> tr.isPlaceable(items))
            .orElseGet(
                () ->
                    NEIGHBORS
                        .stream()
                        .map(below::offset)
                        .filter(BlockHelper::isBlockReplaceable)
                        .map(bp -> BlockHelper.getPlaceableBlockSideTrace(eyes, dir, bp, legit.get()))
                        .filter(Objects::nonNull)
                        .filter(tr -> tr.isPlaceable(items))
                        .max(Comparator.comparing(BlockTraceInfo::isSneakRequired))
                        .orElse(null));
    
    if (trace == null) {
      return;
    }
    
    Vec3d hit = trace.getHitVec();
    looking = Utils.getLookAtAngles(hit);
    state.setServerAngles(looking);
    
    final BlockTraceInfo tr = trace;
    ResetFunction func = LocalPlayerInventory.setSelected(items);
    
    boolean sneak = tr.isSneakRequired() && !LocalPlayerUtils.isSneaking() && !ascend.get();
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

    if (!tr.getPos().offset(tr.getOppositeSide()).equals(last)) {
      placed = true;
    }
    last = tr.getPos().offset(tr.getOppositeSide());
    
    func.revert();
    
    Fields.Minecraft_rightClickDelayTimer.set(MC, 4);
    placing = true;
    tickCount = 0;
  }
}
