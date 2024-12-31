package com.hexagram2021.sweeper_maid.platform.services;

import java.util.List;
import java.util.function.Supplier;

public interface ISMConfig {

    Supplier<Integer> getItemSweepInterval();
    Supplier<List<? extends String>> getExtraEntityTypes();
    Supplier<String> getMessageBeforeSweep15_30_60();
    Supplier<String> getMessageBeforeSweep1_10();
    Supplier<String> getMessageAfterSweep();
    Supplier<String> getChatMessageAfterSweep();
    Supplier<Integer> getPermissionLevelDustBin();

}
