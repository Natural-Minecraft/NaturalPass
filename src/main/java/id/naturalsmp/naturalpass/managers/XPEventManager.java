package id.naturalsmp.naturalpass.managers;

import id.naturalsmp.naturalpass.NaturalPass;
import id.naturalsmp.naturalpass.utils.GradientColorParser;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class XPEventManager {

    private final NaturalPass plugin;
    private int multiplier = 1;
    private long endTime = 0;
    private BukkitRunnable countdownTask;
    private BukkitRunnable auraTask;
    private BukkitRunnable autoSchedulerTask;
    private long totalDuration = 0;
    private final Random random = new Random();

    public XPEventManager(NaturalPass plugin) {
        this.plugin = plugin;
        startAutoScheduler();
    }

    private void startAutoScheduler() {
        // Runs every 30 minutes (36000 ticks)
        autoSchedulerTask = new BukkitRunnable() {
            @Override
            public void run() {
                // Return if there's already an active event
                if (isEventActive()) return;

                // 2% chance every 30 minutes to trigger the surprise event
                // This averages about 1 or 2 times a week. (336 checks a week * 0.02 = ~6.7 times realistically, maybe let's do 1% or 0.5% for once a week)
                // We'll set it to 1.5% chance.
                if (random.nextDouble() <= 0.015) {
                    // Trigger a 30-minute 2x XP Event!
                    startEvent(2, 1800000L); // 1,800,000 ms = 30 minutes
                    
                    String durationFormatted = getTimeRemaining();
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        player.sendMessage(GradientColorParser.parse("<gradient:#FFD700:#FF6B6B>✦ SURPRISE EVENT ✦</gradient>"));
                        player.sendMessage(GradientColorParser.parse("&fThe gods have blessed the realm! All BattlePass XP is now <gradient:#00FF88:#45B7D1>2x</gradient> for <gradient:#4ECDC4:#45B7D1>" + durationFormatted + "</gradient>!"));
                        player.playSound(player.getLocation(), org.bukkit.Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.2f);
                    }
                }
            }
        };
        autoSchedulerTask.runTaskTimer(plugin, 36000L, 36000L);
    }

    public boolean startEvent(int multiplier, long durationMillis) {
        if (isEventActive()) {
            stopEvent();
        }

        this.multiplier = multiplier;
        this.totalDuration = durationMillis;
        this.endTime = System.currentTimeMillis() + durationMillis;

        countdownTask = new BukkitRunnable() {
            @Override
            public void run() {
                long remaining = endTime - System.currentTimeMillis();

                if (remaining <= 0) {
                    stopEvent();
                    String endMsg = GradientColorParser.parse(
                            plugin.getMessageManager().getPrefix() +
                                    "&cThe XP event has ended. &7Mission XP rewards are back to normal.");
                    for (Player p : Bukkit.getOnlinePlayers()) {
                        p.sendMessage(endMsg);
                    }
                    this.cancel();
                }
            }
        };
        countdownTask.runTaskTimer(plugin, 0L, 20L);

        // Start Particle Aura Task
        auraTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isEventActive()) {
                    this.cancel();
                    return;
                }
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.isValid() && !player.isDead()) {
                        // Spawn happy villager/totem cross particles around the player's head
                        player.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, 
                                player.getLocation().add(0, 2.2, 0), 1, 0.3, 0.1, 0.3, 0.0);
                        player.getWorld().spawnParticle(Particle.TOTEM, 
                                player.getLocation().add(0, 1.0, 0), 2, 0.5, 0.5, 0.5, 0.05);
                    }
                }
            }
        };
        auraTask.runTaskTimer(plugin, 0L, 10L); // every 0.5 seconds

        return true;
    }

    public void stopEvent() {
        multiplier = 1;
        endTime = 0;
        totalDuration = 0;

        if (countdownTask != null) {
            countdownTask.cancel();
            countdownTask = null;
        }
        
        if (auraTask != null) {
            auraTask.cancel();
            auraTask = null;
        }
    }
    
    public void shutdown() {
        stopEvent();
        if (autoSchedulerTask != null) {
            autoSchedulerTask.cancel();
            autoSchedulerTask = null;
        }
    }

    public void addPlayerToBossBar(Player player) {
        // Ignored. Bossbar disabled by user request.
    }

    public void removePlayerFromBossBar(Player player) {
        // Ignored. Bossbar disabled by user request.
    }

    public boolean isEventActive() {
        return multiplier > 1 && System.currentTimeMillis() < endTime;
    }

    public int getMultiplier() {
        if (!isEventActive()) {
            return 1;
        }
        return multiplier;
    }

    public String getTimeRemaining() {
        if (!isEventActive()) return "None";

        long remaining = endTime - System.currentTimeMillis();
        if (remaining <= 0) return "None";

        long hours = remaining / 3600000;
        long minutes = (remaining % 3600000) / 60000;
        long seconds = (remaining % 60000) / 1000;

        if (hours > 0) {
            return hours + "h " + minutes + "m " + seconds + "s";
        } else if (minutes > 0) {
            return minutes + "m " + seconds + "s";
        } else {
            return seconds + "s";
        }
    }

    public static long parseDuration(String input) {
        input = input.toLowerCase().trim();
        long total = 0;

        StringBuilder number = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (Character.isDigit(c)) {
                number.append(c);
            } else if (c == 'h' && number.length() > 0) {
                total += Long.parseLong(number.toString()) * 3600000;
                number.setLength(0);
            } else if (c == 'm' && number.length() > 0) {
                total += Long.parseLong(number.toString()) * 60000;
                number.setLength(0);
            } else if (c == 's' && number.length() > 0) {
                total += Long.parseLong(number.toString()) * 1000;
                number.setLength(0);
            }
        }

        return total;
    }

    public static int parseMultiplier(String input) {
        input = input.toLowerCase().trim().replace("x", "");
        try {
            int val = Integer.parseInt(input);
            if (val < 2 || val > 100) return -1;
            return val;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
