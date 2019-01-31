package me.foncused.explosionregeneration;

import me.foncused.explosionregeneration.command.BlockRegenSpeedCommand;
import me.foncused.explosionregeneration.config.ConfigManager;
import me.foncused.explosionregeneration.event.entity.EntityExplode;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class ExplosionRegeneration extends JavaPlugin {

	private ConfigManager cm;

	@Override
	public void onEnable() {
		this.registerConfig();
		this.registerCommands();
		this.registerEvents();
	}

	private void registerConfig() {
		this.saveDefaultConfig();
		final FileConfiguration config = this.getConfig();
		this.cm = new ConfigManager(
				config.getBoolean("random"),
				config.getInt("speed"),
				config.getInt("delay"),
				config.getString("particle"),
				config.getString("sound"),
				config.getStringList("filter"),
				config.getStringList("blacklist"),
				config.getBoolean("worldguard")
		);
	}

	private void registerCommands() {
		this.getCommand("blockregenspeed").setExecutor(new BlockRegenSpeedCommand(this.cm));
	}

	private void registerEvents() {
		Bukkit.getPluginManager().registerEvents(new EntityExplode(this), this);
	}

	public ConfigManager getConfigManager() {
		return this.cm;
	}

}
