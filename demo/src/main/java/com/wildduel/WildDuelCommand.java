package com.wildduel;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WildDuelCommand implements CommandExecutor {

    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final TeamAdminManager teamAdminManager;
    private final TpaManager tpaManager;

    public WildDuelCommand(GameManager gameManager, TeamManager teamManager, TeamAdminManager teamAdminManager, TpaManager tpaManager) {
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

        // Non-player specific commands can be handled here if any

        if (!(sender instanceof Player)) {
            // Handle commands that can be run by console if any, otherwise return
            // For now, all commands require a player
        }

        switch (args[0].toLowerCase()) {
            case "help":
                sendHelpMessage(sender);
                break;
            case "set":
            case "sp":
            case "start":
            case "preptime":
            case "st":
            case "team":
            case "randomteam":
            case "admin":
            case "autosmelt":
                handlePlayerCommands(sender, args);
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

    private void handlePlayerCommands(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player.");
            return;
        }
        Player player = (Player) sender;
        if (!player.isOp()) {
            player.sendMessage("You do not have permission to use this command.");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "set":
                gameManager.setWorldSpawn(player.getLocation());
                player.sendMessage("월드 스폰 위치를 현재 위치로 설정했습니다.");
                break;
            case "sp":
                gameManager.setDuelStartLocation(player.getLocation());
                player.sendMessage("듀얼 시작 위치를 현재 위치로 설정했습니다.");
                break;
            case "start":
                gameManager.startGame();
                player.sendMessage("Wild Duel has started!");
                break;
            case "preptime":
                if (args.length > 1) {
                    try {
                        int seconds = Integer.parseInt(args[1]);
                        gameManager.setPrepTime(seconds);
                        player.sendMessage("Farming time set to " + seconds + " seconds.");
                    } catch (NumberFormatException e) {
                        player.sendMessage("Invalid time specified.");
                    }
                } else {
                    player.sendMessage("Usage: /wd preptime <seconds>");
                }
                break;
            case "st":
                if (args.length > 1) {
                    try {
                        int seconds = Integer.parseInt(args[1]);
                        gameManager.setTime(seconds);
                        player.sendMessage("Remaining farming time set to " + seconds + " seconds.");
                    } catch (NumberFormatException e) {
                        player.sendMessage("Invalid time specified.");
                    }
                } else {
                    player.sendMessage("Usage: /wd st <seconds>");
                }
                break;
            case "team":
                new TeamAdminGUI(teamAdminManager, teamManager).open(player);
                break;
            case "randomteam":
                teamManager.assignRandomTeams();
                player.sendMessage("모든 플레이어를 랜덤으로 팀에 배정했습니다.");
                break;
            case "admin":
                new AdminGUI(gameManager, teamManager, tpaManager, teamAdminManager).open(player);
                break;
            case "autosmelt":
                if (args.length > 1) {
                    if (args[1].equalsIgnoreCase("on")) {
                        gameManager.setAutoSmelt(true);
                        player.sendMessage("Auto-smelt enabled.");
                    } else if (args[1].equalsIgnoreCase("off")) {
                        gameManager.setAutoSmelt(false);
                        player.sendMessage("Auto-smelt disabled.");
                    } else {
                        player.sendMessage("Usage: /wd autosmelt <on|off>");
                    }
                } else {
                    player.sendMessage("Usage: /wd autosmelt <on|off>");
                }
                break;
        }
    }

    private void handleAdminTpaCommands(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("You do not have permission to use this command.");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "tparefresh":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /wd tparefresh <player|all>");
                    return;
                }
                if (args[1].equalsIgnoreCase("all")) {
                    tpaManager.refreshAllCooldowns();
                    sender.sendMessage("§a[TPA] 전체 플레이어의 쿨타임을 초기화했습니다.");
                } else {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage("§c[TPA] 해당 플레이어는 온라인이 아닙니다.");
                        return;
                    }
                    if (tpaManager.refreshCooldown(target)) {
                        sender.sendMessage("§a[TPA] " + target.getName() + "님의 쿨타임을 초기화했습니다.");
                    } else {
                        sender.sendMessage("§c[TPA] " + target.getName() + "님은 현재 쿨타임이 없습니다.");
                    }
                }
                break;
            case "tpastatus":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /wd tpastatus <player>");
                    return;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§c[TPA] 해당 플레이어는 온라인이 아닙니다.");
                    return;
                }
                long remaining = tpaManager.getRemainingCooldown(target);
                if (remaining > 0) {
                    long minutes = remaining / 60;
                    long seconds = remaining % 60;
                    sender.sendMessage(String.format("§e[TPA] %s님의 남은 쿨타임: %d분 %d초", target.getName(), minutes, seconds));
                } else {
                    sender.sendMessage("§a[TPA] " + target.getName() + "님은 현재 쿨타임이 없습니다.");
                }
                break;
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§a--- 와일드 듀얼 명령어 목록 ---");
        sender.sendMessage("§e/wd admin §7- §f(추천) 모든 설정을 관리하는 GUI를 엽니다.");
        sender.sendMessage("§e/wd start §7- §f게임을 즉시 시작합니다.");
        sender.sendMessage("§e/wd randomteam §7- §f모든 플레이어를 랜덤 팀에 배정합니다.");
        sender.sendMessage("§e/wd tparefresh <플레이어|all> §7- §fTPA 쿨타임을 초기화합니다.");
        sender.sendMessage("§e/wd tpastatus <플레이어> §7- §fTPA 쿨타임 상태를 확인합니다.");
        sender.sendMessage("§e/wd help §7- §f이 도움말을 표시합니다.");
        sender.sendMessage("§7- 상세 설정(스폰, 파밍 시간 등)은 /wd admin 패널을 이용해주세요.");
    }
}