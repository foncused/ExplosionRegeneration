package me.foncused.explosionregeneration.event;

import me.foncused.explosionregeneration.ExplosionRegeneration;
import me.foncused.explosionregeneration.config.ConfigManager;
import me.foncused.explosionregeneration.lib.sk89q.WorldGuardHook;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.*;
import org.bukkit.entity.minecart.CommandMinecart;
import org.bukkit.entity.minecart.HopperMinecart;
import org.bukkit.entity.minecart.StorageMinecart;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
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
	private final Map<UUID, MinecartCache> minecarts;
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
		this.minecarts = new HashMap<>();
		this.itemFrames = new HashMap<>();
		this.paintings = new HashMap<>();
		this.time = this.cm.getDelay() + (250 * this.cm.getSpeed());
	}

	private void regenerate(final List<Block> list, final Location location) {

		// Do nothing if there is nothing to regenerate
		if(list.size() == 0) {
			return;
		}

		// Check if world is valid
		final World world = location.getWorld();
		if(world == null) {
			Bukkit.getLogger().warning("World is null, cannot regenerate explosion at (" +
					location.getBlockX() + "," + location.getBlockY() + "," + location.getBlockZ() + ").");
			return;
		}

		// Calculate radius
		double r = 0.0;
		for(final Block block : list) {
			final double distance = location.distance(block.getLocation());
			if(r < distance) {
				r = distance;
			}
		}
		final double radius = r + 1.0;

		// Collect nearby entities within radius
		Collection<Entity> nearby = null;
		final boolean isEntityProtection = this.cm.isEntityProtection();
		if(isEntityProtection) {
			nearby = world.getNearbyEntities(location, radius, radius, radius);
		}
		final Collection<Entity> nearbyEntities = nearby;

		// Filter out air for efficiency
		final List<Block> air = new ArrayList<>();
		list.stream().filter(block -> block.getType() == Material.AIR).forEach(air::add);
		list.removeAll(air);
		final int size = list.size();
		if(size == 0) {
			return;
		}

		// Calculate timing
		final int delay = this.cm.getDelay();
		final int speed = this.cm.getSpeed();
		final int regenerationTime = delay + (size * speed) + 1;
		if(this.time < regenerationTime) {
			this.time = regenerationTime;
		}

		// Check config blacklist
		final Set<String> blacklist = this.cm.getBlacklist();
		try {
			if(blacklist != null && blacklist.contains(world.getName())) {
				return;
			}
		} catch(final NullPointerException e) {
			return;
		}

		// Check config if using WorldGuard integration
		if(this.cm.isWorldGuard()) {
			this.worldguard.getExplosionFiltered(list);
			if(list.size() == 0) {
				return;
			}
		}

		// Check config if drops are enabled and create falling blocks
		final boolean dropsEnabled = this.cm.isDropsEnabled();
		final Set<Material> dropsBlacklist = this.cm.getDropsBlacklist();
		if(this.cm.isFallingBlocks()) {
			list.forEach(block -> {
				final BlockData data = block.getBlockData();
				final Location loc = block.getLocation();
				final FallingBlock falling = world.spawnFallingBlock(loc, data);
				falling.setDropItem(false);
				final Material material = data.getMaterial();
				if(dropsEnabled && (!(dropsBlacklist.contains(material)))) {
					world.dropItemNaturally(
							loc.add(0, 1, 0),
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

		// Check config if TNT chaining is enabled
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

		// Check config if drops are enabled and remove as necessary
		if(dropsEnabled) {
			new BukkitRunnable() {
				@Override
				public void run() {
					world.getEntitiesByClass(Item.class)
							.stream()
							.filter(item -> item.getType() == EntityType.DROPPED_ITEM
									&& dropsBlacklist.contains(item.getItemStack().getType())
									&& item.getLocation().distance(location) <= distance)
							.forEach(Entity::remove);
				}
			}.runTaskLater(this.plugin, 1);
		} else {
			new BukkitRunnable() {
				@Override
				public void run() {
					world.getEntitiesByClass(Item.class)
							.stream()
							.filter(item -> item.getType() == EntityType.DROPPED_ITEM
									&& item.getLocation().distance(location) <= distance)
							.forEach(Entity::remove);
				}
			}.runTaskLater(this.plugin, 1);
		}

		// Collect block data for regeneration
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
				// Signs
				case ACACIA_SIGN, ACACIA_WALL_SIGN, BIRCH_SIGN, BIRCH_WALL_SIGN, CRIMSON_SIGN, CRIMSON_WALL_SIGN,
						DARK_OAK_SIGN, DARK_OAK_WALL_SIGN, JUNGLE_SIGN, JUNGLE_WALL_SIGN, OAK_SIGN, OAK_WALL_SIGN,
						SPRUCE_SIGN, SPRUCE_WALL_SIGN, WARPED_SIGN, WARPED_WALL_SIGN -> cache.setSignLines(((Sign) state).getLines());
				// Banners
				case BLACK_BANNER, BLACK_WALL_BANNER, BLUE_BANNER, BLUE_WALL_BANNER, BROWN_BANNER, BROWN_WALL_BANNER,
						CYAN_BANNER, CYAN_WALL_BANNER, GRAY_BANNER, GRAY_WALL_BANNER, GREEN_BANNER, GREEN_WALL_BANNER,
						LIGHT_BLUE_BANNER, LIGHT_BLUE_WALL_BANNER, LIGHT_GRAY_BANNER, LIGHT_GRAY_WALL_BANNER, LIME_BANNER,
						LIME_WALL_BANNER, MAGENTA_BANNER, MAGENTA_WALL_BANNER, ORANGE_BANNER, ORANGE_WALL_BANNER, PINK_BANNER,
						PINK_WALL_BANNER, PURPLE_BANNER, PURPLE_WALL_BANNER, RED_BANNER, RED_WALL_BANNER, WHITE_BANNER,
						WHITE_WALL_BANNER, YELLOW_BANNER, YELLOW_WALL_BANNER -> {
					final Banner banner = (Banner) state;
					cache.setDyeColor(banner.getBaseColor());
					cache.setPatterns(banner.getPatterns());
				}
				// Containers
				case BARREL -> container = (Barrel) state;
				case BLAST_FURNACE -> container = (BlastFurnace) state;
				case BREWING_STAND -> container = (BrewingStand) state;
				case CHEST, TRAPPED_CHEST -> container = (Chest) state;
				case DISPENSER -> container = (Dispenser) state;
				case DROPPER -> container = (Dropper) state;
				case FURNACE -> container = (Furnace) state;
				case HOPPER -> container = (Hopper) state;
				case BLACK_SHULKER_BOX, BLUE_SHULKER_BOX, BROWN_SHULKER_BOX, CYAN_SHULKER_BOX, GRAY_SHULKER_BOX,
						GREEN_SHULKER_BOX, LIGHT_BLUE_SHULKER_BOX, LIGHT_GRAY_SHULKER_BOX, LIME_SHULKER_BOX,
						MAGENTA_SHULKER_BOX, ORANGE_SHULKER_BOX, PINK_SHULKER_BOX, PURPLE_SHULKER_BOX, RED_SHULKER_BOX,
						SHULKER_BOX, WHITE_SHULKER_BOX, YELLOW_SHULKER_BOX -> container = (ShulkerBox) state;
				case SMOKER -> container = (Smoker) state;
				// Lectern
				case LECTERN -> {
					final Lectern lectern = (Lectern) state;
					final Inventory inventory = lectern.getInventory();
					cache.setInventory(inventory.getContents());
					inventories.add(inventory);
				}
			}
			if(container != null) {
				final Inventory inventory = container.getInventory();
				cache.setInventory(inventory.getContents());
				inventories.add(inventory);
			}
			caches.put(block, cache);
		}
		inventories.forEach(Inventory::clear);

		// Check config for randomized or sorted regeneration
		if(this.cm.isRandom()) {
			Collections.shuffle(list);
		} else {
			list.sort(Comparator.comparingDouble(b -> b.getLocation().getY()));
		}

		// Doors
		final List<Block> doors = new ArrayList<>();
		list.stream().filter(block -> this.isDoor(block.getType())).forEach(doors::add);
		list.removeAll(doors);
		doors.sort(Comparator.comparingDouble(b -> b.getLocation().getY()));
		list.addAll(doors);

		// Regeneration
		new BukkitRunnable() {
			@Override
			public void run() {
				try {
					// Done, so do containers and entities last
					if(list.size() == 0) {
						caches.forEach((block, cache) -> {
							final Material material = cache.getMaterial();
							final BlockState state = cache.getBlockState();
							Container container = null;
							switch(material) {
								// Containers
								case BARREL -> container = (Barrel) state;
								case BLAST_FURNACE -> container = (BlastFurnace) state;
								case BREWING_STAND -> container = (BrewingStand) state;
								case CHEST, TRAPPED_CHEST -> container = (Chest) state;
								case DISPENSER -> container = (Dispenser) state;
								case DROPPER -> container = (Dropper) state;
								case FURNACE -> container = (Furnace) state;
								case HOPPER -> container = (Hopper) state;
								case BLACK_SHULKER_BOX, BLUE_SHULKER_BOX, BROWN_SHULKER_BOX, CYAN_SHULKER_BOX,
										GRAY_SHULKER_BOX, GREEN_SHULKER_BOX, LIGHT_BLUE_SHULKER_BOX,
										LIGHT_GRAY_SHULKER_BOX, LIME_SHULKER_BOX, MAGENTA_SHULKER_BOX,
										ORANGE_SHULKER_BOX, PINK_SHULKER_BOX, PURPLE_SHULKER_BOX, RED_SHULKER_BOX,
										SHULKER_BOX, WHITE_SHULKER_BOX, YELLOW_SHULKER_BOX -> container = (ShulkerBox) state;
								case SMOKER -> container = (Smoker) state;
								// Lectern
								case LECTERN -> {
									final Lectern lectern = (Lectern) state;
									lectern.getInventory().setContents(cache.getInventory());
									lectern.update(true);
								}
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
											final ItemFrameCache itemFrameCache = itemFrames.get(uuid);
											if(itemFrameCache != null) {
												itemFrames.remove(uuid);
												try {
													final ItemFrame frame = ((ItemFrame) world.spawnEntity(location, type));
													frame.setItem(itemFrameCache.getItem());
													frame.setRotation(itemFrameCache.getRotation());
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
							final Set<UUID> minecartsToSpawn = new HashSet<>();
							minecarts.forEach((uuid, minecartCache) -> {
								if(minecartCache.getLocation().distance(location) <= radius) {
									minecartsToSpawn.add(uuid);
								}
							});
							minecartsToSpawn.forEach(uuid -> {
								final MinecartCache minecartCache = minecarts.get(uuid);
								if(minecartCache != null) {
									try {
										final Location location = minecartCache.getLocation();
										final ItemStack[] contents = minecartCache.getContents();
										switch(minecartCache.getType()) {
											case MINECART:
												world.spawnEntity(location, EntityType.MINECART);
												break;
											case MINECART_FURNACE:
												world.spawnEntity(location, EntityType.MINECART_FURNACE);
												break;
											case MINECART_MOB_SPAWNER:
												world.spawnEntity(location, EntityType.MINECART_MOB_SPAWNER);
												break;
											case MINECART_TNT:
												world.spawnEntity(location, EntityType.MINECART_TNT);
												break;
											case MINECART_CHEST:
												((StorageMinecart) world.spawnEntity(location, EntityType.MINECART_CHEST))
														.getInventory().setContents(contents);
												break;
											case MINECART_COMMAND:
												((CommandMinecart) world.spawnEntity(location, EntityType.MINECART_COMMAND))
														.setCommand(minecartCache.getCommand());
												break;
											case MINECART_HOPPER:
												final HopperMinecart hm = ((HopperMinecart) world.spawnEntity(location, EntityType.MINECART_HOPPER));
												hm.getInventory().setContents(contents);
												hm.setEnabled(minecartCache.isEnabled());
												break;
										}
									} catch(final IllegalArgumentException e) {
										e.printStackTrace();
									}
								}
							});
							minecartsToSpawn.forEach(minecarts::remove);
						}
						this.cancel();
						return;
					}
					// Blocks
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
					// Doors
					if(isDoor(material)) {
						// If bottom half, restore the top with it too
						if((!(isDoor(replace.getRelative(BlockFace.DOWN).getType())))) {
							replace.setType(material);
							replace.setBlockData(data);
							final Block other = replace.getRelative(BlockFace.UP);
							final Bisected bisected = (Bisected) data;
							bisected.setHalf(Bisected.Half.BOTTOM);
							replace.setBlockData(bisected, false);
							bisected.setHalf(Bisected.Half.TOP);
							other.setBlockData(bisected, false);
						}
					} else {
						replace.setType(material);
						replace.setBlockData(data);
					}
					final BlockState state = cache.getBlockState();
					switch(material) {
						// Signs
						case ACACIA_SIGN, ACACIA_WALL_SIGN, BIRCH_SIGN, BIRCH_WALL_SIGN, CRIMSON_SIGN,
								CRIMSON_WALL_SIGN, DARK_OAK_SIGN, DARK_OAK_WALL_SIGN, JUNGLE_SIGN, JUNGLE_WALL_SIGN,
								OAK_SIGN, OAK_WALL_SIGN, SPRUCE_SIGN, SPRUCE_WALL_SIGN, WARPED_SIGN, WARPED_WALL_SIGN -> {
							final Sign sign = (Sign) state;
							final String[] lines = cache.getSignLines();
							sign.setLine(0, lines[0]);
							sign.setLine(1, lines[1]);
							sign.setLine(2, lines[2]);
							sign.setLine(3, lines[3]);
							sign.update();
						}
						// Banners
						case BLACK_BANNER, BLACK_WALL_BANNER, BLUE_BANNER, BLUE_WALL_BANNER, BROWN_BANNER,
								BROWN_WALL_BANNER, CYAN_BANNER, CYAN_WALL_BANNER, GRAY_BANNER, GRAY_WALL_BANNER,
								GREEN_BANNER, GREEN_WALL_BANNER, LIGHT_BLUE_BANNER, LIGHT_BLUE_WALL_BANNER,
								LIGHT_GRAY_BANNER, LIGHT_GRAY_WALL_BANNER, LIME_BANNER, LIME_WALL_BANNER,
								MAGENTA_BANNER, MAGENTA_WALL_BANNER, ORANGE_BANNER, ORANGE_WALL_BANNER, PINK_BANNER,
								PINK_WALL_BANNER, PURPLE_BANNER, PURPLE_WALL_BANNER, RED_BANNER, RED_WALL_BANNER,
								WHITE_BANNER, WHITE_WALL_BANNER, YELLOW_BANNER, YELLOW_WALL_BANNER -> {
							final Banner banner = (Banner) state;
							banner.setBaseColor(cache.getDyeColor());
							banner.setPatterns(cache.getPatterns());
							banner.update(true);
						}
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

		// Initially set all exploded blocks to air
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

	@EventHandler
	public void onVehicleDestroy(final VehicleDestroyEvent event) {
		if(this.cm.isEntityProtection()) {
			final Vehicle vehicle = event.getVehicle();
			final UUID uuid = vehicle.getUniqueId();
			final EntityType type = vehicle.getType();
			final Location location = vehicle.getLocation();
			switch(type) {
				case MINECART:
				case MINECART_FURNACE:
				case MINECART_MOB_SPAWNER:
				case MINECART_TNT:
					this.minecarts.put(
							uuid,
							new MinecartCache(
									type,
									location,
									null,
									null,
									false
							)
					);
					break;
				case MINECART_CHEST:
					this.minecarts.put(
							uuid,
							new MinecartCache(
									type,
									location,
									null,
									((StorageMinecart) vehicle).getInventory().getContents(),
									false
							)
					);
					break;
				case MINECART_COMMAND:
					this.minecarts.put(
							uuid,
							new MinecartCache(
									type,
									location,
									((CommandMinecart) vehicle).getCommand(),
									null,
									false
							)
					);
					break;
				case MINECART_HOPPER:
					final HopperMinecart hm = (HopperMinecart) vehicle;
					this.minecarts.put(
							uuid,
							new MinecartCache(
									type,
									location,
									null,
									hm.getInventory().getContents(),
									hm.isEnabled()
							)
					);
					break;
			}
			new BukkitRunnable() {
				@Override
				public void run() {
					minecarts.remove(uuid);
				}
			}.runTaskLater(this.plugin, this.time);
		}
	}

	// Check if material is a door
	private boolean isDoor(final Material material) {
		return switch(material) {
			case ACACIA_DOOR, BIRCH_DOOR, CRIMSON_DOOR, DARK_OAK_DOOR,
					JUNGLE_DOOR, OAK_DOOR, SPRUCE_DOOR, WARPED_DOOR -> true;
			default -> false;
		};
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

class MinecartCache {

	private EntityType type;
	private Location location;
	private String command;
	private ItemStack[] contents;
	private boolean enabled;

	MinecartCache(
		final EntityType type,
		final Location location,
		final String command,
		final ItemStack[] contents,
		final boolean enabled
	) {
		this.type = type;
		this.location = location;
		this.command = command;
		this.contents = contents;
		this.enabled = enabled;
	}

	EntityType getType() {
		return this.type;
	}

	void setType(final EntityType type) {
		this.type = type;
	}

	Location getLocation() {
		return this.location;
	}

	void setLocation(final Location location) {
		this.location = location;
	}

	String getCommand() {
		return this.command;
	}

	void setCommand(final String command) {
		this.command = command;
	}

	ItemStack[] getContents() {
		return this.contents;
	}

	void setContents(final ItemStack[] contents) {
		this.contents = contents;
	}

	boolean isEnabled() {
		return this.enabled;
	}

	void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

}
