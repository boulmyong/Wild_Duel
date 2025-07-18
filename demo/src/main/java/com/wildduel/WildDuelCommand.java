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
        sender.sendMessage("--- 와일드 듀얼 명령어 목록 ---");
        sender.sendMessage("/wd set - 월드의 기본 스폰 위치를 설정합니다.");
        sender.sendMessage("/wd sp - 듀얼 시작 위치를 설정합니다.");
        sender.sendMessage("/wd start - 게임을 시작합니다.");
        sender.sendMessage("/wd preptime <초> - 시작 전 파밍 시간을 설정합니다.");
        sender.sendMessage("/wd st <초> - 남은 파밍 시간을 변경합니다.");
        sender.sendMessage("/wd team - 팀 관리 GUI를 엽니다.");
        sender.sendMessage("/wd tparefresh <플레이어|all> - TPA 쿨타임을 초기화합니다.");
        sender.sendMessage("/wd tpastatus <플레이어> - TPA 쿨타임 상태를 확인합니다.");
        sender.sendMessage("/wd help - 도움말을 표시합니다.");
    }
}