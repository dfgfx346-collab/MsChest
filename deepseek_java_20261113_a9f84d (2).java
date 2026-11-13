package ru.rtp674637.mschest;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.plugin.java.JavaPlugin;

public final class MsChest extends JavaPlugin {

    private static MsChest instance;
    private ChestManager chestManager;
    private LootManager lootManager;
    private AnimationManager animationManager;

    private final LegacyComponentSerializer legacySerializer =
            LegacyComponentSerializer.builder()
                    .character('&')
                    .hexColors()
                    .useUnusualXRepeatedCharacterHexFormat()
                    .build();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        lootManager = new LootManager(this);
        animationManager = new AnimationManager(this);
        chestManager = new ChestManager(this);
        chestManager.loadAll();

        MsChestCommand command = new MsChestCommand(this);
        getCommand("mschest").setExecutor(command);
        getCommand("mschest").setTabCompleter(command);
        getServer().getPluginManager().registerEvents(new ChestListener(this), this);

        getServer().getScheduler().runTaskTimer(this, () -> chestManager.updateHolograms(), 20L, 20L);
        getLogger().info("MsChest enabled. Author: Rtp674637");
    }

    @Override
    public void onDisable() {
        if (chestManager != null) {
            chestManager.saveAll();
            chestManager.removeAllHolograms();
        }
        if (lootManager != null) lootManager.save();
    }

    public static MsChest getInstance() { return instance; }
    public ChestManager getChestManager() { return chestManager; }
    public LootManager getLootManager() { return lootManager; }
    public AnimationManager getAnimationManager() { return animationManager; }

    public Component parseColor(String input) {
        if (input == null) return Component.empty();
        return legacySerializer.deserialize(input);
    }

    public Component getMessage(String key, String... replacements) {
        String prefix = getConfig().getString("messages.prefix", "");
        String msg = getConfig().getString("messages." + key, key);
        String full = prefix + msg;
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            full = full.replace(replacements[i], replacements[i + 1]);
        }
        return parseColor(full);
    }
}