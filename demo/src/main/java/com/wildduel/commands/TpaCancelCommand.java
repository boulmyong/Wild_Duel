package com.wildduel.commands;

import com.wildduel.WildDuel;
import com.wildduel.game.TpaManager;
import com.wildduel.game.TpaRequest;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TpaCancelCommand implements CommandExecutor {

    private final WildDuel plugin;
    private final TpaManager tpaManager;

    public TpaCancelCommand(WildDuel plugin, TpaManager tpaManager) {
        this.plugin = plugin;
        this.tpaManager = tpaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(plugin.getMessage("error.player-only"));
            return true;
        }

        Player requester = (Player) sender;
        TpaRequest request = tpaManager.getPendingRequest(requester.getUniqueId());

        if (request == null) {
            requester.sendMessage(plugin.getMessage("tpa.error.no-request-to-cancel"));
            return true;
        }

        Player target = Bukkit.getPlayer(request.getTarget());

        // TpaManager의 cancelTpa는 target이 null이어도 안전하게 처리하지만,
        // 명령어 클래스 수준에서도 명시적으로 처리해주는 것이 가독성과 안정성에 좋습니다.
        tpaManager.cancelTpa(requester, target, true);
        return true;
    }
}
