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
            // Send help message
            return true;
        }

        switch (args[0].toLowerCase()) {
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
            case "ts":
                if (args.length > 1) {
                    try {
                        int seconds = Integer.parseInt(args[1]);
                        gameManager.setTime(seconds);
                        player.sendMessage("Farming time set to " + seconds + " seconds.");
                    } catch (NumberFormatException e) {
                        player.sendMessage("Invalid time specified.");
                    }
                } else {
                    player.sendMessage("Usage: /wd ts <seconds>");
                }
                break;
            default:
                player.sendMessage("Unknown command.");
                break;
        }

        return true;
    }
}
