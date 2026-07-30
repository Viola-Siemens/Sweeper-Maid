package com.hexagram2021.sweeper_maid;

import com.google.common.collect.Maps;
import com.hexagram2021.sweeper_maid.command.SMCommands;
import com.hexagram2021.sweeper_maid.config.SMCommonConfig;
import com.hexagram2021.sweeper_maid.save.SMSavedData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;

/**
 * 扫帚女仆模组主类，负责定期清理服务器中的掉落物品和多余实体喵~
 * <p>
 * 主要功能：
 * <ul>
 *     <li>定期扫描并清理服务器中的掉落物品</li>
 *     <li>将清理的物品存储到垃圾箱中供玩家查询</li>
 *     <li>支持清理额外的实体类型（如箭矢）</li>
 *     <li>提供倒计时提示和清理结果反馈</li>
 *     <li>支持物品黑名单和白名单配置</li>
 *     <li>检测并警告物品过载的区块</li>
 * </ul>
 * </p>
 *
 * @author liudongyu
 */
@SuppressWarnings({"unused", "java:S1104", "java:S1444", "java:S3010"})
@Mod(SweeperMaid.MODID)
public class SweeperMaid {
	/**
	 * 模组 ID 喵~
	 */
	public static final String MODID = "sweeper_maid";
	/**
	 * 模组名称喵~
	 */
	public static final String MODNAME = "Sweeper Maid";
	/**
	 * 模组版本号喵~
	 */
	public static final String VERSION = ModList.get().getModFileById(MODID).versionString();

	/**
	 * 立即清理回调函数，用于命令执行时触发清理喵~
	 */
	public static Runnable clean = () -> {};

	/**
	 * 构造方法：注册配置和事件监听器喵~
	 *
	 * @param modContainer 模组容器实例喵~
	 */
	public SweeperMaid(ModContainer modContainer) {
		modContainer.registerConfig(ModConfig.Type.COMMON, SMCommonConfig.getConfig());
		NeoForge.EVENT_BUS.register(this);

		clean = () -> this.sweepTickRemain = 0;
	}

	/**
	 * 距离下次清理的剩余刻数喵~
	 */
	private int sweepTickRemain = 0;
	/**
	 * 是否需要在下一刻执行清理喵~
	 */
	private boolean toSweep = false;
	/**
	 * 是否为首次刻 tick 喵~
	 */
	private boolean firstTick = true;

	// 进行中的清理任务；为 null 表示当前没有清理在进行。
	@Nullable
	private SweepJob currentJob = null;

	/**
	 * 注册模组命令事件处理喵~
	 * <p>
	 * 将扫帚女仆的命令注册到命令分发器中喵~
	 * </p>
	 *
	 * @param event 命令注册事件喵~
	 */
	@SubscribeEvent
	public void registerCommands(RegisterCommandsEvent event) {
		final CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
		dispatcher.register(SMCommands.register());
	}

	/**
	 * 服务器刻 tick 前处理：发送倒计时消息喵~
	 * <p>
	 * 在每次刻 tick 前检查是否需要清理，并向所有玩家发送 ActionBar 倒计时消息喵~
	 * 倒计时节点：60 秒、30 秒、15 秒以及 10 秒以内每秒喵~
	 * </p>
	 *
	 * @param event 服务器刻 tick 前事件喵~
	 */
	@SubscribeEvent
	public void onTickPre(ServerTickEvent.Pre event) {
		MinecraftServer server = event.getServer();
		if(SMCommonConfig.ITEM_SWEEP_INTERVAL.get() == 0) {
			return;
		}
		this.prepareAndSendCountdownMessage(server);
	}

	/**
	 * 服务器刻 tick 后处理：执行清理逻辑喵~
	 * <p>
	 * 在首次 tick 时初始化存档数据，之后在需要清理时执行清扫操作喵~
	 * </p>
	 *
	 * @param event 服务器刻 tick 后事件喵~
	 */
	@SubscribeEvent
	public void onTickPost(ServerTickEvent.Post event) {
		MinecraftServer server = event.getServer();
		if(SMCommonConfig.ITEM_SWEEP_INTERVAL.get() == 0) {
			return;
		}
		if(this.firstTick) {
			this.firstTick = false;
			this.toSweep = false;
			SMSavedData.initialize();
		} else if(this.currentJob != null) {
			// 分摊执行进行中的清理：每 tick 处理配置数量的实体，全部完成后再统一反馈。
			if(this.currentJob.process(SMCommonConfig.SWEEP_ENTITIES_PER_TICK.get())) {
				this.currentJob.finalizeSweep(server);
				this.currentJob = null;
			}
		} else if(this.toSweep) {
			// 一个清理任务未结束前不会开始下一个，避免并发清理。
			this.toSweep = false;
			this.currentJob = new SweepJob(server);
		}
	}

	/**
	 * 服务器启动后事件处理：初始化存档数据喵~
	 * <p>
	 * 在服务器启动后，从世界数据存储中加载或创建扫帚女仆的存档数据喵~
	 * </p>
	 *
	 * @param event 服务器启动事件喵~
	 */
	@SubscribeEvent
	public void onServerStarted(ServerStartedEvent event) {
		ServerLevel world = event.getServer().getLevel(Level.OVERWORLD);
		assert world != null;
		if (!world.isClientSide) {
			SMSavedData worldData = world.getDataStorage().computeIfAbsent(new SavedData.Factory<>(SMSavedData::new, SMSavedData::new), SMSavedData.SAVED_DATA_NAME);
			SMSavedData.setInstance(worldData);
		}
	}

	/**
	 * 创建命令源堆栈喵~
	 * <p>
	 * 用于在发送消息时模拟命令执行上下文喵~
	 * </p>
	 *
	 * @param player 玩家实例喵~
	 * @param level 世界实例喵~
	 * @param blockPos 方块位置喵~
	 * @return 命令源堆栈喵~
	 */
	private static CommandSourceStack createCommandSourceStack(Player player, Level level, BlockPos blockPos) {
		return new CommandSourceStack(CommandSource.NULL, Vec3.atCenterOf(blockPos), Vec2.ZERO, (ServerLevel) level, 2, player.getName().getString(), player.getDisplayName(), level.getServer(), player);
	}

	/**
	 * 向管理员广播消息喵~
	 * <p>
	 * 仅当服务器启用命令反馈时，向所有在线管理员发送系统消息喵~
	 * </p>
	 *
	 * @param server 服务器实例喵~
	 * @param message 要发送的消息组件喵~
	 */
	private static void broadcastToAdmins(MinecraftServer server, Component message) {
		if (server.getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
			for(ServerPlayer serverplayer : server.getPlayerList().getPlayers()) {
				if (server.getPlayerList().isOp(serverplayer.getGameProfile())) {
					serverplayer.sendSystemMessage(message);
				}
			}
		}
	}

	/**
	 * 准备并发送倒计时消息给所有玩家喵~
	 * <p>
	 * 更新清理倒计时，并在特定时间点（60/30/15 秒和 10 秒内）向玩家发送 ActionBar 消息喵~
	 * 当倒计时归零时标记需要执行清理喵~
	 * </p>
	 *
	 * @param server 服务器实例喵~
	 */
	private void prepareAndSendCountdownMessage(MinecraftServer server) {
		this.sweepTickRemain -= 1;
		if (this.sweepTickRemain <= 0) {
			this.toSweep = true;
			this.sweepTickRemain = SMCommonConfig.ITEM_SWEEP_INTERVAL.get() * SharedConstants.TICKS_PER_SECOND;
		} else if(this.sweepTickRemain == 15 * SharedConstants.TICKS_PER_SECOND || this.sweepTickRemain == 30 * SharedConstants.TICKS_PER_SECOND || this.sweepTickRemain == 60 * SharedConstants.TICKS_PER_SECOND) {
			server.getPlayerList().getPlayers().forEach(player -> {
				try {
					player.connection.send(new ClientboundSetActionBarTextPacket(ComponentUtils.updateForEntity(
							createCommandSourceStack(player, player.level(), player.blockPosition()),
							Component.literal(SMCommonConfig.MESSAGE_BEFORE_SWEEP_15_30_60.get().replace("$1", String.valueOf(this.sweepTickRemain / SharedConstants.TICKS_PER_SECOND))).withStyle(ChatFormatting.GRAY),
							player, 0
					)));
				} catch (CommandSyntaxException ignored) {
					// Ignored
				}
			});
		} else if(this.sweepTickRemain % SharedConstants.TICKS_PER_SECOND == 0 && this.sweepTickRemain / SharedConstants.TICKS_PER_SECOND <= 10) {
			server.getPlayerList().getPlayers().forEach(player -> {
				try {
					player.connection.send(new ClientboundSetActionBarTextPacket(ComponentUtils.updateForEntity(
							createCommandSourceStack(player, player.level(), player.blockPosition()),
							Component.literal(SMCommonConfig.MESSAGE_BEFORE_SWEEP_1_10.get().replace("$1", String.valueOf(this.sweepTickRemain / SharedConstants.TICKS_PER_SECOND))).withStyle(ChatFormatting.GOLD),
							player, 0
					)));
				} catch (CommandSyntaxException ignored) {
					// Ignored
				}
			});
		}
	}

	// 单次清理任务：创建时对目标实体拍摄快照，随后按配置的每 tick 数量分摊处理，完成后统一反馈结果。
	private static final class SweepJob {
		private final SMSavedData instance;
		private final Registry<Item> itemRegistry;
		private final Registry<EntityType<?>> entityTypeRegistry;
		private final Set<String> whitelist;
		private final Set<String> blacklist;
		private final Set<String> extraEntityTypes;
		private final Deque<Entity> queue = new ArrayDeque<>();
		private final Map<LevelChunk, Map<String, Integer>> chunkItemCounts = Maps.newIdentityHashMap();
		private int droppedItems = 0;
		private int extraEntities = 0;
		private int blacklistedItems = 0;

		private SweepJob(MinecraftServer server) {
			this.instance = SMSavedData.getInstance();
			// 将注册表查询与配置列表在任务开始时解析一次，避免逐个实体重复计算。
			this.itemRegistry = server.registryAccess().registryOrThrow(Registries.ITEM);
			this.entityTypeRegistry = server.registryAccess().registryOrThrow(Registries.ENTITY_TYPE);
			this.whitelist = Set.copyOf(SMCommonConfig.ITEM_WHITELIST.get());
			this.blacklist = Set.copyOf(SMCommonConfig.ITEM_BLACKLIST.get());
			this.extraEntityTypes = Set.copyOf(SMCommonConfig.EXTRA_ENTITY_TYPES.get());
			int minItemAgeTicks = SMCommonConfig.MIN_ITEM_AGE_SECONDS.get() * SharedConstants.TICKS_PER_SECOND;

			// 快照阶段：一次性遍历所有维度的实体，仅登记清理目标的引用，此时不做任何删除。
			// 遍历结束后才逐 tick 删除，因此不会在遍历实体集合的同时修改它；快照后新掉落的物品保留到下次清理。
			server.getAllLevels().forEach(serverLevel -> {
				for (Entity entity : serverLevel.getAllEntities()) {
					if (entity instanceof ItemEntity itemEntity) {
						// 存在时间不足的掉落物本次跳过，避免误清玩家刚丢下的物品。
						if (itemEntity.tickCount >= minItemAgeTicks) {
							this.queue.add(itemEntity);
						}
					} else {
						ResourceLocation typeKey = this.entityTypeRegistry.getKey(entity.getType());
						if (typeKey != null && this.extraEntityTypes.contains(typeKey.toString())) {
							this.queue.add(entity);
						}
					}
				}
			});
		}

		// 处理至多 budget 个实体；budget 小于等于 0 表示本次处理队列中的全部实体。返回队列是否已清空。
		private boolean process(int budget) {
			int limit = budget <= 0 ? Integer.MAX_VALUE : budget;
			int processed = 0;
			while (processed < limit && !this.queue.isEmpty()) {
				Entity entity = this.queue.poll();
				++processed;
				// 快照之后实体可能已被拾取或移除，处理前重新校验。
				if (entity == null || entity.isRemoved()) {
					continue;
				}
				if (entity instanceof ItemEntity itemEntity) {
					this.collectItem(itemEntity);
				} else {
					this.extraEntities += 1;
					entity.discard();
				}
			}
			return this.queue.isEmpty();
		}

		// 处理单个掉落物：黑名单物品仅计数不入箱，其余非白名单物品入箱并统计所在区块的过载数量。
		private void collectItem(ItemEntity itemEntity) {
			ItemStack itemStack = itemEntity.getItem();
			ResourceLocation itemKey = this.itemRegistry.getKey(itemStack.getItem());
			if (itemKey == null) {
				return;
			}
			String item = itemKey.toString();
			if (this.blacklist.contains(item)) {
				this.blacklistedItems += itemStack.getCount();
				itemEntity.discard();
			} else if (!this.whitelist.contains(item)) {
				this.instance.addItemToDustbin(itemStack);
				this.droppedItems += itemStack.getCount();
				itemEntity.discard();

				LevelChunk chunk = itemEntity.level().getChunkAt(itemEntity.blockPosition());
				this.chunkItemCounts.computeIfAbsent(chunk, k -> Maps.newHashMap());
				Map<String, Integer> itemCounts = this.chunkItemCounts.get(chunk);
				itemCounts.put(item, itemCounts.getOrDefault(item, 0) + itemStack.getCount());
			}
		}

		// 清理完成后统一反馈：ActionBar 统计、垃圾箱链接、区块过载警告。
		private void finalizeSweep(MinecraftServer server) {
			server.getPlayerList().getPlayers().forEach(player -> {
				try {
					player.connection.send(new ClientboundSetActionBarTextPacket(ComponentUtils.updateForEntity(
							createCommandSourceStack(player, player.level(), player.blockPosition()),
							Component.literal(SMCommonConfig.MESSAGE_AFTER_SWEEP.get()
											.replace("$1", String.valueOf(this.droppedItems))
											.replace("$2", String.valueOf(this.extraEntities))
											.replace("$3", String.valueOf(this.blacklistedItems)))  // 增加黑名单物品统计
									.withStyle(ChatFormatting.AQUA),
							player, 0
					)));
				} catch (CommandSyntaxException ignored) {
					// Ignored
				}
			});

			// Generate all dustbin messages and links
			server.getPlayerList().getPlayers().forEach(player -> {
				MutableComponent message = Component.literal(SMCommonConfig.CHAT_MESSAGE_AFTER_SWEEP.get());

				this.instance.accessDustbins(dustbins -> {
					if(dustbins.isEmpty()) {
						return;
					}
					boolean first = true;
					for (int i = 0; i < dustbins.size(); ++i) {
						if(SMSavedData.getDustbinContainer(i).isEmpty()) {
							continue;
						}
						if(first) {
							first = false;
						} else {
							message.append(Component.literal(", "));
						}
						final int dustbinIndex = i;
						message.append(Component.literal("[" + SMCommonConfig.DUSTBIN_NAME.get() + dustbinIndex + "]")
								.withStyle(style -> style.withColor(ChatFormatting.GREEN)
										.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/sweepermaid dustbin " + dustbinIndex))));
					}
				});

				player.sendSystemMessage(message);
			});

			// Send overload message to players
			int itemOverloadThreshold = SMCommonConfig.ITEM_OVERLOAD_THRESHOLD.get();
			this.chunkItemCounts.forEach((chunk, itemCounts) -> itemCounts.forEach((itemKey, count) -> {
				if (count > itemOverloadThreshold) {
					BlockPos chunkPos = chunk.getPos().getWorldPosition();
					String overloadMessageText = SMCommonConfig.OVERLOAD_MESSAGE.get()
							.replace("$1", String.valueOf(chunkPos.getX()))
							.replace("$2", String.valueOf(chunkPos.getZ()))
							.replace("$3", String.valueOf(count))
							.replace("$4", itemKey);

					MutableComponent overloadMessage = Component.literal(overloadMessageText).withStyle(ChatFormatting.BLUE);

					broadcastToAdmins(server, overloadMessage);
				}
			}));

			this.instance.setDirty();
		}
	}
}
