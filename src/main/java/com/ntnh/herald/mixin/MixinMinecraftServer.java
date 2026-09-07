package com.ntnh.herald.mixin;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.IChatComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.ntnh.herald.MinecraftServerCommandOutputForwarder;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer {

    @Inject(method = "addChatMessage", at = @At("HEAD"), cancellable = true, require = 1)
    private void herald$forwardCommandOutput(IChatComponent message, CallbackInfo callback) {
        if (MinecraftServerCommandOutputForwarder.forward(message)) callback.cancel();
    }
}
