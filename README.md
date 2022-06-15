# ExplosionRegeneration

### Description:
"ExplosionRegeneration" is a "just-for-fun" plugin which regenerates exploded blocks given that an explosion has occurred. However, this plugin is **not an "anti-grief" plugin** in any way. Do not expect this plugin to protect your server from griefers.

### Installation:
1. Copy ExplosionRegeneration.jar into your /plugins directory
2. Start your server
3. Edit your config.yml

### Configuration:
- random - boolean to select either random or bottom-to-top regeneration (default true)
- speed - integer (ticks) for the regeneration speed (default 2)
- delay - integer (ticks) to delay before regeneration begins (default 0)
- particle - [Particle](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Particle.html) to display when a block regenerates (default VILLAGER_HAPPY)
- sound - [Sound](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Sound.html) played when a block regenerates (default ENTITY_CHICKEN_EGG)
- tnt-chaining
  - enabled - boolean to enable or disable TNT explosion chaining (default false)
  - max-fuse-ticks - integer (ticks) max value for the random fuse time on chained TNT explosions (default 40)
- falling-blocks - boolean to enable or disable visual explosion effect (default false)
- filter - string list of [Material](https://hub.spigotmc.org/javadocs/spigot/org/bukkit/Material.html) names to NOT regenerate (default FIRE)
- blacklist - string list of world names in which to NOT regenerate blocks
- entity-protection - boolean to enable or disable entity protection (armor stands, item frames, paintings) (default true)
- drops
  - enabled - boolean to enable or disable item drops from explosions
  - radius - double to set a radius outward from explosion to check for item drop removal
blacklist - string list of Material names to NOT drop items on the ground
worldguard - boolean to add support for WorldGuard and WorldEdit config and regions (default false)

https://github.com/foncused/ExplosionRegeneration/blob/master/src/main/resources/config.yml

### Hooks:
- WorldGuard's config and explosion region flags are supported
  - Config: **ignition.block-tnt**, **ignition.block-tnt-block-damage**
  - Flags: **creeper-explosion**, **other-explosion**, ~~**tnt** (not needed?)~~

### Issues:
- ~~Entity blocks are not currently being regenerated~~
- ~~Doors regenerate incorrectly (half doors) and still produce item drops~~
- Minecarts are not currently being regenerated
  
### Support:
If you run into any server performance problems, or if the plugin is not working as advertised (console errors, bugs, etc.), please do not hesitate to contact me, post in the discussion thread, or open an issue on GitHub.

### Links:
- Spigot: https://www.spigotmc.org/resources/explosionregeneration.20221/
- Donate: https://paypal.me/foncused
