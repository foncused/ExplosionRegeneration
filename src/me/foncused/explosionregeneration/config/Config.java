package me.foncused.explosionregeneration.config;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Logger;

public final class Config {

	private Config() {
		throw new IllegalStateException();
	}

	public static void createConfig(final JavaPlugin instance) {
		try {
			final File f = instance.getDataFolder();
			if(!(f.exists())) {
				f.mkdirs();
			}
			final Logger logger = instance.getLogger();
			if(!(new File(f, "config.yml").exists())) {
				logger.info("Generating default configuration...");
				instance.saveDefaultConfig();
			} else {
				logger.info("Loading configuration");
			}
		} catch(final Exception e) {
			e.printStackTrace();
		}
	}

}
