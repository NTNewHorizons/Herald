package org.bukkit.command;

import java.util.ArrayList;
import java.util.List;

public class Command {

    private final String name;
    private final List<String> aliases = new ArrayList<>();
    private String description = "";
    private String usage = "";
    private String label;

    public Command(String name) {
        this.name = name;
        this.label = name;
    }

    public String getName() {
        return name;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void setAliases(List<String> aliases) {
        this.aliases.clear();
        this.aliases.addAll(aliases);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUsage() {
        return usage;
    }

    public void setUsage(String usage) {
        this.usage = usage;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean execute(CommandSender sender, String commandLabel, String[] args) {
        return false;
    }
}
