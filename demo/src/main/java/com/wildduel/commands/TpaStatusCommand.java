package com.wildduel.commands;

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
        if (!sender.isOp()) {
            sender.sendMessage("§cYou do not have permission to use this command.");
            return true;
        }

        if (args.length < 1) {
            sender.sendMessage("§cUsage: /tpastatus <player>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§c[TPA] 해당 플레이어는 온라인이 아닙니다.");
            return true;
        }

        long remaining = tpaManager.getRemainingCooldown(target);
        if (remaining > 0) {
            long minutes = remaining / 60;
            long seconds = remaining % 60;
            sender.sendMessage(String.format("§e[TPA] %s님의 남은 쿨타임: %d분 %d초", target.getName(), minutes, seconds));
        } else {
            sender.sendMessage("§a[TPA] " + target.getName() + "님은 현재 쿨타임이 없습니다.");
        }

        return true;
    }
}
