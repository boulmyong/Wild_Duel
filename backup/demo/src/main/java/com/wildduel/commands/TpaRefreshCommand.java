package com.wildduel.commands;

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
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /tparefresh <player|all>");
            return true;
        }

        if (args[0].equalsIgnoreCase("all")) {
            tpaManager.refreshAllCooldowns();
            sender.sendMessage("§a[TPA] 전체 플레이어의 쿨타임을 초기화했습니다.");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§c[TPA] 해당 플레이어는 온라인이 아닙니다.");
            return true;
        }

        if (tpaManager.refreshCooldown(target)) {
            sender.sendMessage("§a[TPA] " + target.getName() + "님의 쿨타임을 초기화했습니다.");
        } else {
            sender.sendMessage("§c[TPA] " + target.getName() + "님은 현재 쿨타임이 없습니다.");
        }

        return true;
    }
}
