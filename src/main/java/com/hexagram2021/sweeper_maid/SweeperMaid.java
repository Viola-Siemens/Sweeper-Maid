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
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
@Mod(SweeperMaid.MODID)
public class SweeperMaid {
	public static final String MODID = "sweeper_maid";
	public static final String MODNAME = "Sweeper Maid";
	public static final String VERSION = ModList.get().getModFileById(MODID).versionString();

	private static int ITEM_OVERLOAD_THRESHOLD;

	public static Runnable clean = () -> {};

	public SweeperMaid() {
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SMCommonConfig.getConfig());
		NeoForge.EVENT_BUS.register(this);

		clean = () -> this.sweepTickRemain = 0;
	}

	private int sweepTickRemain = 0;
	private boolean toSweep = false;
	private boolean firstTick = true;

	@SubscribeEvent
	public void registerCommands(RegisterCommandsEvent event) {
		final CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
		dispatcher.register(SMCommands.register());
	}

	@SubscribeEvent
	public void onTick(TickEvent.ServerTickEvent event) {
		MinecraftServer server = event.getServer();
		if(SMCommonConfig.ITEM_SWEEP_INTERVAL.get() == 0) {
			return;
		}
		switch (event.phase) {
			case START -> this.prepareAndSendCountdownMessage(server);
			case END -> {
				if(this.firstTick) {
					this.firstTick = false;
					this.toSweep = false;
					SMSavedData.initialize();
				} else if(this.toSweep) {
					this.toSweep = false;
					doSweeping(server);
				}
			}
		}
	}

	@SubscribeEvent
	public void onServerStarted(ServerStartedEvent event) {
		ITEM_OVERLOAD_THRESHOLD = SMCommonConfig.ITEM_OVERLOAD_THRESHOLD.get();
		ServerLevel world = event.getServer().getLevel(Level.OVERWORLD);
		assert world != null;
		if (!world.isClientSide) {
			SMSavedData worldData = world.getDataStorage().computeIfAbsent(new SavedData.Factory<>(SMSavedData::new, SMSavedData::new), SMSavedData.SAVED_DATA_NAME);
			SMSavedData.setInstance(worldData);
		}
	}

	private static CommandSourceStack createCommandSourceStack(Player player, Level level, BlockPos blockPos) {
		return new CommandSourceStack(CommandSource.NULL, Vec3.atCenterOf(blockPos), Vec2.ZERO, (ServerLevel) level, 2, player.getName().getString(), player.getDisplayName(), level.getServer(), player);
	}

	private static void broadcastToAdmins(MinecraftServer server, Component message) {
		if (server.getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
			for(ServerPlayer serverplayer : server.getPlayerList().getPlayers()) {
				if (server.getPlayerList().isOp(serverplayer.getGameProfile())) {
					serverplayer.sendSystemMessage(message);
				}
			}
		}
	}

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
							Component.literal(SMCommonConfig.MESSAGE_BEFORE_SWEEP_15_30_60.get().replaceAll("\\$1", String.valueOf(this.sweepTickRemain / SharedConstants.TICKS_PER_SECOND))).withStyle(ChatFormatting.GRAY),
							player, 0
					)));
				} catch (CommandSyntaxException ignored) {
				}
			});
		} else if(this.sweepTickRemain % SharedConstants.TICKS_PER_SECOND == 0 && this.sweepTickRemain / SharedConstants.TICKS_PER_SECOND <= 10) {
			server.getPlayerList().getPlayers().forEach(player -> {
				try {
					player.connection.send(new ClientboundSetActionBarTextPacket(ComponentUtils.updateForEntity(
							createCommandSourceStack(player, player.level(), player.blockPosition()),
							Component.literal(SMCommonConfig.MESSAGE_BEFORE_SWEEP_1_10.get().replaceAll("\\$1", String.valueOf(this.sweepTickRemain / SharedConstants.TICKS_PER_SECOND))).withStyle(ChatFormatting.GOLD),
							player, 0
					)));
				} catch (CommandSyntaxException ignored) {
				}
			});
		}
	}

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
							} else if(!SMCommonConfig.ITEM_WHITELIST.get().contains(item)) {
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
				} else if(entity != null) {
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

			killedEntities.forEach(Entity::discard);
		});

		server.getPlayerList().getPlayers().forEach(player -> {
			try {
				player.connection.send(new ClientboundSetActionBarTextPacket(ComponentUtils.updateForEntity(
						createCommandSourceStack(player, player.level(), player.blockPosition()),
						Component.literal(SMCommonConfig.MESSAGE_AFTER_SWEEP.get()
										.replace("$1", droppedItems.toString())
										.replace("$2", extraEntities.toString())
										.replace("$3", blacklistedItems.toString()))  // 增加黑名单物品统计
								.withStyle(ChatFormatting.AQUA),
						player, 0
				)));
			} catch (CommandSyntaxException ignored) {
			}
		});


		// Generate all dustbin messages and links
		server.getPlayerList().getPlayers().forEach(player -> {
			MutableComponent message = Component.literal(SMCommonConfig.CHAT_MESSAGE_AFTER_SWEEP.get());

			instance.accessDustbins(dustbins -> {
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
		chunkItemCounts.forEach((chunk, itemCounts) -> itemCounts.forEach((itemKey, count) -> {
			if (count > ITEM_OVERLOAD_THRESHOLD) {
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
}
