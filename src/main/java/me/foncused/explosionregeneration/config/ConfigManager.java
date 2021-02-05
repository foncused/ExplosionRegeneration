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
	private boolean tntChainingEnabled;
	private int tntChainingMaxFuseTicks;
	private boolean fallingBlocks;
	private Set<Material> filter;
	private Set<String> blacklist;
	private boolean entityProtection;
	private boolean dropsEnabled;
	private double dropsRadius;
	private Set<Material> dropsBlacklist;
	private boolean worldguard;

	public ConfigManager(
		final boolean random,
		final int speed,
		final int delay,
		String particle,
		String sound,
		final boolean tntChainingEnabled,
		final int tntChainingMaxFuseTicks,
		final boolean fallingBlocks,
		final List<String> filter,
		final List<String> blacklist,
		final boolean entityProtection,
		final boolean dropsEnabled,
		final double dropsRadius,
		final List<String> dropsBlacklist,
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
			if(particle.equals("")) {
				particle = null;
			} else {
				this.particle = Particle.valueOf(particle.toUpperCase());
			}
		} catch(final IllegalArgumentException e) {
			this.particle = Particle.VILLAGER_HAPPY;
			ExplosionRenerationUtil.consoleWarning("Set particle to " + particle + " is not safe, reverting to default...");
		}
		ExplosionRenerationUtil.console(particle == null ? "Disabled particle" : "Set particle to " + this.particle.toString());
		try {
			if(sound.isEmpty()) {
				sound = null;
			} else {
				this.sound = Sound.valueOf(sound.toUpperCase());
			}
		} catch(final IllegalArgumentException e) {
			this.sound = Sound.ENTITY_CHICKEN_EGG;
			ExplosionRenerationUtil.consoleWarning("Set sound to " + sound + " is not safe, reverting to default...");
		}
		ExplosionRenerationUtil.console(sound == null ? "Disabled sound" : "Set sound to " + this.sound.toString());
		this.tntChainingEnabled = tntChainingEnabled;
		ExplosionRenerationUtil.console(this.tntChainingEnabled ? "Chaining mode enabled" : "Chaining mode disabled");
		if(this.tntChainingEnabled) {
			if(tntChainingMaxFuseTicks <= 0 || tntChainingMaxFuseTicks > 200) {
				this.tntChainingMaxFuseTicks = 20;
				ExplosionRenerationUtil.consoleWarning("Set chaining max fuse ticks to " + tntChainingMaxFuseTicks + " ticks is not safe, reverting to default...");
			} else {
				this.tntChainingMaxFuseTicks = tntChainingMaxFuseTicks;
			}
			ExplosionRenerationUtil.console("Set chaining max fuse ticks to " + this.tntChainingMaxFuseTicks + " ticks");
		}
		this.fallingBlocks = fallingBlocks;
		ExplosionRenerationUtil.console(this.fallingBlocks ? "Falling blocks enabled" : "Falling blocks disabled");
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
		this.entityProtection = entityProtection;
		ExplosionRenerationUtil.console(this.entityProtection ? "Entities protected" : "Entities unprotected");
		this.dropsEnabled = dropsEnabled;
		ExplosionRenerationUtil.console(this.dropsEnabled ? "Drops enabled" : "Drops disabled");
		if(dropsRadius < 0.0) {
			this.dropsRadius = 4.0;
			ExplosionRenerationUtil.consoleWarning("Set drops radius to " + dropsRadius + " is not safe, reverting to default...");
		} else {
			this.dropsRadius = dropsRadius;
		}
		ExplosionRenerationUtil.console("Set drops radius to " + this.dropsRadius);
		if(this.dropsEnabled) {
			this.dropsBlacklist = new HashSet<>();
			dropsBlacklist.forEach(material -> {
				Material m;
				try {
					m = Material.valueOf(material.toUpperCase());
					this.dropsBlacklist.add(m);
					ExplosionRenerationUtil.console("Material " + m.toString() + " is filtered from item drops");
				} catch(final IllegalArgumentException e) {
					ExplosionRenerationUtil.consoleWarning("Material " + material + " is invalid, skipping...");
				}
			});
			this.dropsBlacklist = Collections.unmodifiableSet(this.dropsBlacklist);
		}
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

	public boolean isTntChainingEnabled() {
		return this.tntChainingEnabled;
	}

	public int getTntChainingMaxFuseTicks() {
		return this.tntChainingMaxFuseTicks;
	}

	public boolean isFallingBlocks() {
		return this.fallingBlocks;
	}

	public Set<Material> getFilter() {
		return Collections.unmodifiableSet(this.filter);
	}

	public Set<String> getBlacklist() {
		return Collections.unmodifiableSet(this.blacklist);
	}

	public boolean isEntityProtection() {
		return this.entityProtection;
	}

	public boolean isDropsEnabled() {
		return this.dropsEnabled;
	}

	public double getDropsRadius() {
		return this.dropsRadius;
	}

	public Set<Material> getDropsBlacklist() {
		return this.dropsBlacklist;
	}

	public boolean isWorldGuard() {
		return this.worldguard;
	}

}
