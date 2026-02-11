package id.naturalsmp.naturalpass.managers;

import id.naturalsmp.naturalpass.NaturalPass;
import id.naturalsmp.naturalpass.models.Mission;
import id.naturalsmp.naturalpass.models.PlayerData;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MissionProgressTracker {

    private final NaturalPass plugin;
    private final Map<UUID, Map<String, BossBar>> playerBossBars = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Integer>> bossBarTasks = new ConcurrentHashMap<>();
    private final Set<Integer> scheduledTaskIds = ConcurrentHashMap.newKeySet();
    private final Map<UUID, Set<String>> playerCompletedMissions = new ConcurrentHashMap<>();

    public MissionProgressTracker(NaturalPass plugin) {
        this.plugin = plugin;
    }

    public void trackProgress(Player player, String type, String target, int amount, List<Mission> dailyMissions) {
        PlayerData data = plugin.getPlayerDataManager().getPlayerData(player.getUniqueId());
        if (data == null || dailyMissions.isEmpty())
            return;

        UUID playerUUID = player.getUniqueId();
        Set<String> completedKeys = playerCompletedMissions.computeIfAbsent(playerUUID,
                k -> ConcurrentHashMap.newKeySet());

        boolean changed = false;
        MessageManager messageManager = plugin.getMessageManager();

        for (Mission mission : dailyMissions) {
            if (!mission.type.equals(type))
                continue;

            if (!mission.target.equals("ANY") && !mission.target.equals(target))
                continue;

            String missionKey = generateMissionKey(mission);

            if (completedKeys.contains(missionKey)) {
                continue;
            }

            int currentProgress = data.missionProgress.getOrDefault(missionKey, 0);

            if (currentProgress >= mission.required) {
                completedKeys.add(missionKey);
                continue;
            }

            int newProgress = Math.min(currentProgress + amount, mission.required);
            data.missionProgress.put(missionKey, newProgress);
            changed = true;

            if (newProgress >= mission.required && currentProgress < mission.required) {
                completedKeys.add(missionKey);

                data.xp += mission.xpReward;
                checkLevelUp(player, data);

                player.sendMessage(messageManager.getPrefix() + messageManager.getMessage("messages.mission.completed",
                        "%mission%", mission.name,
                        "%reward_xp%", String.valueOf(mission.xpReward)));
                player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);

                showCompletedBossBar(player, mission.name);
            } else {
                showProgressBossBar(player, mission.name, newProgress, mission.required);
            }
        }

        if (changed) {
            plugin.getPlayerDataManager().markForSave(player.getUniqueId());
        }
    }

    private String generateMissionKey(Mission mission) {
        return mission.type + "_" + mission.target + "_" + mission.required + "_" + mission.name.hashCode();
    }

    public void resetProgress(String currentMissionDate) {
        playerCompletedMissions.clear();

        for (Map.Entry<UUID, Map<String, Integer>> entry : bossBarTasks.entrySet()) {
            for (Integer taskId : entry.getValue().values()) {
                Bukkit.getScheduler().cancelTask(taskId);
                scheduledTaskIds.remove(taskId);
            }
        }
        bossBarTasks.clear();

        // Remove all boss bars
        for (Map<String, BossBar> bars : playerBossBars.values()) {
            for (BossBar bar : bars.values()) {
                bar.removeAll();
            }
        }
        playerBossBars.clear();

        for (PlayerData data : plugin.getPlayerDataManager().getPlayerCache().values()) {
            data.missionProgress.clear();
        }
    }

    private void checkLevelUp(Player player, PlayerData data) {
        boolean leveled = false;
        MessageManager messageManager = plugin.getMessageManager();
        int xpPerLevel = plugin.getConfigManager().getXpPerLevel();
        int maxLevel = plugin.getRewardManager().getMaxLevel();

        while (data.xp >= xpPerLevel && data.level < maxLevel) {
            data.xp -= xpPerLevel;
            data.level++;
            data.totalLevels++;
            leveled = true;

            player.sendMessage(messageManager.getPrefix() + messageManager.getMessage("messages.level-up",
                    "%level%", String.valueOf(data.level)));
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);

            int available = plugin.getRewardManager().countAvailableRewards(player, data);
            if (available > 0) {
                player.sendMessage(messageManager.getPrefix() + messageManager.getMessage("messages.new-rewards"));
            }
        }

        if (leveled) {
            plugin.getPlayerDataManager().markForSave(player.getUniqueId());
        }
    }

    private void showProgressBossBar(Player player, String missionName, int current, int required) {
        UUID uuid = player.getUniqueId();
        String key = missionName.toLowerCase().replace(" ", "_");

        Map<String, BossBar> bars = playerBossBars.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        Map<String, Integer> tasks = bossBarTasks.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());

        // Cancel previous hide task if exists
        if (tasks.containsKey(key)) {
            int oldTaskId = tasks.get(key);
            Bukkit.getScheduler().cancelTask(oldTaskId);
            scheduledTaskIds.remove(oldTaskId);
        }

        MessageManager messageManager = plugin.getMessageManager();
        String progressMessage = messageManager.getMessage("messages.mission.bossbar-progress",
                "%current%", String.valueOf(current),
                "%required%", String.valueOf(required),
                "%mission%", missionName);

        // Create or update boss bar
        BossBar bossBar = bars.get(key);
        if (bossBar == null) {
            bossBar = Bukkit.createBossBar(progressMessage, BarColor.GREEN, BarStyle.SEGMENTED_10);
            bossBar.addPlayer(player);
            bars.put(key, bossBar);
        } else {
            bossBar.setTitle(progressMessage);
        }

        double progress = Math.min((double) current / required, 1.0);
        bossBar.setProgress(progress);
        bossBar.setVisible(true);

        // Schedule hide after 5 seconds (100 ticks)
        final BossBar finalBar = bossBar;
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            finalBar.setVisible(false);
            finalBar.removeAll();
            bars.remove(key);
            tasks.remove(key);
        }, 100L).getTaskId();

        tasks.put(key, taskId);
        scheduledTaskIds.add(taskId);
    }

    private void showCompletedBossBar(Player player, String missionName) {
        UUID uuid = player.getUniqueId();
        String key = missionName.toLowerCase().replace(" ", "_") + "_completed";

        Map<String, BossBar> bars = playerBossBars.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());
        Map<String, Integer> tasks = bossBarTasks.computeIfAbsent(uuid, k -> new ConcurrentHashMap<>());

        // Cancel previous task if exists
        if (tasks.containsKey(key)) {
            int oldTaskId = tasks.get(key);
            Bukkit.getScheduler().cancelTask(oldTaskId);
            scheduledTaskIds.remove(oldTaskId);
        }

        MessageManager messageManager = plugin.getMessageManager();
        String completedMessage = messageManager.getMessage("messages.mission.bossbar-completed",
                "%mission%", missionName);

        BossBar bossBar = bars.get(key);
        if (bossBar == null) {
            bossBar = Bukkit.createBossBar(completedMessage, BarColor.GREEN, BarStyle.SOLID);
            bossBar.addPlayer(player);
            bars.put(key, bossBar);
        } else {
            bossBar.setTitle(completedMessage);
        }

        bossBar.setProgress(1.0);
        bossBar.setVisible(true);

        // Schedule hide after 5 seconds (100 ticks)
        final BossBar finalBar = bossBar;
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            finalBar.setVisible(false);
            finalBar.removeAll();
            bars.remove(key);
            tasks.remove(key);
        }, 100L).getTaskId();

        tasks.put(key, taskId);
        scheduledTaskIds.add(taskId);
    }

    public void clearPlayerBossBars(UUID uuid) {
        if (bossBarTasks.containsKey(uuid)) {
            bossBarTasks.get(uuid).values().forEach(taskId -> {
                Bukkit.getScheduler().cancelTask(taskId);
                scheduledTaskIds.remove(taskId);
            });
            bossBarTasks.remove(uuid);
        }
        if (playerBossBars.containsKey(uuid)) {
            playerBossBars.get(uuid).values().forEach(bar -> {
                bar.setVisible(false);
                bar.removeAll();
            });
            playerBossBars.remove(uuid);
        }
        playerCompletedMissions.remove(uuid);
    }

    public void shutdown() {
        scheduledTaskIds.forEach(taskId -> Bukkit.getScheduler().cancelTask(taskId));
        scheduledTaskIds.clear();

        bossBarTasks.values()
                .forEach(tasks -> tasks.values().forEach(taskId -> Bukkit.getScheduler().cancelTask(taskId)));
        bossBarTasks.clear();

        for (Map<String, BossBar> bars : playerBossBars.values()) {
            for (BossBar bar : bars.values()) {
                bar.setVisible(false);
                bar.removeAll();
            }
        }
        playerBossBars.clear();
        playerCompletedMissions.clear();
    }

    public int getCompletedMissionsCount(PlayerData data, List<Mission> missions) {
        int completed = 0;
        for (Mission mission : missions) {
            String key = generateMissionKey(mission);
            int progress = data.missionProgress.getOrDefault(key, 0);
            if (progress >= mission.required) {
                completed++;
            }
        }
        return completed;
    }
}
