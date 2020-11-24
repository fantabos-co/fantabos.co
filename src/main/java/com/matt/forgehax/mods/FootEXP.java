package com.matt.forgehax.mods;

import com.matt.forgehax.mods.managers.PositionRotationManager;
import com.matt.forgehax.mods.managers.PositionRotationManager.RotationState.Local;
import com.matt.forgehax.util.common.PriorityEnum;
import com.matt.forgehax.util.mod.Category;
import com.matt.forgehax.util.mod.ToggleMod;
import com.matt.forgehax.util.mod.loader.RegisterMod;
import net.minecraft.item.ItemExpBottle;

import static com.matt.forgehax.Helper.getLocalPlayer;

@RegisterMod
public class FootEXP extends ToggleMod implements PositionRotationManager.MovementUpdateListener {

    public FootEXP() {
        super(Category.COMBAT, "FootEXP", false, "Drops EXP bottles right at your feet.");
    }

    @Override
    protected void onEnabled() {
      PositionRotationManager.getManager().register(this, PriorityEnum.LOW);
    }
    
    @Override
    protected void onDisabled() {
      PositionRotationManager.getManager().unregister(this);
    }

    @Override
    public void onLocalPlayerMovementUpdate(Local state) {
        if (getLocalPlayer() != null && getLocalPlayer().getHeldItemMainhand().getItem() instanceof ItemExpBottle) {
            state.setServerAngles(90.0F, getLocalPlayer().rotationYaw);;
        }
    }
}