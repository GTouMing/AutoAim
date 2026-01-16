package com.gtouming.autoaim;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(Autoaim.MODID)
public class Autoaim {
    public static final String MODID = "autoaim";
    public Autoaim(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {}
}
