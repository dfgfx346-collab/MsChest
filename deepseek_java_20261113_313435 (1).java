package ru.rtp674637.mschest;

import org.bukkit.Location;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.UUID;

public class LootChest {
    private final UUID id;
    private final Location location;
    private Rarity rarity;
    private long refreshAt;
    private TextDisplay hologram;
    private List<ItemStack> cachedLoot;
    private boolean opened;
    private boolean lootTaken = false;

    private boolean animationFinished = false;
    private int animTick = 0;
    private int animRedIndex = 0;
    private int animGreenIndex = 0;
    private List<Integer> animLootSlots = null;
    private List<Integer> animEmptySlots = null;

    public LootChest(UUID id, Location location, Rarity rarity, long refreshAt) {
        this.id = id;
        this.location = location;
        this.rarity = rarity;
        this.refreshAt = refreshAt;
        this.opened = false;
    }

    public UUID getId() { return id; }
    public Location getLocation() { return location; }
    public Rarity getRarity() { return rarity; }
    public void setRarity(Rarity rarity) { this.rarity = rarity; }
    public long getRefreshAt() { return refreshAt; }
    public void setRefreshAt(long refreshAt) { this.refreshAt = refreshAt; }
    public TextDisplay getHologram() { return hologram; }
    public void setHologram(TextDisplay hologram) { this.hologram = hologram; }
    public List<ItemStack> getCachedLoot() { return cachedLoot; }
    public void setCachedLoot(List<ItemStack> cachedLoot) { this.cachedLoot = cachedLoot; }
    public boolean isOpened() { return opened; }
    public void setOpened(boolean opened) { this.opened = opened; }
    public boolean isLootTaken() { return lootTaken; }
    public void setLootTaken(boolean lootTaken) { this.lootTaken = lootTaken; }
    public boolean isAnimationFinished() { return animationFinished; }
    public void setAnimationFinished(boolean animationFinished) { this.animationFinished = animationFinished; }
    public int getAnimTick() { return animTick; }
    public void setAnimTick(int animTick) { this.animTick = animTick; }
    public int getAnimRedIndex() { return animRedIndex; }
    public void setAnimRedIndex(int animRedIndex) { this.animRedIndex = animRedIndex; }
    public int getAnimGreenIndex() { return animGreenIndex; }
    public void setAnimGreenIndex(int animGreenIndex) { this.animGreenIndex = animGreenIndex; }
    public List<Integer> getAnimLootSlots() { return animLootSlots; }
    public void setAnimLootSlots(List<Integer> animLootSlots) { this.animLootSlots = animLootSlots; }
    public List<Integer> getAnimEmptySlots() { return animEmptySlots; }
    public void setAnimEmptySlots(List<Integer> animEmptySlots) { this.animEmptySlots = animEmptySlots; }
}