package com.duocontrol;

import com.duocontrol.net.MouseInputPayload;
import com.duocontrol.net.RolePayload;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;

import java.util.UUID;

public class DuoControl implements ModInitializer {
    public static final String MOD_ID = "duocontrol";

    @Override
    public void onInitialize() {
        // Packets
        PayloadTypeRegistry.serverboundPlay().register(MouseInputPayload.TYPE, MouseInputPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(MouseInputPayload.TYPE, MouseInputPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(RolePayload.TYPE, RolePayload.CODEC);

        // Mouse player -> server -> body player
        ServerPlayNetworking.registerGlobalReceiver(MouseInputPayload.TYPE, (payload, ctx) -> {
            ServerPlayer sender = ctx.player();
            UUID bodyId = DuoManager.bodyOf(sender.getUUID());
            if (bodyId == null) return; // not a mouse player, ignore
            ServerPlayer body = ctx.server().getPlayerList().getPlayer(bodyId);
            if (body == null) return;

            float yaw = Float.isFinite(payload.yaw()) ? payload.yaw() : 0f;
            float pitch = Float.isFinite(payload.pitch()) ? Mth.clamp(payload.pitch(), -90f, 90f) : 0f;
            int scroll = Mth.clamp(payload.scroll(), -9, 9);
            ServerPlayNetworking.send(body,
                    new MouseInputPayload(yaw, pitch, payload.attack(), payload.use(), scroll));
        });

        // Housekeeping
        ServerTickEvents.END_SERVER_TICK.register(DuoManager::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                DuoManager.unpair(server, handler.getPlayer().getUUID()));
        ServerLifecycleEvents.SERVER_STOPPING.register(DuoManager::unpairAll);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                registerCommands(dispatcher));
    }

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("duo")
                .then(Commands.literal("invite")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(DuoControl::invite)))
                .then(Commands.literal("accept").executes(DuoControl::accept))
                .then(Commands.literal("swap").executes(DuoControl::swap))
                .then(Commands.literal("stop").executes(DuoControl::stop)));
    }

    private static int invite(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer me = ctx.getSource().getPlayerOrException();
        ServerPlayer other = EntityArgument.getPlayer(ctx, "player");
        if (me == other) {
            ctx.getSource().sendFailure(Component.literal("You can't pair with yourself."));
            return 0;
        }
        if (DuoManager.isPaired(me.getUUID()) || DuoManager.isPaired(other.getUUID())) {
            ctx.getSource().sendFailure(Component.literal("One of you is already in a duo. Use /duo stop first."));
            return 0;
        }
        DuoManager.invite(me, other);
        ctx.getSource().sendSuccess(() -> Component.literal(
                "Invite sent to " + other.getName().getString() + ". You will play the KEYBOARD (body)."), false);
        other.sendSystemMessage(Component.literal(
                me.getName().getString() + " wants to share a body with you. You would play the MOUSE. Type /duo accept"));
        return 1;
    }

    private static int accept(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer me = ctx.getSource().getPlayerOrException();
        UUID inviterId = DuoManager.takeInvite(me.getUUID());
        ServerPlayer inviter = inviterId == null ? null : ctx.getSource().getServer().getPlayerList().getPlayer(inviterId);
        if (inviter == null || DuoManager.isPaired(inviter.getUUID()) || DuoManager.isPaired(me.getUUID())) {
            ctx.getSource().sendFailure(Component.literal("No valid invite."));
            return 0;
        }
        DuoManager.pair(ctx.getSource().getServer(), inviter, me);
        Component msg = Component.literal("Duo started! " + inviter.getName().getString()
                + " = KEYBOARD (WASD, jump, sneak, inventory, number keys), "
                + me.getName().getString() + " = MOUSE (look, attack, use, scroll hotbar).");
        inviter.sendSystemMessage(msg);
        me.sendSystemMessage(msg);
        return 1;
    }

    private static int swap(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer me = ctx.getSource().getPlayerOrException();
        if (!DuoManager.isPaired(me.getUUID())) {
            ctx.getSource().sendFailure(Component.literal("You are not in a duo."));
            return 0;
        }
        DuoManager.swap(ctx.getSource().getServer(), me.getUUID());
        ctx.getSource().sendSuccess(() -> Component.literal("Roles swapped!"), true);
        return 1;
    }

    private static int stop(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer me = ctx.getSource().getPlayerOrException();
        if (!DuoManager.unpair(ctx.getSource().getServer(), me.getUUID())) {
            ctx.getSource().sendFailure(Component.literal("You are not in a duo."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Duo ended."), true);
        return 1;
    }
}
