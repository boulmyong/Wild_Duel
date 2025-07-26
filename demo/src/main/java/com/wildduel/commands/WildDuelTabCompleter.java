package com.wildduel.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class WildDuelTabCompleter implements TabCompleter {

    private static final List<String> PLAYER_COMMANDS = Arrays.asList("help");
    private static final List<String> ADMIN_COMMANDS = Arrays.asList(
        "help", "start", "reset", "team", "randomteam", "admin", 
        "autosmelt", "tparefresh", "tpastatus", "st", "setinventory"
    );

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> completions = new ArrayList<>();
            List<String> commandsToShow = sender.hasPermission("wildduel.admin") ? ADMIN_COMMANDS : PLAYER_COMMANDS;
            
            StringUtil.copyPartialMatches(args[0], commandsToShow, completions);
            return completions;
        }
        
        // Example for sub-command completion
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("autosmelt") && sender.hasPermission("wildduel.admin")) {
                return StringUtil.copyPartialMatches(args[1], Arrays.asList("on", "off"), new ArrayList<>());
            }
            if (args[0].equalsIgnoreCase("tparefresh") && sender.hasPermission("wildduel.admin")) {
                return StringUtil.copyPartialMatches(args[1], Arrays.asList("all"), new ArrayList<>());
            }
        }
        
        return null; // No other completions
    }
}
