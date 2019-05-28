package com.theprogrammingturkey.pipes.commands;

import net.minecraft.block.state.IBlockState;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

public class PipesCommands extends CommandBase
{

	@Override
	public String getName()
	{
		return "pipes";
	}

	@Override
	public String getUsage(ICommandSender sender)
	{
		return "/pipes";
	}

	@Override
	public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException
	{
		if(args.length == 0)
			return;
		if(args[0].equalsIgnoreCase("test"))
		{
			BlockPos below = sender.getPosition().add(0, -1, 0);
			IBlockState state = sender.getEntityWorld().getBlockState(below);
			if(state.getBlock().hasTileEntity(state))
			{
				TileEntity te = sender.getEntityWorld().getTileEntity(below);
				sender.sendMessage(new TextComponentString("TE found!" + te.hashCode()));
			}
			else
			{
				sender.sendMessage(new TextComponentString("No TE below!"));
			}
		}
	}

}