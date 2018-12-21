package me.foncused.explosionregeneration.event.entity;

import me.foncused.explosionregeneration.ExplosionRegeneration;
import me.foncused.explosionregeneration.lib.WorldGuardAPI;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class EntityExplode implements Listener {

	private ExplosionRegeneration plugin;
	private boolean random;
	private int speed;
	private int delay;
	private Particle particle;
	private Sound sound;
	private Set<Material> filter;
	private Set<String> blacklist;
	private boolean wg;

	public EntityExplode(final ExplosionRegeneration plugin, final boolean random, final int speed, final int delay, final Particle particle, final Sound sound, final Set<Material> filter, final Set<String> blacklist, final boolean wg) {
		this.plugin = plugin;
		this.random = random;
		this.speed = speed;
		this.delay = delay;
		this.particle = particle;
		this.sound = sound;
		this.filter = filter;
		this.blacklist = blacklist;
		this.wg = wg;
	}

	@EventHandler
	public void onEntityExplodeEvent(final EntityExplodeEvent event) {
		List<Block> list = event.blockList();
		if(list.size() == 0) {
			return;
		}
		final World world = event.getLocation().getWorld();
		if(this.blacklist != null && this.blacklist.contains(world.getName())) {
			return;
		}
		if(this.wg) {
			list = WorldGuardAPI.filter(list);
			if(list.size() == 0) {
				return;
			}
		}
		this.removeDrops(world, event);
		final Map<Block, ExplosionCache> caches = new HashMap<>();
		for(final Block block : list) {
			block.getDrops().clear();
			final Material material = block.getType();
			if(this.filter != null && this.filter.contains(material)) {
				continue;
			}
			final BlockState state = block.getState();
			final ExplosionCache cache = new ExplosionCache(
				material,
				block.getLocation(),
				block.getBlockData(),
				state
			);
			Container container = null;
			switch(material) {
				case SIGN:
				case WALL_SIGN:
				case LEGACY_SIGN_POST: cache.setSignLines(((Sign) state).getLines()); break;
				case CHEST:
				case TRAPPED_CHEST: container = (Chest) state; break;
				case SHULKER_BOX:
				case BLACK_SHULKER_BOX:
				case BROWN_SHULKER_BOX:
				case BLUE_SHULKER_BOX:
				case CYAN_SHULKER_BOX:
				case GRAY_SHULKER_BOX:
				case LIGHT_BLUE_SHULKER_BOX:
				case GREEN_SHULKER_BOX:
				case LIGHT_GRAY_SHULKER_BOX:
				case LIME_SHULKER_BOX:
				case MAGENTA_SHULKER_BOX:
				case ORANGE_SHULKER_BOX:
				case PINK_SHULKER_BOX:
				case PURPLE_SHULKER_BOX:
				case RED_SHULKER_BOX:
				case YELLOW_SHULKER_BOX:
				case WHITE_SHULKER_BOX: container = (ShulkerBox) state; break;
				case FURNACE: container = (Furnace) state; break;
				case HOPPER: container = (Hopper) state; break;
				case DROPPER: container = (Dropper) state; break;
				case DISPENSER: container = (Dispenser) state; break;
				case BREWING_STAND: container = (BrewingStand) state; break;
				case BEACON: container = (Beacon) state; break;
			}
			if(container != null) {
				cache.setInventory(container.getInventory().getContents());
			}
			caches.put(block, cache);
		}
		new BukkitRunnable() {
			public void run() {
				try {
					if(caches.size() == 0) {
						removeDrops(world, event);
						this.cancel();
						return;
					}
					Block block;
					final List<Block> blocks = new ArrayList<>(caches.keySet());
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
					final ExplosionCache cache = caches.get(block);
					final BlockData data = cache.getBlockData();
					if(data == null) {
						return;
					}
					final Material material = cache.getMaterial();
					final Location location = cache.getLocation();
					final Block replace = location.getBlock();
					replace.setType(material);
					replace.setBlockData(data);
					final BlockState state = cache.getBlockState();
					final ItemStack[] inventory = cache.getInventory();
					Container container = null;
					switch(material) {
						case SIGN:
						case WALL_SIGN:
						case LEGACY_SIGN_POST:
							final Sign sign = (Sign) block.getState();
							final String[] lines = cache.getSignLines();
							sign.setLine(0, lines[0]);
							sign.setLine(1, lines[1]);
							sign.setLine(2, lines[2]);
							sign.setLine(3, lines[3]);
							sign.update();
							break;
						case CHEST:
						case TRAPPED_CHEST: container = (Chest) state; break;
						case SHULKER_BOX:
						case BLACK_SHULKER_BOX:
						case BROWN_SHULKER_BOX:
						case BLUE_SHULKER_BOX:
						case CYAN_SHULKER_BOX:
						case GRAY_SHULKER_BOX:
						case LIGHT_BLUE_SHULKER_BOX:
						case GREEN_SHULKER_BOX:
						case LIGHT_GRAY_SHULKER_BOX:
						case LIME_SHULKER_BOX:
						case MAGENTA_SHULKER_BOX:
						case ORANGE_SHULKER_BOX:
						case PINK_SHULKER_BOX:
						case PURPLE_SHULKER_BOX:
						case RED_SHULKER_BOX:
						case YELLOW_SHULKER_BOX:
						case WHITE_SHULKER_BOX: container = (ShulkerBox) state; break;
						case FURNACE: container = (Furnace) state; break;
						case HOPPER: container = (Hopper) state; break;
						case DROPPER: container = (Dropper) state; break;
						case DISPENSER: container = (Dispenser) state; break;
						case BREWING_STAND: container = (BrewingStand) state; break;
						case BEACON: container = (Beacon) state; break;
					}
					if(container != null) {
						container.getInventory().setContents(inventory);
						container.update();
					}
					world.playEffect(location, Effect.STEP_SOUND, material == Material.AIR ? block.getType().getId() : material.getId());
					world.spawnParticle(particle, location.add(0, 1, 0), 1, 0, 0, 0);
					world.playSound(location, sound, 1F, 1F);
					caches.remove(block);
				} catch(final Exception e) {
					e.printStackTrace();
					this.cancel();
				}
			}
		}.runTaskTimer(this.plugin, this.delay, this.speed);
		list.forEach(block -> {
			block.getDrops().clear();
			block.setType(Material.AIR);
		});
		this.removeDrops(world, event);
	}

	private void removeDrops(final World world, final EntityExplodeEvent event) {
		event.setYield(0);
		new BukkitRunnable() {
			public void run() {
				world.getEntitiesByClass(Item.class).stream().filter(item -> item.getLocation().distance(event.getLocation()) <= 20 && item.getType() == EntityType.DROPPED_ITEM).forEach(Item::remove);
			}
		}.runTaskLater(this.plugin, 5);
	}

	public void setSpeed(final int speed) {
		this.speed = speed;
	}

}

class ExplosionCache {

	private Material material;
	private Location location;
	private BlockData data;
	private String[] sign;
	private BlockState state;
	private ItemStack[] inventory;

	ExplosionCache(final Material material, final Location location, final BlockData data, final BlockState state) {
		this(material, location, data, state, null, null);
	}

	private ExplosionCache(final Material material, final Location location, final BlockData data, final BlockState state, final String[] sign, final ItemStack[] inventory) {
		this.material = material;
		this.location = location;
		this.data = data;
		this.state = state;
		this.sign = sign;
		this.inventory = inventory;
	}

	Material getMaterial() {
		return this.material;
	}

	void setMaterial(final Material material) {
		this.material = material;
	}

	Location getLocation() {
		return this.location;
	}

	void setLocation(final Location location) {
		this.location = location;
	}

	BlockData getBlockData() {
		return this.data;
	}

	void setBlockData(final BlockData data) {
		this.data = data;
	}

	String[] getSignLines() {
		return this.sign;
	}

	BlockState getBlockState() {
		return this.state;
	}

	void setBlockState(final BlockState state) {
		this.state = state;
	}

	void setSignLines(final String[] sign) {
		this.sign = sign;
	}

	ItemStack[] getInventory() {
		return this.inventory;
	}

	void setInventory(final ItemStack[] inventory) {
		this.inventory = inventory;
	}

}
