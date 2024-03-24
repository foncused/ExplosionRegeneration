package me.foncused.explosionregeneration;

import me.foncused.explosionregeneration.config.ConfigManager;
import me.foncused.explosionregeneration.event.Regeneration;
import me.foncused.explosionregeneration.lib.sk89q.WorldGuardHook;
import org.bukkit.plugin.java.JavaPlugin;

public class ExplosionRegeneration extends JavaPlugin {

	public static final String PREFIX = "[ExplosionRegeneration] ";

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
		this.cm = new ConfigManager(this.getConfig());
		this.cm.validate();
	}

	private void registerWorldGuard() {
		if(this.cm.isWorldGuard()) {
			this.worldguard = new WorldGuardHook();
		}
	}

	private void registerEvents() {
		this.getServer().getPluginManager().registerEvents(new Regeneration(this), this);
	}

	public ConfigManager getConfigManager() {
		return this.cm;
	}

	public WorldGuardHook getWorldGuard() {
		return this.worldguard;
	}

}
