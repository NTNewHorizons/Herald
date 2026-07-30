
plugins {
    id("com.gtnewhorizons.gtnhconvention")
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
