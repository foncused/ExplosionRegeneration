package me.foncused.explosionregeneration;

import me.foncused.explosionregeneration.command.BlockRegenSpeedCommand;
import me.foncused.explosionregeneration.event.entity.EntityExplode;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ExplosionRegeneration extends JavaPlugin {

	private EntityExplode ee;

	@Override
	public void onEnable() {
		this.registerConfig();
		this.registerEvents();
		this.registerCommands();
	}

	private void registerConfig() {
		this.saveDefaultConfig();
	}

	private void registerEvents() {
		this.ee = new EntityExplode(this);
		final FileConfiguration config = this.getConfig();
		this.ee.setRandom(config.getBoolean("random"));
		this.ee.setSpeed(config.getInt("speed"));
		this.ee.setDelay(config.getInt("delay"));
		this.ee.setParticle(config.getString("particle"));
		this.ee.setSound(config.getString("sound"));
		final Set<Material> filter = new HashSet<>();
		config.getStringList("filter").forEach(s -> filter.add(Material.valueOf(s)));
		this.ee.setFilter(Collections.unmodifiableSet(filter));
		final Set<String> blacklist = new HashSet<>();
		config.getStringList("blacklist").forEach(blacklist::add);
		this.ee.setBlacklist(Collections.unmodifiableSet(blacklist));
		this.ee.setWorldGuard(config.getBoolean("worldguard"));
		Bukkit.getPluginManager().registerEvents(this.ee, this);
	}

	private void registerCommands() {
		this.getCommand("blockregenspeed").setExecutor(new BlockRegenSpeedCommand(this.ee));
	}

}
