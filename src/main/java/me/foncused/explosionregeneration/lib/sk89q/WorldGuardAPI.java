package me.foncused.explosionregeneration.lib.sk89q;

public class WorldGuardAPI {

	/*private static WorldGuard wg;

	public static List<Block> filter(final List<Block> list) {
		final PluginManager pm = Bukkit.getPluginManager();
		if(pm.isPluginEnabled("WorldGuard") && pm.isPluginEnabled("WorldEdit")) {
			final Plugin plugin = pm.getPlugin("WorldGuard");
			if(plugin instanceof WorldGuardPlugin) {
				final FileConfiguration wgConfig = plugin.getConfig();
				if(wgConfig.getBoolean("ignition.block-tnt") || wgConfig.getBoolean("ignition.block-tnt-block-damage")) {
					list.clear();
					return list;
				}
				if(wg == null) {
					wg = WorldGuard.getInstance();
				}
				final List<Block> filter = new ArrayList<>();
				list.forEach(block -> {
					final WorldGuardPlatform wgp = wg.getPlatform();
					if(testFlagDeny(wgp, block, Flags.CREEPER_EXPLOSION) || testFlagDeny(wgp, block, Flags.OTHER_EXPLOSION)*//* || testFlagDeny(wgp, block, Flags.TNT)*//*) {
						filter.add(block);
					}
				});
				list.removeAll(filter);
			}
		}
		return Collections.unmodifiableList(list);
	}

	private static boolean testFlagDeny(final WorldGuardPlatform wgp, final Block block, final StateFlag flag) {
		return (!(StateFlag.test(wgp.getRegionContainer().createQuery().queryState(BukkitAdapter.adapt(block.getLocation()), null, flag))));
	}*/

}
