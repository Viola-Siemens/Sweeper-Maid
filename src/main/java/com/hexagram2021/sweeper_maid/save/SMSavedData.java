package com.hexagram2021.sweeper_maid.save;

import com.google.common.collect.Lists;
import com.hexagram2021.sweeper_maid.config.SMCommonConfig;
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

public class SMSavedData extends SavedData {
	@Nullable
	private static SMSavedData INSTANCE;
	public static final String SAVED_DATA_NAME = "SweeperMaid-SavedData";
	private static final int MAX_CONTAINER_SIZE = 54;

	public final List<SimpleContainer> dustbins = Lists.newArrayList();

	public SMSavedData() {
		super();
	}

	private static final String TAG_ITEMS = "Items";
	private static final String TAG_DUSTBINS = "Dustbins";

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

	public static SMSavedData getInstance() {
		return Objects.requireNonNull(INSTANCE, "SMSavedData has not been initialized yet!");
	}

	public static void setInstance(SMSavedData in) {
		INSTANCE = in;
	}

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

	public static SimpleContainer getDustbinContainer(int index) {
		return getInstance().dustbins.get(index);
	}

	public void accessDustbins(Consumer<List<SimpleContainer>> consumer) {
		synchronized (this.dustbins) {
			consumer.accept(this.dustbins);
		}
		this.setDirty();
	}

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
					dustbins.remove(dustbins.size() - 1);
				} while(dustbins.size() > expectedSize);
			}
		}
	}

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
