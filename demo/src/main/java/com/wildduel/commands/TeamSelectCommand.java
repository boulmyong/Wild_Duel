package com.wildduel.commands;

import com.wildduel.WildDuel;
import com.wildduel.game.GameManager;
import com.wildduel.game.GameState;
import com.wildduel.gui.TeamGUI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeamSelectCommand implements CommandExecutor {

    private final WildDuel plugin;
    private final TeamGUI teamGUI;
    private final GameManager gameManager;

    public TeamSelectCommand(WildDuel plugin, TeamGUI teamGUI, GameManager gameManager) {
        this.plugin = plugin;
        this.teamGUI = teamGUI;
        this.gameManager = gameManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("error.player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (!gameManager.isPlayerTeamSelectionEnabled()) {
            player.sendMessage(plugin.getMessage("command.teamselect.disabled"));
            return true;
        }

        if (gameManager.getGameState() == GameState.FARMING || gameManager.getGameState() == GameState.BATTLE) {
            player.sendMessage(plugin.getMessage("command.teamselect.game-in-progress"));
            return true;
        }

        teamGUI.open(player);
        return true;
    }
}
