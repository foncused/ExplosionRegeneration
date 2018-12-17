package me.foncused.explosionregeneration.lib;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.List;

public class WorldGuardAPI {

	private static WorldGuard wg;

	public static List<Block> filter(final List<Block> list) {
		final PluginManager pm = Bukkit.getPluginManager();
		if(pm.isPluginEnabled("WorldGuard") && pm.isPluginEnabled("WorldEdit")) {
			final Plugin plugin = pm.getPlugin("WorldGuard");
			if(plugin instanceof WorldGuardPlugin) {
				final FileConfiguration wgConfig = plugin.getConfig();
				if(wgConfig.getBoolean("ignition.block-tnt") || wgConfig.getBoolean("ignition.block-tnt-block-damage")) {
					list.clear();
					return list;
				}
				if(wg == null) {
					wg = WorldGuard.getInstance();
				}
				final List<Block> filter = new ArrayList<>();
				list.forEach(block -> {
					final WorldGuardPlatform wgp = wg.getPlatform();
					final ApplicableRegionSet regions = wgp.getRegionContainer().get(wgp.getWorldByName(block.getWorld().getName())).getApplicableRegions(
							new BlockVector(
									block.getX(),
									block.getY(),
									block.getZ()
							)
					);
					if(testFlagDeny(regions, Flags.CREEPER_EXPLOSION) || testFlagDeny(regions, Flags.OTHER_EXPLOSION) || testFlagDeny(regions, Flags.TNT)) {
						filter.add(block);
					}
				});
				list.removeAll(filter);
			}
		}
		return list;
	}

	private static boolean testFlagDeny(final ApplicableRegionSet regions, final StateFlag flag) {
		return regions.queryState(null, flag) == StateFlag.State.DENY;
	}

}
