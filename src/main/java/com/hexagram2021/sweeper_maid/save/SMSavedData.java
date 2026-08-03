package com.hexagram2021.sweeper_maid.save;

import com.google.common.collect.Lists;
import com.hexagram2021.sweeper_maid.config.SMCommonConfig;
import com.mojang.logging.LogUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.saveddata.SavedData;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * 扫帚女仆模组存档数据类喵~
 * <p>
 * 负责管理垃圾箱数据，包括：
 * <ul>
 *     <li>存储清理后的物品到多个垃圾箱容器中喵~</li>
 *     <li>NBT 数据序列化和反序列化喵~</li>
 *     <li>垃圾箱初始化和动态调整喵~</li>
 *     <li>线程安全的垃圾箱访问喵~</li>
 * </ul>
 * </p>
 *
 * @author liudongyu
 */
public final class SMSavedData extends SavedData {
	private static final Logger LOGGER = LogUtils.getLogger();

	@SuppressWarnings("java:S3008")
	@Nullable
	private static SMSavedData INSTANCE;
	/**
	 * 存档数据名称喵~
	 */
	public static final String SAVED_DATA_NAME = "SweeperMaid-SavedData";
	/**
	 * 单个容器的最大物品槽位数喵~
	 */
	private static final int MAX_CONTAINER_SIZE = 54;

	/**
	 * 垃圾箱容器列表喵~
	 */
	public final List<SimpleContainer> dustbins = Lists.newArrayList();
	// 回收箱：轮换清理时受保护的物品会被移入此容器，不参与轮换。为 null 表示未启用。
	@Nullable
	private SimpleContainer recycle;
	// 已进行的清理次数，用于计算当前轮换代（sweepCount % ROTATION_COUNT）。
	private int sweepCount;

	/**
	 * 默认构造方法喵~
	 */
	public SMSavedData() {
		super();
	}

	/**
	 * NBT 数据标签 - 物品列表喵~
	 */
	private static final String TAG_ITEMS = "Items";
	/**
	 * NBT 数据标签 - 垃圾箱列表喵~
	 */
	private static final String TAG_DUSTBINS = "Dustbins";
	// NBT 数据标签 - 回收箱与清理计数。
	private static final String TAG_RECYCLE = "Recycle";
	private static final String TAG_SWEEP_COUNT = "SweepCount";

	/**
	 * 从 NBT 数据加载存档数据喵~
	 *
	 * @param nbt NBT 标签喵~
	 * @param provider 注册表提供者喵~
	 */
	public SMSavedData(CompoundTag nbt, HolderLookup.Provider provider) {
		this();
		if (nbt.contains(TAG_DUSTBINS, Tag.TAG_LIST)) {
			ListTag dustbinList = nbt.getList(TAG_DUSTBINS, Tag.TAG_COMPOUND);
			synchronized (this.dustbins) {
				this.dustbins.clear();
				for (int i = 0; i < dustbinList.size(); ++i) {
					CompoundTag dustbinTag = dustbinList.getCompound(i);
					SimpleContainer dustbin = createNewDustbin();
					dustbin.fromTag(dustbinTag.getList(TAG_ITEMS, Tag.TAG_COMPOUND), provider);
					this.dustbins.add(dustbin);
				}
			}
		}
		// 读取回收箱与清理计数。
		if (nbt.contains(TAG_RECYCLE, Tag.TAG_COMPOUND)) {
			SimpleContainer loadedRecycle = createNewDustbin();
			loadedRecycle.fromTag(nbt.getCompound(TAG_RECYCLE).getList(TAG_ITEMS, Tag.TAG_COMPOUND), provider);
			this.recycle = loadedRecycle;
		}
		this.sweepCount = nbt.getInt(TAG_SWEEP_COUNT);
	}

	/**
	 * 将存档数据保存到 NBT 标签喵~
	 *
	 * @param nbt NBT 标签喵~
	 * @param provider 注册表提供者喵~
	 * @return 保存后的 NBT 标签喵~
	 */
	@Override
	public CompoundTag save(CompoundTag nbt, HolderLookup.Provider provider) {
		synchronized (this.dustbins) {
			ListTag dustbinList = new ListTag();
			for (SimpleContainer dustbin : this.dustbins) {
				CompoundTag dustbinTag = new CompoundTag();
				dustbinTag.put(TAG_ITEMS, dustbin.createTag(provider));
				dustbinList.add(dustbinTag);
			}
			nbt.put(TAG_DUSTBINS, dustbinList);
		}
		// 保存回收箱与清理计数。
		if (this.recycle != null) {
			CompoundTag recycleTag = new CompoundTag();
			recycleTag.put(TAG_ITEMS, this.recycle.createTag(provider));
			nbt.put(TAG_RECYCLE, recycleTag);
		}
		nbt.putInt(TAG_SWEEP_COUNT, this.sweepCount);
		return nbt;
	}

	/**
	 * 获取存档数据实例喵~
	 *
	 * @return 存档数据实例喵~
	 * @throws NullPointerException 如果实例尚未初始化喵~
	 */
	public static SMSavedData getInstance() {
		return Objects.requireNonNull(INSTANCE, "SMSavedData has not been initialized yet!");
	}

	/**
	 * 设置存档数据实例喵~
	 *
	 * @param in 存档数据实例喵~
	 */
	public static void setInstance(SMSavedData in) {
		INSTANCE = in;
	}

	// 每个轮换代包含的垃圾箱数量（x）。
	private static int binsPerRotation() {
		return SMCommonConfig.DUSTBINS_PER_ROTATION.get();
	}

	// 轮换代数量（y）。
	private static int rotationCount() {
		return SMCommonConfig.ROTATION_COUNT.get();
	}

	// 轮换垃圾箱总数（x * y）——配置目标数量，仅用于 initialize 调整列表大小。
	public static int totalDustbinCount() {
		return binsPerRotation() * rotationCount();
	}

	// 当前实际存在的垃圾箱数量——用于所有边界检查与遍历。配置热重载后 initialize 会使其与 totalDustbinCount() 保持一致。
	public static int dustbinCount() {
		return getInstance().dustbins.size();
	}

	// 本次清理使用的轮换代序号。
	private int activeGeneration() {
		return this.sweepCount % rotationCount();
	}

	// 每个轮换代的垃圾箱数量（供反馈消息使用）。
	public int getBinsPerRotation() {
		return binsPerRotation();
	}

	// 轮换代数量（供反馈消息使用）。
	public int getRotationCount() {
		return rotationCount();
	}

	// 本次清理使用的轮换代序号（供反馈消息使用；须在 advanceSweep 之前读取）。
	public int getCurrentGeneration() {
		return activeGeneration();
	}

	/**
	 * 获取指定索引的垃圾箱容器喵~
	 *
	 * @param index 垃圾箱索引喵~
	 * @return 垃圾箱容器喵~
	 */
	public static SimpleContainer getDustbinContainer(int index) {
		return getInstance().dustbins.get(index);
	}

	// 获取回收箱（未启用时为 null）。
	@Nullable
	public SimpleContainer getRecycle() {
		return this.recycle;
	}

	// 开始一次清理前：把即将复用的当前轮换代中的受保护物品移入回收箱，然后清空该代，供本次清理写入。返回移入回收箱的物品数量。
	public int beginSweepGeneration() {
		int moved = 0;
		synchronized (this.dustbins) {
			int start = activeGeneration() * binsPerRotation();
			int end = Math.min(start + binsPerRotation(), this.dustbins.size());
			for (int i = start; i < end; ++i) {
				moved += clearBinWithRescue(this.dustbins.get(i), false);
			}
		}
		this.setDirty();
		return moved;
	}

	// 将物品加入当前轮换代的垃圾箱：从该代第一个垃圾箱起依次尽量放入，一个放不下的余量顺延到下一个垃圾箱。整代都放满时余量被丢弃（与原溢出行为一致）。
	public void addItemToActiveGeneration(ItemStack stack) {
		synchronized (this.dustbins) {
			int start = activeGeneration() * binsPerRotation();
			int end = Math.min(start + binsPerRotation(), this.dustbins.size());
			ItemStack remaining = stack;
			for (int i = start; i < end && !remaining.isEmpty(); ++i) {
				remaining = this.dustbins.get(i).addItem(remaining);
			}
		}
	}

	// 一次清理结束后推进计数，使下次清理使用下一个轮换代。
	public void advanceSweep() {
		this.sweepCount += 1;
		this.setDirty();
	}

	// 清空指定索引的轮换垃圾箱；force 为 false 时先把受保护物品移入回收箱。返回移入回收箱的物品数量。
	public int emptyDustbin(int index, boolean force) {
		int moved = 0;
		synchronized (this.dustbins) {
			if (index >= 0 && index < this.dustbins.size()) {
				moved = clearBinWithRescue(this.dustbins.get(index), force);
			}
		}
		this.setDirty();
		return moved;
	}

	// 清空所有轮换垃圾箱（不含回收箱）。返回移入回收箱的物品总数。
	public int emptyAllDustbins(boolean force) {
		int moved = 0;
		synchronized (this.dustbins) {
			for (SimpleContainer dustbin : this.dustbins) {
				moved += clearBinWithRescue(dustbin, force);
			}
		}
		this.setDirty();
		return moved;
	}

	// 清空回收箱。
	public void emptyRecycle() {
		if (this.recycle != null) {
			this.recycle.clearContent();
			this.setDirty();
		}
	}

	// 清空一个垃圾箱：非 force 且已启用回收箱时，先把受保护物品复制进回收箱，再清空该箱（未放下的部分随清空丢弃）。返回移入回收箱的物品数量。
	private int clearBinWithRescue(SimpleContainer bin, boolean force) {
		int moved = 0;
		if (!force && this.recycle != null) {
			for (int slot = 0; slot < bin.getContainerSize(); ++slot) {
				ItemStack stack = bin.getItem(slot);
				if (!stack.isEmpty() && isProtected(stack)) {
					int before = stack.getCount();
					ItemStack remainder = this.recycle.addItem(stack);
					moved += before - remainder.getCount();
					if (!remainder.isEmpty()) {
						LOGGER.warn("The recycle bin is full; {} protected item(s) of {} could not be rescued and will be deleted.", remainder.getCount(), stack.getItem());
					}
				}
			}
		}
		bin.clearContent();
		return moved;
	}

	// 判断物品是否受保护（稀有度阈值 / 附魔 / 自定义名称 / 物品清单，任一命中即受保护）。
	private static boolean isProtected(ItemStack stack) {
		if (stack.getRarity().compareTo(SMCommonConfig.RECYCLE_PROTECT_RARITY.get()) >= 0) {
			return true;
		}
		if (SMCommonConfig.RECYCLE_PROTECT_ENCHANTED.get()
				&& (!stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty()
				|| !stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty())) {
			return true;
		}
		if (SMCommonConfig.RECYCLE_PROTECT_NAMED.get() && stack.has(DataComponents.CUSTOM_NAME)) {
			return true;
		}
		ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
		return key != null && SMCommonConfig.RECYCLE_PROTECT_ITEMS.get().contains(key.toString());
	}

	/**
	 * 初始化垃圾箱列表喵~
	 * <p>
	 * 根据配置调整垃圾箱数量，确保与配置一致喵~
	 * </p>
	 */
	public static void initialize() {
		if (INSTANCE == null) {
			// 世界尚未加载（例如在主菜单热重载配置）时无实例可初始化，安全跳过。
			return;
		}
		SMSavedData instance = getInstance();
		List<SimpleContainer> dustbins = instance.dustbins;

		//Initialize dustbins.
		synchronized (dustbins) {
			int expectedSize = totalDustbinCount();
			if (dustbins.size() < expectedSize) {
				for (int i = dustbins.size(); i < expectedSize; ++i) {
					dustbins.add(createNewDustbin());
				}
			} else if (dustbins.size() > expectedSize) {
				// 收缩：直接删除多余的垃圾箱及其中的物品，并记录日志。
				int removed = dustbins.size() - expectedSize;
				while (dustbins.size() > expectedSize) {
					dustbins.removeLast();
				}
				LOGGER.warn("Dustbin count shrank; deleted {} dustbin(s) and their contents.", removed);
			}
			// 按配置创建或移除回收箱。
			if (SMCommonConfig.ENABLE_RECYCLE.get()) {
				if (instance.recycle == null) {
					instance.recycle = createNewDustbin();
				}
			} else {
				instance.recycle = null;
			}
			// 清理计数对轮换代数取模，避免配置变化后越界。
			instance.sweepCount %= rotationCount();
		}
		instance.setDirty();
	}

	/**
	 * 创建新的垃圾箱容器喵~
	 * <p>
	 * 创建一个具有最大槽位数的简单容器，并在内容变化时标记存档数据为已修改喵~
	 * </p>
	 *
	 * @return 新的垃圾箱容器喵~
	 */
	private static SimpleContainer createNewDustbin() {
		return new SimpleContainer(MAX_CONTAINER_SIZE) {
			@Override
			public void setChanged() {
				super.setChanged();
				if (INSTANCE != null) {
					INSTANCE.setDirty();
				}
			}
		};
	}
}
