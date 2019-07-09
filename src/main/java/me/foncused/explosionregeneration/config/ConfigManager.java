package me.foncused.explosionregeneration.config;

import me.foncused.explosionregeneration.util.ExplosionRenerationUtil;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigManager {

	private boolean random;
	private int speed;
	private int delay;
	private Particle particle;
	private Sound sound;
	private Set<Material> filter;
	private Set<String> blacklist;
	private boolean worldguard;

	public ConfigManager(
		final boolean random,
		final int speed,
		final int delay,
		final String particle,
		final String sound,
		final List<String> filter,
		final List<String> blacklist,
		final boolean worldguard
	) {
		this.random = random;
		ExplosionRenerationUtil.console(this.random ? "Random mode enabled" : "Random mode disabled");
		if(speed <= 0) {
			this.speed = 10;
			ExplosionRenerationUtil.consoleWarning("Set speed to " + speed + " ticks is not safe, reverting to default...");
		} else {
			this.speed = speed;
		}
		ExplosionRenerationUtil.console("Set speed to " + this.speed + " ticks");
		if(delay < 0) {
			this.delay = 0;
			ExplosionRenerationUtil.consoleWarning("Set delay to " + delay + " ticks is not safe, reverting to default...");
		} else {
			this.delay = delay;
		}
		ExplosionRenerationUtil.console("Set delay to " + this.delay + " ticks");
		try {
			this.particle = Particle.valueOf(particle.toUpperCase());
		} catch(final IllegalArgumentException e) {
			this.particle = Particle.VILLAGER_HAPPY;
			ExplosionRenerationUtil.consoleWarning("Set particle to " + particle + " is not safe, reverting to default...");
		}
		ExplosionRenerationUtil.console("Set particle to " + this.particle.toString());
		try {
			this.sound = Sound.valueOf(sound.toUpperCase());
		} catch(final IllegalArgumentException e) {
			this.sound = Sound.ENTITY_CHICKEN_EGG;
			ExplosionRenerationUtil.consoleWarning("Set sound to " + sound + " is not safe, reverting to default...");
		}
		ExplosionRenerationUtil.console("Set sound to " + this.sound.toString());
		this.filter = new HashSet<>();
		filter.forEach(material -> {
			Material m;
			try {
				m = Material.valueOf(material.toUpperCase());
			} catch(final IllegalArgumentException e) {
				ExplosionRenerationUtil.consoleWarning("Material " + material + " is invalid, reverting to default...");
				m = Material.FIRE;
			}
			this.filter.add(m);
			ExplosionRenerationUtil.console("Material " + m.toString() + " is filtered from regeneration");
		});
		this.filter = Collections.unmodifiableSet(this.filter);
		this.blacklist = new HashSet<>();
		this.blacklist.addAll(blacklist);
		this.blacklist = Collections.unmodifiableSet(this.blacklist);
		this.worldguard = worldguard;
		ExplosionRenerationUtil.console(this.worldguard ? "WorldGuard mode enabled" : "WorldGuard mode disabled");
	}

	public boolean isRandom() {
		return this.random;
	}

	public int getSpeed() {
		return this.speed;
	}

	public void setSpeed(final int speed) {
		this.speed = speed;
	}

	public int getDelay() {
		return this.delay;
	}

	public Particle getParticle() {
		return this.particle;
	}

	public Sound getSound() {
		return this.sound;
	}

	public Set<Material> getFilter() {
		return Collections.unmodifiableSet(this.filter);
	}

	public Set<String> getBlacklist() {
		return Collections.unmodifiableSet(this.blacklist);
	}

	public boolean isWorldGuard() {
		return this.worldguard;
	}

}
