package com.theprogrammingturkey.pipes.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.Level;

import com.theprogrammingturkey.pipes.PipesCore;
import com.theprogrammingturkey.pipes.util.RegistryHelper;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class PipeNetworkManager
{
	public static final PipeNetworkManager ITEM_NETWORK = new PipeNetworkManager(NetworkType.ITEM);
	public static final PipeNetworkManager FLUID_NETWORK = new PipeNetworkManager(NetworkType.FLUID);

	public int nextID = 0;
	public Map<Integer, PipeNetwork> networks = new HashMap<Integer, PipeNetwork>();
	public Map<Long, Integer> posToNetworkID = new HashMap<Long, Integer>();
	public Map<Integer, Integer> idForwardingTable = new HashMap<Integer, Integer>();

	private NetworkType type;

	public PipeNetworkManager(NetworkType type)
	{
		this.type = type;
	}

	public void tick()
	{
		List<Integer> deadNetworks = new ArrayList<>();
		for(Entry<Integer, PipeNetwork> network : networks.entrySet())
		{
			if(network.getValue().isActive())
				network.getValue().tick();
			else
				deadNetworks.add(network.getKey());
		}

		for(Integer i : deadNetworks)
			networks.remove(i);

		deadNetworks.clear();
	}

	public PipeNetwork addPipeToNetwork(World world, BlockPos pos)
	{
		List<PipeNetwork> adjacentNetworks = new ArrayList<>();
		for(EnumFacing side : EnumFacing.VALUES)
		{
			BlockPos offset = pos.offset(side);
			IBlockState neighbor = world.getBlockState(offset);
			if(areBlockAndTypeEqual(neighbor.getBlock()))
			{
				PipeNetwork network = getNetwork(offset);
				if(network != null)
					adjacentNetworks.add(network);
			}
		}

		if(adjacentNetworks.size() == 0)
		{
			PipeNetwork newNetwork = new PipeNetwork(nextID++, type);
			networks.put(newNetwork.getNetworkID(), newNetwork);
			addPosToNetwork(newNetwork, pos);
			return newNetwork;
		}
		else if(adjacentNetworks.size() == 1)
		{
			addPosToNetwork(adjacentNetworks.get(0), pos);
			return adjacentNetworks.get(0);
		}
		else
		{
			PipeNetwork firstNetwork = adjacentNetworks.get(0);
			for(int i = 1; i < adjacentNetworks.size(); i++)
			{
				PipeNetwork otherNetwork = adjacentNetworks.get(i);
				if(!firstNetwork.equals(otherNetwork))
					mergeNetworks(firstNetwork, otherNetwork);
			}

			addPosToNetwork(firstNetwork, pos);
			return firstNetwork;
		}
	}

	public boolean areBlockAndTypeEqual(Block block)
	{
		if(type == NetworkType.ITEM)
			return block.equals(RegistryHelper.ITEM_PIPE);
		else if(type == NetworkType.FLUID)
			return block.equals(RegistryHelper.FLUID_PIPE);
		return false;
	}

	public void addPosToNetwork(PipeNetwork network, BlockPos pos)
	{
		network.addBlockPosToNetwork(pos);
		posToNetworkID.put(pos.toLong(), network.getNetworkID());
	}

	public void removePipeFromNetwork(World world, BlockPos pos)
	{
		PipeNetwork network = this.getNetwork(pos);
		if(network == null)
			return;
		List<BlockPos> adjecentPipes = new ArrayList<BlockPos>();
		for(EnumFacing side : EnumFacing.VALUES)
		{
			BlockPos offset = pos.offset(side);
			IBlockState neighbor = world.getBlockState(offset);
			if(areBlockAndTypeEqual(neighbor.getBlock()))
				adjecentPipes.add(offset);
		}

		if(adjecentPipes.size() == 0)
		{
			this.removePosFromNetwork(network, pos);
			deleteNetwork(network);
		}
		else if(adjecentPipes.size() == 1)
		{
			PipeNetwork adjNetwork = this.getNetwork(adjecentPipes.get(0));
			if(adjNetwork != null)
				this.removePosFromNetwork(adjNetwork, pos);
		}
		else
		{
			boolean firstTry = true;
			PipeNetwork origNetwork = this.getNetwork(adjecentPipes.get(0));
			PipeNetwork newNetwork = null;
			while(adjecentPipes.size() > 0)
			{
				if(!firstTry)
				{
					newNetwork = new PipeNetwork(nextID++, type);
					networks.put(newNetwork.getNetworkID(), newNetwork);
				}
				BlockPos startPos = adjecentPipes.remove(0);
				Map<BlockPos, Float> priorityQueue = new HashMap<>();
				List<BlockPos> visited = new ArrayList<>();
				visited.add(pos);
				priorityQueue.put(startPos, 0f);
				if(!firstTry)
				{
					removePosFromNetwork(origNetwork, startPos);
					addPosToNetwork(newNetwork, startPos);
				}

				while(priorityQueue.size() > 0)
				{
					Entry<BlockPos, Float> nextEntry = null;
					for(Entry<BlockPos, Float> entry : priorityQueue.entrySet())
						if(nextEntry == null || nextEntry.getValue() > entry.getValue())
							nextEntry = entry;

					visited.add(nextEntry.getKey());
					priorityQueue.remove(nextEntry.getKey());

					for(EnumFacing side : EnumFacing.VALUES)
					{
						BlockPos offset = nextEntry.getKey().offset(side);
						if(visited.contains(offset))
							continue;
						else if(adjecentPipes.contains(offset))
						{
							adjecentPipes.remove(offset);
							if(adjecentPipes.size() == 0 && firstTry)
							{
								priorityQueue.clear();
								break;
							}
						}
						IBlockState neighbor = world.getBlockState(offset);
						if(areBlockAndTypeEqual(neighbor.getBlock()))
						{
							float distPts = 0f;
							for(BlockPos goalPos : adjecentPipes)
								distPts += goalPos.getDistance(offset.getX(), offset.getY(), offset.getZ());
							priorityQueue.put(offset, distPts / (float) adjecentPipes.size());
							if(!firstTry)
							{
								removePosFromNetwork(origNetwork, offset);
								addPosToNetwork(newNetwork, offset);
							}
						}
					}
				}
				firstTry = false;
			}
			removePosFromNetwork(origNetwork, pos);
		}
	}

	public void removePosFromNetwork(PipeNetwork network, BlockPos pos)
	{
		network.removeBlockPosFromNetwork(pos);
		posToNetworkID.remove(pos.toLong());
	}

	public void mergeNetworks(PipeNetwork master, PipeNetwork merged)
	{
		master.mergeWithNetwork(merged);
		idForwardingTable.put(merged.getNetworkID(), master.getNetworkID());
		deleteNetwork(merged);
	}

	public void deleteNetwork(PipeNetwork network)
	{
		networks.remove(network.getNetworkID());
		network.deleteNetwork();
	}

	public Integer getNetworkID(BlockPos pos)
	{
		Integer id = posToNetworkID.get(pos.toLong());
		if(id == null)
			return null;

		int originID = id;
		while(idForwardingTable.containsKey(id))
		{
			id = idForwardingTable.get(id);

			//TODO: Possibly throw an error
			if(originID == id)
				break;
		}

		return id;
	}

	public PipeNetwork getNetwork(BlockPos pos)
	{
		Integer id = getNetworkID(pos);
		if(id == null)
			return null;

		return networks.get(id);
	}

	public void purgeForwardingTable()
	{
		PipesCore.logger.log(Level.INFO, "Purging Forwaring Table");
		for(Entry<Long, Integer> blockpos : posToNetworkID.entrySet())
			blockpos.setValue(getNetworkID(BlockPos.fromLong(blockpos.getKey())));
		idForwardingTable.clear();
	}

	public enum NetworkType
	{
		ITEM, FLUID, ENERGY;
	}
}
