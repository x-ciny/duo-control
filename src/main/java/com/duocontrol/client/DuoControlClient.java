package com.duocontrol.client;

import com.duocontrol.mixin.KeyMappingAccessor;
import com.duocontrol.net.MouseInputPayload;
import com.duocontrol.net.RolePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;

public class DuoControlClient implements ClientModInitializer {

    /** Current role of this client: RolePayload.NONE / BODY / MOUSE. */
    public static int role = RolePayload.NONE;

    // ---- BODY side ----
    private static MouseInputPayload remote = null;
    private static boolean prevAttack, prevUse;
    private static int remoteScroll;

    // ---- MOUSE side ----
    /** Filled by MouseHandlerMixin. */
    public static int pendingScroll;
    private static boolean needsRotationSync;

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(RolePayload.TYPE, (payload, ctx) -> {
            role = payload.role();
            remote = null;
            prevAttack = prevUse = false;
            remoteScroll = 0;
            pendingScroll = 0;
            needsRotationSync = role == RolePayload.MOUSE;
            if (role == RolePayload.NONE) {
                Options o = ctx.client().options;
                o.keyAttack.setDown(false);
                o.keyUse.setDown(false);
            }
        });

        // Body receives the mouse player's input
        ClientPlayNetworking.registerGlobalReceiver(MouseInputPayload.TYPE, (payload, ctx) -> {
            if (role != RolePayload.BODY) return;
            remote = payload;
            remoteScroll += payload.scroll();
        });

        ClientTickEvents.START_CLIENT_TICK.register(DuoControlClient::bodyTick);
        ClientTickEvents.END_CLIENT_TICK.register(DuoControlClient::mouseTick);

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            role = RolePayload.NONE;
            remote = null;
        });
    }

    /** BODY: ignore own mouse buttons, apply the mouse player's input instead. */
    private static void bodyTick(Minecraft mc) {
        if (role != RolePayload.BODY || mc.player == null) return;
        if (mc.gui.screen() != null) { // inventory etc: the body uses its own mouse here
            prevAttack = prevUse = false;
            return;
        }
        Options o = mc.options;

        // throw away the body's physical clicks
        while (o.keyAttack.consumeClick()) { }
        while (o.keyUse.consumeClick()) { }
        while (o.keyPickItem.consumeClick()) { }

        if (remote == null) { // nothing received yet
            o.keyAttack.setDown(false);
            o.keyUse.setDown(false);
            return;
        }

        MouseInputPayload in = remote;
        o.keyAttack.setDown(in.attack());
        o.keyUse.setDown(in.use());
        if (in.attack() && !prevAttack) addClick(o.keyAttack);
        if (in.use() && !prevUse) addClick(o.keyUse);
        prevAttack = in.attack();
        prevUse = in.use();

        mc.player.setYRot(in.yaw());
        mc.player.setXRot(Mth.clamp(in.pitch(), -90f, 90f));

        if (remoteScroll != 0) {
            Inventory inv = mc.player.getInventory();
            int slot = Math.floorMod(inv.getSelectedSlot() - remoteScroll, 9);
            inv.setSelectedSlot(slot);
            remoteScroll = 0;
        }
    }

    /** MOUSE: read what the mouse is doing and send it. */
    private static void mouseTick(Minecraft mc) {
        if (role != RolePayload.MOUSE || mc.player == null || mc.getConnection() == null) return;

        // On start, copy the body's view direction so the camera doesn't jump.
        if (needsRotationSync) {
            Entity cam = mc.getCameraEntity();
            if (cam != null && cam != mc.player) {
                mc.player.setYRot(cam.getYRot());
                mc.player.setXRot(cam.getXRot());
                needsRotationSync = false;
            }
            return;
        }

        boolean inGame = mc.gui.screen() == null;
        boolean attack = inGame && mc.options.keyAttack.isDown();
        boolean use = inGame && mc.options.keyUse.isDown();
        int scroll = pendingScroll;
        pendingScroll = 0;

        ClientPlayNetworking.send(new MouseInputPayload(
                mc.player.getYRot(), mc.player.getXRot(), attack, use, scroll));
    }

    private static void addClick(KeyMapping key) {
        KeyMappingAccessor acc = (KeyMappingAccessor) key;
        acc.duocontrol$setClickCount(acc.duocontrol$getClickCount() + 1);
    }
}
