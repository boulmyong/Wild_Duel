package com.wildduel;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WildDuelCommand implements CommandExecutor {

    private final GameManager gameManager;

    public WildDuelCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.isOp()) {
            player.sendMessage("You do not have permission to use this command.");
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "help":
                sendHelpMessage(player);
                break;
            case "set":
                gameManager.setLobby(player.getLocation());
                player.sendMessage("Wild Duel lobby has been set and all players have been teleported.");
                break;
            case "sp":
                gameManager.setWorldSpawn(player.getLocation());
                player.sendMessage("Wild Duel world spawn has been set.");
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
            default:
                sendHelpMessage(player);
                break;
        }

        return true;
    }

    private void sendHelpMessage(Player player) {
        player.sendMessage("--- 와일드 듀얼 명령어 목록 ---");
        player.sendMessage("/wd set - 로비를 설정하고 모든 플레이어를 텔레포트합니다.");
        player.sendMessage("/wd sp - 월드 스폰 위치를 설정합니다.");
        player.sendMessage("/wd start - 게임을 시작합니다.");
        player.sendMessage("/wd preptime <초> - 시작 전 파밍 시간을 설정합니다.");
        player.sendMessage("/wd st <초> - 남은 파밍 시간을 변경합니다.");
        player.sendMessage("/wd help - 도움말을 표시합니다.");
    }
}