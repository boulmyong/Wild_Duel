package com.wildduel.commands;

import com.wildduel.WildDuel;
import com.wildduel.game.TpaManager;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpaRefreshCommand implements CommandExecutor {

    private final TpaManager tpaManager;

    public TpaRefreshCommand(TpaManager tpaManager) {
        this.tpaManager = tpaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("wildduel.admin")) {
            sender.sendMessage(WildDuel.getInstance().getMessage("error.no-permission"));
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage(WildDuel.getInstance().getMessage("command.tparefresh.usage"));
            return true;
        }

        if (args[0].equalsIgnoreCase("all")) {
            tpaManager.refreshAllCooldowns();
            sender.sendMessage(WildDuel.getInstance().getMessage("command.tparefresh.success-all"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(WildDuel.getInstance().getMessage("error.player-not-found"));
            return true;
        }

        if (tpaManager.refreshCooldown(target)) {
            sender.sendMessage(WildDuel.getInstance().getMessage("command.tparefresh.success-player", "%player%", target.getName()));
        } else {
            sender.sendMessage(WildDuel.getInstance().getMessage("command.tparefresh.no-cooldown", "%player%", target.getName()));
        }

        return true;
    }
}
