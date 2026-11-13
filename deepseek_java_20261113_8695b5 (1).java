package ru.rtp674637.mschest;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class ChestListener implements Listener {

    private final MsChest plugin;

    public ChestListener(MsChest plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.useInteractedBlock() == Event.Result.DENY) return;

        Player player = event.getPlayer();
        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        ItemStack item = player.getInventory().getItemInMainHand();
        boolean isWand = item.getType() == Material.STICK && item.hasItemMeta() && item.getItemMeta().hasDisplayName();

        if (isWand) {
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                if (plugin.getChestManager().isLootChestBlock(clicked.getLocation())) return;
                Location loc = clicked.getRelative(event.getBlockFace()).getLocation();
                if (loc.getBlock().getType() != Material.AIR) {
                    player.sendMessage(plugin.parseColor("&cМесто занято!"));
                    return;
                }
                plugin.getChestManager().createChest(loc, player);
                return;
            }
            if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
                LootChest lc = plugin.getChestManager().getChestAt(clicked.getLocation());
                if (lc != null) {
                    event.setCancelled(true);
                    plugin.getChestManager().removeChest(lc);
                    player.sendMessage(plugin.getMessage("chest-removed"));
                }
                return;
            }
        }

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            LootChest lc = plugin.getChestManager().getChestAt(clicked.getLocation());
            if (lc != null) {
                event.setCancelled(true);
                
                Inventory fakeInv = org.bukkit.Bukkit.createInventory(new BlockInventoryHolder() {
                    @Override public Block getBlock() { return clicked; }
                    @Override public Inventory getInventory() { return null; }
                }, 27);
                player.openInventory(fakeInv);
                
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    plugin.getChestManager().openChest(lc, player);
                });
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        
        LootChest lc = plugin.getChestManager().getOpenChest(player);
        if (lc == null) return;

        if (plugin.getAnimationManager().isAnimating(player) && !plugin.getAnimationManager().canPickup(player)) {
            event.setCancelled(true);
            return;
        }

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (isInventoryEmpty(event.getInventory())) {
                lc.setLootTaken(true);
                player.closeInventory();
            }
        });
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!plugin.getAnimationManager().isAnimating(player)) return;
        if (plugin.getAnimationManager().canPickup(player)) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        LootChest lc = plugin.getChestManager().getOpenChest(player);
        
        if (plugin.getAnimationManager().isAnimating(player)) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (lc != null) {
                    plugin.getChestManager().openChest(lc, player);
                }
            });
            return;
        }

        if (lc != null && lc.isAnimationFinished()) {
            if (isInventoryEmpty(event.getInventory())) {
                lc.setLootTaken(true);
            }
        }
        
        plugin.getChestManager().setOpenChest(player, null);
        plugin.getAnimationManager().stopAnimation(player);
    }

    private boolean isInventoryEmpty(Inventory inv) {
        for (ItemStack item : inv.getContents()) {
            if (item != null && !item.getType().isAir()) {
                return false;
            }
        }
        return true;
    }
}