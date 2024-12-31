package com.hexagram2021.sweeper_maid.config;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public class SMConfig {

    //Define a field to keep the config and spec for later
    public static final SMConfig CONFIG;
    public static final ModConfigSpec CONFIG_SPEC;

    public final ModConfigSpec.ConfigValue<Integer> ITEM_SWEEP_INTERVAL;
    public final ModConfigSpec.ConfigValue<List<? extends String>> EXTRA_ENTITY_TYPES;
    public final ModConfigSpec.ConfigValue<String> MESSAGE_BEFORE_SWEEP_15_30_60;
    public final ModConfigSpec.ConfigValue<String> MESSAGE_BEFORE_SWEEP_1_10;
    public final ModConfigSpec.ConfigValue<String> MESSAGE_AFTER_SWEEP;
    public final ModConfigSpec.ConfigValue<String> CHAT_MESSAGE_AFTER_SWEEP;
    public final ModConfigSpec.ConfigValue<Integer> PERMISSION_LEVEL_DUSTBIN;

    private SMConfig(ModConfigSpec.Builder builder) {
        ITEM_SWEEP_INTERVAL = builder
                .comment("If 0, disable item sweeping. If > 0, cool down (in seconds) between two item sweeping.")
                .defineInRange("ITEM_SWEEP_INTERVAL", 600, 0, 360000);

        EXTRA_ENTITY_TYPES = builder
                .comment("Other entities of types will be killed when cleaning, e.g. arrows. You can also kill mobs or even players by setting this.")
                .defineListAllowEmpty("EXTRA_ENTITY_TYPES", List.of(
                        ResourceLocation.withDefaultNamespace("arrow").toString(),
                        ResourceLocation.withDefaultNamespace("spectral_arrow").toString(),
                        ResourceLocation.fromNamespaceAndPath("oceanworld", "drip_ice").toString()
                ), o -> o instanceof String str && ResourceLocation.tryParse(str) != null);

        MESSAGE_BEFORE_SWEEP_15_30_60 = builder
                .comment("What message will be sent to players when there's 15s, 30s and 60s left to sweep. \"$1\" stands for the remaining time (in seconds).")
                .define("MESSAGE_BEFORE_SWEEP_15_30_60", "[Sweeper Maid]: I'll sweep the floor in $1 seconds!");

        MESSAGE_BEFORE_SWEEP_1_10 = builder
                .comment("What message will be sent to players when there's 1s~10s left to sweep. \"$1\" stands for the remaining time (in seconds).")
                .define("MESSAGE_BEFORE_SWEEP_1_10", "[Sweeper Maid]: I'll sweep the floor in $1 seconds!");

        MESSAGE_AFTER_SWEEP = builder
                .comment("What message will be sent to players after a sweep. \"$1\" stands for the number of killed dropped items, and \"$2\" stands for the number of killed entities.")
                .define("MESSAGE_AFTER_SWEEP", "[Sweeper Maid]: $1 dropped items and $2 unnecessary entities are cleaned during this sweeping.");

        CHAT_MESSAGE_AFTER_SWEEP = builder
                .comment("What chat message will be sent to players after a sweep. Command will be appended to the end of the chat message.")
                .define("CHAT_MESSAGE_AFTER_SWEEP", "[Sweeper Maid]: Anything's missing? Let's checkout the dustbin: ");

        PERMISSION_LEVEL_DUSTBIN = builder.
                comment("Permission level of a player to open the dustbin.")
                .defineInRange("PERMISSION_LEVEL_DUSTBIN", 0, 0, 4);
    }

    static {
        var pair = new ModConfigSpec.Builder().configure(SMConfig::new);

        CONFIG = pair.getLeft();
        CONFIG_SPEC = pair.getRight();
    }
}
