package me.foncused.explosionregeneration.config;

import me.foncused.explosionregeneration.utility.ExplosionRenerationUtilities;
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

	public ConfigManager(final boolean random, final int speed, final int delay, final String particle, final String sound, final List<String> filter, final List<String> blacklist, final boolean worldguard) {
		this.random = random;
		this.setSpeed(speed);
		this.setDelay(delay);
		this.setParticle(particle);
		this.setSound(sound);
		this.setFilter(filter);
		this.setBlacklist(blacklist);
		this.worldguard = worldguard;
	}

	public boolean isRandom() {
		return this.random;
	}

	public void setRandom(final boolean random) {
		ExplosionRenerationUtilities.console(random ? "Random mode enabled" : "Random mode disabled");
		this.random = random;
	}

	public int getSpeed() {
		return this.speed;
	}

	public void setSpeed(final int speed) {
		if(speed <= 0) {
			ExplosionRenerationUtilities.consoleWarning("Set speed to " + speed + " ticks is not safe, reverting to default...");
			this.speed = 10;
		} else {
			this.speed = speed;
		}
		ExplosionRenerationUtilities.console("Set speed to " + this.speed + " ticks");
	}

	public int getDelay() {
		return this.delay;
	}

	public void setDelay(final int delay) {
		if(delay < 0) {
			ExplosionRenerationUtilities.consoleWarning("Set delay to " + delay + " ticks is not safe, reverting to default...");
			this.delay = 0;
		} else {
			this.delay = delay;
		}
		ExplosionRenerationUtilities.console("Set delay to " + this.delay + " ticks");
	}

	public Particle getParticle() {
		return this.particle;
	}

	public void setParticle(final String particle) {
		try {
			this.particle = Particle.valueOf(particle.toUpperCase());
		} catch(final IllegalArgumentException e) {
			ExplosionRenerationUtilities.consoleWarning("Set particle to " + particle + " is not safe, reverting to default...");
			this.particle = Particle.VILLAGER_HAPPY;
		}
		ExplosionRenerationUtilities.console("Set particle to " + this.particle.toString());
	}

	public Sound getSound() {
		return this.sound;
	}

	public void setSound(final String sound) {
		try {
			this.sound = Sound.valueOf(sound.toUpperCase());
		} catch(final IllegalArgumentException e) {
			ExplosionRenerationUtilities.consoleWarning("Set sound to " + sound + " is not safe, reverting to default...");
			this.sound = Sound.ENTITY_CHICKEN_EGG;
		}
		ExplosionRenerationUtilities.console("Set sound to " + this.sound.toString());
	}

	public Set<Material> getFilter() {
		return this.filter;
	}

	public void setFilter(final List<String> filter) {
		this.filter = new HashSet<>();
		filter.forEach(material -> {
			Material m;
			try {
				m = Material.valueOf(material.toUpperCase());
			} catch(final IllegalArgumentException e) {
				ExplosionRenerationUtilities.consoleWarning("Material " + material + " is invalid, reverting to default...");
				m = Material.FIRE;
			}
			ExplosionRenerationUtilities.console("Material " + m.toString() + " has filtered from regeneration");
			this.filter.add(m);
		});
		this.filter = Collections.unmodifiableSet(this.filter);
	}

	public Set<String> getBlacklist() {
		return this.blacklist;
	}

	public void setBlacklist(final List<String> blacklist) {
		this.blacklist = new HashSet<>();
		this.blacklist.addAll(blacklist);
		this.blacklist = Collections.unmodifiableSet(this.blacklist);
	}

	public boolean isWorldGuard() {
		return this.worldguard;
	}

	public void setWorldGuard(final boolean worldguard) {
		this.worldguard = worldguard;
	}

}
