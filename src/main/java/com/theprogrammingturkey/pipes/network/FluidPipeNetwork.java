package com.theprogrammingturkey.pipes.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.theprogrammingturkey.pipes.network.filtering.FilterStackItem;
import com.theprogrammingturkey.pipes.network.filtering.InterfaceFilter;
import com.theprogrammingturkey.pipes.network.filtering.InterfaceFilter.DirectionFilter;
import com.theprogrammingturkey.pipes.util.StackInfo;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;

public class FluidPipeNetwork extends PipeNetwork<IFluidHandler>
{
	public FluidPipeNetwork(int networkID, int dimID)
	{
		super(networkID, dimID, NetworkType.FLUID, CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY);
	}

	@Override
	public void processTransfers()
	{
		Map<Fluid, List<StackInfo<IFluidHandler>>> avilable = new HashMap<>();

		//TODO: Should we sort all interfaces? Even ones not configured as both inputs and outputs

		List<InterfaceInfo<IFluidHandler>> sortedInterfaces = new ArrayList<>(interfaces.values());

		Collections.sort(sortedInterfaces, extractPrioritySort);
		for(InterfaceInfo<IFluidHandler> info : sortedInterfaces)
		{
			info.filter.setShowInsertFilter(false);
			if(!info.filter.isEnabled())
				continue;

			FluidStack fs;
			if(info.inv instanceof IFluidTank)
				fs = info.inv.drain(((IFluidTank) info.inv).getCapacity(), false);
			else
				fs = info.inv.drain(Fluid.BUCKET_VOLUME, false);

			if(fs == null)
				continue;
			List<StackInfo<IFluidHandler>> fsInfo = avilable.get(fs.getFluid());
			if(fsInfo == null)
			{
				fsInfo = new ArrayList<StackInfo<IFluidHandler>>();
				avilable.put(fs.getFluid(), fsInfo);
			}
			fsInfo.add(new StackInfo<IFluidHandler>(info.inv, info.filter, fs.amount));
		}

		Collections.sort(sortedInterfaces, insertPrioritySort);
		for(InterfaceInfo<IFluidHandler> info : sortedInterfaces)
		{
			info.filter.setShowInsertFilter(true);
			if(!info.filter.isEnabled())
				continue;
			for(Fluid fluid : avilable.keySet())
			{
				FilterStackItem stack = new FilterStackItem(FluidUtil.getFilledBucket(new FluidStack(fluid, 1)));
				boolean hasStack = info.filter.hasStackInFilter(stack);
				if((hasStack && info.filter.isWhiteList()) || (!hasStack && !info.filter.isWhiteList()))
				{
					//Inserting into a specific inventory
					int stackInfoIndex = 0;
					List<StackInfo<IFluidHandler>> fromStacks = avilable.get(fluid);
					ItemStack toInsert = null;
					for(int j = stackInfoIndex; j < fromStacks.size(); j++)
					{
						StackInfo<IFluidHandler> stackInfo = fromStacks.get(j);
						if(stackInfo.amountLeft != 0 && !info.inv.equals(stackInfo.inv) && wontSendBack(info.filter, stackInfo.filter, fluid, stack))
						{
							toInsert = stack.getAsItemStack();
							toInsert.setCount(stackInfo.amountLeft);

							int amonutUsed = info.inv.fill(stackInfo.getFluidStack(fluid), true);
							if(amonutUsed != 0)
							{
								stackInfo.amountLeft -= amonutUsed;
								if(stackInfo.amountLeft == 0)
									stackInfo.inv.drain(stackInfo.amount, true);
							}
						}
					}
				}
			}
		}
	}

	private boolean wontSendBack(InterfaceFilter toFilter, InterfaceFilter fromFilter, Fluid fluid, FilterStackItem stack)
	{
		DirectionFilter fromOpposite = fromFilter.insertFilter;
		DirectionFilter toOpposite = toFilter.extractFilter;

		if(!fromOpposite.enabled || !toOpposite.enabled)
			return true;

		boolean hasStack = fromOpposite.hasStackInFilter(stack);
		if((hasStack && !fromOpposite.isWhiteList) || (!hasStack && fromOpposite.isWhiteList))
			return true;

		hasStack = toOpposite.hasStackInFilter(stack);
		if((hasStack && !toOpposite.isWhiteList) || (!hasStack && toOpposite.isWhiteList))
			return true;

		if(toOpposite.priority < fromOpposite.priority)
			return true;

		return false;
	}
}
