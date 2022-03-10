package me.foncused.explosionregeneration;

import me.foncused.explosionregeneration.config.ConfigManager;
import me.foncused.explosionregeneration.event.Regeneration;
import me.foncused.explosionregeneration.lib.sk89q.WorldGuardHook;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ExplosionRegeneration extends JavaPlugin {

	private ConfigManager cm;
	private WorldGuardHook worldguard;

	@Override
	public void onEnable() {
		this.registerConfig();
		this.registerWorldGuard();
		this.registerEvents();
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
				config.getBoolean("entity-protection", true),
				config.getBoolean("drops.enabled", false),
				config.getDouble("drops.radius", 6.0),
				config.getStringList("drops.blacklist"),
				config.getBoolean("worldguard", false)
		);
	}

	private void registerWorldGuard() {
		if(this.cm.isWorldGuard()) {
			this.worldguard = new WorldGuardHook();
		}
	}

	private void registerEvents() {
		Bukkit.getPluginManager().registerEvents(new Regeneration(this), this);
	}

	public ConfigManager getConfigManager() {
		return this.cm;
	}

	public WorldGuardHook getWorldGuard() {
		return this.worldguard;
	}

}
