package me.clip.placeholderapi.expansion;

import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class PlaceholderExpansion {
    public abstract String getIdentifier();
    public abstract String getAuthor();
    public abstract String getVersion();
    public boolean persist() { return false; }
    public boolean canRegister() { return false; }
    public String onRequest(@Nullable OfflinePlayer player, @NotNull String identifier) { return null; }
    public void register() {}
}
