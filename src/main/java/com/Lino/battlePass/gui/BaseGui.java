package com.Lino.battlePass.gui;

import com.Lino.battlePass.BattlePass;
import com.Lino.battlePass.utils.GradientColorParser;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.inventory.InventoryHolder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseGui implements InventoryHolder {

    protected final BattlePass plugin;
    protected final String title;
    protected final int size;

    public BaseGui(BattlePass plugin, String title, int size) {
        this.plugin = plugin;
        this.title = title;
        this.size = size;
    }

    public Inventory createInventory() {
        return Bukkit.createInventory(this, size, title);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return null; // Not needed as we use the holder for identification
    }

    protected ItemStack createItem(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (displayName != null) {
            meta.setDisplayName(displayName);
        }

        if (lore != null && !lore.isEmpty()) {
            meta.setLore(lore);
        }

        item.setItemMeta(meta);
        return item;
    }

    protected ItemStack createBackButton() {
        ItemStack back = new ItemStack(Material.BARRIER);
        ItemMeta meta = back.getItemMeta();
        meta.setDisplayName(plugin.getMessageManager().getMessage("items.back-button.name"));

        List<String> lore = new ArrayList<>();
        for (String line : plugin.getMessageManager().getMessagesConfig().getStringList("items.back-button.lore")) {
            lore.add(GradientColorParser.parse(line));
        }
        meta.setLore(lore);
        back.setItemMeta(meta);

        return back;
    }

    protected void fillEmptySlots(Inventory inventory) {
        ItemStack separator = new ItemStack(plugin.getConfigManager().getGuiSeparatorMaterial());
        ItemMeta meta = separator.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getMessageManager().getMessage("items.separator.name"));
            separator.setItemMeta(meta);
        }

        for (int i = 0; i < inventory.getSize(); i++) {
            if (inventory.getItem(i) == null || inventory.getItem(i).getType() == Material.AIR) {
                inventory.setItem(i, separator);
            }
        }
    }

    protected String formatMaterial(Material material) {
        return material.name().toLowerCase().replace("_", " ");
    }
}