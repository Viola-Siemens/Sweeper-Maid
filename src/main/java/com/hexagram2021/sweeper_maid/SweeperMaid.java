package com.hexagram2021.sweeper_maid;

import com.google.common.collect.Lists;
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
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
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

	public SweeperMaid() {
		MinecraftForge.EVENT_BUS.register(this);
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SMCommonConfig.getConfig());
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
		if (SMCommonConfig.ITEM_SWEEP_INTERVAL.get() == 0) {
			return;
		}
		switch (event.phase) {
			case START -> {
				this.sweepTickRemain -= 1;
				if (this.sweepTickRemain <= 0) {
					this.toSweep = true;
					this.sweepTickRemain = SMCommonConfig.ITEM_SWEEP_INTERVAL.get() * SharedConstants.TICKS_PER_SECOND;
				} else if (this.sweepTickRemain == 15 * SharedConstants.TICKS_PER_SECOND || this.sweepTickRemain == 30 * SharedConstants.TICKS_PER_SECOND || this.sweepTickRemain == 60 * SharedConstants.TICKS_PER_SECOND) {
					event.getServer().getPlayerList().getPlayers().forEach(player -> {
						try {
							player.connection.send(new ClientboundSetActionBarTextPacket(ComponentUtils.updateForEntity(
									createCommandSourceStack(player, player.level(), player.blockPosition()),
									Component.literal(SMCommonConfig.MESSAGE_BEFORE_SWEEP_15_30_60.get().replace("$1", String.valueOf(this.sweepTickRemain / SharedConstants.TICKS_PER_SECOND))).withStyle(ChatFormatting.GRAY),
									player, 0
							)));
						} catch (CommandSyntaxException ignored) {
						}
					});
				} else if (this.sweepTickRemain % SharedConstants.TICKS_PER_SECOND == 0 && this.sweepTickRemain / SharedConstants.TICKS_PER_SECOND <= 10) {
					event.getServer().getPlayerList().getPlayers().forEach(player -> {
						try {
							player.connection.send(new ClientboundSetActionBarTextPacket(ComponentUtils.updateForEntity(
									createCommandSourceStack(player, player.level(), player.blockPosition()),
									Component.literal(SMCommonConfig.MESSAGE_BEFORE_SWEEP_1_10.get().replace("$1", String.valueOf(this.sweepTickRemain / SharedConstants.TICKS_PER_SECOND))).withStyle(ChatFormatting.GOLD),
									player, 0
							)));
						} catch (CommandSyntaxException ignored) {
						}
					});
				}
			}
			case END -> {
				if (this.firstTick) {
					this.firstTick = false;
					this.toSweep = false;
				} else if (this.toSweep) {
					this.toSweep = false;

					// 获取所有垃圾桶
					List<SimpleContainer> dustbins = SMSavedData.getDustbins();

					SMSavedData.getInstance().removeAllDustbins();

					AtomicInteger droppedItems = new AtomicInteger();
					AtomicInteger extraEntities = new AtomicInteger();
					AtomicInteger blacklistedItems = new AtomicInteger();
					Map<LevelChunk, Map<String, Integer>> chunkItemCounts = new HashMap<>();

					event.getServer().getAllLevels().forEach(serverLevel -> {
						Iterable<Entity> entities = serverLevel.getAllEntities();
						List<Entity> killedEntities = Lists.newArrayList();

						for (Entity entity : entities) {
							if (entity instanceof ItemEntity itemEntity) {
								ItemStack itemStack = itemEntity.getItem();
								ResourceLocation itemKeyResourceLocation = ForgeRegistries.ITEMS.getKey(itemStack.getItem());

								if (itemKeyResourceLocation != null) {
									String itemKey = itemKeyResourceLocation.toString();

									// 添加处理逻辑，确保每个物品实体只被处理一次
									if (!killedEntities.contains(entity)) {
										if (SMCommonConfig.ITEM_BLACKLIST.get().contains(itemKey)) {
											blacklistedItems.addAndGet(itemStack.getCount());
											killedEntities.add(itemEntity);
										} else {
											SMSavedData.getInstance().addItemToDustbin(itemStack);
											droppedItems.addAndGet(itemStack.getCount());
											killedEntities.add(itemEntity);

											LevelChunk chunk = serverLevel.getChunkAt(entity.blockPosition());
											chunkItemCounts.computeIfAbsent(chunk, k -> new HashMap<>());
											Map<String, Integer> itemCounts = chunkItemCounts.get(chunk);
											itemCounts.put(itemKey, itemCounts.getOrDefault(itemKey, 0) + itemStack.getCount());
										}
									}
								}
							} else if (entity != null) {
								ResourceLocation typeKey = ForgeRegistries.ENTITY_TYPES.getKey(entity.getType());
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

					// 发送消息通知玩家
					event.getServer().getPlayerList().getPlayers().forEach(player -> {
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


					// 动态生成垃圾桶列表信息并发送给每个玩家
					event.getServer().getPlayerList().getPlayers().forEach(player -> {
						MutableComponent message = Component.literal(SMCommonConfig.CHAT_MESSAGE_AFTER_SWEEP.get());

						// 生成每个垃圾桶的命令链接
						for (int i = 0; i < dustbins.size(); i++) {
							final int dustbinIndex = i;
							message = message.append(Component.literal("[" + SMCommonConfig.DUSTBIN_NAME.get() + (dustbinIndex + 1) + "]")
									.withStyle(style -> style.withColor(ChatFormatting.GREEN)
											.withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/sweepermaid dustbin " + dustbinIndex))));

							if (dustbinIndex < dustbins.size() - 1) {
								message = message.append(Component.literal(", "));
							}
						}

						// 发送消息给当前玩家
						player.sendSystemMessage(message);
					});

					// 发送掉落物超量区块信息给每个玩家
					chunkItemCounts.forEach((chunk, itemCounts) -> itemCounts.forEach((itemKey, count) -> {
                        if (count > ITEM_OVERLOAD_THRESHOLD) {
                            BlockPos chunkPos = chunk.getPos().getWorldPosition();
                            String overloadMessageText = SMCommonConfig.OVERLOAD_MESSAGE.get()
                                    .replace("$1", String.valueOf(chunkPos.getX()))
                                    .replace("$2", String.valueOf(chunkPos.getZ()))
                                    .replace("$3", String.valueOf(count))
                                    .replace("$4", itemKey);

                            MutableComponent overloadMessage = Component.literal(overloadMessageText).withStyle(ChatFormatting.BLUE);

                            event.getServer().getPlayerList().getPlayers().forEach(player -> player.sendSystemMessage(overloadMessage));
                        }
                    }));

					// 保存垃圾桶状态
					SMSavedData.getInstance().setDirty();
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
			SMSavedData worldData = world.getDataStorage().computeIfAbsent(SMSavedData::new, SMSavedData::new, SMSavedData.SAVED_DATA_NAME);
			SMSavedData.setInstance(worldData);
		}
	}

	private static CommandSourceStack createCommandSourceStack(Player player, Level level, BlockPos blockPos) {
		return new CommandSourceStack(CommandSource.NULL, Vec3.atCenterOf(blockPos), Vec2.ZERO, (ServerLevel) level, 2, player.getName().getString(), player.getDisplayName(), level.getServer(), player);
	}
}