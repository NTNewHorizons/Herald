package com.ntnh.herald.compat;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;

import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import cpw.mods.fml.common.Loader;
import github.scarsz.discordsrv.hooks.vanish.VanishHook;

/**
 * Reports players hidden with ServerUtilities' {@code /vanish} as vanished so DiscordSRV suppresses their join/leave,
 * death and advancement messages and omits them from the Discord player list.
 * <p>
 * ServerUtilities persists the flag as {@code PlayerPersisted.vanish} in the player's Forge entity data (see
 * {@code serverutils.lib.util.ServerUtils#isVanished}). Reading the NBT directly avoids a compile-time or reflective
 * dependency on ServerUtilities internals, and the tag is already loaded when {@code PlayerLoggedInEvent} fires.
 */
@SuppressWarnings("deprecation")
public class ServerUtilitiesVanishHook implements VanishHook {

    public static final String MOD_ID = "serverutilities";
    private static final String VANISH_TAG = "vanish";

    public static boolean isAvailable() {
        return Loader.isModLoaded(MOD_ID);
    }

    @Override
    public boolean isVanished(Player player) {
        if (!(player instanceof CraftPlayer)) return false;
        EntityPlayerMP handle = ((CraftPlayer) player).getHandle();
        if (handle == null) return false;
        NBTTagCompound entityData = handle.getEntityData();
        if (entityData == null || !entityData.hasKey(EntityPlayer.PERSISTED_NBT_TAG)) return false;
        return entityData.getCompoundTag(EntityPlayer.PERSISTED_NBT_TAG)
            .getBoolean(VANISH_TAG);
    }

    @Override
    public Plugin getPlugin() {
        return null;
    }

    @Override
    public boolean isEnabled() {
        return isAvailable();
    }
}
