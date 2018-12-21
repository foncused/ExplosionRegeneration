package me.foncused.explosionregeneration.command;

import me.foncused.explosionregeneration.event.entity.EntityExplode;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class BlockRegenSpeedCommand implements CommandExecutor {

	private EntityExplode ee;

	public BlockRegenSpeedCommand(final EntityExplode ee) {
		this.ee = ee;
	}

	public boolean onCommand(final CommandSender sender, final Command cmd, final String label, final String[] args) {
		if(cmd.getName().equalsIgnoreCase("blockregenspeed")) {
			if(sender.hasPermission("explosionregeneration.blockregenspeed")) {
				if(args.length == 1) {
					try {
						final int speed = Integer.parseInt(args[0]);
						if(speed <= 0) {
							sender.sendMessage(ChatColor.RED + "Invalid regeneration speed. Please enter a number greater than or equal to zero.");
							return true;
						}
						if(speed > 200) {
							sender.sendMessage(ChatColor.RED + "Warning - a slow regeneration speed may cause lag on your server. It should be recommended to keep the regeneration speed less than 200 ticks per block.");
						}
						this.ee.setSpeed(speed);
						sender.sendMessage(ChatColor.GREEN + "Block regeneration speed successfully changed to " + ChatColor.YELLOW + speed + ChatColor.GREEN + " ticks!");
					} catch(final Exception e) {
						e.printStackTrace();
						this.printUsage(sender);
					}
				} else {
					this.printUsage(sender);
				}
			} else {
				sender.sendMessage(ChatColor.RED + "You do not have permission to do this!");
			}
		}
		return true;
	}

	private void printUsage(final CommandSender sender) {
		sender.sendMessage(ChatColor.RED + "Incorrect usage. Use /blockregenspeed <speed>!");
	}

}
