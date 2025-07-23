package com.wildduel.commands;

import com.wildduel.WildDuel;
import com.wildduel.game.TpaManager;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpaCommand implements CommandExecutor {

    private final TpaManager tpaManager;

    public TpaCommand(TpaManager tpaManager) {
        this.tpaManager = tpaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(WildDuel.getInstance().getMessage("error-must-be-player"));
            return true;
        }

        Player requester = (Player) sender;
        if (args.length < 1) {
            requester.sendMessage(WildDuel.getInstance().getMessage("tpa-usage"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            requester.sendMessage(WildDuel.getInstance().getMessage("error-player-not-found"));
            return true;
        }

        if (requester.equals(target)) {
            requester.sendMessage(WildDuel.getInstance().getMessage("error-cannot-tpa-self"));
            return true;
        }

        tpaManager.requestTpa(requester, target);
        return true;
    }
}
