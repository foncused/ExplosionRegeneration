package me.foncused.explosionregeneration.event.entity;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class EntityExplode implements Listener {

	private static JavaPlugin instance;
	private static boolean random = true;
	private static int speed = 2;
	private static int delay = 0;
	private static Particle particle = Particle.VILLAGER_HAPPY;
	private static Sound sound = Sound.ENTITY_CHICKEN_EGG;
	private static Set<Material> filter = new HashSet<>();

	public static void inject(final JavaPlugin instance) { EntityExplode.instance = instance; }

	@EventHandler
	public void onEntityExplodeEvent(final EntityExplodeEvent event) {

		final List<Block> list = event.blockList();
		if(list.size() == 0) {
			return;
		}

		final World world = list.get(0).getWorld();
		removeDrops(world, event);
		
		final List<Block> blocks = new ArrayList<>();
		final Map<Block, Material> blockMaterial = new HashMap<>();
		final Map<Block, Location> blockLocation = new HashMap<>();
		final Map<Block, BlockData> blockData = new HashMap<>();
		final Map<Block, String[]> signLines = new HashMap<>();

		for(final Block block : list) {

			block.getDrops().clear();

			final Material material = block.getType();
			if(filter.contains(material)) {
				continue;
			}

			blocks.add(block);
			blockMaterial.put(block, material);
			blockLocation.put(block, block.getLocation());
			blockData.put(block, block.getBlockData());
			if(material == Material.SIGN || material == Material.WALL_SIGN) {
				signLines.put(block, ((Sign) block.getState()).getLines());
			}

		}

		new BukkitRunnable() {
			public void run() {
				try {
					if(blocks.size() == 0) {
						this.cancel();
						return;
					}
					Block block;
					if(random) {
						block = blocks.get(new Random().nextInt(blocks.size()));
					} else {
						int min = 0;
						for(int i = 0; i < blocks.size(); i++) {
							if(blocks.get(i).getY() < blocks.get(min).getY()) {
								min = i;
							}
						}
						block = blocks.get(min);
					}
					final BlockData data = blockData.get(block);
					if(data == null) {
						return;
					}
					final Material material = blockMaterial.get(block);
					final Location location = blockLocation.get(block);
					final Block replace = location.getBlock();
					replace.setType(material);
					replace.setBlockData(data);
					if(material == Material.SIGN || material == Material.WALL_SIGN) {
						final Sign sign = (Sign) block.getState();
						sign.setLine(0, signLines.get(block)[0]);
						sign.setLine(1, signLines.get(block)[1]);
						sign.setLine(2, signLines.get(block)[2]);
						sign.setLine(3, signLines.get(block)[3]);
						sign.update();
					}
					world.playEffect(location, Effect.STEP_SOUND, material == Material.AIR ? block.getType().getId() : material.getId());
					world.spawnParticle(particle, location.add(0, 1, 0), 1, 0, 0, 0);
					world.playSound(location, sound, 1F, 1F);
					blocks.remove(block);
					blockMaterial.remove(block);
					blockLocation.remove(block);
					blockData.remove(block);
					signLines.remove(block);
				} catch(final Exception e) {
					e.printStackTrace();
					this.cancel();
				}
			}
		}.runTaskTimer(instance, delay, speed);

		list.forEach(block -> {
			block.getDrops().clear();
			block.setType(Material.AIR);
		});

		removeDrops(world, event);

	}

	public static void setRandom(final boolean random) {
		EntityExplode.random = random;
	}

	public static void setSpeed(final int speed) {
		EntityExplode.speed = speed;
	}

	public static void setDelay(final int delay) {
		EntityExplode.delay = delay;
	}

	public static void setParticle(final String p) {
		particle = Particle.valueOf(p);
	}

	public static void setSound(final String s) { sound = Sound.valueOf(s); }

	public static void setFilter(final Set<Material> filter) { EntityExplode.filter = filter; }

	private static void removeDrops(final World world, final EntityExplodeEvent event) {
		event.setYield(0);
		new BukkitRunnable() {
			public void run() {
				world.getEntitiesByClass(Item.class).stream().filter(item -> item.getLocation().distance(event.getLocation()) <= 20 && item.getType() == EntityType.DROPPED_ITEM).forEach(Item::remove);
			}
		}.runTaskLater(instance, 5);
	}

}
