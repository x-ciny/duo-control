package com.duocontrol.mixin;

import com.duocontrol.client.DuoControlClient;
import com.duocontrol.net.RolePayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The BODY player's own mouse movement must not turn the camera. */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "turn(DD)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void duocontrol$blockBodyLook(double yaw, double pitch, CallbackInfo ci) {
        if (DuoControlClient.role == RolePayload.BODY && (Object) this instanceof LocalPlayer) {
            ci.cancel();
        }
    }
}
