package org.bukkit.advancement;

import org.bukkit.NamespacedKey;

public interface Advancement {

    NamespacedKey getKey();

    AdvancementDisplay getDisplay();
}
