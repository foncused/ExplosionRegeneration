package me.foncused.explosionregeneration.event.entity;

import me.foncused.explosionregeneration.ExplosionRegeneration;
import me.foncused.explosionregeneration.config.ConfigManager;
import me.foncused.explosionregeneration.lib.sk89q.WorldGuardHook;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Comparator;
import java.util.*;

public class EntityExplode implements Listener {

	private final ExplosionRegeneration plugin;
	private final WorldGuardHook worldguard;
	private final ConfigManager cm;
	private final List<FallingBlock> fallingBlocks;

	public EntityExplode(final ExplosionRegeneration plugin) {
		this.plugin = plugin;
		this.worldguard = this.plugin.getWorldGuard();
		this.cm = this.plugin.getConfigManager();
		this.fallingBlocks = new ArrayList<>();
	}

	@EventHandler
	public void onEntityExplodeEvent(final EntityExplodeEvent event) {
		final List<Block> list = event.blockList();
		if(list.size() == 0) {
			return;
		}
		final List<Block> air = new ArrayList<>();
		list.stream().filter(block -> block.getType() == Material.AIR).forEach(air::add);
		list.removeAll(air);
		final Set<String> blacklist = this.cm.getBlacklist();
		final Location location = event.getLocation();
		final World world = location.getWorld();
		if(world == null) {
			Bukkit.getLogger().warning("World is null, cannot regenerate explosion at (" +
					location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + ").");
			return;
		}
		try {
			if(blacklist != null && blacklist.contains(world.getName())) {
				return;
			}
		} catch(final NullPointerException e) {
			return;
		}
		if(this.cm.isWorldGuard()) {
			this.worldguard.getExplosionFiltered(list);
			if(list.size() == 0) {
				return;
			}
		}
		if(this.cm.isFallingBlocks()) {
			list.forEach(block -> {
				final FallingBlock falling = world.spawnFallingBlock(block.getLocation(), block.getBlockData());
				falling.setDropItem(false);
				final Random random = new Random();
				falling.setVelocity(
						new Vector(
								random.nextBoolean() ? random.nextDouble() : -random.nextDouble(),
								random.nextDouble(),
								random.nextBoolean() ? random.nextDouble() : -random.nextDouble()
						)
				);
				this.fallingBlocks.add(falling);
			});
		}
		if(this.cm.isTntChainingEnabled()) {
			final List<Block> tnt = new ArrayList<>();
			list.stream().filter(block -> block.getType() == Material.TNT).forEach(tnt::add);
			tnt.forEach(block -> {
				block.setType(Material.AIR);
				final TNTPrimed entity = (TNTPrimed) world.spawnEntity(block.getLocation(), EntityType.PRIMED_TNT);
				entity.setFuseTicks(new Random().nextInt(this.cm.getTntChainingMaxFuseTicks()) + 1);
			});
			list.removeAll(tnt);
		}
		event.setYield(0F);
		new BukkitRunnable() {
			@Override
			public void run() {
				world.getEntitiesByClass(Item.class)
						.stream()
						.filter(item -> item.getLocation().distance(location) <= 8.0 && item.getType() == EntityType.DROPPED_ITEM)
						.forEach(Entity::remove);
			}
		}.runTaskLater(this.plugin, 1);
		final Map<Block, ExplosionCache> caches = new HashMap<>();
		final List<Inventory> inventories = new ArrayList<>();
		for(final Block block : list) {
			final Material material = block.getType();
			final Set<Material> filter = this.cm.getFilter();
			if(filter != null && filter.contains(material)) {
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
				case ACACIA_SIGN:
				case ACACIA_WALL_SIGN:
				case BIRCH_SIGN:
				case BIRCH_WALL_SIGN:
				case DARK_OAK_SIGN:
				case DARK_OAK_WALL_SIGN:
				case JUNGLE_SIGN:
				case JUNGLE_WALL_SIGN:
				case OAK_SIGN:
				case OAK_WALL_SIGN:
				case SPRUCE_SIGN:
				case SPRUCE_WALL_SIGN:
				case LEGACY_SIGN:
				case LEGACY_WALL_SIGN:
				case LEGACY_SIGN_POST: cache.setSignLines(((Sign) state).getLines()); break;
				case BLACK_BANNER:
				case BLACK_WALL_BANNER:
				case BROWN_BANNER:
				case BROWN_WALL_BANNER:
				case BLUE_BANNER:
				case BLUE_WALL_BANNER:
				case CYAN_BANNER:
				case CYAN_WALL_BANNER:
				case GRAY_BANNER:
				case GRAY_WALL_BANNER:
				case LIGHT_BLUE_BANNER:
				case LIGHT_BLUE_WALL_BANNER:
				case GREEN_BANNER:
				case GREEN_WALL_BANNER:
				case LIGHT_GRAY_BANNER:
				case LIGHT_GRAY_WALL_BANNER:
				case LIME_BANNER:
				case LIME_WALL_BANNER:
				case MAGENTA_BANNER:
				case MAGENTA_WALL_BANNER:
				case ORANGE_BANNER:
				case ORANGE_WALL_BANNER:
				case PINK_BANNER:
				case PINK_WALL_BANNER:
				case PURPLE_BANNER:
				case PURPLE_WALL_BANNER:
				case RED_BANNER:
				case RED_WALL_BANNER:
				case YELLOW_BANNER:
				case YELLOW_WALL_BANNER:
					final Banner banner = (Banner) state;
					cache.setDyeColor(banner.getBaseColor());
					cache.setPatterns(banner.getPatterns());
					break;
				case CHEST: container = (Chest) state; break;
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
				case BARREL: container = (Barrel) state; break;
				case BLAST_FURNACE: container = (BlastFurnace) state; break;
				case SMOKER: container = (Smoker) state; break;
			}
			if(container != null) {
				final Inventory inventory = container.getInventory();
				cache.setInventory(inventory.getContents());
				inventories.add(inventory);
			}
			caches.put(block, cache);
		}
		inventories.forEach(Inventory::clear);
		if(this.cm.isRandom()) {
			Collections.shuffle(list);
		} else {
			list.sort(Comparator.comparingDouble(b -> b.getLocation().getY()));
		}
		// Regeneration
		new BukkitRunnable() {
			@Override
			public void run() {
				try {
					if(list.size() == 0) {
						// Restore container contents
						caches.forEach((block, cache) -> {
							final Material material = cache.getMaterial();
							final BlockState state = cache.getBlockState();
							Container container = null;
							switch(material) {
								case CHEST: container = (Chest) state; break;
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
								case BARREL: container = (Barrel) state; break;
								case BLAST_FURNACE: container = (BlastFurnace) state; break;
								case SMOKER: container = (Smoker) state; break;
							}
							if(container != null) {
								try {
									container.getInventory().setContents(cache.getInventory());
									container.update(true);
								} catch(final IllegalArgumentException e) {
									final Location l = cache.getLocation();
									Bukkit.getLogger()
											.severe(
													"Could not restore container contents at " +
															"(" + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ() + "). " +
															"This is likely due to multiple explosions occurring in the same area."
											);
								}
							}
						});
						this.cancel();
						return;
					}
					final Block block = list.get(0);
					final ExplosionCache cache = caches.get(block);
					BlockData data;
					try {
						data = cache.getBlockData();
					} catch(final NullPointerException e) {
						list.remove(block);
						return;
					}
					if(data == null) {
						list.remove(block);
						return;
					}
					final Material material = cache.getMaterial();
					final Location l = cache.getLocation();
					final Block replace = l.getBlock();
					replace.setType(material);
					replace.setBlockData(data);
					final BlockState state = cache.getBlockState();
					switch(material) {
						case ACACIA_SIGN:
						case ACACIA_WALL_SIGN:
						case BIRCH_SIGN:
						case BIRCH_WALL_SIGN:
						case DARK_OAK_SIGN:
						case DARK_OAK_WALL_SIGN:
						case JUNGLE_SIGN:
						case JUNGLE_WALL_SIGN:
						case OAK_SIGN:
						case OAK_WALL_SIGN:
						case SPRUCE_SIGN:
						case SPRUCE_WALL_SIGN:
						case LEGACY_SIGN:
						case LEGACY_WALL_SIGN:
						case LEGACY_SIGN_POST:
							final Sign sign = (Sign) state;
							final String[] lines = cache.getSignLines();
							sign.setLine(0, lines[0]);
							sign.setLine(1, lines[1]);
							sign.setLine(2, lines[2]);
							sign.setLine(3, lines[3]);
							sign.update();
							break;
						case BLACK_BANNER:
						case BLACK_WALL_BANNER:
						case BROWN_BANNER:
						case BROWN_WALL_BANNER:
						case BLUE_BANNER:
						case BLUE_WALL_BANNER:
						case CYAN_BANNER:
						case CYAN_WALL_BANNER:
						case GRAY_BANNER:
						case GRAY_WALL_BANNER:
						case LIGHT_BLUE_BANNER:
						case LIGHT_BLUE_WALL_BANNER:
						case GREEN_BANNER:
						case GREEN_WALL_BANNER:
						case LIGHT_GRAY_BANNER:
						case LIGHT_GRAY_WALL_BANNER:
						case LIME_BANNER:
						case LIME_WALL_BANNER:
						case MAGENTA_BANNER:
						case MAGENTA_WALL_BANNER:
						case ORANGE_BANNER:
						case ORANGE_WALL_BANNER:
						case PINK_BANNER:
						case PINK_WALL_BANNER:
						case PURPLE_BANNER:
						case PURPLE_WALL_BANNER:
						case RED_BANNER:
						case RED_WALL_BANNER:
						case YELLOW_BANNER:
						case YELLOW_WALL_BANNER:
							final Banner banner = (Banner) state;
							banner.setBaseColor(cache.getDyeColor());
							banner.setPatterns(cache.getPatterns());
							banner.update(true);
							break;
					}
					world.playEffect(l, Effect.STEP_SOUND, material == Material.AIR ? block.getType() : material);
					final Sound sound = cm.getSound();
					if(sound != null) {
						world.playSound(l, sound, 1F, 1F);
					}
					final Particle particle = cm.getParticle();
					if(particle != null) {
						world.spawnParticle(particle, l.add(0, 1, 0), 1, 0, 0, 0);
					}
					list.remove(block);
				} catch(final Exception e) {
					e.printStackTrace();
					this.cancel();
				}
			}
		}.runTaskTimer(this.plugin, this.cm.getDelay(), this.cm.getSpeed());
		list.forEach(block -> block.setType(Material.AIR));
	}

	@EventHandler
	public void onEntityChangeBlock(final EntityChangeBlockEvent event) {
		final Entity entity = event.getEntity();
		if(entity instanceof FallingBlock && this.fallingBlocks.remove(entity)) {
			event.setCancelled(true);
		}
	}

}

class ExplosionCache {

	private Material material;
	private Location location;
	private BlockData data;
	private String[] sign;
	private BlockState state;
	private ItemStack[] inventory;
	private DyeColor color;
	private List<Pattern> patterns;

	ExplosionCache(final Material material, final Location location, final BlockData data, final BlockState state) {
		this(material, location, data, state, null, null, null, null);
	}

	private ExplosionCache(
		final Material material,
		final Location location,
		final BlockData data,
		final BlockState state,
		final String[] sign,
		final ItemStack[] inventory,
		final DyeColor color,
		final List<Pattern> patterns
	) {
		this.material = material;
		this.location = location;
		this.data = data;
		this.state = state;
		this.sign = sign;
		this.inventory = inventory;
		this.color = color;
		this.patterns = patterns;
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

	DyeColor getDyeColor() {
		return this.color;
	}

	void setDyeColor(final DyeColor color) {
		this.color = color;
	}

	List<Pattern> getPatterns() {
		return this.patterns;
	}

	void setPatterns(final List<Pattern> patterns) {
		this.patterns = patterns;
	}

}
