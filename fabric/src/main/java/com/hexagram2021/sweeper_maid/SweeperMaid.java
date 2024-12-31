package com.hexagram2021.sweeper_maid;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class SweeperMaid implements ModInitializer {
    
    @Override
    public void onInitialize() {
        ServerTickEvents.START_SERVER_TICK.register(SweeperMaidCommon::onServerPreTick);
        ServerTickEvents.END_SERVER_TICK.register(SweeperMaidCommon::onServerPostTick);
        CommandRegistrationCallback.EVENT.register((dispatcher, a, b) -> SweeperMaidCommon.registerCommands(() -> dispatcher));
        ServerLifecycleEvents.SERVER_STARTED.register(SweeperMaidCommon::onServerStarted);
    }
}
