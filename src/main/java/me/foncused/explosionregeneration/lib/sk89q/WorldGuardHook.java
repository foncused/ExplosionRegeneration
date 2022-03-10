package me.foncused.explosionregeneration.lib.sk89q;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.internal.platform.WorldGuardPlatform;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.List;

public class WorldGuardHook {

	private final WorldGuard wg;
	private final WorldGuardPlatform platform;

	public WorldGuardHook() {
		this.wg = WorldGuard.getInstance();
		this.platform = this.wg.getPlatform();
	}

	public boolean isInRegion(final String region, final Block block) {
		return this.isInRegion(region, block.getLocation());
	}

	public boolean isInRegion(final String region, final Location location) {
		try {
			return this.platform
					.getRegionContainer()
					.get(BukkitAdapter.adapt(location.getWorld()))
					.getRegion(region)
					.contains(location.getBlockX(), location.getBlockY(), location.getBlockZ());
		} catch(final NullPointerException e) {
			return false;
		}
	}

	public boolean testDeny(final Block block, final StateFlag flag) {
		return this.testDeny(block.getLocation(), flag);
	}

	public boolean testDeny(final Location location, final StateFlag flag) {
		return (!(StateFlag.test(this.platform.getRegionContainer().createQuery().queryState(BukkitAdapter.adapt(location), null, flag))));
	}

	public void getExplosionFiltered(final List<Block> blocks) {
		final PluginManager pm = Bukkit.getPluginManager();
		if(pm.isPluginEnabled("WorldGuard") && pm.isPluginEnabled("WorldEdit")) {
			final Plugin plugin = pm.getPlugin("WorldGuard");
			if(plugin instanceof WorldGuardPlugin) {
				final FileConfiguration config = plugin.getConfig();
				if(config.getBoolean("ignition.block-tnt") || config.getBoolean("ignition.block-tnt-block-damage")) {
					blocks.clear();
				}
				final List<Block> filter = new ArrayList<>();
				blocks.forEach(block -> {
					if(this.testDeny(block, Flags.CREEPER_EXPLOSION) || this.testDeny(block, Flags.OTHER_EXPLOSION)) {
						filter.add(block);
					}
				});
				blocks.removeAll(filter);
			}
		}
	}

}
