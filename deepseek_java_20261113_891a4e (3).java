package ru.rtp674637.mschest;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;

public class AnimationManager {

    private final MsChest plugin;
    private final File animFile;
    private YamlConfiguration animConfig;
    private final Set<UUID> animatingPlayers = new HashSet<>();
    private final Set<UUID> allowPickupPlayers = new HashSet<>();
    private final Map<UUID, BukkitRunnable> activeAnimations = new HashMap<>();

    public AnimationManager(MsChest plugin) {
        this.plugin = plugin;
        this.animFile = new File(plugin.getDataFolder(), "animations.yml");
        reload();
    }

    public void reload() {
        if (!animFile.exists()) plugin.saveResource("animations.yml", false);
        animConfig = YamlConfiguration.loadConfiguration(animFile);
    }

    public boolean isAnimating(Player player) {
        return animatingPlayers.contains(player.getUniqueId());
    }

    public boolean canPickup(Player player) {
        return allowPickupPlayers.contains(player.getUniqueId());
    }

    public void setAnimating(Player player, boolean value, boolean allowPickup) {
        if (value) {
            animatingPlayers.add(player.getUniqueId());
            if (allowPickup) allowPickupPlayers.add(player.getUniqueId());
            else allowPickupPlayers.remove(player.getUniqueId());
        } else {
            animatingPlayers.remove(player.getUniqueId());
            allowPickupPlayers.remove(player.getUniqueId());
        }
    }

    public void stopAnimation(Player player) {
        UUID uuid = player.getUniqueId();
        BukkitRunnable task = activeAnimations.remove(uuid);
        if (task != null) task.cancel();
        setAnimating(player, false, false);
    }

    public void playAnimation(String type, LootChest lc, Player player) {
        if (lc.isAnimationFinished()) {
            Inventory inv = Bukkit.createInventory(null, 27, plugin.parseColor("Сундук"));
            fillLoot(inv, lc.getCachedLoot());
            player.openInventory(inv);
            return;
        }

        if (lc.getAnimLootSlots() != null && lc.getAnimEmptySlots() != null) {
            resumeAnimation(type, lc, player);
            return;
        }

        String path = type.toLowerCase();
        if (!animConfig.contains(path)) path = "default";
        
        if (path.equals("ordered")) {
            startOrderedAnimation(lc, player);
            return;
        }

        startPaneAnimation(path, lc, player);
    }

    private void startPaneAnimation(String path, LootChest lc, Player player) {
        int duration = animConfig.getInt(path + ".duration", 4);
        Material redMat = Material.matchMaterial(animConfig.getString(path + ".red-pane", "RED_STAINED_GLASS_PANE"));
        Material greenMat = Material.matchMaterial(animConfig.getString(path + ".green-pane", "LIME_STAINED_GLASS_PANE"));
        String redName = animConfig.getString(path + ".red-name", "");
        String greenName = animConfig.getString(path + ".green-name", "");
        Sound redSound = parseSound(animConfig.getString(path + ".red-sound"));
        Sound greenSound = parseSound(animConfig.getString(path + ".green-sound"));
        Sound finalSound = parseSound(animConfig.getString(path + ".final-sound"));

        if (redMat == null) redMat = Material.RED_STAINED_GLASS_PANE;
        if (greenMat == null) greenMat = Material.LIME_STAINED_GLASS_PANE;

        final ItemStack redPane = createPane(redMat, redName);
        final ItemStack greenPane = createPane(greenMat, greenName);

        int lootCount = Math.min(lc.getCachedLoot().size(), 27);
        List<Integer> allSlots = new ArrayList<>();
        for (int i = 0; i < 27; i++) allSlots.add(i);
        Collections.shuffle(allSlots);
        
        List<Integer> lootSlots = new ArrayList<>(allSlots.subList(0, lootCount));
        List<Integer> emptySlots = new ArrayList<>(allSlots.subList(lootCount, 27));

        if (path.equals("snake")) {
            Collections.sort(emptySlots);
            Collections.sort(lootSlots);
        } else {
            Collections.shuffle(emptySlots);
            Collections.shuffle(lootSlots);
        }

        lc.setAnimLootSlots(lootSlots);
        lc.setAnimEmptySlots(emptySlots);
        lc.setAnimTick(0);
        lc.setAnimRedIndex(0);
        lc.setAnimGreenIndex(0);
        lc.setAnimationFinished(false);

        resumePaneAnimation(path, lc, player, redPane, greenPane, redSound, greenSound, finalSound);
    }

    private void resumeAnimation(String path, LootChest lc, Player player) {
        String type = path.toLowerCase();
        if (!animConfig.contains(type)) type = "default";
        
        if (type.equals("ordered")) {
            startOrderedAnimation(lc, player);
            return;
        }
        
        int duration = animConfig.getInt(type + ".duration", 4);
        Material redMat = Material.matchMaterial(animConfig.getString(type + ".red-pane", "RED_STAINED_GLASS_PANE"));
        Material greenMat = Material.matchMaterial(animConfig.getString(type + ".green-pane", "LIME_STAINED_GLASS_PANE"));
        String redName = animConfig.getString(type + ".red-name", "");
        String greenName = animConfig.getString(type + ".green-name", "");
        Sound redSound = parseSound(animConfig.getString(type + ".red-sound"));
        Sound greenSound = parseSound(animConfig.getString(type + ".green-sound"));
        Sound finalSound = parseSound(animConfig.getString(type + ".final-sound"));

        if (redMat == null) redMat = Material.RED_STAINED_GLASS_PANE;
        if (greenMat == null) greenMat = Material.LIME_STAINED_GLASS_PANE;

        ItemStack redPane = createPane(redMat, redName);
        ItemStack greenPane = createPane(greenMat, greenName);

        resumePaneAnimation(type, lc, player, redPane, greenPane, redSound, greenSound, finalSound);
    }

    private void resumePaneAnimation(String path, LootChest lc, Player player, ItemStack redPane, ItemStack greenPane, Sound redSound, Sound greenSound, Sound finalSound) {
        Inventory inv = Bukkit.createInventory(null, 27, plugin.parseColor("Сундук"));
        player.openInventory(inv);
        setAnimating(player, true, false);

        List<Integer> lootSlots = lc.getAnimLootSlots();
        List<Integer> emptySlots = lc.getAnimEmptySlots();
        for (int i = 0; i < lc.getAnimRedIndex(); i++) inv.setItem(emptySlots.get(i), redPane);
        for (int i = 0; i < lc.getAnimGreenIndex(); i++) inv.setItem(lootSlots.get(i), greenPane);

        int duration = animConfig.getInt(path + ".duration", 4);
        int totalTicks = duration * 20;
        int redPhaseEnd = totalTicks / 2;

        UUID uuid = player.getUniqueId();
        BukkitRunnable task = new BukkitRunnable() {
            int ticks = lc.getAnimTick();
            int redIndex = lc.getAnimRedIndex();
            int greenIndex = lc.getAnimGreenIndex();

            @Override
            public void run() {
                if (player.getOpenInventory().getTopInventory() != inv) {
                    lc.setAnimTick(ticks);
                    lc.setAnimRedIndex(redIndex);
                    lc.setAnimGreenIndex(greenIndex);
                    activeAnimations.remove(uuid);
                    cancel();
                    return;
                }

                if (ticks >= totalTicks) {
                    finishAnimation(inv, lc, player, lootSlots, finalSound);
                    activeAnimations.remove(uuid);
                    cancel();
                    return;
                }

                if (ticks < redPhaseEnd && redIndex < emptySlots.size()) {
                    inv.setItem(emptySlots.get(redIndex), redPane);
                    redIndex++;
                    if (redSound != null) player.playSound(player.getLocation(), redSound, 0.5f, 1.2f);
                } else if (ticks >= redPhaseEnd && greenIndex < lootSlots.size()) {
                    inv.setItem(lootSlots.get(greenIndex), greenPane);
                    greenIndex++;
                    if (greenSound != null) player.playSound(player.getLocation(), greenSound, 0.6f, 1.5f);
                }
                ticks++;
            }
        };
        task.runTaskTimer(plugin, 1L, 1L);
        activeAnimations.put(uuid, task);
    }

    private void startOrderedAnimation(LootChest lc, Player player) {
        String path = "ordered";
        int duration = animConfig.getInt(path + ".duration", 4);
        Sound sound = parseSound(animConfig.getString(path + ".sound"));
        Sound finalSound = parseSound(animConfig.getString(path + ".final-sound"));

        Inventory inv = Bukkit.createInventory(null, 27, plugin.parseColor("Сундук"));
        player.openInventory(inv);
        setAnimating(player, true, true);

        List<ItemStack> loot = new ArrayList<>(lc.getCachedLoot());
        Collections.shuffle(loot);

        List<Integer> allSlots = new ArrayList<>();
        for (int i = 0; i < 27; i++) allSlots.add(i);
        Collections.shuffle(allSlots);
        
        List<Integer> lootSlots = new ArrayList<>(allSlots.subList(0, Math.min(loot.size(), 27)));
        lc.setAnimLootSlots(lootSlots);

        int totalTicks = duration * 20;
        int interval = Math.max(1, totalTicks / Math.max(1, loot.size()));

        UUID uuid = player.getUniqueId();
        BukkitRunnable task = new BukkitRunnable() {
            int ticks = lc.getAnimTick();
            int index = lc.getAnimRedIndex();

            @Override
            public void run() {
                if (player.getOpenInventory().getTopInventory() != inv) {
                    lc.setAnimTick(ticks);
                    lc.setAnimRedIndex(index);
                    activeAnimations.remove(uuid);
                    cancel();
                    return;
                }

                if (ticks >= totalTicks || index >= loot.size()) {
                    inv.clear();
                    for (int i = 0; i < loot.size(); i++) {
                        inv.setItem(lootSlots.get(i), loot.get(i));
                    }
                    if (finalSound != null) player.playSound(player.getLocation(), finalSound, 0.8f, 1.5f);
                    player.sendMessage(plugin.getMessage("chest-opened"));
                    lc.setOpened(true);
                    lc.setAnimationFinished(true);
                    Bukkit.getScheduler().runTaskLater(plugin, () -> setAnimating(player, false, false), 2L);
                    activeAnimations.remove(uuid);
                    cancel();
                    return;
                }

                if (ticks % interval == 0) {
                    inv.setItem(lootSlots.get(index), loot.get(index));
                    if (sound != null) player.playSound(player.getLocation(), sound, 0.6f, 1.0f + (index * 0.05f));
                    index++;
                }
                ticks++;
            }
        };
        task.runTaskTimer(plugin, 1L, 1L);
        activeAnimations.put(uuid, task);
    }

    private void finishAnimation(Inventory inv, LootChest lc, Player player, List<Integer> lootSlots, Sound finalSound) {
        inv.clear();
        List<ItemStack> loot = new ArrayList<>(lc.getCachedLoot());
        Collections.shuffle(loot);
        int count = Math.min(loot.size(), lootSlots.size());
        for (int i = 0; i < count; i++) {
            inv.setItem(lootSlots.get(i), loot.get(i));
        }

        if (finalSound != null) {
            player.playSound(player.getLocation(), finalSound, 0.8f, 1.5f);
            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, 1.0f, 1.0f);
        }

        int glowSec = plugin.getConfig().getInt("chest.glow-seconds", 7);
        player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                org.bukkit.potion.PotionEffectType.GLOWING, glowSec * 20, 0, false, false, false));

        player.sendMessage(plugin.getMessage("chest-opened"));
        lc.setOpened(true);
        lc.setAnimationFinished(true);
        lc.setAnimLootSlots(null);
        lc.setAnimEmptySlots(null);
        Bukkit.getScheduler().runTaskLater(plugin, () -> setAnimating(player, false, false), 2L);
    }

    private void fillLoot(Inventory inv, List<ItemStack> loot) {
        if (loot == null) return;
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 27; i++) slots.add(i);
        Collections.shuffle(slots);
        for (int i = 0; i < Math.min(loot.size(), 27); i++) {
            inv.setItem(slots.get(i), loot.get(i));
        }
    }

    private ItemStack createPane(Material mat, String name) {
        ItemStack pane = new ItemStack(mat);
        ItemMeta meta = pane.getItemMeta();
        if (name != null && !name.isEmpty()) meta.displayName(plugin.parseColor(name));
        else meta.displayName(Component.empty());
        pane.setItemMeta(meta);
        return pane;
    }

    private Sound parseSound(String s) {
        if (s == null || s.isEmpty()) return null;
        try { return Sound.valueOf(s.toUpperCase()); } catch (Exception e) { return null; }
    }
}