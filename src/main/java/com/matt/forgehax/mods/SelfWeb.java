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
import com.matt.forgehax.util.math.VectorUtils;
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
public class SelfWeb extends ToggleMod implements PositionRotationManager.MovementUpdateListener {

    InvItem items = null;//That sucks but it's temp not perma
    public enum UsedBlock {
        OBSIDIAN,
        WEB
    }


    public final Setting<Boolean> smart =
            getCommandStub()
                    .builders()
                    .<Boolean>newSettingBuilder()
                    .name("smart")
                    .description("Places if player is trying to enter your hole")
                    .defaultTo(false)
                    .build();

    public final Setting<Boolean> legit =
            getCommandStub()
                    .builders()
                    .<Boolean>newSettingBuilder()
                    .name("legit")
                    .description("Place only in valid locations")
                    .defaultTo(false)
                    .build();


    private SimpleTimer timer = new SimpleTimer(); // The timer
    private boolean warned = false;

    public SelfWeb() {
        super(Category.COMBAT, "SelfWeb", false, "Automatically places a cobweb at your feet");
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
        if (!timer.hasTimeElapsed(200)) return;
        if (MC.player == null || getModManager().get("Freecam").get().isEnabled()) return;

        if(5 > 4){
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
            if (getLocalPlayer().getDistanceSqToCenter(hole) > (.6 * .6)) continue;

            Vec3d pos = new Vec3d(hole.getX() + 0.5, hole.getY(), hole.getZ() + 0.5);
            if (smart.get() && null == getWorld().playerEntities.stream()
                    .filter(p -> !p.equals(getLocalPlayer()))
                    .filter(p -> VectorUtils.distance(pos, p.getPositionVector()) < 3.069420)
                    .findAny()
                    .orElse(null)) continue;

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
