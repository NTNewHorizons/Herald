package org.bukkit;

public enum Sound {

    BLOCK_NOTE_BLOCK_PLING("block.note_block.pling"),
    ENTITY_PLAYER_LEVELUP("entity.player.levelup");

    private final String key;

    Sound(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }
}
