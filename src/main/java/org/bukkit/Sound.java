package org.bukkit;

public enum Sound {

    BLOCK_NOTE_BLOCK_PLING("block.note_block.pling", "note.pling"),
    ENTITY_PLAYER_LEVELUP("entity.player.levelup", "random.levelup");

    private final String key;
    private final String legacyKey;

    Sound(String key, String legacyKey) {
        this.key = key;
        this.legacyKey = legacyKey;
    }

    public String getKey() {
        return key;
    }

    public String getLegacyKey() {
        return legacyKey;
    }
}
