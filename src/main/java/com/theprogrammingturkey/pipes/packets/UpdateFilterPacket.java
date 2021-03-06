package com.theprogrammingturkey.pipes.packets;

import com.theprogrammingturkey.pipes.network.IPipeNetwork;
import com.theprogrammingturkey.pipes.network.NetworkType;
import com.theprogrammingturkey.pipes.network.PipeNetworkManager;
import com.theprogrammingturkey.pipes.network.filtering.FilterStackItem;
import com.theprogrammingturkey.pipes.network.filtering.IFilterStack;
import com.theprogrammingturkey.pipes.network.filtering.InterfaceFilter;

import io.netty.buffer.ByteBuf;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class UpdateFilterPacket implements IMessage
{
	private InterfaceFilter filter;
	private BlockPos pos;

	public UpdateFilterPacket()
	{
	}

	/**
	 * 
	 * @param pos
	 *            Position of the pipe block/ block holding the filter
	 * @param facing
	 *            The interfacing face of the ItemHandler
	 */
	public UpdateFilterPacket(BlockPos pos, InterfaceFilter filter)
	{
		this.pos = pos;
		this.filter = filter;
	}

	@Override
	public void toBytes(ByteBuf buf)
	{
		buf.writeInt(pos.getX());
		buf.writeInt(pos.getY());
		buf.writeInt(pos.getZ());

		buf.writeByte(filter.facing.getIndex());
		buf.writeInt(filter.getNetworkType().getID());

		buf.writeBoolean(filter.extractFilter.enabled);
		buf.writeInt(filter.extractFilter.priority);
		buf.writeBoolean(filter.extractFilter.isWhiteList);
		buf.writeInt(filter.extractFilter.getStacks().size());
		for(IFilterStack stack : filter.extractFilter.getStacks())
			ByteBufUtils.writeTag(buf, stack.serializeNBT());

		buf.writeBoolean(filter.insertFilter.enabled);
		buf.writeInt(filter.insertFilter.priority);
		buf.writeBoolean(filter.insertFilter.isWhiteList);
		buf.writeInt(filter.insertFilter.getStacks().size());
		for(IFilterStack stack : filter.insertFilter.getStacks())
			ByteBufUtils.writeTag(buf, stack.serializeNBT());
	}

	@Override
	public void fromBytes(ByteBuf buf)
	{
		this.pos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());

		filter = new InterfaceFilter(EnumFacing.VALUES[buf.readByte()], NetworkType.getFromID(buf.readInt()));

		filter.extractFilter.enabled = buf.readBoolean();
		filter.extractFilter.priority = buf.readInt();
		filter.extractFilter.isWhiteList = buf.readBoolean();
		int amount = buf.readInt();
		//TODO: Don't use FilterStackItem
		for(int i = 0; i < amount; i++)
			filter.extractFilter.addStackToFilter(new FilterStackItem(ByteBufUtils.readItemStack(buf)));

		filter.insertFilter.enabled = buf.readBoolean();
		filter.insertFilter.priority = buf.readInt();
		filter.insertFilter.isWhiteList = buf.readBoolean();
		amount = buf.readInt();
		for(int i = 0; i < amount; i++)
			filter.insertFilter.addStackToFilter(new FilterStackItem(ByteBufUtils.readItemStack(buf)));
	}

	public static final class Handler implements IMessageHandler<UpdateFilterPacket, IMessage>
	{
		@Override
		public IMessage onMessage(UpdateFilterPacket message, MessageContext ctx)
		{
			World world = ctx.getServerHandler().player.world;
			PipeNetworkManager networkManager = PipeNetworkManager.getNetworkManagerForType(message.filter.getNetworkType());
			IPipeNetwork network = networkManager.getNetwork(message.pos, world.provider.getDimension());
			if(network == null)
				return null;
			network.updateFilter(message.pos, message.filter);
			return null;
		}
	}
}
