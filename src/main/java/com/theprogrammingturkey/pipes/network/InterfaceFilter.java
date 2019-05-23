package com.theprogrammingturkey.pipes.network;

import java.util.ArrayList;
import java.util.List;

import com.theprogrammingturkey.pipes.network.PipeNetworkManager.NetworkType;
import com.theprogrammingturkey.pipes.util.FilterStack;
import com.theprogrammingturkey.pipes.util.ItemStackHelper;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;

public class InterfaceFilter
{
	public DirectionFilter insertFilter = new DirectionFilter();
	public DirectionFilter extractFilter = new DirectionFilter();

	//Facing direction of the ItemHandler that this is interfacing with
	public EnumFacing facing;

	private boolean showingInsert = true;

	private NetworkType type;

	public InterfaceFilter(EnumFacing facing, NetworkType type)
	{
		this.facing = facing;
		this.type = type;
	}

	public NetworkType getNetworkType()
	{
		return this.type;
	}

	public boolean hasStackInFilter(FilterStack stack)
	{
		if(showingInsert)
			return insertFilter.hasStackInFilter(stack);
		return extractFilter.hasStackInFilter(stack);
	}

	public void addStackToFilter(FilterStack stack)
	{
		if(showingInsert)
			insertFilter.addStackToFilter(stack);
		else
			extractFilter.addStackToFilter(stack);
	}

	public List<FilterStack> getStacks()
	{
		return showingInsert ? insertFilter.getStacks() : extractFilter.getStacks();
	}

	public boolean isWhiteList()
	{
		return showingInsert ? insertFilter.isWhiteList : extractFilter.isWhiteList;
	}

	public void setWhiteList(boolean whiteList)
	{
		if(showingInsert)
			insertFilter.isWhiteList = whiteList;
		else
			extractFilter.isWhiteList = whiteList;
	}

	public boolean isEnabled()
	{
		return showingInsert ? insertFilter.enabled : extractFilter.enabled;
	}

	public void setEnabled(boolean enabled)
	{
		if(showingInsert)
			insertFilter.enabled = enabled;
		else
			extractFilter.enabled = enabled;
	}

	public int getPriority()
	{
		return showingInsert ? insertFilter.priority : extractFilter.priority;
	}

	public void setPriority(int priority)
	{
		if(showingInsert)
			insertFilter.priority = priority;
		else
			extractFilter.priority = priority;
	}

	public void incPriority()
	{
		if(showingInsert)
			insertFilter.priority++;
		else
			extractFilter.priority++;
	}

	public void decPriority()
	{
		if(showingInsert)
			insertFilter.priority--;
		else
			extractFilter.priority--;
	}

	public void setShowInsertFilter(boolean showingInsert)
	{
		this.showingInsert = showingInsert;
	}

	public NBTTagCompound toNBT()
	{
		NBTTagCompound nbt = new NBTTagCompound();
		nbt.setTag("insert", insertFilter.toNBT());
		nbt.setTag("extract", extractFilter.toNBT());
		return nbt;
	}

	public static InterfaceFilter fromNBT(EnumFacing facing, NetworkType type, NBTTagCompound nbt)
	{
		InterfaceFilter filter = new InterfaceFilter(facing, type);
		filter.insertFilter = new DirectionFilter();
		filter.insertFilter.fromNBT(nbt.getCompoundTag("insert"));
		filter.extractFilter = new DirectionFilter();
		filter.extractFilter.fromNBT(nbt.getCompoundTag("extract"));
		return filter;
	}

	public static class DirectionFilter
	{
		public boolean isWhiteList = false;
		public int priority = 0;
		public boolean enabled = true;
		private List<FilterStack> filterStacks = new ArrayList<>();

		public boolean hasStackInFilter(FilterStack stack)
		{
			for(FilterStack s : filterStacks)
				if(ItemStackHelper.areFilterStacksEqual(stack, s))
					return true;
			return false;
		}

		public void addStackToFilter(FilterStack stack)
		{
			if(!hasStackInFilter(stack))
				filterStacks.add(stack);
		}

		public List<FilterStack> getStacks()
		{
			return this.filterStacks;
		}

		public void setStacks(FilterStack stack)
		{
			if(!hasStackInFilter(stack))
				filterStacks.add(stack);
		}

		public NBTTagCompound toNBT()
		{
			NBTTagCompound nbt = new NBTTagCompound();
			nbt.setBoolean("isWhiteList", isWhiteList);
			nbt.setInteger("priority", priority);
			nbt.setBoolean("enabled", enabled);
			NBTTagList filterStacksNBT = new NBTTagList();
			for(FilterStack fs : filterStacks)
				filterStacksNBT.appendTag(fs.getAsItemStack().serializeNBT());
			nbt.setTag("filterStacks", filterStacksNBT);
			return nbt;
		}

		public void fromNBT(NBTTagCompound nbt)
		{
			isWhiteList = nbt.getBoolean("isWhiteList");
			priority = nbt.getInteger("priority");
			enabled = nbt.getBoolean("enabled");

			List<FilterStack> filterStacks = new ArrayList<>();
			for(NBTBase stackNBT : nbt.getTagList("filterStacks", 10))
				filterStacks.add(new FilterStack(new ItemStack((NBTTagCompound) stackNBT)));
			this.filterStacks = filterStacks;
		}
	}
}
