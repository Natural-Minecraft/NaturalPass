package id.naturalsmp.naturalpass.gui;

import id.naturalsmp.naturalpass.NaturalPass;
import id.naturalsmp.naturalpass.models.PlayerData;
import id.naturalsmp.naturalpass.models.Reward;
import id.naturalsmp.naturalpass.utils.GradientColorParser;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class NaturalPassGui extends BaseGui {

    private final Player player;
    private final PlayerData playerData;
    private final int page;
    private final int maxLevel;

    public NaturalPassGui(NaturalPass plugin, Player player, int page) {
        super(plugin, plugin.getMessageManager().getMessage("gui.NaturalPass", "%page%", String.valueOf(page)), 54);
        this.player = player;
        this.playerData = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        this.page = page;
        this.maxLevel = plugin.getRewardManager().getMaxLevel();
    }

    public void open() {
        Inventory gui = createInventory();

        setupBackground(gui);
        setupProgressItem(gui);
        setupRewards(gui);
        setupNavigationButtons(gui);
        setupActionButtons(gui);

        fillEmptySlots(gui);

        player.openInventory(gui);
        plugin.getGuiManager().getCurrentPages().put(player.getEntityId(), page);
    }

    private void setupBackground(Inventory gui) {
        // We'll use fillEmptySlots at the end, but we can set specific background
        // patterns here if needed.
        // For now, let's just ensure the middle row (separator) is set explicitly if we
        // want a specific style.
        ItemStack separator = new ItemStack(plugin.getConfigManager().getGuiSeparatorMaterial());
        ItemMeta meta = separator.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(plugin.getMessageManager().getMessage("items.separator.name"));
            separator.setItemMeta(meta);
        }

        for (int i = 18; i < 27; i++) {
            gui.setItem(i, separator);
        }
    }

    private void setupProgressItem(Inventory gui) {
        ItemStack info = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = info.getItemMeta();
        meta.setDisplayName(plugin.getMessageManager().getMessage("items.progress.name"));

        List<String> lore = new ArrayList<>();
        String premiumStatus = playerData.hasPremium
                ? plugin.getMessageManager().getMessage("items.premium-status.active")
                : plugin.getMessageManager().getMessage("items.premium-status.inactive");

        for (String line : plugin.getMessageManager().getMessagesConfig().getStringList("items.progress.lore")) {
            String processedLine = line
                    .replace("%level%", String.valueOf(playerData.level))
                    .replace("%xp%", String.valueOf(playerData.xp))
                    .replace("%xp_needed%", String.valueOf(plugin.getConfigManager().getXpPerLevel()))
                    .replace("%premium_status%", premiumStatus)
                    .replace("%season_time%", plugin.getMissionManager().getTimeUntilSeasonEnd());
            lore.add(GradientColorParser.parse(processedLine));
        }

        meta.setLore(lore);
        info.setItemMeta(meta);
        gui.setItem(4, info);
    }

    private void setupRewards(Inventory gui) {
        int startLevel = (page - 1) * 7 + 1; // Changed from 9 to 7 to center rewards
        Map<Integer, List<Reward>> premiumRewards = plugin.getRewardManager().getPremiumRewardsByLevel();
        Map<Integer, List<Reward>> freeRewards = plugin.getRewardManager().getFreeRewardsByLevel();

        // Slots for centered rewards (7 rewards per page, skipping first and last
        // columns)
        int[] slots = { 10, 11, 12, 13, 14, 15, 16 };
        int[] freeSlots = { 28, 29, 30, 31, 32, 33, 34 };

        for (int i = 0; i < slots.length; i++) {
            int level = startLevel + i;
            if (level > maxLevel)
                break;

            // Premium Rewards Row (Middle-Top)
            List<Reward> premiumLevel = premiumRewards.get(level);
            if (premiumLevel != null && !premiumLevel.isEmpty()) {
                if (!playerData.claimedPremiumRewards.contains(level)) {
                    ItemStack premiumItem = createRewardItem(premiumLevel, level, playerData, playerData.hasPremium,
                            true);
                    gui.setItem(slots[i], premiumItem);
                } else {
                    if (!plugin.getConfigManager().shouldHidePremiumClaimedRewards()) {
                        ItemStack claimedItem = createClaimedRewardItem(premiumLevel, level, true);
                        if (claimedItem != null) {
                            gui.setItem(slots[i], claimedItem);
                        }
                    }
                }
            }

            // Free Rewards Row (Middle-Bottom)
            List<Reward> freeLevel = freeRewards.get(level);
            if (freeLevel != null && !freeLevel.isEmpty()) {
                if (!playerData.claimedFreeRewards.contains(level)) {
                    ItemStack freeItem = createRewardItem(freeLevel, level, playerData, true, false);
                    gui.setItem(freeSlots[i], freeItem);
                } else {
                    if (!plugin.getConfigManager().shouldHideFreeClaimedRewards()) {
                        ItemStack claimedItem = createClaimedRewardItem(freeLevel, level, false);
                        if (claimedItem != null) {
                            gui.setItem(freeSlots[i], claimedItem);
                        }
                    }
                }
            }
        }
    }

    private ItemStack createRewardItem(List<Reward> rewards, int level, PlayerData data, boolean hasAccess,
            boolean isPremium) {
        String rewardType = plugin.getMessageManager()
                .getMessage(isPremium ? "reward-types.premium" : "reward-types.free");

        if (data.level >= level && hasAccess) {
            ItemStack item = new ItemStack(plugin.getConfigManager().getGuiRewardAvailableMaterial());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(plugin.getMessageManager().getMessage("items.reward-available.name",
                    "%level%", String.valueOf(level),
                    "%type%", rewardType));

            List<String> lore = createRewardLore(rewards, "items.reward-available", rewardType);
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;

        } else if (!hasAccess && isPremium) {
            ItemStack item = new ItemStack(plugin.getConfigManager().getGuiPremiumNoPassMaterial());
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(plugin.getMessageManager().getMessage("items.reward-premium-locked.name",
                    "%level%", String.valueOf(level)));

            List<String> lore = createRewardLore(rewards, "items.reward-premium-locked", rewardType);
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;

        } else {
            Material lockedMaterial = isPremium ? plugin.getConfigManager().getGuiPremiumLockedMaterial()
                    : plugin.getConfigManager().getGuiFreeLockedMaterial();

            ItemStack item = new ItemStack(lockedMaterial);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(plugin.getMessageManager().getMessage("items.reward-level-locked.name",
                    "%level%", String.valueOf(level),
                    "%type%", rewardType));

            List<String> lore = createRewardLore(rewards, "items.reward-level-locked", rewardType);
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        }
    }

    private ItemStack createClaimedRewardItem(List<Reward> rewards, int level, boolean isPremium) {
        Material material = isPremium ? plugin.getConfigManager().getGuiPremiumClaimedMaterial()
                : plugin.getConfigManager().getGuiFreeClaimedMaterial();

        if (material == null) {
            return null;
        }

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(plugin.getMessageManager().getMessage("items.reward-claimed.name",
                "%level%", String.valueOf(level),
                "%type%",
                plugin.getMessageManager().getMessage(isPremium ? "reward-types.premium" : "reward-types.free")));

        String rewardType = plugin.getMessageManager()
                .getMessage(isPremium ? "reward-types.premium" : "reward-types.free");
        List<String> lore = createRewardLore(rewards, "items.reward-claimed", rewardType);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private List<String> createRewardLore(List<Reward> rewards, String configPath, String rewardType) {
        List<String> lore = new ArrayList<>();

        for (String line : plugin.getMessageManager().getMessagesConfig().getStringList(configPath + ".lore-header")) {
            String processedLine = line.replace("%type%", rewardType);
            lore.add(GradientColorParser.parse(processedLine));
        }

        for (Reward r : rewards) {
            lore.add(plugin.getMessageManager().getMessage("messages.rewards.command-reward", "%reward%",
                    r.displayName));
        }

        for (String line : plugin.getMessageManager().getMessagesConfig().getStringList(configPath + ".lore-footer")) {
            String processedLine = line
                    .replace("%level%", String.valueOf(rewards.get(0).level))
                    .replace("%season_time%", plugin.getMissionManager().getTimeUntilSeasonEnd());
            lore.add(GradientColorParser.parse(processedLine));
        }

        return lore;
    }

    private void setupSeparator(Inventory gui) {
        // This is now redundant due to fillEmptySlots and setupBackground,
        // but kept for compatibility if needed.
    }

    private void setupNavigationButtons(Inventory gui) {
        if (page > 1) {
            gui.setItem(45, createNavigationItem(false, page - 1));
        }

        int maxPages = (int) Math.ceil(maxLevel / 7.0); // Updated to 7
        if (page < maxPages) {
            gui.setItem(53, createNavigationItem(true, page + 1));
        }
    }

    private ItemStack createNavigationItem(boolean next, int targetPage) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        if (next) {
            meta.setDisplayName(plugin.getMessageManager().getMessage("items.next-page.name"));
            List<String> lore = new ArrayList<>();
            for (String line : plugin.getMessageManager().getMessagesConfig().getStringList("items.next-page.lore")) {
                String processedLine = line.replace("%page%", String.valueOf(targetPage));
                lore.add(GradientColorParser.parse(processedLine));
            }
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(
                    plugin.getEventManager().getNavigationKey(),
                    PersistentDataType.STRING,
                    "next");
        } else {
            meta.setDisplayName(plugin.getMessageManager().getMessage("items.previous-page.name"));
            List<String> lore = new ArrayList<>();
            for (String line : plugin.getMessageManager().getMessagesConfig()
                    .getStringList("items.previous-page.lore")) {
                String processedLine = line.replace("%page%", String.valueOf(targetPage));
                lore.add(GradientColorParser.parse(processedLine));
            }
            meta.setLore(lore);
            meta.getPersistentDataContainer().set(
                    plugin.getEventManager().getNavigationKey(),
                    PersistentDataType.STRING,
                    "previous");
        }

        item.setItemMeta(meta);
        return item;
    }

    private void setupActionButtons(Inventory gui) {
        ItemStack missions = new ItemStack(Material.BOOK);
        ItemMeta missionsMeta = missions.getItemMeta();
        missionsMeta.setDisplayName(plugin.getMessageManager().getMessage("items.missions-button.name"));

        List<String> missionsLore = new ArrayList<>();
        for (String line : plugin.getMessageManager().getMessagesConfig().getStringList("items.missions-button.lore")) {
            String processedLine = line.replace("%reset_time%", plugin.getMissionManager().getTimeUntilReset());
            missionsLore.add(GradientColorParser.parse(processedLine));
        }
        missionsMeta.setLore(missionsLore);
        missions.setItemMeta(missionsMeta);
        gui.setItem(49, missions);

        ItemStack leaderboard = new ItemStack(Material.GOLDEN_HELMET);
        ItemMeta leaderboardMeta = leaderboard.getItemMeta();
        leaderboardMeta.setDisplayName(plugin.getMessageManager().getMessage("items.leaderboard-button.name"));

        List<String> lboardLore = new ArrayList<>();
        String coinsTime = plugin.getCoinsDistributionTask() != null
                ? plugin.getCoinsDistributionTask().getTimeUntilNextDistribution()
                : "Unknown";

        for (String line : plugin.getMessageManager().getMessagesConfig()
                .getStringList("items.leaderboard-button.lore")) {
            String processedLine = line.replace("%coins_time%", coinsTime);
            lboardLore.add(GradientColorParser.parse(processedLine));
        }
        leaderboardMeta.setLore(lboardLore);
        leaderboard.setItemMeta(leaderboardMeta);
        gui.setItem(48, leaderboard);

        if (plugin.getConfigManager().isShopEnabled()) {
            ItemStack shop = new ItemStack(Material.GOLD_INGOT);
            ItemMeta shopMeta = shop.getItemMeta();
            shopMeta.setDisplayName(plugin.getMessageManager().getMessage("items.shop-button.name"));

            List<String> shopLore = new ArrayList<>();
            for (String line : plugin.getMessageManager().getMessagesConfig().getStringList("items.shop-button.lore")) {
                String processedLine = line.replace("%coins%", String.valueOf(playerData.battleCoins));
                shopLore.add(GradientColorParser.parse(processedLine));
            }
            shopMeta.setLore(shopLore);
            shop.setItemMeta(shopMeta);
            gui.setItem(47, shop);
        }

        ItemStack dailyReward = new ItemStack(Material.SUNFLOWER);
        ItemMeta dailyMeta = dailyReward.getItemMeta();
        dailyMeta.setDisplayName(plugin.getMessageManager().getMessage("items.daily-reward.name"));

        List<String> dailyLore = new ArrayList<>();
        boolean canClaim = System.currentTimeMillis() - playerData.lastDailyReward >= 24 * 60 * 60 * 1000;
        String timeUntil = plugin.getMissionManager().getTimeUntilDailyReward(playerData.lastDailyReward);

        for (String line : plugin.getMessageManager().getMessagesConfig().getStringList(
                canClaim ? "items.daily-reward.lore-available" : "items.daily-reward.lore-cooldown")) {
            String processedLine = line
                    .replace("%xp%", String.valueOf(plugin.getConfigManager().getDailyRewardXP()))
                    .replace("%time%", timeUntil);
            dailyLore.add(GradientColorParser.parse(processedLine));
        }
        dailyMeta.setLore(dailyLore);
        dailyReward.setItemMeta(dailyMeta);
        gui.setItem(50, dailyReward);

        if (player.hasPermission("NaturalPass.admin")) {
            ItemStack rewardsEditor = new ItemStack(Material.COMMAND_BLOCK);
            ItemMeta editorMeta = rewardsEditor.getItemMeta();
            editorMeta.setDisplayName(GradientColorParser.parse("&#00AAFF&l⚙ Rewards Editor"));

            List<String> editorLore = new ArrayList<>();
            editorLore.add("");
            editorLore.add(GradientColorParser.parse("&#FF5555&lAdmin Only"));
            editorLore.add(GradientColorParser.parse("&7Edit battle pass rewards"));
            editorLore.add(GradientColorParser.parse("&7for all levels"));
            editorLore.add("");
            editorLore.add(GradientColorParser.parse("&#00AAFF▶ CLICK TO OPEN"));

            editorMeta.setLore(editorLore);
            rewardsEditor.setItemMeta(editorMeta);
            gui.setItem(46, rewardsEditor);
        }
    }
}
