package com.hexagram2021.sweeper_maid.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public final class SMCommonConfig {
	private SMCommonConfig() {
	}

	private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
	private static final ForgeConfigSpec SPEC;

	public static final ForgeConfigSpec.IntValue ITEM_SWEEP_INTERVAL;
	public static final ForgeConfigSpec.ConfigValue<List<? extends String>> EXTRA_ENTITY_TYPES;
	public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_BLACKLIST; // 黑名单物品配置
	public static final ForgeConfigSpec.ConfigValue<String> MESSAGE_BEFORE_SWEEP_15_30_60;
	public static final ForgeConfigSpec.ConfigValue<String> MESSAGE_BEFORE_SWEEP_1_10;
	public static final ForgeConfigSpec.ConfigValue<String> MESSAGE_AFTER_SWEEP;
	public static final ForgeConfigSpec.ConfigValue<String> MESSAGE_WRONG_DUSTBIN;
	public static final ForgeConfigSpec.ConfigValue<String> DUSTBIN_NAME;
	public static final ForgeConfigSpec.ConfigValue<String> CHAT_MESSAGE_AFTER_SWEEP;
	public static final ForgeConfigSpec.IntValue PERMISSION_LEVEL_DUSTBIN;
	public static final ForgeConfigSpec.IntValue ITEM_OVERLOAD_THRESHOLD;
	public static final ForgeConfigSpec.ConfigValue<String> OVERLOAD_MESSAGE;

	static {
		BUILDER.push("sweeper_maid-common-config");
		ITEM_SWEEP_INTERVAL = BUILDER.comment("If 0, disable item sweeping. If > 0, cool down (in seconds) between two item sweeping.")
				.defineInRange("ITEM_SWEEP_INTERVAL", 600, 0, 360000);

		ITEM_OVERLOAD_THRESHOLD = BUILDER.comment("Item overload in a chunk. If exceeded, a warning message will be sent.")
				.defineInRange("ITEM_OVERLOAD_THRESHOLD", 640, 1, 6400000);

		OVERLOAD_MESSAGE = BUILDER.comment("Message to be sent when item overload threshold is exceeded. \"$1\" stands for chunk X position, \"$2\" stands for chunk Z position, and \"$3\" stands for item count.")
				.define("OVERLOAD_MESSAGE", "[Sweeper Maid]: The number of dropped items in the region ($1, $2) is too high, with a total of $3 items!");

		EXTRA_ENTITY_TYPES = BUILDER.comment("Other entities of types will be killed when cleaning, e.g. arrows. You can also kill mobs or even players by setting this.")
				.defineListAllowEmpty("EXTRA_ENTITY_TYPES", List.of(
						new ResourceLocation("arrow").toString(), new ResourceLocation("spectral_arrow").toString(), new ResourceLocation("oceanworld", "drip_ice").toString()
				), o -> o instanceof String str && ResourceLocation.isValidResourceLocation(str));
		// 添加黑名单配置
		ITEM_BLACKLIST = BUILDER.comment("Items in this list will be cleaned but not added to the dustbin.")
				.defineListAllowEmpty("ITEM_BLACKLIST", List.of(
						new ResourceLocation("minecraft", "cobblestone").toString(), new ResourceLocation("minecraft", "sand").toString()
				), o -> o instanceof String str && ResourceLocation.isValidResourceLocation(str));
		MESSAGE_BEFORE_SWEEP_15_30_60 = BUILDER.comment("What message will be sent to players when there's 15s, 30s and 60s left to sweep. \"$1\" stands for the remaining time (in seconds).")
				.define("MESSAGE_BEFORE_SWEEP_15_30_60", "[Sweeper Maid]: I'll sweep the floor in $1 seconds!");
		MESSAGE_BEFORE_SWEEP_1_10 = BUILDER.comment("What message will be sent to players when there's 1s~10s left to sweep. \"$1\" stands for the remaining time (in seconds).")
				.define("MESSAGE_BEFORE_SWEEP_1_10", "[Sweeper Maid]: I'll sweep the floor in $1 seconds!");
		MESSAGE_AFTER_SWEEP = BUILDER.comment("What message will be sent to players after a sweep. \"$1\" stands for the number of killed dropped items, and \"$2\" stands for the number of killed entities.")
				.define("MESSAGE_AFTER_SWEEP", "[Sweeper Maid]: $1 dropped items , $2 unnecessary entities and $3 blacklist items are cleaned during this sweeping.");
		MESSAGE_WRONG_DUSTBIN = BUILDER.comment("What message will be sent to players when open a wrong dustbin. ")
				.define("MESSAGE_WRONG_DUSTBIN", "[Sweeper Maid]: Wrong dustbin.");
		DUSTBIN_NAME = BUILDER.comment("Dustbin. ")
				.define("DUSTBIN_NAME", "Dustbin ");
		CHAT_MESSAGE_AFTER_SWEEP = BUILDER.comment("What chat message will be sent to players after a sweep. Command will be appended to the end of the chat message.")
				.define("CHAT_MESSAGE_AFTER_SWEEP", "[Sweeper Maid]: Anything's missing? Let's checkout the dustbin: ");
		PERMISSION_LEVEL_DUSTBIN = BUILDER.comment("Permission level of a player to open the dustbin.").defineInRange("PERMISSION_LEVEL_DUSTBIN", 0, 0, 4);
		BUILDER.pop();
		SPEC = BUILDER.build();

	}


	public static ForgeConfigSpec getConfig() {
		return SPEC;
	}
}