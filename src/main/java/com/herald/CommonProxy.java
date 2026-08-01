package com.herald;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());
        Herald.LOG.info("Herald version " + Tags.VERSION + " loading");
    }

    public void init(FMLInitializationEvent event) {
        HeraldDiscordSRV.getInstance()
            .init(event);
    }

    public void postInit(FMLPostInitializationEvent event) {
        HeraldDiscordSRV.getInstance()
            .postInit(event);
    }

    public void serverStarting(FMLServerStartingEvent event) {
        HeraldDiscordSRV.getInstance()
            .serverStarting(event);
    }

    public void serverStopping(FMLServerStoppingEvent event) {
        HeraldDiscordSRV.getInstance()
            .shutdown();
    }
}
