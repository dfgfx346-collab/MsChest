package ru.rtp674637.mschest;

import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class ChestManager {

    private final MsChest plugin;
    private final Map<UUID, LootChest> chests = new HashMap<>();
    private final Map<UUID, LootChest> openChests = new HashMap<>();
    private final File dataFile;
    private final NamespacedKey hologramKey;

    public ChestManager(MsChest plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "chests.yml");
        this.hologramKey = new NamespacedKey(plugin, "mschest_hologram");
    }

    public void loadAll() {
        if (!dataFile.exists()) return;
        YamlConfiguration config = YamlConfiguration.loadConfiguration(dataFile);
        ConfigurationSection section = config.getConfigurationSection("chests");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                String worldName = section.getString(key + ".world");
                World world = Bukkit.getWorld(worldName);
                if (world == null) continue;
                Location loc = new Location(world,
                        section.getDouble(key + ".x"),
                        section.getDouble(key + ".y"),
                        section.getDouble(key + ".z"));
                Rarity rarity = Rarity.fromString(section.getString(key + ".rarity"));
                if (rarity == null) rarity = Rarity.COMMON;
                long refreshAt = section.getLong(key + ".refreshAt", 0);

                LootChest lc = new LootChest(id, loc, rarity, refreshAt);
                chests.put(id, lc);

                removeOrphanHolograms(loc);
                spawnHologram(lc);
            } catch (Exception e) {
                plugin.getLogger().warning("Could not load chest: " + key);
            }
        }
    }

    public void saveAll() {
        YamlConfiguration config = new YamlConfiguration();
        for (LootChest lc : chests.values()) {
            String path = "chests." + lc.getId();
            config.set(path + ".world", lc.getLocation().getWorld().getName());
            config.set(path + ".x", lc.getLocation().getX());
            config.set(path + ".y", lc.getLocation().getY());
            config.set(path + ".z", lc.getLocation().getZ());
            config.set(path + ".rarity", lc.getRarity().getConfigKey());
            config.set(path + ".refreshAt", lc.getRefreshAt());
        }
        try {
            config.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save chests.yml: " + e.getMessage());
        }
    }

    public void createChest(Location loc, Player player) {
        Rarity rarity = rollRarity();
        UUID id = UUID.randomUUID();
        LootChest lc = new LootChest(id, loc.clone(), rarity, System.currentTimeMillis() + getRefreshMillis());
        chests.put(id, lc);

        String blockTypeStr = plugin.getConfig().getString("chest.block-type", "CHEST");
        Material blockMat = Material.matchMaterial(blockTypeStr);
        if (blockMat == null || !blockMat.isBlock()) blockMat = Material.CHEST;
        loc.getBlock().setType(blockMat);

        spawnHologram(lc);

        String rarityName = plugin.getConfig().getString("chest.rarity-names." + rarity.getConfigKey(), rarity.name());
        player.sendMessage(plugin.getMessage("chest-placed", "%rarity%", rarityName));
        saveAll();
    }

    public void removeChest(LootChest lc) {
        lc.getLocation().getBlock().setType(Material.AIR);
        if (lc.getHologram() != null && !lc.getHologram().isDead()) {
            lc.getHologram().remove();
        }
        removeOrphanHolograms(lc.getLocation());
        chests.remove(lc.getId());
        saveAll();
    }

    private void removeOrphanHolograms(Location loc) {
        if (loc.getWorld() == null) return;
        for (Entity entity : loc.getWorld().getNearbyEntities(loc, 2, 2, 2)) {
            if (entity instanceof TextDisplay display) {
                if (display.getPersistentDataContainer().has(hologramKey, PersistentDataType.STRING)) {
                    display.remove();
                }
            }
        }
    }

    public LootChest getChestAt(Location loc) {
        for (LootChest lc : chests.values()) {
            if (lc.getLocation().getBlock().equals(loc.getBlock())) return lc;
        }
        return null;
    }

    public boolean isLootChestBlock(Location loc) {
        return getChestAt(loc) != null;
    }

    public LootChest getOpenChest(Player player) {
        return openChests.get(player.getUniqueId());
    }

    public void setOpenChest(Player player, LootChest lc) {
        if (lc == null) openChests.remove(player.getUniqueId());
        else openChests.put(player.getUniqueId(), lc);
    }

    private Rarity rollRarity() {
        Map<Rarity, Double> chances = new EnumMap<>(Rarity.class);
        double total = 0;
        for (Rarity r : Rarity.values()) {
            double c = plugin.getConfig().getDouble("chest.rarity-chances." + r.getConfigKey(), 0);
            chances.put(r, c);
            total += c;
        }
        double roll = ThreadLocalRandom.current().nextDouble(total);
        double cumulative = 0;
        for (Map.Entry<Rarity, Double> e : chances.entrySet()) {
            cumulative += e.getValue();
            if (roll <= cumulative) return e.getKey();
        }
        return Rarity.COMMON;
    }

    private long getRefreshMillis() {
        return plugin.getConfig().getLong("chest.refresh-seconds", 600) * 1000L;
    }

    private void spawnHologram(LootChest lc) {
        if (lc.getHologram() != null && !lc.getHologram().isDead()) {
            lc.getHologram().remove();
        }
        double offsetY = plugin.getConfig().getDouble("hologram.offset-y", 1.8);
        Location holoLoc = lc.getLocation().clone().add(0.5, offsetY, 0.5);
        float scale = (float) plugin.getConfig().getDouble("hologram.scale", 1.0);
        boolean background = plugin.getConfig().getBoolean("hologram.background", true);
        boolean shadow = plugin.getConfig().getBoolean("hologram.shadow", false);

        TextDisplay display = lc.getLocation().getWorld().spawn(holoLoc, TextDisplay.class, entity -> {
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setPersistent(false);
            entity.setInvulnerable(true);
            entity.setSeeThrough(false);
            entity.setDefaultBackground(background);
            entity.setShadowed(shadow);
            if (scale != 1.0f) {
                entity.setTransformation(new org.bukkit.util.Transformation(
                        new org.joml.Vector3f(0f, 0f, 0f),
                        new org.joml.Quaternionf(),
                        new org.joml.Vector3f(scale, scale, scale),
                        new org.joml.Quaternionf()));
            }
            entity.getPersistentDataContainer().set(hologramKey, PersistentDataType.STRING, lc.getId().toString());
            updateHologramText(entity, lc);
        });
        lc.setHologram(display);
    }

    public void updateHolograms() {
        for (LootChest lc : chests.values()) {
            if (lc.getHologram() == null || lc.getHologram().isDead()) {
                spawnHologram(lc);
            } else {
                updateHologramText(lc.getHologram(), lc);
            }
        }
    }

    private void updateHologramText(TextDisplay display, LootChest lc) {
        long remaining = Math.max(0, lc.getRefreshAt() - System.currentTimeMillis());
        long seconds = remaining / 1000;
        String timeStr = String.format("%02d:%02d", seconds / 60, seconds % 60);
        String rarityName = plugin.getConfig().getString("chest.rarity-names." + lc.getRarity().getConfigKey(), "Common");
        String title = plugin.getConfig().getString("hologram.title", "&eСундук с лутом");
        List<String> lines = plugin.getConfig().getStringList("hologram.lines");

        StringBuilder sb = new StringBuilder(title);
        for (String line : lines) {
            if (line == null || line.isEmpty()) continue;
            sb.append('\n').append(line
                    .replace("%rarity%", rarityName)
                    .replace("%time%", timeStr)
                    .replace("%chest%", stripColor(title)));
        }
        display.text(plugin.parseColor(sb.toString()));
    }

    private String stripColor(String s) {
        return s.replaceAll("(?i)[&§][0-9A-FK-OR]", "").replaceAll("(?i)&#[0-9A-F]{6}", "");
    }

    public void removeAllHolograms() {
        for (LootChest lc : chests.values()) {
            if (lc.getHologram() != null && !lc.getHologram().isDead()) {
                lc.getHologram().remove();
            }
        }
    }

    public void openChest(LootChest lc, Player player) {
        if (lc.isLootTaken() && System.currentTimeMillis() < lc.getRefreshAt()) {
            long remaining = lc.getRefreshAt() - System.currentTimeMillis();
            long mins = remaining / 60000;
            long secs = (remaining % 60000) / 1000;
            player.sendMessage(plugin.getMessage("chest-empty", "%time%", mins + "м " + secs + "с"));
            return;
        }

        if (!lc.isAnimationFinished() && lc.getAnimLootSlots() != null) {
            String animationType = plugin.getConfig().getString("chest.animation-type", "default");
            plugin.getAnimationManager().playAnimation(animationType, lc, player);
            return;
        }

        boolean isRefreshed = lc.getCachedLoot() == null || System.currentTimeMillis() >= lc.getRefreshAt();
        if (isRefreshed) {
            lc.setRarity(rollRarity());
            lc.setCachedLoot(plugin.getLootManager().generateLoot(lc.getRarity()));
            lc.setRefreshAt(System.currentTimeMillis() + getRefreshMillis());
            lc.setLootTaken(false);

            lc.setAnimationFinished(false);
            lc.setAnimTick(0);
            lc.setAnimRedIndex(0);
            lc.setAnimGreenIndex(0);
            lc.setAnimLootSlots(null);
            lc.setAnimEmptySlots(null);

            String animationType = plugin.getConfig().getString("chest.animation-type", "default");
            if (animationType == null || animationType.trim().isEmpty() || animationType.equalsIgnoreCase("none")) {
                showLootDirectly(lc, player);
                lc.setAnimationFinished(true);
            } else {
                plugin.getAnimationManager().playAnimation(animationType, lc, player);
            }
        } else {
            showLootDirectly(lc, player);
            lc.setAnimationFinished(true);
        }
    }

    private void showLootDirectly(LootChest lc, Player player) {
        Inventory inv = Bukkit.createInventory(null, 27, plugin.parseColor("Сундук"));
        fillInventoryWithLoot(inv, lc.getCachedLoot());
        player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
        player.openInventory(inv);
        
        int glowSec = plugin.getConfig().getInt("chest.glow-seconds", 7);
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.GLOWING, glowSec * 20, 0, false, false, false));
        player.sendMessage(plugin.getMessage("chest-opened"));
    }

    private void fillInventoryWithLoot(Inventory inv, List<ItemStack> loot) {
        if (loot == null || loot.isEmpty()) return;
        int size = inv.getSize();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < size; i++) slots.add(i);
        Collections.shuffle(slots);
        int count = Math.min(loot.size(), size);
        for (int i = 0; i < count; i++) {
            inv.setItem(slots.get(i), loot.get(i));
        }
    }

    public void restartAllChests() {
        for (LootChest lc : chests.values()) {
            lc.setRarity(rollRarity());
            lc.setCachedLoot(null);
            lc.setRefreshAt(0);
            lc.setOpened(false);
            lc.setLootTaken(false);
            lc.setAnimationFinished(false);
            lc.setAnimTick(0);
            lc.setAnimRedIndex(0);
            lc.setAnimGreenIndex(0);
            lc.setAnimLootSlots(null);
            lc.setAnimEmptySlots(null);
        }
        updateHolograms();
        saveAll();
    }

    public Collection<LootChest> getAllChests() {
        return chests.values();
    }
}