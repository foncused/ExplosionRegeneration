package me.foncused.explosionregeneration.event;

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
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Comparator;
import java.util.*;

public class Regeneration implements Listener {

	private final ExplosionRegeneration plugin;
	private final ConfigManager cm;
	private final WorldGuardHook worldguard;
	private final List<FallingBlock> fallingBlocks;
	private final Set<UUID> entities;
	private final Map<UUID, ItemStack[]> armorStands;
	private final Map<UUID, ItemFrameCache> itemFrames;
	private final Map<UUID, Art> paintings;
	private int time;

	public Regeneration(final ExplosionRegeneration plugin) {
		this.plugin = plugin;
		this.cm = this.plugin.getConfigManager();
		this.worldguard = this.plugin.getWorldGuard();
		this.fallingBlocks = new ArrayList<>();
		this.entities = new HashSet<>();
		this.armorStands = new HashMap<>();
		this.itemFrames = new HashMap<>();
		this.paintings = new HashMap<>();
		this.time = this.cm.getDelay() + (250 * this.cm.getSpeed());
	}

	private void regenerate(final List<Block> list, final Location location) {
		if(list.size() == 0) {
			return;
		}
		final World world = location.getWorld();
		if(world == null) {
			Bukkit.getLogger().warning("World is null, cannot regenerate explosion at (" +
					location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + ").");
			return;
		}
		double r = 0.0;
		for(final Block block : list) {
			final double distance = location.distance(block.getLocation());
			if(r < distance) {
				r = distance;
			}
		}
		final double radius = r + 1.0;
		Collection<Entity> nearby = null;
		final boolean isEntityProtection = this.cm.isEntityProtection();
		if(isEntityProtection) {
			nearby = world.getNearbyEntities(location, radius, radius, radius);
		}
		final Collection<Entity> nearbyEntities = nearby;
		final List<Block> air = new ArrayList<>();
		list.stream().filter(block -> block.getType() == Material.AIR).forEach(air::add);
		list.removeAll(air);
		final int size = list.size();
		if(size == 0) {
			return;
		}
		final int delay = this.cm.getDelay();
		final int speed = this.cm.getSpeed();
		final int regenerationTime = delay + (size * speed) + 1;
		if(this.time < regenerationTime) {
			this.time = regenerationTime;
		}
		final Set<String> blacklist = this.cm.getBlacklist();
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
		final boolean dropsEnabled = this.cm.isDropsEnabled();
		final Set<Material> dropsBlacklist = this.cm.getDropsBlacklist();
		if(this.cm.isFallingBlocks()) {
			list.forEach(block -> {
				final BlockData data = block.getBlockData();
				final FallingBlock falling = world.spawnFallingBlock(block.getLocation(), data);
				falling.setDropItem(false);
				final Material material = data.getMaterial();
				if(dropsEnabled && (!(dropsBlacklist.contains(material)))) {
					world.dropItemNaturally(
							block.getLocation().add(0, 1, 0),
							new ItemStack(material, 1)
					);
				}
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
		} else if(dropsEnabled) {
			list
					.stream()
					.filter(block -> (!(dropsBlacklist.contains(block.getBlockData().getMaterial()))))
					.forEach(block ->
							world.dropItemNaturally(
									block.getLocation().add(0, 1, 0),
									new ItemStack(block.getBlockData().getMaterial(), 1)
							)
					);
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
		final double distance = this.cm.getDropsRadius() * 2;
		if(dropsEnabled) {
			new BukkitRunnable() {
				@Override
				public void run() {
					world.getEntitiesByClass(Item.class)
							.stream()
							.filter(item -> item.getLocation().distance(location) <= distance
									&& item.getType() == EntityType.DROPPED_ITEM
									&& dropsBlacklist.contains(item.getItemStack().getType()))
							.forEach(Entity::remove);
				}
			}.runTaskLater(this.plugin, 1);
		} else {
			//event.setYield(0F);
			new BukkitRunnable() {
				@Override
				public void run() {
					world.getEntitiesByClass(Item.class)
							.stream()
							.filter(item -> item.getLocation().distance(location) <= distance
									&& item.getType() == EntityType.DROPPED_ITEM)
							.forEach(Entity::remove);
				}
			}.runTaskLater(this.plugin, 1);
		}
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
					cache.setSignLines(((Sign) state).getLines());
					break;
				case BLACK_BANNER:
				case BLACK_WALL_BANNER:
				case BLUE_BANNER:
				case BLUE_WALL_BANNER:
				case BROWN_BANNER:
				case BROWN_WALL_BANNER:
				case CYAN_BANNER:
				case CYAN_WALL_BANNER:
				case GRAY_BANNER:
				case GRAY_WALL_BANNER:
				case GREEN_BANNER:
				case GREEN_WALL_BANNER:
				case LIGHT_BLUE_BANNER:
				case LIGHT_BLUE_WALL_BANNER:
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
				case WHITE_BANNER:
				case WHITE_WALL_BANNER:
				case YELLOW_BANNER:
				case YELLOW_WALL_BANNER:
					final Banner banner = (Banner) state;
					cache.setDyeColor(banner.getBaseColor());
					cache.setPatterns(banner.getPatterns());
					break;
				case LECTERN:
					final Lectern lectern = (Lectern) state;
					final Inventory inventory = lectern.getInventory();
					cache.setInventory(inventory.getContents());
					inventories.add(inventory);
					break;
				case CHEST:
				case TRAPPED_CHEST:
					container = (Chest) state;
					break;
				case BLACK_SHULKER_BOX:
				case BLUE_SHULKER_BOX:
				case BROWN_SHULKER_BOX:
				case CYAN_SHULKER_BOX:
				case GRAY_SHULKER_BOX:
				case GREEN_SHULKER_BOX:
				case LIGHT_BLUE_SHULKER_BOX:
				case LIGHT_GRAY_SHULKER_BOX:
				case LIME_SHULKER_BOX:
				case MAGENTA_SHULKER_BOX:
				case ORANGE_SHULKER_BOX:
				case PINK_SHULKER_BOX:
				case PURPLE_SHULKER_BOX:
				case RED_SHULKER_BOX:
				case SHULKER_BOX:
				case WHITE_SHULKER_BOX:
				case YELLOW_SHULKER_BOX:
					container = (ShulkerBox) state;
					break;
				case FURNACE:
					container = (Furnace) state;
					break;
				case HOPPER:
					container = (Hopper) state;
					break;
				case DROPPER:
					container = (Dropper) state;
					break;
				case DISPENSER:
					container = (Dispenser) state;
					break;
				case BREWING_STAND:
					container = (BrewingStand) state;
					break;
				case BARREL:
					container = (Barrel) state;
					break;
				case BLAST_FURNACE:
					container = (BlastFurnace) state;
					break;
				case SMOKER:
					container = (Smoker) state;
					break;
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
								case LECTERN:
									final Lectern lectern = (Lectern) state;
									lectern.getInventory().setContents(cache.getInventory());
									lectern.update(true);
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
								case WHITE_SHULKER_BOX:
									container = (ShulkerBox) state;
									break;
								case FURNACE:
									container = (Furnace) state;
									break;
								case HOPPER:
									container = (Hopper) state;
									break;
								case DROPPER:
									container = (Dropper) state;
									break;
								case DISPENSER:
									container = (Dispenser) state;
									break;
								case BREWING_STAND:
									container = (BrewingStand) state;
									break;
								case BARREL:
									container = (Barrel) state;
									break;
								case BLAST_FURNACE:
									container = (BlastFurnace) state;
									break;
								case SMOKER:
									container = (Smoker) state;
									break;
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
						if(isEntityProtection) {
							nearbyEntities.forEach(entity -> {
								final UUID uuid = entity.getUniqueId();
								if(entities.remove(uuid)) {
									final Location location = entity.getLocation();
									final EntityType type = entity.getType();
									switch(type) {
										case ARMOR_STAND:
											final ArmorStand stand = ((ArmorStand) world.spawnEntity(location, type));
											stand.setGravity(false);
											new BukkitRunnable() {
												@Override
												public void run() {
													if(stand.isValid()) {
														stand.setGravity(true);
													}
												}
											}.runTaskLater(plugin, time);
											final ItemStack[] armor = armorStands.get(uuid);
											if(armor != null) {
												armorStands.remove(uuid);
												stand.getEquipment().setArmorContents(armor);
											}
											break;
										case ITEM_FRAME:
											final ItemFrameCache cache = itemFrames.get(uuid);
											if(cache != null) {
												itemFrames.remove(uuid);
												try {
													final ItemFrame frame = ((ItemFrame) world.spawnEntity(location, type));
													frame.setItem(cache.getItem());
													frame.setRotation(cache.getRotation());
												} catch(final IllegalArgumentException e) {
													if(!(e.getMessage().contains("Cannot spawn hanging entity for org.bukkit.entity.ItemFrame"))) {
														throw e;
													}
												}
											}
											break;
										case PAINTING:
											final Art art = paintings.get(uuid);
											if(art != null) {
												paintings.remove(uuid);
												try {
													((Painting) world.spawnEntity(location, type)).setArt(art);
												} catch(final IllegalStateException e) {
													if(!(e.getMessage().contains("Unable to get CCW facing"))) {
														throw e;
													}
												} catch(final IllegalArgumentException e) {
													if(!(e.getMessage().contains("Cannot spawn hanging entity for org.bukkit.entity.Painting"))) {
														throw e;
													}
												}
											}
											break;
										default:
											try {
												world.spawnEntity(location, type);
											} catch(final IllegalArgumentException e) {
												if(!(e.getMessage().matches("Cannot spawn an entity for org\\.bukkit\\.entity\\.(Item|Player)"))) {
													throw e;
												}
											}
											break;
									}
								}
							});
						}
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
						case BLUE_BANNER:
						case BLUE_WALL_BANNER:
						case BROWN_BANNER:
						case BROWN_WALL_BANNER:
						case CYAN_BANNER:
						case CYAN_WALL_BANNER:
						case GRAY_BANNER:
						case GRAY_WALL_BANNER:
						case GREEN_BANNER:
						case GREEN_WALL_BANNER:
						case LIGHT_BLUE_BANNER:
						case LIGHT_BLUE_WALL_BANNER:
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
						case WHITE_BANNER:
						case WHITE_WALL_BANNER:
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
		}.runTaskTimer(this.plugin, delay, speed);
		list.forEach(block -> block.setType(Material.AIR));
	}

	@EventHandler
	public void onBlockExplode(final BlockExplodeEvent event) {
		if(!(this.cm.isDropsEnabled())) {
			event.setYield(0F);
		}
		this.regenerate(event.blockList(), event.getBlock().getLocation());
	}

	@EventHandler
	public void onEntityExplode(final EntityExplodeEvent event) {
		if(!(this.cm.isDropsEnabled())) {
			event.setYield(0F);
		}
		this.regenerate(event.blockList(), event.getLocation());
	}

	@EventHandler
	public void onEntityChangeBlock(final EntityChangeBlockEvent event) {
		final Entity entity = event.getEntity();
		if(entity instanceof FallingBlock && this.fallingBlocks.remove(entity)) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onEntityDamage(final EntityDamageEvent event) {
		if(this.cm.isEntityProtection()) {
			final EntityDamageEvent.DamageCause cause = event.getCause();
			final Entity entity = event.getEntity();
			final EntityType type = entity.getType();
			if((cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION || cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)
					&& type != EntityType.DROPPED_ITEM
			) {
				final UUID uuid = entity.getUniqueId();
				this.entities.add(uuid);
				new BukkitRunnable() {
					@Override
					public void run() {
						entities.remove(uuid);
					}
				}.runTaskLater(this.plugin, this.time);
				if(type == EntityType.ARMOR_STAND) {
					this.armorStands.put(uuid, ((ArmorStand) entity).getEquipment().getArmorContents());
					new BukkitRunnable() {
						@Override
						public void run() {
							armorStands.remove(uuid);
						}
					}.runTaskLater(this.plugin, this.time);
				}
			}
		}
	}

	@EventHandler
	public void onHangingBreak(final HangingBreakEvent event) {
		if(this.cm.isEntityProtection() && event.getCause() == HangingBreakEvent.RemoveCause.EXPLOSION) {
			final Entity entity = event.getEntity();
			final UUID uuid = entity.getUniqueId();
			final EntityType type = entity.getType();
			if(type == EntityType.ITEM_FRAME || type == EntityType.PAINTING) {
				this.entities.add(uuid);
				new BukkitRunnable() {
					@Override
					public void run() {
						entities.remove(uuid);
					}
				}.runTaskLater(this.plugin, this.time);
				if(type == EntityType.ITEM_FRAME) {
					final ItemFrame frame = (ItemFrame) entity;
					this.itemFrames.put(uuid, new ItemFrameCache(frame.getItem(), frame.getRotation()));
					new BukkitRunnable() {
						@Override
						public void run() {
							itemFrames.remove(uuid);
						}
					}.runTaskLater(this.plugin, this.time);
				} else {
					this.paintings.put(uuid, ((Painting) entity).getArt());
					new BukkitRunnable() {
						@Override
						public void run() {
							paintings.remove(uuid);
						}
					}.runTaskLater(this.plugin, this.time);
				}
			}
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

class ItemFrameCache {

	private ItemStack stack;
	private Rotation rotation;

	ItemFrameCache(
		final ItemStack stack,
		final Rotation rotation
	) {
		this.stack = stack;
		this.rotation = rotation;
	}

	ItemStack getItem() {
		return this.stack;
	}

	void setItem(final ItemStack stack) {
		this.stack = stack;
	}

	Rotation getRotation() {
		return this.rotation;
	}

	void setRotation(final Rotation rotation) {
		this.rotation = rotation;
	}

}
