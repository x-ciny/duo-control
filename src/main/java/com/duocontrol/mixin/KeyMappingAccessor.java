package com.duocontrol.mixin;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KeyMapping.class)
public interface KeyMappingAccessor {
    @Accessor("clickCount")
    int duocontrol$getClickCount();

    @Accessor("clickCount")
    void duocontrol$setClickCount(int value);
}
