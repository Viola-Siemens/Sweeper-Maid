package com.hexagram2021.sweeper_maid.platform;

import com.hexagram2021.sweeper_maid.config.SMConfig;
import com.hexagram2021.sweeper_maid.platform.services.ISMConfig;
import io.github.frqnny.omegaconfig.OmegaConfig;

import java.util.List;
import java.util.function.Supplier;

public class FabricConfig implements ISMConfig {
    public static final SMConfig CONFIG = OmegaConfig.register(SMConfig.class);

    @Override public Supplier<Integer> getItemSweepInterval() { return () -> CONFIG.itemSweepInterval; }
    @Override public Supplier<List<? extends String>> getExtraEntityTypes() { return () -> CONFIG.extraEntityTypes; }
    @Override public Supplier<String> getMessageBeforeSweep15_30_60() { return () -> CONFIG.messageBeforeSweep15_30_60; }
    @Override public Supplier<String> getMessageBeforeSweep1_10() { return () -> CONFIG.messageBeforeSweep1_10; }
    @Override public Supplier<String> getMessageAfterSweep() { return () -> CONFIG.messageAfterSweep; }
    @Override public Supplier<String> getChatMessageAfterSweep() { return () -> CONFIG.chatMessageAfterSweep; }
    @Override public Supplier<Integer> getPermissionLevelDustBin() { return () -> CONFIG.permissionLevelDustbin; }
}
