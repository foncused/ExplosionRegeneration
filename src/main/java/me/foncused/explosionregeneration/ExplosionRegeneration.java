package me.foncused.explosionregeneration;

import me.foncused.explosionregeneration.command.BlockRegenSpeedCommand;
import me.foncused.explosionregeneration.config.ConfigManager;
import me.foncused.explosionregeneration.event.entity.EntityExplode;
import me.foncused.explosionregeneration.lib.sk89q.WorldGuardHook;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ExplosionRegeneration extends JavaPlugin {

	private WorldGuardHook worldguard;
	private ConfigManager cm;

	@Override
	public void onEnable() {
		this.loadDependencies();
		this.registerConfig();
		this.registerCommands();
		this.registerEvents();
	}

	private void loadDependencies() {
		this.worldguard = new WorldGuardHook();
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
				config.getStringList("filter"),
				config.getStringList("blacklist"),
				config.getBoolean("worldguard", false)
		);
	}

	private void registerCommands() {
		this.getCommand("blockregenspeed").setExecutor(new BlockRegenSpeedCommand(this));
	}

	private void registerEvents() {
		Bukkit.getPluginManager().registerEvents(new EntityExplode(this), this);
	}

	public WorldGuardHook getWorldGuard() {
		return this.worldguard;
	}

	public ConfigManager getConfigManager() {
		return this.cm;
	}

}
