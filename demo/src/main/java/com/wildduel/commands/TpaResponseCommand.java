package com.wildduel.commands;

import com.wildduel.game.TpaManager;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpaResponseCommand implements CommandExecutor {

    private final TpaManager tpaManager;

    public TpaResponseCommand(TpaManager tpaManager) {
        this.tpaManager = tpaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        Player target = (Player) sender;
        if (args.length < 2) {
            // Invalid command usage, should not happen from click event
            return true;
        }

        boolean accepted = args[0].equalsIgnoreCase("accept");
        String requesterName = args[1];

        tpaManager.handleResponse(target, requesterName, accepted);
        return true;
    }
}
