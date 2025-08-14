package com.wildduel.commands;

import com.wildduel.WildDuel;
import com.wildduel.game.TpaManager;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpaStatusCommand implements CommandExecutor {

    private final TpaManager tpaManager;

    public TpaStatusCommand(TpaManager tpaManager) {
        this.tpaManager = tpaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wildduel.admin")) {
            sender.sendMessage(WildDuel.getInstance().getMessage("error.no-permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(WildDuel.getInstance().getMessage("command.tpastatus.usage"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(WildDuel.getInstance().getMessage("error.player-not-found"));
            return true;
        }

        long remaining = tpaManager.getRemainingCooldown(target);
        if (remaining > 0) {
            long minutes = remaining / 60;
            long seconds = remaining % 60;
            sender.sendMessage(WildDuel.getInstance().getMessage("command.tpastatus.status", 
                "%player%", target.getName(), 
                "%minutes%", String.valueOf(minutes), 
                "%seconds%", String.valueOf(seconds)));
        } else {
            sender.sendMessage(WildDuel.getInstance().getMessage("command.tpastatus.no-cooldown", "%player%", target.getName()));
        }

        return true;
    }
}
