package ru.rtp674637.mschest;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MsChestCommand implements CommandExecutor, TabCompleter {

    private final MsChest plugin;

    public MsChestCommand(MsChest plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
                             @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage(plugin.parseColor("&eКоманды MsChest:"));
            player.sendMessage(plugin.parseColor("&e/MsChest want &7- палка для создания"));
            player.sendMessage(plugin.parseColor("&e/MsChest edit <rarity> &7- сменить редкость сундука"));
            player.sendMessage(plugin.parseColor("&e/MsChest edit add <rarity> [chance] &7- добавить лут из руки"));
            player.sendMessage(plugin.parseColor("&e/MsChest edit remove <rarity> <material> &7- удалить лут"));
            player.sendMessage(plugin.parseColor("&e/MsChest edit list <rarity> &7- показать список"));
            player.sendMessage(plugin.parseColor("&e/MsChest restart &7- обновить лут во всех сундуках"));
            player.sendMessage(plugin.parseColor("&e/MsChest reloadloot &7- перезагрузить loot.yml"));
            return true;
        }

        if (args[0].equalsIgnoreCase("want")) {
            ItemStack stick = new ItemStack(Material.STICK);
            ItemMeta meta = stick.getItemMeta();
            if (meta != null) {
                meta.displayName(plugin.parseColor("&#FF00FF&lПалка MsChest"));
                meta.lore(List.of(
                        plugin.parseColor("&8&m-----------------------"),
                        plugin.parseColor("&7ПКМ по блоку: &aсоздать сундук"),
                        plugin.parseColor("&7ЛКМ по сундуку: &cудалить"),
                        plugin.parseColor("&8&m-----------------------")
                ));
                meta.addEnchant(Enchantment.UNBREAKING, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
                stick.setItemMeta(meta);
            }
            player.getInventory().addItem(stick);
            player.sendMessage(plugin.getMessage("wand-given"));
            return true;
        }

        if (args[0].equalsIgnoreCase("edit")) {
            if (!player.hasPermission("mschest.admin")) {
                player.sendMessage(plugin.getMessage("no-permission"));
                return true;
            }
            if (args.length < 2) {
                player.sendMessage(plugin.parseColor("<red>Использование: /MsChest edit <add|remove|list> <rarity> ..."));
                return true;
            }
            String sub = args[1].toLowerCase();

            if (sub.equals("add")) {
                if (args.length < 3) {
                    player.sendMessage(plugin.parseColor("<red>Использование: /MsChest edit add <rarity> [chance]"));
                    return true;
                }
                Rarity rarity = Rarity.fromString(args[2]);
                if (rarity == null) {
                    player.sendMessage(plugin.parseColor("<red>Неверная редкость."));
                    return true;
                }
                ItemStack inHand = player.getInventory().getItemInMainHand();
                if (inHand == null || inHand.getType().isAir()) {
                    player.sendMessage(plugin.parseColor("<red>Возьмите предмет в главную руку."));
                    return true;
                }
                double chance = 10.0;
                if (args.length >= 4) {
                    try { chance = Double.parseDouble(args[3]); } catch (NumberFormatException e) { chance = 10.0; }
                }
                if (chance < 0) chance = 0;
                if (chance > 100) chance = 100;
                int amount = inHand.getAmount();
                plugin.getLootManager().addItem(rarity, inHand.getType(), amount, chance);

                String rarityName = plugin.getConfig().getString("chest.rarity-names." + rarity.getConfigKey(), rarity.name());
                player.sendMessage(plugin.getMessage("loot-added",
                        "%item%", inHand.getType().name() + " x" + amount + " (" + chance + "%)",
                        "%rarity%", rarityName));
                return true;
            }

            if (sub.equals("remove")) {
                if (args.length < 4) {
                    player.sendMessage(plugin.parseColor("<red>Использование: /MsChest edit remove <rarity> <material>"));
                    return true;
                }
                Rarity rarity = Rarity.fromString(args[2]);
                if (rarity == null) {
                    player.sendMessage(plugin.parseColor("<red>Неверная редкость."));
                    return true;
                }
                Material material = Material.matchMaterial(args[3]);
                if (material == null) {
                    player.sendMessage(plugin.parseColor("<red>Неизвестный материал: " + args[3]));
                    return true;
                }
                boolean removed = plugin.getLootManager().removeItem(rarity, material);
                if (removed) {
                    String rarityName = plugin.getConfig().getString("chest.rarity-names." + rarity.getConfigKey(), rarity.name());
                    player.sendMessage(plugin.getMessage("loot-removed",
                            "%item%", material.name(),
                            "%rarity%", rarityName));
                } else {
                    player.sendMessage(plugin.parseColor("<red>Этот предмет не найден в списке редкости."));
                }
                return true;
            }

            if (sub.equals("list")) {
                if (args.length < 3) {
                    player.sendMessage(plugin.parseColor("<red>Использование: /MsChest edit list <rarity>"));
                    return true;
                }
                Rarity rarity = Rarity.fromString(args[2]);
                if (rarity == null) {
                    player.sendMessage(plugin.parseColor("<red>Неверная редкость."));
                    return true;
                }
                List<String> list = plugin.getLootManager().getLootList(rarity);
                String rarityName = plugin.getConfig().getString("chest.rarity-names." + rarity.getConfigKey(), rarity.name());
                player.sendMessage(plugin.getMessage("loot-list-header", "%rarity%", rarityName));
                if (list.isEmpty()) {
                    player.sendMessage(plugin.getMessage("loot-list-empty"));
                } else {
                    for (String entry : list) {
                        player.sendMessage(plugin.getMessage("loot-list-entry", "%item%", entry));
                    }
                }
                return true;
            }

            Rarity rarity = Rarity.fromString(args[1]);
            if (rarity == null) {
                player.sendMessage(plugin.parseColor("<red>Неверная редкость или подкоманда."));
                return true;
            }
            var targetBlock = player.getTargetBlockExact(5);
            if (targetBlock == null || !plugin.getChestManager().isLootChestBlock(targetBlock.getLocation())) {
                player.sendMessage(plugin.parseColor("<red>Посмотрите на сундук MsChest в пределах 5 блоков."));
                return true;
            }
            LootChest lc = plugin.getChestManager().getChestAt(targetBlock.getLocation());
            lc.setRarity(rarity);
            plugin.getChestManager().updateHolograms();
            plugin.getChestManager().saveAll();
            String rarityName = plugin.getConfig().getString("chest.rarity-names." + rarity.getConfigKey(), rarity.name());
            player.sendMessage(plugin.getMessage("edit-rarity-set", "%rarity%", rarityName));
            return true;
        }

        if (args[0].equalsIgnoreCase("restart")) {
            if (!player.hasPermission("mschest.admin")) {
                player.sendMessage(plugin.getMessage("no-permission"));
                return true;
            }
            plugin.getChestManager().restartAllChests();
            player.sendMessage(plugin.getMessage("restart-done"));
            return true;
        }

        if (args[0].equalsIgnoreCase("reloadloot")) {
            if (!player.hasPermission("mschest.admin")) {
                player.sendMessage(plugin.getMessage("no-permission"));
                return true;
            }
            plugin.getLootManager().reload();
            player.sendMessage(plugin.getMessage("loot-file-reloaded"));
            return true;
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                 @NotNull String alias, @NotNull String[] args) {
        List<String> completions = new ArrayList<>();
        if (args.length == 1) {
            completions.add("want");
            if (sender.hasPermission("mschest.admin")) {
                completions.add("edit");
                completions.add("restart");
                completions.add("reloadloot");
            }
        } else if (args.length == 2 && args[0].equalsIgnoreCase("edit") && sender.hasPermission("mschest.admin")) {
            completions.addAll(Arrays.asList("add", "remove", "list"));
            for (Rarity r : Rarity.values()) completions.add(r.getConfigKey());
        } else if (args.length == 3 && args[0].equalsIgnoreCase("edit") && sender.hasPermission("mschest.admin")) {
            for (Rarity r : Rarity.values()) completions.add(r.getConfigKey());
        } else if (args.length == 4 && args[0].equalsIgnoreCase("edit")
                && args[1].equalsIgnoreCase("remove")
                && sender.hasPermission("mschest.admin")) {
            for (Material m : Material.values()) {
                if (m.isItem() && !m.isAir()) completions.add(m.name());
            }
        }
        String input = args[args.length - 1].toLowerCase();
        completions.removeIf(s -> !s.toLowerCase().startsWith(input));
        if (completions.size() > 100) return completions.subList(0, 100);
        return completions;
    }
}