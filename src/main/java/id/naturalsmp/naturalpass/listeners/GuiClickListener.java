package id.naturalsmp.naturalpass.listeners;

import id.naturalsmp.naturalpass.NaturalPass;
import id.naturalsmp.naturalpass.models.PlayerData;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

public class GuiClickListener implements Listener {

    private final NaturalPass plugin;
    private final java.util.Set<UUID> processing = new java.util.HashSet<>();

    public GuiClickListener(NaturalPass plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof id.naturalsmp.naturalpass.gui.BaseGui)) {
            return;
        }

        String title = event.getView().getTitle();
        Player player = (Player) event.getWhoClicked();

        boolean isNaturalPassGUI = title
                .startsWith(plugin.getMessageManager().getMessage("gui.NaturalPass").split("%")[0]);

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta())
            return;

        if (processing.contains(player.getUniqueId()))
            return;
        processing.add(player.getUniqueId());
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> processing.remove(player.getUniqueId()), 10L); // 0.5s
                                                                                                                    // cooldown

        if (isNaturalPassGUI) {
            handleNaturalPassClick(player, clicked, event.getSlot());
        } else if (title.equals(plugin.getMessageManager().getMessage("gui.leaderboard"))) {
            if (clicked.getType() == Material.BARRIER) {
                int page = plugin.getGuiManager().getCurrentPages().getOrDefault(player.getEntityId(), 1);
                plugin.getGuiManager().openNaturalPassGUI(player, page);
            }
        } else if (title.equals(plugin.getMessageManager().getMessage("gui.missions"))) {
            handleMissionsClick(player, clicked, event.getSlot());
        } else if (title.equals(plugin.getMessageManager().getMessage("gui.shop"))) {
            handleShopClick(player, clicked, event.getSlot());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof id.naturalsmp.naturalpass.gui.BaseGui) {
            event.setCancelled(true);
        }
    }

    private void handleMissionsClick(Player player, ItemStack clicked, int slot) {
        if (clicked.getType() == Material.BARRIER) {
            int page = plugin.getGuiManager().getCurrentPages().getOrDefault(player.getEntityId(), 1);
            plugin.getGuiManager().openNaturalPassGUI(player, page);
        } else if (clicked.getType() == Material.TNT && slot == 45) {
            if (player.hasPermission("NaturalPass.admin")) {
                player.closeInventory();
                plugin.getMissionManager().forceResetMissions();
                player.sendMessage(plugin.getMessageManager().getPrefix() +
                        plugin.getMessageManager().getMessage("messages.mission.reset-complete"));

                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                    plugin.getGuiManager().openMissionsGUI(player);
                }, 20L);
            }
        } else if (clicked.getType() == Material.WRITABLE_BOOK && slot == 53) {
            if (player.hasPermission("NaturalPass.admin")) {
                plugin.getMissionEditorManager().openMissionEditor(player, 1);
            }
        }
    }

    private void handleNaturalPassClick(Player player, ItemStack clicked, int slot) {
        int currentPage = plugin.getGuiManager().getCurrentPages().getOrDefault(player.getEntityId(), 1);

        if (clicked.getType() == Material.ARROW && clicked.hasItemMeta()) {
            var meta = clicked.getItemMeta();
            if (meta.getPersistentDataContainer().has(plugin.getEventManager().getNavigationKey(),
                    PersistentDataType.STRING)) {
                String action = meta.getPersistentDataContainer().get(plugin.getEventManager().getNavigationKey(),
                        PersistentDataType.STRING);

                // Calcolo dinamico delle pagine massime
                int maxLevel = plugin.getRewardManager().getMaxLevel();
                int maxPages = (int) Math.ceil(maxLevel / 7.0);
                if (maxPages < 1)
                    maxPages = 1;

                if ("previous".equals(action) && currentPage > 1) {
                    plugin.getGuiManager().openNaturalPassGUI(player, currentPage - 1);
                } else if ("next".equals(action) && currentPage < maxPages) {
                    plugin.getGuiManager().openNaturalPassGUI(player, currentPage + 1);
                }
                return;
            }
        }

        switch (clicked.getType()) {
            case BOOK:
                plugin.getGuiManager().openMissionsGUI(player);
                break;

            case GOLDEN_HELMET:
                plugin.getGuiManager().openLeaderboardGUI(player);
                break;

            case GOLD_INGOT:
                plugin.getGuiManager().openShopGUI(player);
                break;

            case SUNFLOWER:
                handleDailyRewardClaim(player, currentPage);
                break;

            case COMMAND_BLOCK:
                if (player.hasPermission("NaturalPass.admin") && slot == 46) {
                    new id.naturalsmp.naturalpass.gui.RewardsEditorGui(plugin, player).open();
                }
                break;

            default:
                if (clicked.getType() == plugin.getConfigManager().getGuiRewardAvailableMaterial()) {
                    handleRewardClaim(player, slot, currentPage);
                }
                break;
        }
    }

    private void handleShopClick(Player player, ItemStack clicked, int slot) {
        if (clicked.getType() == Material.BARRIER) {
            int page = plugin.getGuiManager().getCurrentPages().getOrDefault(player.getEntityId(), 1);
            plugin.getGuiManager().openNaturalPassGUI(player, page);
            return;
        }

        if (slot == 4)
            return;

        plugin.getShopManager().purchaseItem(player, slot);
    }

    private void handleRewardClaim(Player player, int slot, int currentPage) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        int startLevel = (currentPage - 1) * 7 + 1;

        // Slots: 10, 11, 12, 13, 14, 15, 16
        if (slot >= 10 && slot <= 16) {
            int index = slot - 10;
            int level = startLevel + index;

            if (!data.hasPremium) {
                player.sendMessage(plugin.getMessageManager().getPrefix() +
                        plugin.getMessageManager().getMessage("messages.premium.required"));
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                return;
            }

            List<id.naturalsmp.naturalpass.models.Reward> levelRewards = plugin.getRewardManager()
                    .getPremiumRewardsByLevel()
                    .get(level);
            if (levelRewards != null && !levelRewards.isEmpty()) {
                if (data.level >= level && !data.claimedPremiumRewards.contains(level)) {
                    plugin.getRewardManager().claimRewards(player, data, levelRewards, level, true);
                    plugin.getGuiManager().openNaturalPassGUI(player, currentPage);
                } else {
                    player.sendMessage(plugin.getMessageManager().getPrefix() +
                            plugin.getMessageManager().getMessage("messages.rewards.cannot-claim"));
                }
            }

        }
        // Slots: 28, 29, 30, 31, 32, 33, 34
        else if (slot >= 28 && slot <= 34) {
            int index = slot - 28;
            int level = startLevel + index;

            List<id.naturalsmp.naturalpass.models.Reward> levelRewards = plugin.getRewardManager()
                    .getFreeRewardsByLevel()
                    .get(level);
            if (levelRewards != null && !levelRewards.isEmpty()) {
                if (data.level >= level && !data.claimedFreeRewards.contains(level)) {
                    plugin.getRewardManager().claimRewards(player, data, levelRewards, level, false);
                    plugin.getGuiManager().openNaturalPassGUI(player, currentPage);
                } else {
                    player.sendMessage(plugin.getMessageManager().getPrefix() +
                            plugin.getMessageManager().getMessage("messages.rewards.cannot-claim"));
                }
            }
        }
    }

    private void handleDailyRewardClaim(Player player, int currentPage) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        long now = System.currentTimeMillis();
        long dayInMillis = 24 * 60 * 60 * 1000;

        if (now - data.lastDailyReward >= dayInMillis) {
            int xpReward = plugin.getConfigManager().getDailyRewardXP();
            data.xp += xpReward;
            data.lastDailyReward = now;

            int xpPerLevel = plugin.getConfigManager().getXpPerLevel();
            boolean leveled = false;
            int maxLevel = plugin.getRewardManager().getMaxLevel();

            while (data.xp >= xpPerLevel && data.level < maxLevel) {
                data.xp -= xpPerLevel;
                data.level++;
                data.totalLevels++;
                leveled = true;

                player.sendMessage(plugin.getMessageManager().getPrefix() +
                        plugin.getMessageManager().getMessage("messages.level-up",
                                "%level%", String.valueOf(data.level)));
                player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

                int available = plugin.getRewardManager().countAvailableRewards(player, data);
                if (available > 0) {
                    player.sendMessage(plugin.getMessageManager().getPrefix() +
                            plugin.getMessageManager().getMessage("messages.new-rewards"));
                }
            }

            plugin.getPlayerDataManager().markForSave(player.getUniqueId());

            player.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage("messages.daily-reward.claimed",
                            "%amount%", String.valueOf(xpReward)));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

            plugin.getGuiManager().openNaturalPassGUI(player, currentPage);
        } else {
            player.sendMessage(plugin.getMessageManager().getPrefix() +
                    plugin.getMessageManager().getMessage("messages.daily-reward.already-claimed"));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
        }
    }
}
