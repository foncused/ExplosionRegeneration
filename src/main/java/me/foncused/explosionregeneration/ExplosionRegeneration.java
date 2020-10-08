package me.foncused.explosionregeneration;

import me.foncused.explosionregeneration.command.BlockRegenSpeedCommand;
import me.foncused.explosionregeneration.config.ConfigManager;
import me.foncused.explosionregeneration.event.entity.Regeneration;
import me.foncused.explosionregeneration.lib.sk89q.WorldGuardHook;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ExplosionRegeneration extends JavaPlugin {

	private WorldGuardHook worldguard;
	private ConfigManager cm;

	@Override
	public void onEnable() {
		this.registerConfig();
		this.registerCommands();
		this.registerEvents();
		this.registerWorldGuard();
	}

	private void registerConfig() {
		this.saveDefaultConfig();
		final FileConfiguration config = this.getConfig();
		this.cm = new ConfigManager(
				config.getBoolean("random", true),
				config.getInt("speed", 2),
				config.getInt("delay", 0),
				config.getString("particle", "VILLAGER_HAPPY"),
				config.getString("sound", "ENTITY_CHICKEN_EGG"),
				config.getBoolean("tnt-chaining.enabled", false),
				config.getInt("tnt-chaining.max-fuse-ticks", 40),
				config.getBoolean("falling-blocks", false),
				config.getStringList("filter"),
				config.getStringList("blacklist"),
				config.getBoolean("drops.enabled", false),
				config.getDouble("drops.radius", 4.0),
				config.getStringList("drops.blacklist"),
				config.getBoolean("worldguard", false)
		);
	}

	private void registerCommands() {
		this.getCommand("blockregenspeed").setExecutor(new BlockRegenSpeedCommand(this));
	}

	private void registerEvents() {
		Bukkit.getPluginManager().registerEvents(new Regeneration(this), this);
	}

	private void registerWorldGuard() {
		if(this.cm.isWorldGuard()) {
			this.worldguard = new WorldGuardHook();
		}
	}

	public WorldGuardHook getWorldGuard() {
		return this.worldguard;
	}

	public ConfigManager getConfigManager() {
		return this.cm;
	}

}
