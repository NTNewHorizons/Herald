package com.ntnh.herald.mixin;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.NetHandlerLoginServer;
import net.minecraft.util.IChatComponent;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.authlib.GameProfile;
import com.ntnh.herald.HeraldDiscordSRV;
import com.ntnh.herald.auth.LoginDecision;

/** Holds a login in the vanilla negotiation state until DiscordSRV and Herald approve it. */
@Mixin(NetHandlerLoginServer.class)
public abstract class MixinNetHandlerLoginServer {

    @Shadow
    private MinecraftServer field_147327_f;

    @Shadow
    public NetworkManager field_147333_a;

    @Shadow
    private GameProfile field_147337_i;

    @Unique
    private Future<LoginDecision> herald$loginDecision;

    @Unique
    private boolean herald$vanillaAdmissionChecked;

    @Unique
    private boolean herald$authenticationPassed;

    @Unique
    private boolean herald$authenticationRejected;

    @Shadow
    public abstract void func_147322_a(String reason);

    @Inject(
        method = "func_147326_c",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/management/ServerConfigurationManager;allowUserToConnect(Ljava/net/SocketAddress;Lcom/mojang/authlib/GameProfile;)Ljava/lang/String;"),
        cancellable = true,
        require = 1)
    private void herald$authenticateBeforeAdmission(CallbackInfo callback) {
        if (!field_147333_a.isChannelOpen()) {
            herald$cancelAuthentication();
            callback.cancel();
            return;
        }

        if (herald$authenticationPassed) return;
        if (herald$authenticationRejected) {
            callback.cancel();
            return;
        }

        if (!herald$vanillaAdmissionChecked) {
            herald$vanillaAdmissionChecked = true;
            String vanillaRejection = field_147327_f.getConfigurationManager()
                .allowUserToConnect(field_147333_a.getSocketAddress(), field_147337_i);
            if (vanillaRejection != null) return;
        }

        if (herald$loginDecision == null) {
            try {
                herald$loginDecision = HeraldDiscordSRV.getInstance()
                    .beginPreAdmissionLogin(field_147337_i, field_147333_a.getSocketAddress());
            } catch (RuntimeException e) {
                com.ntnh.herald.Herald.LOG.error("Could not start Herald pre-admission authentication", e);
                herald$reject("Herald could not verify this login. Please contact a server administrator.");
            }
            callback.cancel();
            return;
        }

        if (!herald$loginDecision.isDone()) {
            callback.cancel();
            return;
        }

        try {
            LoginDecision decision = herald$loginDecision.get();
            if (decision.isAllowed()) {
                herald$authenticationPassed = true;
                return;
            }
            herald$reject(decision.getKickMessage());
        } catch (InterruptedException e) {
            Thread.currentThread()
                .interrupt();
            herald$reject("Herald login authentication was interrupted. Please try again.");
        } catch (ExecutionException e) {
            com.ntnh.herald.Herald.LOG.error("Herald pre-admission login authentication failed", e.getCause());
            herald$reject("Herald could not verify this login. Please contact a server administrator.");
        } catch (CancellationException ignored) {
            herald$authenticationRejected = true;
            if (field_147333_a.isChannelOpen()) {
                func_147322_a("Herald authentication was cancelled. Please try again.");
            }
        }
        callback.cancel();
    }

    @Inject(method = "onDisconnect", at = @At("HEAD"), require = 1)
    private void herald$cancelAuthenticationOnDisconnect(IChatComponent reason, CallbackInfo callback) {
        herald$cancelAuthentication();
    }

    @Unique
    private void herald$cancelAuthentication() {
        Future<LoginDecision> decision = herald$loginDecision;
        if (decision != null && !decision.isDone()) {
            decision.cancel(true);
        }
        herald$authenticationRejected = true;
    }

    @Unique
    private void herald$reject(String message) {
        herald$authenticationRejected = true;
        func_147322_a(message == null ? "" : message);
    }
}
