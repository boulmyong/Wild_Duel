package com.wildduel;

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
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        Player requester = (Player) sender;
        if (args.length < 1) {
            requester.sendMessage("§cUsage: /tpa <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            requester.sendMessage("§c플레이어를 찾을 수 없습니다.");
            return true;
        }

        if (requester.equals(target)) {
            requester.sendMessage("§c자기 자신에게 TPA 요청을 보낼 수 없습니다.");
            return true;
        }

        tpaManager.requestTpa(requester, target);
        return true;
    }
}
