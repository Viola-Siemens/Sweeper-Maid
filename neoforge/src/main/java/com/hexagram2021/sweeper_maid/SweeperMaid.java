package com.hexagram2021.sweeper_maid;

import com.hexagram2021.sweeper_maid.config.SMConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.NotNull;

@Mod(Constants.MOD_ID)
public class SweeperMaid {
    public SweeperMaid(@NotNull ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SMConfig.CONFIG_SPEC);

        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Pre.class,   e -> SweeperMaidCommon.onServerPreTick(e.getServer()));
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class,  e -> SweeperMaidCommon.onServerPostTick(e.getServer()));
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, e -> SweeperMaidCommon.registerCommands(e::getDispatcher));
        NeoForge.EVENT_BUS.addListener(ServerStartedEvent.class,    e -> SweeperMaidCommon.onServerStarted(e.getServer()));

        // Use NeoForge to bootstrap the Common mod.
        Constants.LOG.info("Hello NeoForge world!");
    }
}