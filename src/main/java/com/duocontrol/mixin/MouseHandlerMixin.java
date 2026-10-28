package com.duocontrol.mixin;

import com.duocontrol.client.DuoControlClient;
import com.duocontrol.net.RolePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Scroll wheel: the BODY can't use it, the MOUSE sends it over as hotbar scrolling. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
    @Inject(method = "onScroll(JDD)V", at = @At("HEAD"), cancellable = true, require = 0)
    private void duocontrol$scroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
        int role = DuoControlClient.role;
        if (role == RolePayload.NONE) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.gui.screen() != null) return; // menus scroll normally
        if (role == RolePayload.MOUSE) {
            DuoControlClient.pendingScroll += (int) Math.signum(yOffset);
        }
        ci.cancel();
    }
}
