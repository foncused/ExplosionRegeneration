package me.foncused.explosionregeneration.utility;

import org.bukkit.Bukkit;

public class ExplosionRenerationUtilities {

	private static final String PREFIX = "[ExplosionRegeneration] ";

	public static void console(final String message) {
		Bukkit.getLogger().info(PREFIX + message);
	}

	public static void consoleWarning(final String message) {
		Bukkit.getLogger().warning(PREFIX + message);
	}

}
