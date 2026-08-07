

plugins {
    id("com.gtnewhorizons.gtnhconvention")
}
// Other mods on the pack (e.g. ForgeEssentials) bundle real org.bukkit classes, which can break
// classloading of our Bukkit shims at runtime. Relocate the shims into a private namespace.
// The ShadowJar type is bundled by the convention plugin but not exposed to this script's
// classpath, so we configure it reflectively.
tasks.named("shadowJar").configure {
    javaClass.getMethod("relocate", String::class.java, String::class.java)
        .invoke(this, "org.bukkit", "relocated.org.bukkit")
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
