# AGENTS.md

## Mission
Port **DiscordSRV** (Spigot plugin) into a server-side **Forge 1.7.10** mod named **Herald**, root package `com.ntnh.herald`. Original brief is in `AGENT.md`; this file supersedes it.

## Reference material
- `DiscordSRV-master/` — vendored upstream DiscordSRV v1.30.5 source, gitignored. This is what gets ported; do not commit it.
- Git history on `main` holds a **prior complete port attempt** (package `com.herald`). Mine it with `git show <sha>:<path>` for the proven approach, dependency list, and build config. The working tree was intentionally reset to the bare GTNH template, so the current uncommitted deletions are expected — do not "fix" or commit them.

## Current state
Working tree is the fresh GTNH template. Rename placeholder identity everywhere while porting:
- `gradle.properties`: `modName`, `modId`, `modGroup` (→ `com.ntnh.herald`), `generateGradleTokenClass`
- `src/main/resources/mcmod.info`, and the stub classes under `src/main/java/com/myname/`

## Toolchain
- GTNH gradle convention (`com.gtnewhorizons.gtnhconvention`): Minecraft 1.7.10 / Forge `10.13.4.1614`, Java 8 target bytecode, Java 25 toolchain (`.java-version`).
- `enableModernJavaSyntax = jabel` permits modern Java syntax on the Java 8 target.
- No test sources exist.

## Commands
- `./gradlew build` — compile + Spotless + Checkstyle
- `./gradlew spotlessApply` — required first; formatting is enforced and CI otherwise auto-opens a fix PR
- `./gradlew runServer` — smoke test locally
- CI (shared GTNH-Actions-Workflows): `setupCIWorkspace` → `assemble` → `build` → boots `runServer` for ~90s and **fails on any error report in the logs**. Also rejects `-pre` prerelease dependency versions in `dependencies.gradle`.

## Port architecture (proven in git history)
- Keep DiscordSRV code near-verbatim; write **Bukkit API shims** under `src/main/java/org/bukkit/...` (incl. `org.bukkit.craftbukkit`, `org.bukkit.event`) that wrap real 1.7.10 MCP/Forge classes (`EntityPlayerMP`, `MinecraftServer`, ...).
- Also shim third-party APIs DiscordSRV touches: `me.clip.placeholderapi.*`, `com.gmail.nossr50.api.ChatAPI`, `net.kyori.adventure.platform.bukkit.BukkitAudiences`, `io.papermc.paper.advancement.*`.
- A bridge class (`HeraldDiscordSRV`) subscribes to Forge events and re-fires them as Bukkit events so DiscordSRV listeners run unchanged.
- `@Mod` entry point (`Herald`), `@SidedProxy` with a no-op `ClientProxy extends CommonProxy`; must boot a dedicated server cleanly.

## Dependencies (from prior attempt)
- Shadowed into the jar: JDA `4.4.1_DiscordSRV.fix-7` — DiscordSRV-patched build, **only on Scarsz Maven** (`nexus.scarsz.me`), not Maven Central; plus `configuralize:1.3.2`, adventure libs, commons-* , guava, `slf4j-jdk14`.
- Lombok as `compileOnly` + `annotationProcessor` (1.18.38; needs a recent version to run on JDK 25).
- Set `usesShadowedDependencies = true` in `gradle.properties`; add repos: Scarsz Maven, `oss.sonatype.org` snapshots, `maven.devs.rip`, `maven.parchmentmc.net`.

## Gotchas
- DiscordSRV relies on **Lombok** (`@Getter`/`@Setter`, etc.); without the `annotationProcessor` dependency the ported code will not compile.
- Vendored DiscordSRV sources keep upstream wildcard imports; `disableCheckstyle = true` is set in `gradle.properties` so `build` passes. Do not re-enable Checkstyle while the vendored files stay verbatim.
- `relocateShadowedDependencies = false` and `minimizeShadowedDependencies = false` are REQUIRED: the prior port's crash (`NoClassDefFoundError: org/bukkit/World` at boot) came from a jar that did not bundle every `org.bukkit.*` shim. All shims live in `src/main/java` and must stay in the jar; verify with `jar tf build/libs/<jar> | grep -c '^org/bukkit/'`.
- 1.7.10 MCP/Forge APIs differ sharply from modern/Bukkit APIs — the shims are the translation layer; keep them minimal and use MCP mappings names.
