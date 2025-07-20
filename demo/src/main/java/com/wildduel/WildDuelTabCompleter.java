package com.wildduel;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WildDuelTabCompleter implements TabCompleter {

    private static final String[] COMMANDS = { "help", "set", "sp", "start", "preptime", "st", "team", "randomteam", "admin", "autosmelt", "tparefresh", "tpastatus" };

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], Arrays.asList(COMMANDS), new ArrayList<>());
        }
        return null;
    }
}
