package com.hexagram2021.sweeper_maid.save;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.saveddata.SavedData;

import javax.annotation.Nullable;

public class SMSavedData extends SavedData {
	@Nullable
	private static SMSavedData INSTANCE;
	public static final String SAVED_DATA_NAME = "SweeperMaid-SavedData";

	public final SimpleContainer dustbin = new SimpleContainer(54) {
		@Override
		public void setChanged() {
			super.setChanged();
			if(INSTANCE != null) {
				INSTANCE.setDirty();
			}
		}
	};

	public SMSavedData() {
		super();
	}

	private static final String TAG_ITEMS = "Items";

	public SMSavedData(CompoundTag nbt) {
		this();
		if(nbt.contains(TAG_ITEMS, Tag.TAG_LIST)) {
			synchronized (this.dustbin) {
				this.dustbin.fromTag(nbt.getList(TAG_ITEMS, Tag.TAG_COMPOUND));
			}
		}
	}

	@Override
	public CompoundTag save(CompoundTag nbt) {
		synchronized (this.dustbin) {
			nbt.put(TAG_ITEMS, this.dustbin.createTag());
		}
		return nbt;
	}

	public static void setInstance(SMSavedData in) {
		INSTANCE = in;
	}

	public static SimpleContainer getDustbin() {
		assert INSTANCE != null;
		return INSTANCE.dustbin;
	}
}
