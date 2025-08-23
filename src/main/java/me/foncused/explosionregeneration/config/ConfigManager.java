package me.foncused.explosionregeneration.config;

import me.foncused.explosionregeneration.util.ExplosionRenerationUtil;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ConfigManager {

	private final FileConfiguration config;
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

	public ConfigManager(final FileConfiguration config) {
		this.config = config;
	}

	public void validate() {

		// random
		this.random = this.config.getBoolean("random", true);
		ExplosionRenerationUtil.console(this.random ? "Random mode enabled" : "Random mode disabled");

		// speed
		final int speed = this.config.getInt("speed", 3);
		if(speed <= 0) {
			this.speed = 3;
			ExplosionRenerationUtil.consoleWarning("Set speed to " + speed + " ticks is not safe, reverting to default ...");
		} else {
			this.speed = speed;
		}
		ExplosionRenerationUtil.console("Set speed to " + this.speed + " ticks");

		// delay
		final int delay = this.config.getInt("delay", 0);
		if(delay < 0) {
			this.delay = 0;
			ExplosionRenerationUtil.consoleWarning("Set delay to " + delay + " ticks is not safe, reverting to default ...");
		} else {
			this.delay = delay;
		}
		ExplosionRenerationUtil.console("Set delay to " + this.delay + " ticks");

		// particle
		String particle = this.config.getString("particle", "VILLAGER_HAPPY");
		try {
			if(particle.isEmpty()) {
				particle = null;
			} else {
				this.particle = Particle.valueOf(particle.toUpperCase());
			}
		} catch(final IllegalArgumentException e) {
			this.particle = Particle.HAPPY_VILLAGER;
			ExplosionRenerationUtil.consoleWarning("Set particle to " + particle + " is not safe, reverting to default ...");
		}
		ExplosionRenerationUtil.console(particle == null ? "Disabled particle" : "Set particle to " + this.particle.toString());

		// sound
		String sound = this.config.getString("sound", "ENTITY_CHICKEN_EGG");
		try {
			if(sound.isEmpty()) {
				sound = null;
			} else {
				this.sound = Sound.valueOf(sound.toUpperCase());
			}
		} catch(final IllegalArgumentException e) {
			this.sound = Sound.ENTITY_CHICKEN_EGG;
			ExplosionRenerationUtil.consoleWarning("Set sound to " + sound + " is not safe, reverting to default ...");
		}
		ExplosionRenerationUtil.console(sound == null ? "Disabled sound" : "Set sound to " + this.sound.toString());

		// tnt-chaining.enabled
		this.tntChainingEnabled = this.config.getBoolean("tnt-chaining.enabled", false);
		ExplosionRenerationUtil.console(this.tntChainingEnabled ? "TNT chaining mode enabled" : "TNT chaining mode disabled");

		// tnt-chaining.max-fuse-ticks
		if(this.tntChainingEnabled) {
			final int tntChainingMaxFuseTicks = this.config.getInt("tnt-chaining.max-fuse-ticks", 40);
			if(tntChainingMaxFuseTicks <= 0 || tntChainingMaxFuseTicks > 200) {
				this.tntChainingMaxFuseTicks = 20;
				ExplosionRenerationUtil.consoleWarning("Set TNT chaining max fuse ticks to " + tntChainingMaxFuseTicks + " ticks is not safe, reverting to default ...");
			} else {
				this.tntChainingMaxFuseTicks = tntChainingMaxFuseTicks;
			}
			ExplosionRenerationUtil.console("Set TNT chaining max fuse ticks to " + this.tntChainingMaxFuseTicks + " ticks");
		}

		// falling-blocks
		this.fallingBlocks = this.config.getBoolean("falling-blocks", false);
		ExplosionRenerationUtil.console(this.fallingBlocks ? "Falling blocks enabled" : "Falling blocks disabled");

		// filter
		final List<String> filter = this.config.getStringList("filter");
		this.filter = new HashSet<>();
		filter.forEach(material -> {
			Material m;
			try {
				m = Material.valueOf(material.toUpperCase());
			} catch(final IllegalArgumentException e) {
				ExplosionRenerationUtil.consoleWarning("Material " + material + " is invalid, reverting to default ...");
				m = Material.FIRE;
			}
			this.filter.add(m);
			ExplosionRenerationUtil.console("Material " + m + " is filtered from regeneration");
		});
		this.filter = Collections.unmodifiableSet(this.filter);

		// blacklist
		final List<String> blacklist = this.config.getStringList("blacklist");
		this.blacklist = new HashSet<>();
		this.blacklist.addAll(blacklist);
		this.blacklist = Collections.unmodifiableSet(this.blacklist);

		// entity-protection
		this.entityProtection = this.config.getBoolean("entity-protection", true);
		ExplosionRenerationUtil.console(this.entityProtection ? "Entities protected" : "Entities unprotected");

		// drops.enabled
		this.dropsEnabled = this.config.getBoolean("drops.enabled", true);
		ExplosionRenerationUtil.console(this.dropsEnabled ? "Drops enabled" : "Drops disabled");

		// drops.radius
		final double dropsRadius = this.config.getDouble("drops.radius", 6.0);
		if(dropsRadius < 0.0) {
			this.dropsRadius = 4.0;
			ExplosionRenerationUtil.consoleWarning("Set drops radius to " + dropsRadius + " is not safe, reverting to default ...");
		} else {
			this.dropsRadius = dropsRadius;
		}
		ExplosionRenerationUtil.console("Set drops radius to " + this.dropsRadius);

		// drops.blacklist
		if(this.dropsEnabled) {
			final List<String> dropsBlacklist = this.config.getStringList("drops.blacklist");
			this.dropsBlacklist = new HashSet<>();
			dropsBlacklist.forEach(material -> {
				Material m;
				try {
					m = Material.valueOf(material.toUpperCase());
					this.dropsBlacklist.add(m);
					ExplosionRenerationUtil.console("Material " + m + " is filtered from item drops");
				} catch(final IllegalArgumentException e) {
					ExplosionRenerationUtil.consoleWarning("Material " + material + " is invalid, skipping ...");
				}
			});
			this.dropsBlacklist = Collections.unmodifiableSet(this.dropsBlacklist);
		}

		// worldguard
		this.worldguard = this.config.getBoolean("worldguard", false);
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
