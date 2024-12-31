package com.hexagram2021.sweeper_maid.config;

import io.github.frqnny.omegaconfig.api.Comment;
import io.github.frqnny.omegaconfig.api.Config;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class SMConfig implements Config {

    @Comment("If 0, disable item sweeping. If > 0, cool down (in seconds) between two item sweeping.")
    // Should be between 0 and 360000
    public int itemSweepInterval = 600;

    @Comment("Other entities of types will be killed when cleaning, e.g. arrows. You can also kill mobs or even players by setting this.")
    public List<? extends String> extraEntityTypes = List.of(
        ResourceLocation.withDefaultNamespace("arrow").toString(),
        ResourceLocation.withDefaultNamespace("spectral_arrow").toString(),
        ResourceLocation.fromNamespaceAndPath("oceanworld", "drip_ice").toString()
    );

    @Comment("What message will be sent to players when there's 15s, 30s and 60s left to sweep. \"$1\" stands for the remaining time (in seconds).")
    public String messageBeforeSweep15_30_60 = "[Sweeper Maid]: I'll sweep the floor in $1 seconds!";

    @Comment("What message will be sent to players when there's 1s~10s left to sweep. \"$1\" stands for the remaining time (in seconds).")
    public String messageBeforeSweep1_10 = "[Sweeper Maid]: I'll sweep the floor in $1 seconds!";

    @Comment("What message will be sent to players after a sweep. \"$1\" stands for the number of killed dropped items, and \"$2\" stands for the number of killed entities.")
    public String messageAfterSweep = "[Sweeper Maid]: $1 dropped items and $2 unnecessary entities are cleaned during this sweeping.";

    @Comment("What chat message will be sent to players after a sweep. Command will be appended to the end of the chat message.")
    public String chatMessageAfterSweep = "[Sweeper Maid]: Anything's missing? Let's checkout the dustbin: ";

    @Comment("Permission level of a player to open the dustbin.")
    // Should be between 0 and 4
    public int permissionLevelDustbin = 0;

    @Override
    public String getName() {
        return "sweeper_maid-common-config";
    }
}
