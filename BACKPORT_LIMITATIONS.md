# Deliberately Omitted Backport Features

Herald targets Forge 1.7.10. The following DiscordSRV features are intentionally outside the scope of the backport and
should not be treated as unfinished parity work.

## Third-party plugin integrations

Integrations with the Bukkit, Spigot, and Paper plugin ecosystems are not backported. ForgeEssentials is the deliberate
exception: Herald uses its Forge-native groups and permissions API when the mod is installed, with a vanilla
operator/default fallback when it is not.

## Plugin API auto-discovery

Automatic discovery of optional Bukkit services, plugins, and integration APIs is not included. Herald uses explicit
Forge-side integration points suitable for a 1.7.10 mod.

## VentureChat API

VentureChat-specific channel and message handling is not included because VentureChat is a Bukkit plugin integration,
not a Forge 1.7.10 API.

## bStats metrics

bStats telemetry and its associated configuration, charts, and submission code are not included.

## Newer advancement events

Advancement support introduced in newer Minecraft versions is not backported. Minecraft 1.7.10 uses the older
achievement system and does not expose the newer advancement APIs or events.

## Paper chat APIs

Paper-specific chat events and component-based chat handling are not included. Paper is a separate server platform, and
its chat APIs do not exist in Forge 1.7.10.
