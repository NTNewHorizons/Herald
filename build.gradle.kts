

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}
// Relocate everything bundled into the jar into a private namespace so Herald works standalone
// AND alongside other mods: other mods on the pack (e.g. ForgeEssentials) ship their own
// JDA/okhttp/okio and real org.bukkit classes, which otherwise collide with ours at runtime.
// The ShadowJar type is bundled by the convention plugin but not exposed to this script's
// classpath, so we configure it reflectively.
tasks.named("shadowJar").configure {
    val relocate = javaClass.getMethod("relocate", String::class.java, String::class.java)
    // Our org.bukkit.* shims must not be shadowed by real Bukkit classes from other mods.
    relocate.invoke(this, "org.bukkit", "relocated.org.bukkit")
    // Third-party libraries shaded into the jar, mirroring the upstream DiscordSRV relocation set.
    val p = "com.ntnh.herald.libs."
    relocate.invoke(this, "net.dv8tion.jda", p + "jda")
    relocate.invoke(this, "com.iwebpp.crypto", p + "iwebpp.crypto")
    relocate.invoke(this, "com.vdurmont.emoji", p + "emoji")
    relocate.invoke(this, "com.neovisionaries.ws", p + "ws")
    relocate.invoke(this, "com.fasterxml.jackson", p + "jackson")
    relocate.invoke(this, "net.kyori", p + "kyori")
    relocate.invoke(this, "dev.vankka.mcdiscordreserializer", p + "mcdiscordreserializer")
    relocate.invoke(this, "dev.vankka.simpleast", p + "simpleast")
    relocate.invoke(this, "dev.vankka.dynamicproxy", p + "dynamicproxy")
    relocate.invoke(this, "org.apache.commons", p + "commons")
    relocate.invoke(this, "com.google.common", p + "google.common")
    relocate.invoke(this, "com.google.errorprone", p + "google.errorprone")
    relocate.invoke(this, "com.google.gson", p + "google.gson")
    relocate.invoke(this, "com.google.j2objc", p + "google.j2objc")
    relocate.invoke(this, "org.json", p + "json")
    relocate.invoke(this, "org.minidns", p + "minidns")
    relocate.invoke(this, "de.measite.minidns", p + "minidns.measite")
    relocate.invoke(this, "com.github.kevinsawicki", p + "kevinsawicki")
    relocate.invoke(this, "com.github.zafarkhaja", p + "zafarkhaja")
    relocate.invoke(this, "com.github.alexheretic", p + "alexheretic")
    relocate.invoke(this, "com.hrakaroo", p + "hrakaroo")
    relocate.invoke(this, "net.sf.trove4j", p + "trove4j")
    relocate.invoke(this, "org.springframework", p + "springframework")
    relocate.invoke(this, "okhttp3", p + "okhttp3")
    relocate.invoke(this, "okio", p + "okio")
}

repositories {
    maven {
        name = "Scarsz Maven"
        url = uri("https://nexus.scarsz.me/content/groups/public/")
    }
    maven {
        name = "Sonatype OSS Snapshots"
        url = uri("https://oss.sonatype.org/content/repositories/snapshots")
    }
    maven {
        name = "DV8FromTheWorld"
        url = uri("https://maven.devs.rip/")
    }
    maven {
        name = "ParchmentMC"
        url = uri("https://maven.parchmentmc.net/releases/")
    }
}
