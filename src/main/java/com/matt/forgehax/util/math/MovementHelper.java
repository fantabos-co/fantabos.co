package com.matt.forgehax.util.math;

import com.matt.forgehax.Globals;

import net.minecraft.util.math.Vec3d;

import static com.matt.forgehax.Helper.getLocalPlayer;

public class MovementHelper implements Globals {
    public static Vec3d directionSpeed(double speed) {
        float forward = getLocalPlayer().movementInput.moveForward;
        float side = getLocalPlayer().movementInput.moveStrafe;
        //TODO: Having Y as well would be interesting to be sure
        float yaw = getLocalPlayer().prevRotationYaw + (getLocalPlayer().rotationYaw - getLocalPlayer().prevRotationYaw) * MC.getRenderPartialTicks();
        if (forward != 0) {
            if (side > 0) yaw += (forward > 0 ? -45 : 45);
            else if (side < 0) yaw += (forward > 0 ? 45 : -45);
            side = 0;
            if (forward > 0) forward = 1;
            else if (forward < 0) forward = -1;
        }
        final double sin = Math.sin(Math.toRadians(yaw + 90));
        final double cos = Math.cos(Math.toRadians(yaw + 90));
        final double posX = (forward * speed * cos + side * speed * sin);
        final double posZ = (forward * speed * sin - side * speed * cos);
        return new Vec3d(posX, 0, posZ);
    }
}
