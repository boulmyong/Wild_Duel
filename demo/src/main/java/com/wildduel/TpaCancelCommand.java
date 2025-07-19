package com.wildduel;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpaCancelCommand implements CommandExecutor {

    private final TpaManager tpaManager;

    public TpaCancelCommand(TpaManager tpaManager) {
        this.tpaManager = tpaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player.");
            return true;
        }

        Player requester = (Player) sender;
        TpaRequest request = tpaManager.getPendingRequest(requester.getUniqueId());

        if (request == null) {
            requester.sendMessage("§c취소할 TPA 요청이 없습니다.");
            return true;
        }

        Player target = Bukkit.getPlayer(request.getTarget());
        tpaManager.cancelTpa(requester, target, true);
        return true;
    }
}
