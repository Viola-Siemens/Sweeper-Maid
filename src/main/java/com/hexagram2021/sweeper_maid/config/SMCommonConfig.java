package com.hexagram2021.sweeper_maid.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

/**
 * 扫帚女仆模组通用配置类喵~
 * <p>
 * 定义所有可配置选项，包括：
 * <ul>
 *     <li>清理间隔和时间设置喵~</li>
 *     <li>物品白名单和黑名单喵~</li>
 *     <li>额外实体类型喵~</li>
 *     <li>消息模板和提示语喵~</li>
 *     <li>垃圾箱数量和权限等级喵~</li>
 *     <li>物品过载阈值和警告消息喵~</li>
 * </ul>
 * </p>
 *
 * @author liudongyu
 */
@SuppressWarnings("java:S4968")
public final class SMCommonConfig {
	private static final String REGISTRY_NAME_MATCHER = "([a-z0-9_.-]+:[a-z0-9_/.-]+)";

	/**
	 * 私有构造方法，防止实例化喵~
	 */
	private SMCommonConfig() {
	}

	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
	private static final ModConfigSpec SPEC;

	/**
	 * 物品清理间隔（秒），0 表示禁用自动清理喵~
	 */
	public static final ModConfigSpec.IntValue ITEM_SWEEP_INTERVAL;
	// 物品最小存在时间（秒）。存在时间小于此值的掉落物本次不清理，0 表示禁用该保护。
	public static final ModConfigSpec.IntValue MIN_ITEM_AGE_SECONDS;
	// 每 tick 处理的实体数量上限。清理会分摊到多个 tick 以避免卡顿，0 表示不限制（单 tick 完成）。
	public static final ModConfigSpec.IntValue SWEEP_ENTITIES_PER_TICK;
	/**
	 * 物品过载阈值，超过此数量的区块将发送警告喵~
	 */
	public static final ModConfigSpec.IntValue ITEM_OVERLOAD_THRESHOLD;
	/**
	 * 物品过载警告消息模板喵~
	 */
	public static final ModConfigSpec.ConfigValue<String> OVERLOAD_MESSAGE;
	/**
	 * 需要清理的额外实体类型列表喵~
	 */
	public static final ModConfigSpec.ConfigValue<List<? extends String>> EXTRA_ENTITY_TYPES;
	/**
	 * 物品白名单，列表中的物品不会被清理喵~
	 */
	public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_WHITELIST;
	/**
	 * 物品黑名单，列表中的物品会被清理但不存入垃圾箱喵~
	 */
	public static final ModConfigSpec.ConfigValue<List<? extends String>> ITEM_BLACKLIST;
	/**
	 * 清理前 15/30/60 秒的提示消息模板喵~
	 */
	public static final ModConfigSpec.ConfigValue<String> MESSAGE_BEFORE_SWEEP_15_30_60;
	/**
	 * 清理前 1-10 秒的提示消息模板喵~
	 */
	public static final ModConfigSpec.ConfigValue<String> MESSAGE_BEFORE_SWEEP_1_10;
	/**
	 * 清理完成后的 ActionBar 消息模板喵~
	 */
	public static final ModConfigSpec.ConfigValue<String> MESSAGE_AFTER_SWEEP;
	/**
	 * 打开错误垃圾箱时的提示消息模板喵~
	 */
	public static final ModConfigSpec.ConfigValue<String> MESSAGE_WRONG_DUSTBIN;
	/**
	 * 垃圾箱名称前缀喵~
	 */
	public static final ModConfigSpec.ConfigValue<String> DUSTBIN_NAME;
	// 每个轮换代包含的垃圾箱数量（x）。
	public static final ModConfigSpec.IntValue DUSTBINS_PER_ROTATION;
	// 轮换代数量（y），同时是物品被自动清理前可被找回的清理次数（保护期）。
	public static final ModConfigSpec.IntValue ROTATION_COUNT;
	// 是否启用回收箱：清理时受保护的物品会被移入不参与轮换的回收箱，而非被删除。
	public static final ModConfigSpec.BooleanValue ENABLE_RECYCLE;
	// 回收箱名称。
	public static final ModConfigSpec.ConfigValue<String> RECYCLE_NAME;
	// 稀有度保护阈值：稀有度不低于该值的物品会被移入回收箱。
	public static final ModConfigSpec.EnumValue<Rarity> RECYCLE_PROTECT_RARITY;
	// 是否保护带附魔的物品。
	public static final ModConfigSpec.BooleanValue RECYCLE_PROTECT_ENCHANTED;
	// 是否保护带自定义名称的物品。
	public static final ModConfigSpec.BooleanValue RECYCLE_PROTECT_NAMED;
	// 额外始终受保护的物品清单。
	public static final ModConfigSpec.ConfigValue<List<? extends String>> RECYCLE_PROTECT_ITEMS;
	// 手动清空垃圾箱所需的权限等级。
	public static final ModConfigSpec.IntValue PERMISSION_LEVEL_EMPTY;
	/**
	 * 清理完成后的聊天消息模板喵~
	 */
	public static final ModConfigSpec.ConfigValue<String> CHAT_MESSAGE_AFTER_SWEEP;
	// 有物品被移入回收箱时的提示消息模板（$1 为移入数量）。
	public static final ModConfigSpec.ConfigValue<String> MESSAGE_RECYCLE_MOVED;
	// 下次清理将被清空的垃圾箱提示前缀，其后附加垃圾箱区间（如 Dustbin 0~7）。
	public static final ModConfigSpec.ConfigValue<String> MESSAGE_EMPTIED_NEXT_SWEEP;
	// 手动清空后的反馈消息模板（$1 为被清空的目标名称）。
	public static final ModConfigSpec.ConfigValue<String> MESSAGE_DUSTBIN_EMPTIED;
	/**
	 * 打开垃圾箱所需的权限等级喵~
	 */
	public static final ModConfigSpec.IntValue PERMISSION_LEVEL_DUSTBIN;
	/**
	 * 立即清理所需的权限等级喵~
	 */
	public static final ModConfigSpec.IntValue PERMISSION_LEVEL_CLEAN;

	static {
		BUILDER.push("sweeper_maid-common-config");
		ITEM_SWEEP_INTERVAL = BUILDER.comment("If 0, disable item sweeping. If > 0, cool down (in seconds) between two item sweeping.")
				.defineInRange("ITEM_SWEEP_INTERVAL", 600, 0, 360000);

		MIN_ITEM_AGE_SECONDS = BUILDER.comment("Dropped items younger than this age (in seconds) will not be swept, giving players a grace period to pick items up. 0 disables this protection.")
				.defineInRange("MIN_ITEM_AGE_SECONDS", 5, 0, 3600);

		SWEEP_ENTITIES_PER_TICK = BUILDER.comment("Max entities processed per server tick while a sweep runs; the sweep is spread across ticks to avoid lag spikes. 0 means no limit (finish the whole sweep in a single tick, not recommended).")
				.defineInRange("SWEEP_ENTITIES_PER_TICK", 200, 0, 1000000);

		ITEM_OVERLOAD_THRESHOLD = BUILDER.comment("Item overload in a chunk. If exceeded, a warning message will be sent.")
				.defineInRange("ITEM_OVERLOAD_THRESHOLD", 640, 1, 6400000);

		OVERLOAD_MESSAGE = BUILDER.comment("Message to be sent when item overload threshold is exceeded. \"$1\" = chunk X, \"$2\" = chunk Z, \"$3\" = item count, \"$4\" = item id.")
				.define("OVERLOAD_MESSAGE", "[Sweeper Maid]: The number of dropped items in the region ($1, $2) is too large, with a total of $3 items!");

		EXTRA_ENTITY_TYPES = BUILDER.comment("Other entities of types will be killed when cleaning, e.g. arrows. You can also kill mobs or even players by setting this.")
				.defineListAllowEmpty("EXTRA_ENTITY_TYPES", List.of(
						ResourceLocation.withDefaultNamespace("arrow").toString(),
						ResourceLocation.withDefaultNamespace("spectral_arrow").toString(),
						ResourceLocation.fromNamespaceAndPath("oceanworld", "drip_ice").toString()
				), () -> "minecraft:zombie", o -> o instanceof String str && str.matches(REGISTRY_NAME_MATCHER));
		ITEM_WHITELIST = BUILDER.comment("Items in this list will never be cleaned and will be kept as item entity until it disappear.")
				.defineListAllowEmpty("ITEM_WHITELIST", List.of(
						ResourceLocation.withDefaultNamespace("nether_star").toString(),
						ResourceLocation.withDefaultNamespace("heavy_core").toString()
				), () -> "minecraft:dirt", o -> o instanceof String str && str.matches(REGISTRY_NAME_MATCHER));
		ITEM_BLACKLIST = BUILDER.comment("Items in this list will be cleaned but not added to the dustbin.")
				.defineListAllowEmpty("ITEM_BLACKLIST", List.of(
						ResourceLocation.withDefaultNamespace("cobblestone").toString(),
						ResourceLocation.withDefaultNamespace("sand").toString()
				), () -> "minecraft:dirt", o -> o instanceof String str && str.matches(REGISTRY_NAME_MATCHER));
		MESSAGE_BEFORE_SWEEP_15_30_60 = BUILDER.comment("What message will be sent to players when there's 15s, 30s and 60s left to sweep. \"$1\" stands for the remaining time (in seconds).")
				.define("MESSAGE_BEFORE_SWEEP_15_30_60", "[Sweeper Maid]: I'll sweep the floor in $1 seconds!");
		MESSAGE_BEFORE_SWEEP_1_10 = BUILDER.comment("What message will be sent to players when there's 1s~10s left to sweep. \"$1\" stands for the remaining time (in seconds).")
				.define("MESSAGE_BEFORE_SWEEP_1_10", "[Sweeper Maid]: I'll sweep the floor in $1 seconds!");
		MESSAGE_AFTER_SWEEP = BUILDER.comment("What message will be sent to players after a sweep. \"$1\" = dropped items stored, \"$2\" = other entities removed, \"$3\" = blacklisted items removed.")
				.define("MESSAGE_AFTER_SWEEP", "[Sweeper Maid]: $1 dropped items, $2 unnecessary entities and $3 blacklist items are cleaned during this sweeping.");
		MESSAGE_WRONG_DUSTBIN = BUILDER.comment("What message will be sent to players when open a wrong dustbin.")
				.define("MESSAGE_WRONG_DUSTBIN", "[Sweeper Maid]: Wrong dustbin.");
		DUSTBIN_NAME = BUILDER.comment("Name of dustbins.").define("DUSTBIN_NAME", "Dustbin ");
		DUSTBINS_PER_ROTATION = BUILDER.comment("Number of dustbins in each rotation (x). Total dustbins = DUSTBINS_PER_ROTATION * ROTATION_COUNT.")
				.defineInRange("DUSTBINS_PER_ROTATION", 8, 1, 64);
		ROTATION_COUNT = BUILDER.comment("Number of rotations (y). Also the number of sweeps that swept items stay recoverable before being auto-cleared. 1 clears the previous sweep's items on every sweep.")
				.defineInRange("ROTATION_COUNT", 2, 1, 64);
		ENABLE_RECYCLE = BUILDER.comment("If true, protected items (see RECYCLE_PROTECT_* options) are moved to a non-rotating recycle bin before a dustbin is auto-cleared, instead of being deleted.")
				.define("ENABLE_RECYCLE", true);
		RECYCLE_NAME = BUILDER.comment("Name of the recycle bin.").define("RECYCLE_NAME", "Recycle");
		RECYCLE_PROTECT_RARITY = BUILDER.comment("Items whose rarity is at or above this value are protected. One of COMMON, UNCOMMON, RARE, EPIC.")
				.defineEnum("RECYCLE_PROTECT_RARITY", Rarity.RARE);
		RECYCLE_PROTECT_ENCHANTED = BUILDER.comment("If true, enchanted items are protected.").define("RECYCLE_PROTECT_ENCHANTED", true);
		RECYCLE_PROTECT_NAMED = BUILDER.comment("If true, items with a custom name are protected.").define("RECYCLE_PROTECT_NAMED", true);
		RECYCLE_PROTECT_ITEMS = BUILDER.comment("Items in this list are always protected (moved to the recycle bin).")
				.defineListAllowEmpty("RECYCLE_PROTECT_ITEMS", List.of(), () -> "minecraft:nether_star", o -> o instanceof String str && str.matches(REGISTRY_NAME_MATCHER));
		CHAT_MESSAGE_AFTER_SWEEP = BUILDER.comment("What chat message will be sent to players after a sweep. Links to this sweep's non-empty dustbins (and the recycle bin if non-empty) are appended.")
				.define("CHAT_MESSAGE_AFTER_SWEEP", "[Sweeper Maid]: Anything's missing? Let's checkout the dustbin:");
		MESSAGE_RECYCLE_MOVED = BUILDER.comment("Message sent when protected items are moved to the recycle bin during a sweep (only shown when at least one item is moved). \"$1\" is the number of items moved.")
				.define("MESSAGE_RECYCLE_MOVED", "[Sweeper Maid]: $1 protected item(s) were moved to the recycle bin.");
		MESSAGE_EMPTIED_NEXT_SWEEP = BUILDER.comment("Prefix of the message warning which dustbins will be emptied on the next sweep. A dustbin range (e.g. Dustbin 0~7) is appended.")
				.define("MESSAGE_EMPTIED_NEXT_SWEEP", "[Sweeper Maid]: Will be emptied next sweep (grab your items!):");
		MESSAGE_DUSTBIN_EMPTIED = BUILDER.comment("Feedback after manually emptying. \"$1\" is the emptied target (dustbin name or the recycle bin name). If any protected items were moved, a separate MESSAGE_RECYCLE_MOVED line follows.")
				.define("MESSAGE_DUSTBIN_EMPTIED", "[Sweeper Maid]: $1 has been emptied.");
		PERMISSION_LEVEL_DUSTBIN = BUILDER.comment("Permission level of a player to open the dustbin.").defineInRange("PERMISSION_LEVEL_DUSTBIN", 0, 0, 4);
		PERMISSION_LEVEL_CLEAN = BUILDER.comment("Permission level of a player to clean immediately.").defineInRange("PERMISSION_LEVEL_CLEAN", 2, 0, 4);
		PERMISSION_LEVEL_EMPTY = BUILDER.comment("Permission level of a player to empty dustbins manually.").defineInRange("PERMISSION_LEVEL_EMPTY", 2, 0, 4);
		BUILDER.pop();
		SPEC = BUILDER.build();
	}

	/**
	 * 获取配置规范实例喵~
	 *
	 * @return 配置规范对象喵~
	 */
	public static ModConfigSpec getConfig() {
		return SPEC;
	}
}
