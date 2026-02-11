package id.naturalsmp.naturalpass.gui;

import id.naturalsmp.naturalpass.NaturalPass;
import id.naturalsmp.naturalpass.models.EditableReward;
import id.naturalsmp.naturalpass.models.Reward;
import id.naturalsmp.naturalpass.utils.GradientColorParser;
import id.naturalsmp.naturalpass.utils.ItemSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class LevelRewardEditGui extends BaseGui {

    private final Player player;
    private final int level;
    private final boolean isPremium;
    private final List<EditableReward> currentRewards;

    public LevelRewardEditGui(NaturalPass plugin, Player player, int level, boolean isPremium) {
        super(plugin, GradientColorParser.parse((isPremium
                ? "<gradient:#FFD700:#FF6B6B>"
                : "<gradient:#4ECDC4:#45B7D1>") + "Edit Level " + level + " " +
                (isPremium ? "Premium" : "Free") + " Rewards</gradient>"), 54);
        this.player = player;
        this.level = level;
        this.isPremium = isPremium;
        this.currentRewards = new ArrayList<>();
        loadCurrentRewards();
    }

    private void loadCurrentRewards() {
        try {
            List<EditableReward> rewards = plugin.getRewardManager().getEditableRewards(level, isPremium);
            if (rewards != null) {
                currentRewards.addAll(rewards);
            }
        } catch (Exception e) {
            // If the method doesn't exist, leave the list empty
        }
    }

    public void open() {
        if (!player.hasPermission("NaturalPass.admin")) {
            player.sendMessage(plugin.getMessageManager().getPrefix() +
                    GradientColorParser.parse(
                            "<gradient:#FF0000:#FF6B6B>\u2717 You don't have permission to access this!</gradient>"));
            return;
        }

        Inventory gui = createInventory();

        int slot = 0;
        for (EditableReward reward : currentRewards) {
            if (slot >= 36)
                break;

            if (reward.isCommand()) {
                ItemStack commandItem = new ItemStack(Material.COMMAND_BLOCK);
                ItemMeta meta = commandItem.getItemMeta();
                meta.setDisplayName(GradientColorParser.parse("<gradient:#FFD700:#FF6B6B>Command Reward</gradient>"));

                List<String> lore = new ArrayList<>();
                lore.add("");
                lore.add(GradientColorParser.parse("\u00a77Display: \u00a7f" + reward.getDisplayName()));
                lore.add(GradientColorParser.parse("\u00a77Command: \u00a7f" + reward.getCommand()));
                lore.add("");
                lore.add(GradientColorParser.parse("<gradient:#FF6B6B:#FF0000>\u25b6 CLICK TO REMOVE</gradient>"));

                meta.setLore(lore);
                commandItem.setItemMeta(meta);
                gui.setItem(slot, commandItem);
            } else {
                ItemStack item = reward.getItemStack();
                ItemMeta meta = item.getItemMeta();
                List<String> originalLore = meta.hasLore() ? meta.getLore() : new ArrayList<>();

                List<String> newLore = new ArrayList<>(originalLore);
                newLore.add("");
                newLore.add(GradientColorParser.parse("<gradient:#FF6B6B:#FF0000>\u25b6 CLICK TO REMOVE</gradient>"));

                meta.setLore(newLore);
                item.setItemMeta(meta);
                gui.setItem(slot, item);
            }
            slot++;
        }

        ItemStack addItem = new ItemStack(Material.LIME_DYE);
        ItemMeta addMeta = addItem.getItemMeta();
        addMeta.setDisplayName(GradientColorParser.parse("<gradient:#00FF88:#45B7D1>+ Add Item Reward</gradient>"));

        List<String> addLore = new ArrayList<>();
        addLore.add("");
        addLore.add(GradientColorParser.parse("\u00a77Drag items from your inventory"));
        addLore.add(GradientColorParser.parse("\u00a77to the empty slots above to add"));
        addLore.add(GradientColorParser.parse("\u00a77them as rewards for this level"));
        addLore.add("");
        addLore.add(GradientColorParser.parse("\u00a77Supports all item types including:"));
        addLore.add(GradientColorParser.parse("\u00a78\u2022 \u00a77Custom items with NBT"));
        addLore.add(GradientColorParser.parse("\u00a78\u2022 \u00a77Items with lore"));
        addLore.add(GradientColorParser.parse("\u00a78\u2022 \u00a77Enchanted items"));

        addMeta.setLore(addLore);
        addItem.setItemMeta(addMeta);
        gui.setItem(45, addItem);

        ItemStack addCommand = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta cmdMeta = addCommand.getItemMeta();
        cmdMeta.setDisplayName(GradientColorParser.parse("<gradient:#FFD700:#FF6B6B>+ Add Command Reward</gradient>"));

        List<String> cmdLore = new ArrayList<>();
        cmdLore.add("");
        cmdLore.add(GradientColorParser.parse("\u00a77Add a command reward"));
        cmdLore.add(GradientColorParser.parse("\u00a77Type in chat after clicking:"));
        cmdLore.add(GradientColorParser.parse("\u00a7e/cmdreward <display> | <command>"));
        cmdLore.add("");
        cmdLore.add(GradientColorParser.parse("\u00a77Example:"));
        cmdLore.add(GradientColorParser.parse("\u00a7e/cmdreward $1000 | eco give <player> 1000"));
        cmdLore.add("");
        cmdLore.add(GradientColorParser.parse("<gradient:#FFD700:#FF6B6B>\u25b6 CLICK TO ADD COMMAND</gradient>"));

        cmdMeta.setLore(cmdLore);
        addCommand.setItemMeta(cmdMeta);
        gui.setItem(46, addCommand);

        ItemStack saveButton = new ItemStack(Material.EMERALD);
        ItemMeta saveMeta = saveButton.getItemMeta();
        saveMeta.setDisplayName(GradientColorParser.parse("<gradient:#00FF88:#45B7D1>\u2713 Save Changes</gradient>"));

        List<String> saveLore = new ArrayList<>();
        saveLore.add("");
        saveLore.add(GradientColorParser.parse("\u00a77Save changes for this level"));
        saveLore.add(GradientColorParser.parse("\u00a77and return to level selection"));
        saveLore.add("");
        saveLore.add(GradientColorParser.parse("<gradient:#00FF88:#45B7D1>\u25b6 CLICK TO SAVE</gradient>"));

        saveMeta.setLore(saveLore);
        saveButton.setItemMeta(saveMeta);
        gui.setItem(49, saveButton);

        ItemStack cancelButton = new ItemStack(Material.BARRIER);
        ItemMeta cancelMeta = cancelButton.getItemMeta();
        cancelMeta.setDisplayName(GradientColorParser.parse("<gradient:#FF6B6B:#FF0000>\u2717 Cancel</gradient>"));

        List<String> cancelLore = new ArrayList<>();
        cancelLore.add("");
        cancelLore.add(GradientColorParser.parse("\u00a77Discard changes and return"));
        cancelLore.add(GradientColorParser.parse("\u00a77to level selection"));

        cancelMeta.setLore(cancelLore);
        cancelButton.setItemMeta(cancelMeta);
        gui.setItem(48, cancelButton);

        ItemStack clearButton = new ItemStack(Material.TNT);
        ItemMeta clearMeta = clearButton.getItemMeta();
        clearMeta.setDisplayName(
                GradientColorParser.parse("<gradient:#FF0000:#FF6B6B>\u26a0 Clear All Rewards</gradient>"));

        List<String> clearLore = new ArrayList<>();
        clearLore.add("");
        clearLore.add(GradientColorParser.parse("\u00a77Remove all rewards"));
        clearLore.add(GradientColorParser.parse("\u00a77from this level"));
        clearLore.add("");
        clearLore.add(GradientColorParser.parse("<gradient:#FF0000:#FF6B6B>\u26a0 This cannot be undone!</gradient>"));

        clearMeta.setLore(clearLore);
        clearButton.setItemMeta(clearMeta);
        gui.setItem(50, clearButton);

        ItemStack info = new ItemStack(Material.BOOK);
        ItemMeta infoMeta = info.getItemMeta();
        infoMeta.setDisplayName(
                GradientColorParser.parse("<gradient:#FFE66D:#FF6B6B>Level " + level + " Information</gradient>"));

        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add(GradientColorParser.parse("\u00a77Type: " + (isPremium ? "\u00a76Premium" : "\u00a7bFree")));
        infoLore.add(GradientColorParser.parse("\u00a77Current rewards: \u00a7f" + currentRewards.size()));
        infoLore.add("");
        infoLore.add(GradientColorParser.parse("\u00a78Drag items to add"));
        infoLore.add(GradientColorParser.parse("\u00a78Click items to remove"));

        infoMeta.setLore(infoLore);
        info.setItemMeta(infoMeta);
        gui.setItem(53, info);

        player.openInventory(gui);
    }

    public List<EditableReward> getCurrentRewards() {
        return currentRewards;
    }

    public void addReward(EditableReward reward) {
        currentRewards.add(reward);
    }

    public void removeReward(int index) {
        if (index >= 0 && index < currentRewards.size()) {
            currentRewards.remove(index);
        }
    }

    public void clearRewards() {
        currentRewards.clear();
    }

    public int getLevel() {
        return level;
    }

    public boolean isPremium() {
        return isPremium;
    }
}
