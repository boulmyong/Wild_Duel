package com.wildduel;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class TpaManager {

    private final Map<UUID, TpaRequest> pendingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final TeamManager teamManager;
    private final long COOLDOWN_SECONDS = 180;
    private final int TELEPORT_DELAY_SECONDS = 3;

    public TpaManager(TeamManager teamManager) {
        this.teamManager = teamManager;
    }

    public void requestTpa(Player requester, Player target) {
        String requesterTeam = teamManager.getPlayerTeam(requester);
        if (requesterTeam == null || !requesterTeam.equals(teamManager.getPlayerTeam(target))) {
            requester.sendMessage("§c같은 팀원에게만 TPA 요청을 보낼 수 있습니다.");
            return;
        }

        if (cooldowns.containsKey(requester.getUniqueId()) && System.currentTimeMillis() - cooldowns.get(requester.getUniqueId()) < COOLDOWN_SECONDS * 1000) {
            long remaining = (cooldowns.get(requester.getUniqueId()) + COOLDOWN_SECONDS * 1000 - System.currentTimeMillis()) / 1000;
            requester.sendMessage("§cTPA 쿨타임이 " + remaining + "초 남았습니다.");
            return;
        }

        if (pendingRequests.containsKey(requester.getUniqueId())) {
            requester.sendMessage("§c이미 보낸 TPA 요청이 있습니다. 취소하려면 /tpacancel을 입력하세요.");
            return;
        }

        TpaRequest request = new TpaRequest(requester.getUniqueId(), target.getUniqueId());
        pendingRequests.put(requester.getUniqueId(), request);
        cooldowns.put(requester.getUniqueId(), System.currentTimeMillis());

        requester.sendMessage("§a" + target.getName() + "님에게 TPA 요청을 보냈습니다.");

        TextComponent message = new TextComponent("§e[TPA] §f" + requester.getName() + "님이 텔레포트 요청을 보냈습니다. ");
        TextComponent accept = new TextComponent("§a[수락]");
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tparesponse accept " + requester.getName()));
        TextComponent deny = new TextComponent(" §c[거절]");
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tparesponse deny " + requester.getName()));

        message.addExtra(accept);
        message.addExtra(deny);
        target.spigot().sendMessage(message);

        // Auto-cancel after 30 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                if (pendingRequests.containsValue(request)) {
                    cancelTpa(requester, target, false);
                    requester.sendMessage("§cTPA 요청이 시간 초과로 취소되었습니다.");
                    target.sendMessage("§c" + requester.getName() + "님의 TPA 요청이 시간 초과로 취소되었습니다.");
                }
            }
        }.runTaskLater(WildDuel.getInstance(), 30 * 20);
    }

    public void handleResponse(Player target, String requesterName, boolean accepted) {
        Player requester = Bukkit.getPlayer(requesterName);
        if (requester == null) {
            target.sendMessage("§c요청자가 오프라인 상태입니다.");
            return;
        }

        TpaRequest request = pendingRequests.get(requester.getUniqueId());
        if (request == null || !request.getTarget().equals(target.getUniqueId())) {
            target.sendMessage("§c해당 TPA 요청을 찾을 수 없습니다.");
            return;
        }

        pendingRequests.remove(requester.getUniqueId());

        if (accepted) {
            target.sendMessage("§aTPA 요청을 수락했습니다. 3초 후 " + requester.getName() + "님이 텔레포트됩니다.");
            requester.sendMessage("§a" + target.getName() + "님이 TPA 요청을 수락했습니다. 3초 후 텔레포트됩니다. 움직이지 마세요.");
            startTeleportCountdown(requester, target);
        } else {
            target.sendMessage("§cTPA 요청을 거절했습니다.");
            requester.sendMessage("§c" + target.getName() + "님이 TPA 요청을 거절했습니다.");
        }
    }

    public void cancelTpa(Player requester, Player target, boolean manual) {
        pendingRequests.remove(requester.getUniqueId());
        if (manual) {
            requester.sendMessage("§aTPA 요청을 취소했습니다.");
        }
        if (target != null) {
            target.sendMessage("§c" + requester.getName() + "님의 TPA 요청이 취소되었습니다.");
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
                    requester.sendMessage("§c움직여서 텔레포트가 취소되었습니다.");
                    this.cancel();
                    return;
                }

                if (countdown <= 0) {
                    requester.teleport(target.getLocation());
                    requester.playSound(requester.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
                    this.cancel();
                    return;
                }

                requester.sendMessage("§e" + countdown + "초 후 텔레포트...");
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

    public long getRemainingCooldown(Player player) {
        if (!cooldowns.containsKey(player.getUniqueId())) {
            return 0;
        }
        long timeElapsed = System.currentTimeMillis() - cooldowns.get(player.getUniqueId());
        if (timeElapsed >= COOLDOWN_SECONDS * 1000) {
            return 0;
        }
        return (COOLDOWN_SECONDS * 1000 - timeElapsed) / 1000;
    }
}