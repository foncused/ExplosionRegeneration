package me.foncused.explosionregeneration.event;

import org.bukkit.plugin.java.JavaPlugin;

public abstract class AbstractEvent {

	protected static JavaPlugin instance;

	public static void inject(final JavaPlugin instance) {
		AbstractEvent.instance = instance;
	}

}
