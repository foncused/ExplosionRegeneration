package me.foncused.explosionregeneration;

import me.foncused.explosionregeneration.command.BlockRegenSpeedCommand;
import me.foncused.explosionregeneration.event.entity.EntityExplode;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ExplosionRegeneration extends JavaPlugin {

	private EntityExplode ee;
	private final String PREFIX = "[ExplosionRegeneration] ";

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
		final FileConfiguration config = this.getConfig();
		final boolean random = config.getBoolean("random");
		this.console(random ? "Random mode activated" : "Random mode deactivated");
		int speed = config.getInt("speed");
		if(speed <= 0) {
			this.consoleWarning("Set speed to " + speed + " ticks is not safe, reverting to default...");
			speed = 10;
		}
		this.console("Set speed to " + speed + " ticks");
		int delay = config.getInt("delay");
		if(delay < 0) {
			this.consoleWarning("Set delay to " + delay + " ticks is not safe, reverting to default...");
			delay = 0;
		}
		this.console("Set delay to " + delay + " ticks");
		final String particle = config.getString("particle");
		Particle p;
		try {
			p = Particle.valueOf(particle.toUpperCase());
		} catch(final IllegalArgumentException e) {
			this.consoleWarning("Set particle to " + particle + " is not safe, reverting to default...");
			p = Particle.VILLAGER_HAPPY;
		}
		this.console("Set particle to " + p.toString());
		final String sound = config.getString("sound");
		Sound s;
		try {
			s = Sound.valueOf(sound.toUpperCase());
		} catch(final IllegalArgumentException e) {
			this.consoleWarning("Set sound to " + sound + " is not safe, reverting to default...");
			s = Sound.ENTITY_CHICKEN_EGG;
		}
		this.console("Set sound to " + s.toString());
		final Set<Material> filter = new HashSet<>();
		config.getStringList("filter").forEach(m -> filter.add(Material.valueOf(m)));
		final Set<String> blacklist = new HashSet<>();
		config.getStringList("blacklist").forEach(blacklist::add);
		final boolean wg = config.getBoolean("worldguard");
		this.console(wg ? "WorldGuard mode activated" : "WorldGuard mode deactivated");
		this.ee = new EntityExplode(
				this,
				random,
				speed,
				delay,
				p,
				s,
				Collections.unmodifiableSet(filter),
				Collections.unmodifiableSet(blacklist),
				wg
		);
		Bukkit.getPluginManager().registerEvents(this.ee, this);
	}

	private void registerCommands() {
		this.getCommand("blockregenspeed").setExecutor(new BlockRegenSpeedCommand(this.ee));
	}

	private void console(final String message) {
		Bukkit.getLogger().info(this.PREFIX + message);
	}

	private void consoleWarning(final String message) {
		Bukkit.getLogger().warning(this.PREFIX + message);
	}

}
