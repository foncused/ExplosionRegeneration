package me.foncused.explosionregeneration.main;

import me.foncused.explosionregeneration.command.BlockRegenSpeedCommand;
import me.foncused.explosionregeneration.config.Config;
import me.foncused.explosionregeneration.event.AbstractEvent;
import me.foncused.explosionregeneration.event.entity.EntityExplode;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ExplosionRegeneration extends JavaPlugin {

	@Override
	public void onEnable() {
		this.initialize();
		this.registerCommands();
		this.registerConfig();
		this.registerEvents();
	}

	private void initialize() {
		AbstractEvent.inject(this);
	}

	private void registerCommands() {
		this.getCommand("blockregenspeed").setExecutor(new BlockRegenSpeedCommand());
	}

	private void registerConfig() {
		Config.createConfig(this);
		final FileConfiguration config = this.getConfig();
		EntityExplode.setRandom(config.getBoolean("random"));
		EntityExplode.setSpeed(config.getInt("speed"));
		EntityExplode.setDelay(config.getInt("delay"));
		EntityExplode.setParticle(config.getString("particle"));
		EntityExplode.setSound(config.getString("sound"));
		final Set<Material> filter = new HashSet<>();
		new HashSet<>(config.getStringList("filter")).forEach(s -> filter.add(Material.valueOf(s)));
		EntityExplode.setFilter(Collections.unmodifiableSet(filter));
	}

	private void registerEvents() {
		Bukkit.getPluginManager().registerEvents(new EntityExplode(), this);
	}

}
