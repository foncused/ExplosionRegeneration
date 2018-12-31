package me.foncused.explosionregeneration.util;

import org.bukkit.Bukkit;

public class ExplosionRenerationUtil {

	private static final String PREFIX = "[ExplosionRegeneration] ";

	public static void console(final String message) {
		Bukkit.getLogger().info(PREFIX + message);
	}

	public static void consoleWarning(final String message) {
		Bukkit.getLogger().warning(PREFIX + message);
	}

}
