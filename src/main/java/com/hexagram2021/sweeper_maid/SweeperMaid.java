package com.hexagram2021.sweeper_maid;

import com.google.common.collect.Lists;
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 清洁女仆模组主类，负责定期清理服务器中的掉落物品和多余实体喵~
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
	 */
	public SweeperMaid() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SMCommonConfig.getConfig());
		MinecraftForge.EVENT_BUS.register(this);

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

	/**
	 * 注册模组命令事件处理喵~
	 * <p>
	 * 将清洁女仆的命令注册到命令分发器中喵~
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
	 * 服务器刻 tick 处理：发送倒计时消息和执行清理逻辑喵~
	 * <p>
	 * 在 START 阶段发送倒计时消息，在 END 阶段执行清理操作喵~
	 * 倒计时节点：60 秒、30 秒、15 秒以及 10 秒以内每秒喵~
	 * </p>
	 *
	 * @param event 服务器刻 tick 事件喵~
	 */
	@SubscribeEvent
	public void onTick(TickEvent.ServerTickEvent event) {
		MinecraftServer server = event.getServer();
		if (SMCommonConfig.ITEM_SWEEP_INTERVAL.get() == 0) {
			return;
		}
		if (event.phase == TickEvent.Phase.START) {
			this.prepareAndSendCountdownMessage(server);
		} else if (event.phase == TickEvent.Phase.END) {
			if (this.firstTick) {
				this.firstTick = false;
				this.toSweep = false;
				SMSavedData.initialize();
			} else if (this.toSweep) {
				this.toSweep = false;
				doSweeping(server);
			}
		}
	}

	/**
	 * 服务器启动后事件处理：初始化存档数据喵~
	 * <p>
	 * 在服务器启动后，从世界数据存储中加载或创建清洁女仆的存档数据喵~
	 * </p>
	 *
	 * @param event 服务器启动事件喵~
	 */
	@SubscribeEvent
	public void onServerStarted(ServerStartedEvent event) {
		ServerLevel world = event.getServer().getLevel(Level.OVERWORLD);
		assert world != null;
		if (!world.isClientSide) {
			SMSavedData worldData = world.getDataStorage().computeIfAbsent(SMSavedData::new, SMSavedData::new, SMSavedData.SAVED_DATA_NAME);
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
			for (ServerPlayer serverplayer : server.getPlayerList().getPlayers()) {
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
		} else if (this.sweepTickRemain == 15 * SharedConstants.TICKS_PER_SECOND || this.sweepTickRemain == 30 * SharedConstants.TICKS_PER_SECOND || this.sweepTickRemain == 60 * SharedConstants.TICKS_PER_SECOND) {
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
		} else if (this.sweepTickRemain % SharedConstants.TICKS_PER_SECOND == 0 && this.sweepTickRemain / SharedConstants.TICKS_PER_SECOND <= 10) {
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

	/**
	 * 执行清理操作喵~
	 * <p>
	 * 扫描所有维度的实体，清理掉落物品和额外实体类型，并将物品存入垃圾箱喵~
	 * 清理完成后向玩家发送统计信息和垃圾箱访问提示喵~
	 * 检测并警告物品过载的区块喵~
	 * </p>
	 *
	 * @param server 服务器实例喵~
	 */
	private static void doSweeping(MinecraftServer server) {
		SMSavedData instance = SMSavedData.getInstance();

		AtomicInteger droppedItems = new AtomicInteger();
		AtomicInteger extraEntities = new AtomicInteger();
		AtomicInteger blacklistedItems = new AtomicInteger();
		Map<LevelChunk, Map<String, Integer>> chunkItemCounts = Maps.newIdentityHashMap();

		server.getAllLevels().forEach(serverLevel -> {
			Iterable<Entity> entities = serverLevel.getAllEntities();
			List<Entity> killedEntities = Lists.newArrayList();

			for (Entity entity : entities) {
				checkAndCollectEntity(server, serverLevel, entity, killedEntities, blacklistedItems, instance, droppedItems, chunkItemCounts, extraEntities);
			}

			killedEntities.forEach(Entity::discard);
		});

		server.getPlayerList().getPlayers().forEach(player -> {
			try {
				player.connection.send(new ClientboundSetActionBarTextPacket(ComponentUtils.updateForEntity(
						createCommandSourceStack(player, player.level(), player.blockPosition()),
						Component.literal(SMCommonConfig.MESSAGE_AFTER_SWEEP.get()
										.replace("$1", droppedItems.toString())
										.replace("$2", extraEntities.toString())
										.replace("$3", blacklistedItems.toString()))
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

			instance.accessDustbins(dustbins -> {
				if (dustbins.isEmpty()) {
					return;
				}
				boolean first = true;
				for (int i = 0; i < dustbins.size(); ++i) {
					if (SMSavedData.getDustbinContainer(i).isEmpty()) {
						continue;
					}
					if (first) {
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
		chunkItemCounts.forEach((chunk, itemCounts) -> itemCounts.forEach((itemKey, count) -> {
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

		instance.setDirty();
	}

	private static void checkAndCollectEntity(MinecraftServer server, ServerLevel serverLevel, @Nullable Entity entity, List<Entity> killedEntities, AtomicInteger blacklistedItems, SMSavedData instance, AtomicInteger droppedItems, Map<LevelChunk, Map<String, Integer>> chunkItemCounts, AtomicInteger extraEntities) {
		if (entity instanceof ItemEntity itemEntity) {
			ItemStack itemStack = itemEntity.getItem();
			ResourceLocation itemKey = server.registryAccess().registryOrThrow(Registries.ITEM).getKey(itemStack.getItem());

			if (itemKey != null) {
				String item = itemKey.toString();

				// Make sure each entity will be processed only once.
				if (!killedEntities.contains(entity)) {
					if (SMCommonConfig.ITEM_BLACKLIST.get().contains(item)) {
						blacklistedItems.addAndGet(itemStack.getCount());
						killedEntities.add(itemEntity);
					} else if (!SMCommonConfig.ITEM_WHITELIST.get().contains(item)) {
						instance.addItemToDustbin(itemStack);
						droppedItems.addAndGet(itemStack.getCount());
						killedEntities.add(itemEntity);

						LevelChunk chunk = serverLevel.getChunkAt(entity.blockPosition());
						chunkItemCounts.computeIfAbsent(chunk, k -> Maps.newHashMap());
						Map<String, Integer> itemCounts = chunkItemCounts.get(chunk);
						itemCounts.put(item, itemCounts.getOrDefault(item, 0) + itemStack.getCount());
					}
				}
			}
		} else if (entity != null) {
			ResourceLocation typeKey = server.registryAccess().registryOrThrow(Registries.ENTITY_TYPE).getKey(entity.getType());
			if (typeKey != null) {
				String type = typeKey.toString();
				if (SMCommonConfig.EXTRA_ENTITY_TYPES.get().contains(type)) {
					extraEntities.incrementAndGet();
					killedEntities.add(entity);
				}
			}
		}
	}
}
