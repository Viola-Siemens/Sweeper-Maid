package com.hexagram2021.sweeper_maid.save;

import com.google.common.collect.Lists;
import com.hexagram2021.sweeper_maid.config.SMCommonConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

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

	/**
	 * 添加物品到垃圾箱喵~
	 * <p>
	 * 遍历所有垃圾箱，找到第一个可以容纳该物品的容器并添加喵~
	 * </p>
	 *
	 * @param stack 要添加的物品堆喵~
	 */
	public void addItemToDustbin(ItemStack stack) {
		synchronized (this.dustbins) {
			// Find the first dustbin that can be added items.
			for (SimpleContainer dustbin: this.dustbins) {
				if (dustbin.canAddItem(stack)) {
					dustbin.addItem(stack);
					return;
				}
			}
		}
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

	/**
	 * 访问垃圾箱列表喵~
	 * <p>
	 * 以线程安全的方式访问垃圾箱列表，并标记数据为已修改喵~
	 * </p>
	 *
	 * @param consumer 访问垃圾箱列表的消费者函数喵~
	 */
	public void accessDustbins(Consumer<List<SimpleContainer>> consumer) {
		synchronized (this.dustbins) {
			consumer.accept(this.dustbins);
		}
		this.setDirty();
	}

	/**
	 * 初始化垃圾箱列表喵~
	 * <p>
	 * 根据配置调整垃圾箱数量，确保与配置一致喵~
	 * </p>
	 */
	public static void initialize() {
		List<SimpleContainer> dustbins = getInstance().dustbins;

		//Initialize dustbins.
		synchronized (dustbins) {
			int expectedSize = SMCommonConfig.DUSTBIN_COUNT.get();
			if(dustbins.size() == expectedSize) {
				return;
			}
			if(dustbins.size() < expectedSize) {
				for(int i = dustbins.size(); i < expectedSize; ++i) {
					dustbins.add(createNewDustbin());
				}
			} else {
				do {
					dustbins.removeLast();
				} while(dustbins.size() > expectedSize);
			}
		}
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
