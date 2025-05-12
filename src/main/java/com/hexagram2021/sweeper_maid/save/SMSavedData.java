package com.hexagram2021.sweeper_maid.save;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class SMSavedData extends SavedData {
	@Nullable
	private static SMSavedData INSTANCE;
	public static final String SAVED_DATA_NAME = "SweeperMaid-SavedData";
	private static final int MAX_CONTAINER_SIZE = 54;  // 每个垃圾桶的容量

	// 使用List来存储多个垃圾桶
	public final List<SimpleContainer> dustbins = new ArrayList<>();

	public SMSavedData() {
		super();
		// 初始化第一个垃圾桶
		this.dustbins.add(createNewDustbin());
	}

	// 创建新的垃圾桶
	private SimpleContainer createNewDustbin() {
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

	private static final String TAG_ITEMS = "Items";
	private static final String TAG_DUSTBINS = "Dustbins";

	// 加载数据时从NBT恢复所有垃圾桶
	public SMSavedData(CompoundTag nbt) {
		this();
		if (nbt.contains(TAG_DUSTBINS, Tag.TAG_LIST)) {
			ListTag dustbinList = nbt.getList(TAG_DUSTBINS, Tag.TAG_COMPOUND);
			synchronized (this.dustbins) {
				this.dustbins.clear();
				for (int i = 0; i < dustbinList.size(); ++i) {
					CompoundTag dustbinTag = dustbinList.getCompound(i);
					SimpleContainer dustbin = createNewDustbin();
					dustbin.fromTag(dustbinTag.getList(TAG_ITEMS, Tag.TAG_COMPOUND));
					this.dustbins.add(dustbin);
				}
			}
		}
	}

	@Override
	public CompoundTag save(CompoundTag nbt) {
		synchronized (this.dustbins) {
			ListTag dustbinList = new ListTag();
			for (SimpleContainer dustbin : this.dustbins) {
				CompoundTag dustbinTag = new CompoundTag();
				dustbinTag.put(TAG_ITEMS, dustbin.createTag());
				dustbinList.add(dustbinTag);
			}
			nbt.put(TAG_DUSTBINS, dustbinList);
		}
		return nbt;
	}

	// 添加 getInstance 方法
	public static SMSavedData getInstance() {
		if (INSTANCE == null) {
			throw new IllegalStateException("SMSavedData has not been initialized yet!");
		}
		return INSTANCE;
	}


	public static void setInstance(SMSavedData in) {
		INSTANCE = in;
	}

	// 获取所有垃圾桶
	public static List<SimpleContainer> getDustbins() {
		assert INSTANCE != null;
		return INSTANCE.dustbins;
	}

	// 向垃圾桶添加物品
	public void addItemToDustbin(ItemStack stack) {
		synchronized (this.dustbins) {
			// 找到一个没有满的垃圾桶
			for (SimpleContainer dustbin : this.dustbins) {
				if (dustbin.canAddItem(stack)) {
					dustbin.addItem(stack);
					return;
				}
			}
			// 如果所有垃圾桶都已满，创建一个新的垃圾桶
			SimpleContainer newDustbin = createNewDustbin();
			newDustbin.addItem(stack);
			this.dustbins.add(newDustbin);
		}
	}

	public void removeAllDustbins() {
		// 清空所有垃圾桶
		this.dustbins.clear();  // 直接清空垃圾桶列表
		this.setDirty();  // 标记数据为已更改，以便保存
	}


}
