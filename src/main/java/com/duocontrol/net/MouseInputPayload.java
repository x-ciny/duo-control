package com.duocontrol.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Mouse player's input. Sent C2S by the mouse player, relayed S2C to the body player.
 */
public record MouseInputPayload(float yaw, float pitch, boolean attack, boolean use, int scroll)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<MouseInputPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("duocontrol", "mouse_input"));

    public static final StreamCodec<FriendlyByteBuf, MouseInputPayload> CODEC = StreamCodec.of(
            (buf, v) -> {
                buf.writeFloat(v.yaw);
                buf.writeFloat(v.pitch);
                buf.writeBoolean(v.attack);
                buf.writeBoolean(v.use);
                buf.writeVarInt(v.scroll);
            },
            buf -> new MouseInputPayload(
                    buf.readFloat(), buf.readFloat(),
                    buf.readBoolean(), buf.readBoolean(), buf.readVarInt()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
