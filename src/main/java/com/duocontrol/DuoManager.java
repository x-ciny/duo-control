package com.duocontrol;

import com.duocontrol.net.RolePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.*;

/** Server-side bookkeeping for body/mouse pairs. */
public final class DuoManager {
    private DuoManager() {}

    private static final Map<UUID, UUID> BODY_TO_MOUSE = new HashMap<>();
    private static final Map<UUID, UUID> MOUSE_TO_BODY = new HashMap<>();
    /** invitee -> inviter. The inviter becomes the BODY (keyboard), the invitee the MOUSE. */
    private static final Map<UUID, UUID> INVITES = new HashMap<>();
    private static final Map<UUID, GameType> PREVIOUS_MODE = new HashMap<>();

    public static boolean isPaired(UUID id) {
        return BODY_TO_MOUSE.containsKey(id) || MOUSE_TO_BODY.containsKey(id);
    }

    /** The body for a given mouse player, or null. */
    public static UUID bodyOf(UUID mouse) {
        return MOUSE_TO_BODY.get(mouse);
    }

    public static void invite(ServerPlayer inviter, ServerPlayer invitee) {
        INVITES.put(invitee.getUUID(), inviter.getUUID());
    }

    public static UUID takeInvite(UUID invitee) {
        return INVITES.remove(invitee);
    }

    public static void pair(MinecraftServer server, ServerPlayer body, ServerPlayer mouse) {
        BODY_TO_MOUSE.put(body.getUUID(), mouse.getUUID());
        MOUSE_TO_BODY.put(mouse.getUUID(), body.getUUID());
        INVITES.remove(body.getUUID());
        INVITES.remove(mouse.getUUID());

        PREVIOUS_MODE.put(mouse.getUUID(), mouse.gameMode.getGameModeForPlayer());
        mouse.setGameMode(GameType.SPECTATOR);
        mouse.setCamera(body);

        ServerPlayNetworking.send(body, new RolePayload(RolePayload.BODY));
        ServerPlayNetworking.send(mouse, new RolePayload(RolePayload.MOUSE));
    }

    /** Breaks the pair that contains this player (either role). Returns false if there was none. */
    public static boolean unpair(MinecraftServer server, UUID anyMember) {
        UUID bodyId;
        UUID mouseId;
        if (BODY_TO_MOUSE.containsKey(anyMember)) {
            bodyId = anyMember;
            mouseId = BODY_TO_MOUSE.get(anyMember);
        } else if (MOUSE_TO_BODY.containsKey(anyMember)) {
            mouseId = anyMember;
            bodyId = MOUSE_TO_BODY.get(anyMember);
        } else {
            return false;
        }
        BODY_TO_MOUSE.remove(bodyId);
        MOUSE_TO_BODY.remove(mouseId);

        ServerPlayer body = server.getPlayerList().getPlayer(bodyId);
        ServerPlayer mouse = server.getPlayerList().getPlayer(mouseId);
        GameType prev = PREVIOUS_MODE.remove(mouseId);

        if (mouse != null) {
            mouse.setCamera(mouse);
            mouse.setGameMode(prev != null ? prev : GameType.SURVIVAL);
            ServerPlayNetworking.send(mouse, new RolePayload(RolePayload.NONE));
        }
        if (body != null) {
            ServerPlayNetworking.send(body, new RolePayload(RolePayload.NONE));
        }
        return true;
    }

    public static void swap(MinecraftServer server, UUID anyMember) {
        UUID bodyId = BODY_TO_MOUSE.containsKey(anyMember) ? anyMember : MOUSE_TO_BODY.get(anyMember);
        if (bodyId == null) return;
        UUID mouseId = BODY_TO_MOUSE.get(bodyId);
        unpair(server, bodyId);
        ServerPlayer oldBody = server.getPlayerList().getPlayer(bodyId);
        ServerPlayer oldMouse = server.getPlayerList().getPlayer(mouseId);
        if (oldBody != null && oldMouse != null) {
            pair(server, oldMouse, oldBody);
        }
    }

    public static void unpairAll(MinecraftServer server) {
        for (UUID body : new ArrayList<>(BODY_TO_MOUSE.keySet())) {
            unpair(server, body);
        }
        INVITES.clear();
    }

    /** Called every server tick: keeps mice glued to their bodies. */
    public static void tick(MinecraftServer server) {
        for (Map.Entry<UUID, UUID> e : new ArrayList<>(BODY_TO_MOUSE.entrySet())) {
            ServerPlayer body = server.getPlayerList().getPlayer(e.getKey());
            ServerPlayer mouse = server.getPlayerList().getPlayer(e.getValue());
            if (body == null || mouse == null) {
                unpair(server, e.getKey());
                continue;
            }
            if (!body.isAlive()) continue;
            if (!mouse.isSpectator()) mouse.setGameMode(GameType.SPECTATOR);
            if (mouse.getCamera() != body) mouse.setCamera(body);
        }
    }
}
