package me.foncused.explosionregeneration.util;

import me.foncused.explosionregeneration.ExplosionRegeneration;
import org.bukkit.Bukkit;

public class ExplosionRenerationUtil {

	public static void console(final String message) {
		Bukkit.getLogger().info(ExplosionRegeneration.PREFIX + message);
	}

	public static void consoleWarning(final String message) {
		Bukkit.getLogger().warning(ExplosionRegeneration.PREFIX + message);
	}

}
