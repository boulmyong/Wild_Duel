package com.wildduel.commands;

import com.wildduel.WildDuel;
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
            sender.sendMessage(WildDuel.getInstance().getMessage("error.player-only"));
            return true;
        }

        Player target = (Player) sender;
        if (args.length != 2) {
            // 클릭 이벤트를 통해 실행되므로, 정상적인 경우 항상 인자는 2개여야 합니다.
            // 잘못된 접근을 방지하기 위해 아무런 메시지 없이 명령을 종료합니다.
            return true;
        }

        boolean accepted = args[0].equalsIgnoreCase("accept");
        String requesterName = args[1];

        tpaManager.handleResponse(target, requesterName, accepted);
        return true;
    }
}
