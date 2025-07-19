package com.wildduel;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TeamCommand implements CommandExecutor {

    private final TeamAdminManager adminManager;
    private final TeamManager teamManager;

    public TeamCommand(TeamAdminManager adminManager, TeamManager teamManager) {
        this.adminManager = adminManager;
        this.teamManager = teamManager;
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

        new TeamAdminGUI(adminManager, teamManager).open(player);
        return true;
    }
}
