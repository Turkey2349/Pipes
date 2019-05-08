package com.theprogrammingturkey.pipes.blocks;

import com.theprogrammingturkey.pipes.network.PipeNetworkManager;
import com.theprogrammingturkey.pipes.util.RegistryHelper;

import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class ItemPipeBlock extends BasePipeBlock
{
	public ItemPipeBlock()
	{
		super("item_pipe");
		this.setDefaultState(this.blockState.getBaseState().withProperty(NORTH, EnumAttachType.NONE).withProperty(EAST, EnumAttachType.NONE).withProperty(SOUTH, EnumAttachType.NONE).withProperty(WEST, EnumAttachType.NONE).withProperty(UP, EnumAttachType.NONE).withProperty(DOWN, EnumAttachType.NONE));
	}

	@Override
	public IBlockState getActualState(IBlockState state, IBlockAccess world, BlockPos origin)
	{
		for(EnumFacing side : EnumFacing.VALUES)
		{
			BlockPos offset = origin.offset(side);
			IBlockState neighbor = world.getBlockState(offset);
			if(neighbor.getBlock().equals(RegistryHelper.ITEM_PIPE))
				state = state.withProperty(FACING_MAPPING.get(side), EnumAttachType.PIPE);
			else if(neighbor.getBlock().hasTileEntity(state) && world.getTileEntity(offset) instanceof IInventory)
				state = state.withProperty(FACING_MAPPING.get(side), EnumAttachType.INVENTORY);
		}
		return state;
	}

	@Override
	public boolean removedByPlayer(IBlockState state, World world, BlockPos pos, EntityPlayer player, boolean willHarvest)
	{
		if(!world.isRemote)
			PipeNetworkManager.ITEM_NETWORK.removePipeFromNetwork(world, pos);
		return super.removedByPlayer(state, world, pos, player, willHarvest);
	}

	@Override
	public void onBlockPlacedBy(World world, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack)
	{
		if(!world.isRemote)
			PipeNetworkManager.ITEM_NETWORK.addPipeToNetwork(world, pos);
	}
}
