package com.wildduel.game;

import com.wildduel.WildDuel;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TpaManager {

    private final Map<UUID, TpaRequest> pendingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final TeamManager teamManager;
    private final long cooldownSeconds;
    private final int TELEPORT_DELAY_SECONDS = 3;
    private final long tpaTimeout;

    public TpaManager(TeamManager teamManager) {
        this.teamManager = teamManager;
        this.tpaTimeout = WildDuel.getInstance().getConfig().getLong("tpa-request-timeout", 60);
        this.cooldownSeconds = WildDuel.getInstance().getConfig().getLong("tpa-cooldown-seconds", 180);
    }

    public void requestTpa(Player requester, Player target) {
        String requesterTeam = teamManager.getPlayerTeam(requester);
        if (requesterTeam == null || !requesterTeam.equals(teamManager.getPlayerTeam(target))) {
            requester.sendMessage(WildDuel.getInstance().getMessage("tpa.error.not-same-team"));
            return;
        }

        if (cooldowns.containsKey(requester.getUniqueId()) && System.currentTimeMillis() - cooldowns.get(requester.getUniqueId()) < cooldownSeconds * 1000) {
            long remaining = (cooldowns.get(requester.getUniqueId()) + cooldownSeconds * 1000 - System.currentTimeMillis()) / 1000;
            requester.sendMessage(WildDuel.getInstance().getMessage("tpa.error.cooldown", "%seconds%", String.valueOf(remaining)));
            return;
        }

        if (pendingRequests.containsKey(requester.getUniqueId())) {
            requester.sendMessage(WildDuel.getInstance().getMessage("tpa.error.already-sent"));
            return;
        }

        TpaRequest request = new TpaRequest(requester.getUniqueId(), target.getUniqueId());
        pendingRequests.put(requester.getUniqueId(), request);
        cooldowns.put(requester.getUniqueId(), System.currentTimeMillis());

        requester.sendMessage(WildDuel.getInstance().getMessage("tpa.success.request-sent", "%player%", target.getName()));

        TextComponent message = new TextComponent(WildDuel.getInstance().getMessage("tpa.info.request-received", "%player%", requester.getName()));
        TextComponent accept = new TextComponent(WildDuel.getInstance().getMessage("tpa.info.accept"));
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tparesponse accept " + requester.getName()));
        TextComponent deny = new TextComponent(WildDuel.getInstance().getMessage("tpa.info.deny"));
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tparesponse deny " + requester.getName()));

        message.addExtra(accept);
        message.addExtra(deny);
        target.spigot().sendMessage(message);

        // Auto-cancel after timeout
        BukkitTask timeoutTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingRequests.remove(requester.getUniqueId(), request)) {
                    requester.sendMessage(WildDuel.getInstance().getMessage("tpa.error.timeout-requester"));
                    target.sendMessage(WildDuel.getInstance().getMessage("tpa.error.timeout-target", "%player%", requester.getName()));
                }
            }
        }.runTaskLater(WildDuel.getInstance(), tpaTimeout * 20);
        request.setTimeoutTask(timeoutTask);
    }

    public void handleResponse(Player target, String requesterName, boolean accepted) {
        Player requester = Bukkit.getPlayer(requesterName);
        if (requester == null) {
            target.sendMessage(WildDuel.getInstance().getMessage("tpa.error.requester-offline"));
            return;
        }

        TpaRequest request = pendingRequests.get(requester.getUniqueId());
        if (request == null || !request.getTarget().equals(target.getUniqueId())) {
            target.sendMessage(WildDuel.getInstance().getMessage("tpa.error.request-not-found"));
            return;
        }

        request.cancelTimeoutTask(); // Cancel the timeout task
        pendingRequests.remove(requester.getUniqueId());

        if (accepted) {
            target.sendMessage(WildDuel.getInstance().getMessage("tpa.success.accepted-target", "%player%", requester.getName()));
            requester.sendMessage(WildDuel.getInstance().getMessage("tpa.success.accepted-requester", "%player%", target.getName()));
            startTeleportCountdown(requester, target);
        } else {
            target.sendMessage(WildDuel.getInstance().getMessage("tpa.success.denied-target"));
            requester.sendMessage(WildDuel.getInstance().getMessage("tpa.success.denied-requester", "%player%", target.getName()));
        }
    }

    public void cancelTpa(Player requester, Player target, boolean manual) {
        TpaRequest request = pendingRequests.remove(requester.getUniqueId());
        if (request != null) {
            request.cancelTimeoutTask();
            if (manual) {
                requester.sendMessage(WildDuel.getInstance().getMessage("tpa.success.cancelled-requester"));
            }
            if (target != null) {
                target.sendMessage(WildDuel.getInstance().getMessage("tpa.info.cancelled-target", "%player%", requester.getName()));
            }
        }
    }

    private void startTeleportCountdown(Player requester, Player target) {
        Location initialLocation = requester.getLocation();

        new BukkitRunnable() {
            int countdown = TELEPORT_DELAY_SECONDS;

            @Override
            public void run() {
                if (!requester.isOnline() || !target.isOnline()) {
                    this.cancel();
                    return;
                }

                if (requester.getLocation().distance(initialLocation) > 1) {
                    requester.sendMessage(WildDuel.getInstance().getMessage("tpa.error.moved"));
                    this.cancel();
                    return;
                }

                if (countdown <= 0) {
                    requester.teleport(target.getLocation());
                    requester.playSound(requester.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    this.cancel();
                    return;
                }

                requester.sendMessage(WildDuel.getInstance().getMessage("tpa.info.teleporting-in", "%seconds%", String.valueOf(countdown)));
                countdown--;
            }
        }.runTaskTimer(WildDuel.getInstance(), 0, 20);
    }

    public TpaRequest getPendingRequest(UUID requesterId) {
        return pendingRequests.get(requesterId);
    }

    public boolean refreshCooldown(Player player) {
        if (cooldowns.containsKey(player.getUniqueId())) {
            cooldowns.remove(player.getUniqueId());
            return true;
        }
        return false;
    }

    public void refreshAllCooldowns() {
        cooldowns.clear();
    }

    public void resetTpa() {
        pendingRequests.values().forEach(TpaRequest::cancelTimeoutTask);
        pendingRequests.clear();
        cooldowns.clear();
    }

    public long getRemainingCooldown(Player player) {
        if (!cooldowns.containsKey(player.getUniqueId())) {
            return 0;
        }
        long timeElapsed = System.currentTimeMillis() - cooldowns.get(player.getUniqueId());
        if (timeElapsed >= cooldownSeconds * 1000) {
            return 0;
        }
        return (cooldownSeconds * 1000 - timeElapsed) / 1000;
    }

    public void handlePlayerQuit(Player player) {
        UUID playerId = player.getUniqueId();

        // Cancel requests sent BY the player who quit
        if (pendingRequests.containsKey(playerId)) {
            TpaRequest request = pendingRequests.get(playerId);
            Player target = Bukkit.getPlayer(request.getTarget());
            cancelTpa(player, target, false); // false for not manual
        }

        // Cancel requests sent TO the player who quit
        for (TpaRequest request : pendingRequests.values()) {
            if (request.getTarget().equals(playerId)) {
                Player requester = Bukkit.getPlayer(request.getRequester());
                if (requester != null) {
                    // Found the request, cancel it and notify the requester
                    cancelTpa(requester, player, false);
                    requester.sendMessage(WildDuel.getInstance().getMessage("tpa.error.target-offline-on-quit", "%player%", player.getName()));
                    break; // Since a player can only be the target of one request, we can stop searching
                }
            }
        }
    }
}