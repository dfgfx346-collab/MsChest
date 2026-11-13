package ru.rtp674637.mschest;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class LootManager {

    private final MsChest plugin;
    private final File lootFile;
    private YamlConfiguration lootConfig;

    public LootManager(MsChest plugin) {
        this.plugin = plugin;
        this.lootFile = new File(plugin.getDataFolder(), "loot.yml");
        reload();
    }

    public void reload() {
        if (!lootFile.exists()) {
            lootFile.getParentFile().mkdirs();
            lootConfig = new YamlConfiguration();
            lootConfig.set("common", List.of("BREAD:4:60", "COAL:8:50", "IRON_INGOT:2:40", "APPLE:3:30"));
            lootConfig.set("rare", List.of("GOLD_INGOT:4:35", "IRON_SWORD:1:25", "DIAMOND:1:15", "GOLDEN_APPLE:1:10"));
            lootConfig.set("epic", List.of("DIAMOND_SWORD:1:25", "DIAMOND:3:20", "NETHERITE_SCRAP:1:15", "ENCHANTED_GOLDEN_APPLE:1:10"));
            lootConfig.set("mythic", List.of("ENCHANTED_GOLDEN_APPLE:3:20", "NETHERITE_CHESTPLATE:1:15", "NETHERITE_INGOT:1:10", "ELYTRA:1:5"));
            save();
        } else {
            lootConfig = YamlConfiguration.loadConfiguration(lootFile);
        }
    }

    public void save() {
        try {
            lootConfig.save(lootFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save loot.yml: " + e.getMessage());
        }
    }

    public List<String> getLootList(Rarity rarity) {
        List<String> list = lootConfig.getStringList(rarity.getConfigKey());
        return list != null ? list : new ArrayList<>();
    }

    public void setLootList(Rarity rarity, List<String> list) {
        lootConfig.set(rarity.getConfigKey(), list);
        save();
    }

    public void addItem(Rarity rarity, Material material, int amount, double chance) {
        if (material == null || material.isAir()) return;
        if (amount < 1) amount = 1;
        if (chance < 0) chance = 0;
        if (chance > 100) chance = 100;

        List<String> list = getLootList(rarity);
        for (int i = 0; i < list.size(); i++) {
            String[] parts = list.get(i).split(":");
            if (parts[0].equalsIgnoreCase(material.name())) {
                int oldAmount = parts.length > 1 ? parseIntSafe(parts[1], 1) : 1;
                int newAmount = oldAmount + amount;
                list.set(i, material.name() + ":" + newAmount + ":" + formatChance(chance));
                setLootList(rarity, list);
                return;
            }
        }
        list.add(material.name() + ":" + amount + ":" + formatChance(chance));
        setLootList(rarity, list);
    }

    public boolean removeItem(Rarity rarity, Material material) {
        List<String> list = getLootList(rarity);
        boolean removed = list.removeIf(s -> s.split(":")[0].equalsIgnoreCase(material.name()));
        if (removed) setLootList(rarity, list);
        return removed;
    }

    public List<ItemStack> generateLoot(Rarity rarity) {
        List<ItemStack> result = new ArrayList<>();
        List<String> entries = new ArrayList<>(getLootList(rarity));
        if (entries.isEmpty()) {
            result.add(new ItemStack(Material.STONE, 1));
            return result;
        }

        int min = plugin.getConfig().getInt("chest.item-count." + rarity.getConfigKey() + ".min", 1);
        int max = plugin.getConfig().getInt("chest.item-count." + rarity.getConfigKey() + ".max", 4);
        if (min < 1) min = 1;
        if (max < min) max = min;

        ThreadLocalRandom random = ThreadLocalRandom.current();
        int target = random.nextInt(min, max + 1);

        List<String> rolled = new ArrayList<>();
        Collections.shuffle(entries);
        for (String entry : entries) {
            String[] parts = entry.split(":");
            double chance = parts.length > 2 ? parseDoubleSafe(parts[2], 0) : 0;
            if (random.nextDouble(100.0) < chance) {
                rolled.add(entry);
            }
        }

        if (rolled.size() < target) {
            List<String> pool = new ArrayList<>(entries);
            while (rolled.size() < target && !pool.isEmpty()) {
                rolled.add(weightedPick(pool, random));
            }
        }

        if (rolled.size() > target) {
            Collections.shuffle(rolled);
            rolled = new ArrayList<>(rolled.subList(0, target));
        }

        for (String entry : rolled) {
            String[] parts = entry.split(":");
            Material mat = Material.matchMaterial(parts[0]);
            if (mat == null) continue;
            int amount = parts.length > 1 ? parseIntSafe(parts[1], 1) : 1;
            result.add(new ItemStack(mat, Math.max(1, amount)));
        }

        if (result.isEmpty()) result.add(new ItemStack(Material.STONE, 1));
        return result;
    }

    private String weightedPick(List<String> pool, ThreadLocalRandom random) {
        double total = 0;
        double[] weights = new double[pool.size()];
        for (int i = 0; i < pool.size(); i++) {
            String[] parts = pool.get(i).split(":");
            double w = parts.length > 2 ? parseDoubleSafe(parts[2], 0) : 1;
            if (w < 0) w = 0;
            weights[i] = w;
            total += w;
        }
        if (total <= 0) return pool.get(random.nextInt(pool.size()));
        double roll = random.nextDouble(total);
        double cumulative = 0;
        for (int i = 0; i < pool.size(); i++) {
            cumulative += weights[i];
            if (roll <= cumulative) return pool.get(i);
        }
        return pool.get(pool.size() - 1);
    }

    private String formatChance(double chance) {
        if (chance == Math.floor(chance)) return String.valueOf((int) chance);
        return String.valueOf(chance);
    }

    private int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return def; }
    }

    private double parseDoubleSafe(String s, double def) {
        try { return Double.parseDouble(s.trim()); } catch (NumberFormatException e) { return def; }
    }
}