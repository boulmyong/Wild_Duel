package com.wildduel;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeamSelectCommand implements CommandExecutor {

    private final GameManager gameManager;
    private final TeamManager teamManager;

    public TeamSelectCommand(GameManager gameManager, TeamManager teamManager) {
        this.gameManager = gameManager;
        this.teamManager = teamManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("이 명령어는 플레이어만 사용할 수 있습니다.");
            return true;
        }

        Player player = (Player) sender;

        if (!gameManager.isPlayerTeamSelectionEnabled()) {
            player.sendMessage("§c팀 선택 기능이 현재 비활성화되어 있습니다.");
            return true;
        }
        
        if (gameManager.getGameState() == GameState.FARMING || gameManager.getGameState() == GameState.BATTLE) {
            player.sendMessage("§c게임 중에는 팀을 변경할 수 없습니다.");
            return true;
        }

        new TeamGUI(teamManager).open(player);
        return true;
    }
}
