package com.duocontrol.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** S2C: tells a client which role it has. 0 = none, 1 = body (keyboard), 2 = mouse. */
public record RolePayload(int role) implements CustomPacketPayload {

    public static final int NONE = 0;
    public static final int BODY = 1;
    public static final int MOUSE = 2;

    public static final CustomPacketPayload.Type<RolePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("duocontrol", "role"));

    public static final StreamCodec<FriendlyByteBuf, RolePayload> CODEC = StreamCodec.of(
            (buf, v) -> buf.writeByte(v.role),
            buf -> new RolePayload(buf.readByte()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
