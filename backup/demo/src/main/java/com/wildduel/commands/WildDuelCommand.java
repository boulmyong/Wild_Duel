package com.wildduel.commands;

import com.wildduel.gui.AdminGUI;
import com.wildduel.gui.TeamAdminGUI;
import com.wildduel.game.GameManager;
import com.wildduel.game.TeamManager;
import com.wildduel.game.TpaManager;
import com.wildduel.game.TeamAdminManager;
import com.wildduel.gui.StartItemGUI;
import com.wildduel.WildDuel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WildDuelCommand implements CommandExecutor {

    private final WildDuel plugin;
    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final TeamAdminManager teamAdminManager;
    private final TpaManager tpaManager;

    public WildDuelCommand(WildDuel plugin, GameManager gameManager, TeamManager teamManager, TeamAdminManager teamAdminManager, TpaManager tpaManager) {
        this.plugin = plugin;
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.teamAdminManager = teamAdminManager;
        this.tpaManager = tpaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                sendHelpMessage(sender);
                break;
            case "start":
            case "team":
            case "randomteam":
            case "admin":
            case "autosmelt":
            case "setinventory":
            case "st": // for addTime
                handleAdminCommands(sender, args);
                break;
            case "reset":
                handleResetCommand(sender, args);
                break;
            case "tparefresh":
            case "tpastatus":
                handleAdminTpaCommands(sender, args);
                break;
            default:
                sendHelpMessage(sender);
                break;
        }

        return true;
    }

    private void handleAdminCommands(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wildduel.admin")) {
            sender.sendMessage(plugin.getMessage("error.no-permission"));
            return;
        }

        Player player = (sender instanceof Player) ? (Player) sender : null;

        switch (args[0].toLowerCase()) {
            case "start":
                if (gameManager.startGame()) {
                    sender.sendMessage(plugin.getMessage("command.start.success"));
                } else {
                    sender.sendMessage(plugin.getMessage("command.start.fail", "%reason%", "게임 상태가 로비가 아니거나, 플레이어가 2명 미만입니다."));
                }
                break;
            case "team":
                if (player == null) {
                    sender.sendMessage(plugin.getMessage("error.player-only"));
                    return;
                }
                new TeamAdminGUI(teamAdminManager, teamManager).open(player);
                break;
            case "randomteam":
                if (Bukkit.getOnlinePlayers().isEmpty()) {
                    sender.sendMessage(plugin.getMessage("command.randomteam.no-players"));
                    return;
                }
                teamManager.assignRandomTeams();
                sender.sendMessage(plugin.getMessage("command.randomteam.success"));
                break;
            case "admin":
                if (player == null) {
                    sender.sendMessage(plugin.getMessage("error.player-only"));
                    return;
                }
                new AdminGUI(gameManager, teamManager, tpaManager, teamAdminManager).open(player);
                break;
            case "st":
                if (args.length > 1) {
                    try {
                        int seconds = Integer.parseInt(args[1]);
                        gameManager.addTime(seconds);
                        sender.sendMessage(plugin.getMessage("command.addtime.success", "%seconds%", String.valueOf(seconds)));
                    } catch (NumberFormatException e) {
                        sender.sendMessage(plugin.getMessage("error.invalid-number"));
                    }
                } else {
                    sender.sendMessage(plugin.getMessage("command.addtime.usage"));
                }
                break;
            case "autosmelt":
                if (args.length > 1) {
                    if (args[1].equalsIgnoreCase("on")) {
                        gameManager.setAutoSmelt(true);
                        sender.sendMessage(plugin.getMessage("command.autosmelt.enabled"));
                    } else if (args[1].equalsIgnoreCase("off")) {
                        gameManager.setAutoSmelt(false);
                        sender.sendMessage(plugin.getMessage("command.autosmelt.disabled"));
                    } else {
                        sender.sendMessage(plugin.getMessage("command.autosmelt.usage"));
                    }
                } else {
                    boolean currentState = gameManager.isAutoSmeltEnabled();
                    sender.sendMessage(plugin.getMessage("command.autosmelt.status", "%status%", (currentState ? "활성화" : "비활성화")));
                }
                break;
            case "setinventory":
                if (player == null) {
                    sender.sendMessage(plugin.getMessage("error.player-only"));
                    return;
                }
                new StartItemGUI(plugin.getDefaultStartInventory()).open(player);
                player.sendMessage(plugin.getMessage("command.setinventory.gui-opened"));
                break;
        }
    }

    private void handleResetCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wildduel.admin")) {
            sender.sendMessage(plugin.getMessage("error.no-permission"));
            return;
        }

        if (args.length > 1 && args[1].equalsIgnoreCase("confirm")) {
            resetGameAndWorld(sender);
            return;
        }

        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6============================================="));
        sender.sendMessage(plugin.getMessage("command.reset.confirm-warning"));
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6============================================="));
    }

    private void resetGameAndWorld(CommandSender sender) {
        Bukkit.broadcastMessage(plugin.getMessage("command.reset.broadcast-start"));
        sender.sendMessage(plugin.getMessage("command.reset.admin-start"));

        // GameManager의 resetGame이 모든 로직(플레이어 이동, 태스크 취소 등)을 처리합니다.
        gameManager.resetGame();
        // 팀과 TPA 정보는 GameManager가 아닌 별도의 매니저에서 관리되므로 별도 초기화가 필요합니다.
        teamManager.resetTeams();
        tpaManager.resetTpa();

        Bukkit.broadcastMessage(plugin.getMessage("command.reset.broadcast-complete"));
        sender.sendMessage(plugin.getMessage("command.reset.admin-complete"));
    }

    private void handleAdminTpaCommands(CommandSender sender, String[] args) {
        if (!sender.hasPermission("wildduel.admin")) {
            sender.sendMessage(plugin.getMessage("error.no-permission"));
            return;
        }

        switch (args[0].toLowerCase()) {
            case "tparefresh":
                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessage("command.tparefresh.usage"));
                    return;
                }
                if (args[1].equalsIgnoreCase("all")) {
                    tpaManager.refreshAllCooldowns();
                    sender.sendMessage(plugin.getMessage("command.tparefresh.success-all"));
                } else {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage(plugin.getMessage("error.player-not-found"));
                        return;
                    }
                    if (tpaManager.refreshCooldown(target)) {
                        sender.sendMessage(plugin.getMessage("command.tparefresh.success-player", "%player%", target.getName()));
                    } else {
                        sender.sendMessage(plugin.getMessage("command.tparefresh.no-cooldown", "%player%", target.getName()));
                    }
                }
                break;
            case "tpastatus":
                if (args.length < 2) {
                    sender.sendMessage(plugin.getMessage("command.tpastatus.usage"));
                    return;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(plugin.getMessage("error.player-not-found"));
                    return;
                }
                long remaining = tpaManager.getRemainingCooldown(target);
                if (remaining > 0) {
                    long minutes = remaining / 60;
                    long seconds = remaining % 60;
                    sender.sendMessage(plugin.getMessage("command.tpastatus.status", "%player%", target.getName(), "%minutes%", String.valueOf(minutes), "%seconds%", String.valueOf(seconds)));
                } else {
                    sender.sendMessage(plugin.getMessage("command.tpastatus.no-cooldown", "%player%", target.getName()));
                }
                break;
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(plugin.getMessage("help.header"));
        sender.sendMessage(" "); // Spacer
        sender.sendMessage(plugin.getMessage("help.player.title"));
        sender.sendMessage(plugin.getMessage("help.player.wd-help"));
        sender.sendMessage(plugin.getMessage("help.player.team-select"));
        sender.sendMessage(plugin.getMessage("help.player.tpa"));
        sender.sendMessage(plugin.getMessage("help.player.tpacancel"));

        if (sender.hasPermission("wildduel.admin")) {
            sender.sendMessage(" "); // Spacer
            sender.sendMessage(plugin.getMessage("help.admin.title"));
            sender.sendMessage(plugin.getMessage("help.admin.admin-gui"));
            sender.sendMessage(plugin.getMessage("help.admin.set-inventory"));
            sender.sendMessage(plugin.getMessage("help.admin.start"));
            sender.sendMessage(plugin.getMessage("help.admin.random-team"));
            sender.sendMessage(plugin.getMessage("help.admin.add-time"));
            sender.sendMessage(plugin.getMessage("help.admin.autosmelt"));
            sender.sendMessage(plugin.getMessage("help.admin.reset"));
            sender.sendMessage(plugin.getMessage("help.admin.tpa-refresh"));
            sender.sendMessage(plugin.getMessage("help.admin.tpa-status"));
        }
        sender.sendMessage(" "); // Spacer
        sender.sendMessage(plugin.getMessage("help.footer"));
    }
}