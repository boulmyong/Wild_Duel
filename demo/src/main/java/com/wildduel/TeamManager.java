package com.wildduel;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TeamManager {

    private final Map<String, TeamData> teams = new HashMap<>();
    private final Map<Player, String> playerTeams = new HashMap<>();

    public TeamManager() {
        // Pre-defined teams
        createTeam("레드", ChatColor.RED);
        createTeam("블루", ChatColor.BLUE);
    }

    public void createTeam(String name, ChatColor color) {
        if (!teams.containsKey(name)) {
            teams.put(name, new TeamData(name, color));
        }
    }

    public boolean joinTeam(Player player, String teamName) {
        if (!teams.containsKey(teamName)) {
            return false; // Team does not exist
        }

        leaveTeam(player); // Leave current team first
        teams.get(teamName).getPlayers().add(player);
        playerTeams.put(player, teamName);
        return true;
    }

    public void leaveTeam(Player player) {
        if (playerTeams.containsKey(player)) {
            String teamName = playerTeams.get(player);
            teams.get(teamName).getPlayers().remove(player);
            playerTeams.remove(player);
        }
    }

    public String getPlayerTeam(Player player) {
        return playerTeams.get(player);
    }

    public Set<Player> getTeamPlayers(String teamName) {
        return teams.get(teamName).getPlayers();
    }

    public Map<String, TeamData> getTeams() {
        return teams;
    }

    public static class TeamData {
        private final String name;
        private final ChatColor color;
        private final Set<Player> players = new HashSet<>();

        public TeamData(String name, ChatColor color) {
            this.name = name;
            this.color = color;
        }

        public String getName() {
            return name;
        }

        public ChatColor getColor() {
            return color;
        }

        public Set<Player> getPlayers() {
            return players;
        }
    }
}
