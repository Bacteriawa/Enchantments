package dev.bacteriawa;

import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashMap;
import java.util.Map;

public class EnchantmentsExtractor implements Listener {

    public EnchantmentsExtractor(Enchantments plugins) {
        this.plugins = plugins;
    }

    private final Enchantments plugins;

    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        AnvilInventory inventory = event.getInventory();
        ItemStack firstItem = inventory.getItem(0);
        ItemStack secondItem = inventory.getItem(1);

        if (firstItem == null || secondItem == null) return;
        if (firstItem.getType() != Material.BOOK) return;
        if (!hasEnchantments(secondItem)) return;

        HumanEntity human = event.getView().getPlayer();
        if (!(human instanceof Player)) return;
        Player player = (Player) human;

        int requiredLevel = plugins.getConfig().getInt("enchantment-extractor.required-level", 10);
        if (!player.getGameMode().equals(GameMode.CREATIVE) && player.getLevel() < requiredLevel) {
            inventory.setRepairCost(requiredLevel);
            inventory.setItem(2, createErrorItem(requiredLevel));
            event.setResult(null);
        }

        ItemStack result = new ItemStack(Material.ENCHANTED_BOOK);
        EnchantmentStorageMeta meta = (EnchantmentStorageMeta) result.getItemMeta();
        getEnchantments(secondItem).forEach((enchant, level) ->
                meta.addStoredEnchant(enchant, level, true)
        );
        result.setItemMeta(meta);

        Map<Enchantment, Integer> enchants = getEnchantments(secondItem);
        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            meta.addStoredEnchant(entry.getKey(),entry.getValue(),true);
        }

        event.setResult(result);
        inventory.setRepairCost(requiredLevel);
    }

    @EventHandler
    public void onAnvilComplete(InventoryCloseEvent event) {
        if (!(event.getInventory() instanceof AnvilInventory)) return;
        AnvilInventory inventory = (AnvilInventory) event.getInventory();

        ItemStack firstItem = inventory.getItem(0);
        ItemStack secondItem = inventory.getItem(1);
        ItemStack resultItem = inventory.getItem(2);

        boolean isValidOperation =
                firstItem != null &&
                        secondItem != null &&
                        firstItem.getType() == Material.BOOK &&
                        hasEnchantments(secondItem) &&
                        resultItem != null &&
                        resultItem.getType() == Material.ENCHANTED_BOOK;
        if (!isValidOperation) return;

        HumanEntity human = event.getView().getPlayer();
        if (!(human instanceof Player)) return;
        Player player = (Player) human;

        int requiredLevel = plugins.getConfig().getInt("enchantment-extractor.required-level", 10);
        if (!player.getGameMode().equals(GameMode.CREATIVE) && player.getLevel() < requiredLevel) {
            player.sendMessage(ChatColor.RED + "经验不足，无法提取附魔！");
            return;
        }

        if (firstItem.getAmount() > 1) {
            firstItem.setAmount(firstItem.getAmount() - 1);
            inventory.setItem(0, firstItem);
        } else {
            inventory.setItem(0, null);
        }

        ItemStack cleanItem = removeEnchants(secondItem);
        if (secondItem.getAmount() > 1) {
            secondItem.setAmount(secondItem.getAmount() - 1);
            inventory.setItem(1, secondItem);
            if (player.getInventory().addItem(cleanItem).isEmpty()) {
                player.getWorld().dropItem(player.getLocation(), cleanItem);
            }
        } else {
            inventory.setItem(1, cleanItem);
        }

        if (resultItem != null && resultItem.getType() == Material.ENCHANTED_BOOK) {
            if (player.getInventory().firstEmpty() == -1) {
                player.getWorld().dropItem(player.getLocation(), resultItem);
                player.sendMessage(ChatColor.YELLOW + "你的背包已满，附魔书已掉落在地上！");
            } else {
                player.getInventory().addItem(resultItem);
            }
        }

        if (!player.getGameMode().equals(GameMode.CREATIVE)) {
            player.setLevel(player.getLevel() - requiredLevel);
        }

        if (plugins.getConfig().getBoolean("enchantment-extractor.show-success-message", true)) {
            String message = plugins.getConfig().getString("enchantment-extractor.success-message", "&a成功提取附魔！消耗了10级经验！");
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
        }
    }

    private boolean hasEnchantments(ItemStack item) {
        return item != null && item.hasItemMeta() && !item.getItemMeta().getEnchants().isEmpty();
    }

    private Map<Enchantment, Integer> getEnchantments(ItemStack item) {
        if (item == null) return new HashMap<>();
        ItemMeta meta = item.getItemMeta();
        return meta != null ? meta.getEnchants() : new HashMap<>();
    }

    private ItemStack createErrorItem(int requiredLevel) {
        ItemStack errorItem = new ItemStack(Material.BARRIER);
        ItemMeta meta = errorItem.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "需要" + requiredLevel + "级经验");
        errorItem.setItemMeta(meta);
        return errorItem;
    }

    private ItemStack removeEnchants(ItemStack item) {
        ItemStack cleanItem = item.clone();
        ItemMeta meta = cleanItem.getItemMeta();
        new HashMap<>(meta.getEnchants()).keySet().forEach(meta::removeEnchant);
        cleanItem.setItemMeta(meta);
        return cleanItem;
    }
}
