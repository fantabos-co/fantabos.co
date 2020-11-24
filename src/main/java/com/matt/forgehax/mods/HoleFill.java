package com.matt.forgehax.mods;

import static com.matt.forgehax.Helper.getLocalPlayer;
import static com.matt.forgehax.Helper.getNetworkManager;
import static com.matt.forgehax.Helper.getPlayerController;
import static com.matt.forgehax.Helper.getWorld;
import static com.matt.forgehax.Helper.getModManager;

import com.matt.forgehax.Helper;
import com.matt.forgehax.mods.managers.PositionRotationManager;
import com.matt.forgehax.mods.managers.PositionRotationManager.RotationState.Local;
import com.matt.forgehax.mods.services.HoleService;
import com.matt.forgehax.mods.services.HotbarSelectionService.ResetFunction;
import com.matt.forgehax.util.BlockHelper;
import com.matt.forgehax.util.BlockHelper.BlockTraceInfo;
import com.matt.forgehax.util.PacketHelper;
import com.matt.forgehax.util.SimpleTimer;
import com.matt.forgehax.util.Utils;
import com.matt.forgehax.util.common.PriorityEnum;
import com.matt.forgehax.util.entity.EntityUtils;
import com.matt.forgehax.util.entity.LocalPlayerInventory;
import com.matt.forgehax.util.entity.LocalPlayerInventory.InvItem;
import com.matt.forgehax.util.entity.LocalPlayerUtils;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import com.matt.forgehax.util.command.Setting;
import java.util.Comparator;
import java.util.Optional;
import net.minecraft.block.BlockObsidian;
import net.minecraft.block.BlockWeb;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.network.play.client.CPacketAnimation;
import net.minecraft.network.play.client.CPacketEntityAction;
import net.minecraft.network.play.client.CPacketEntityAction.Action;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@RegisterMod
public class HoleFill extends ToggleMod implements PositionRotationManager.MovementUpdateListener {

  InvItem items = null;//That sucks but it's temp not perma
  public enum UsedBlock {
    OBSIDIAN,
    WEB
  }

  public final Setting<Integer> cooldown =
      getCommandStub()
          .builders()
          .<Integer>newSettingBuilder()
          .name("cooldown")
          .description("ms to wait before filling again")
          .min(0)
          .max(10000)
          .defaultTo(200)
          .build();

  public final Setting<Double> range =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("range")
          .description("Range to fill holes")
          .min(0D)
          .max(15D)
          .defaultTo(4D)
          .build();

  public final Setting<Double> player_threshold =
      getCommandStub()
          .builders()
          .<Double>newSettingBuilder()
          .name("player-threshold")
          .description("Max distance from enemy to hole to trigger fill")
          .min(0D)
          .max(10D)
          .defaultTo(2D)
          .build();
  
  public final Setting<Boolean> legit =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("legit")
          .description("Place only in valid locations")
          .defaultTo(false)
          .build();

  public final Setting<Boolean> stop_on_eating =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("eating-stop")
          .description("Stop crystaling when eating")
          .defaultTo(true)
          .build();

  public final Setting<Boolean> stop_on_mining =
      getCommandStub()
          .builders()
          .<Boolean>newSettingBuilder()
          .name("mining-stop")
          .description("Stop crystaling when mining")
          .defaultTo(true)
          .build();

  public final Setting<UsedBlock> mode =
          getCommandStub()
              .builders()
              .<UsedBlock>newSettingEnumBuilder()
              .name("mode")
              .description("The block used to holefill [Obsidian/Web]")
              .defaultTo(UsedBlock.OBSIDIAN)
              .build();
  
  private SimpleTimer timer = new SimpleTimer(); // The timer
  private boolean warned = false;
  
  public HoleFill() {
    super(Category.COMBAT, "HoleFill", false, "Fill holes around you");
  }
  
  @Override
  protected void onEnabled() {
    PositionRotationManager.getManager().register(this, PriorityEnum.HIGH);
  }
  
  @Override
  protected void onDisabled() {
    PositionRotationManager.getManager().unregister(this);
  }
  

  @Override
  public void onLocalPlayerMovementUpdate(Local state) {
    if (!timer.hasTimeElapsed(cooldown.get())) return;
    if (MC.player == null || getModManager().get("Freecam").get().isEnabled()) return;

    // Don't interfere with chorush, gaps, potions and xp bottles
    if (stop_on_eating.get() && MC.gameSettings.keyBindUseItem.isKeyDown() && 
        (LocalPlayerInventory.getSelected().getItem().equals(Items.GOLDEN_APPLE) ||
        LocalPlayerInventory.getSelected().getItem().equals(Items.CHORUS_FRUIT) ||
        LocalPlayerInventory.getSelected().getItem().equals(Items.POTIONITEM) ||
        LocalPlayerInventory.getSelected().getItem().equals(Items.EXPERIENCE_BOTTLE)))
      return;

    // Don't intergere with mining (with top gear)
    if (stop_on_mining.get() && MC.gameSettings.keyBindAttack.isKeyDown() && 
        (LocalPlayerInventory.getSelected().getItem().equals(Items.DIAMOND_PICKAXE)))
      return;



    // Search for Obsidian
    if(mode.get()==UsedBlock.OBSIDIAN) {
     items = LocalPlayerInventory.getHotbarInventory()
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
   }

   if(mode.get()==UsedBlock.WEB){
    // Search for Cowebs
    items = LocalPlayerInventory.getHotbarInventory()
      .stream()
      .filter(InvItem::nonNull)
      .filter(held_item -> held_item.getItem() instanceof ItemBlock &&
                           ((ItemBlock) held_item.getItem()).getBlock() instanceof BlockWeb)
      .max(Comparator.comparingInt(LocalPlayerInventory::getHotbarDistance))
      .orElse(InvItem.EMPTY);
    
    if (items == null || items.equals(InvItem.EMPTY)) {
      if (!warned) {
        Helper.printError("Out of Webs");
        warned = true;
      }
      return;
    }
    warned = false;
  }

    final Vec3d eyes = EntityUtils.getEyePos(getLocalPlayer());
    final Vec3d dir = LocalPlayerUtils.getViewAngles().getDirectionVector();

    for (BlockPos hole : HoleService.getAllHoles()) {
      if (getLocalPlayer().getDistanceSqToCenter(hole) > (range.get() * range.get())) continue;
    
      BlockTraceInfo trace =
      Optional.ofNullable(BlockHelper.getPlaceableBlockSideTrace(eyes, dir, hole, legit.get()))
          .filter(tr -> tr.isPlaceable(items))
          .orElse(null);

      if (trace == null) {
          continue;
      }
      Vec3d hit = trace.getHitVec();
      state.setServerAngles(Utils.getLookAtAngles(hit));
      if (legit.get()) {
        state.setClientAngles(Utils.getLookAtAngles(hit));
      }
      
      final BlockTraceInfo tr = trace;
      ResetFunction func = LocalPlayerInventory.setSelected(items);

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
      func.revert();
      break;
    }
  }
}
