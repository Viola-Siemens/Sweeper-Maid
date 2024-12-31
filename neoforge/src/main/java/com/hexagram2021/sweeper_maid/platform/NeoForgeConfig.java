package com.hexagram2021.sweeper_maid.platform;

import com.hexagram2021.sweeper_maid.config.SMConfig;
import com.hexagram2021.sweeper_maid.platform.services.ISMConfig;

import java.util.List;
import java.util.function.Supplier;

public class NeoForgeConfig implements ISMConfig {
    @Override public Supplier<Integer> getItemSweepInterval() { return SMConfig.CONFIG.ITEM_SWEEP_INTERVAL; }
    @Override public Supplier<List<? extends String>> getExtraEntityTypes() { return SMConfig.CONFIG.EXTRA_ENTITY_TYPES; }
    @Override public Supplier<String> getMessageBeforeSweep15_30_60() { return SMConfig.CONFIG.MESSAGE_BEFORE_SWEEP_15_30_60; }
    @Override public Supplier<String> getMessageBeforeSweep1_10() { return SMConfig.CONFIG.MESSAGE_BEFORE_SWEEP_1_10; }
    @Override public Supplier<String> getMessageAfterSweep() { return SMConfig.CONFIG.MESSAGE_AFTER_SWEEP; }
    @Override public Supplier<String> getChatMessageAfterSweep() { return SMConfig.CONFIG.CHAT_MESSAGE_AFTER_SWEEP; }
    @Override public Supplier<Integer> getPermissionLevelDustBin() { return SMConfig.CONFIG.PERMISSION_LEVEL_DUSTBIN; }
}
