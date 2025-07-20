package com.wildduel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class TeamManager {

    private final Map<String, TeamData> teams = new HashMap<>();
    private final Map<Player, String> playerTeams = new HashMap<>();

    public TeamManager() {
        // Pre-defined teams
        createTeam("Red", ChatColor.RED);
        createTeam("Blue", ChatColor.BLUE);
    }

    public void createTeam(String name, ChatColor color) {
        if (!teams.containsKey(name)) {
            teams.put(name, new TeamData(name, color));
            // Also create in scoreboard
            Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();
            Team team = scoreboard.getTeam(name);
            if (team == null) {
                team = scoreboard.registerNewTeam(name);
                team.setAllowFriendlyFire(false);
                team.setColor(color);
            }
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

    public void assignRandomTeams() {
        leaveAllTeams();
        Player[] players = Bukkit.getOnlinePlayers().toArray(new Player[0]);
        List<Player> playerList = new ArrayList<>(Arrays.asList(players));
        Collections.shuffle(playerList);

        List<String> teamNames = new ArrayList<>(teams.keySet());
        int teamIndex = 0;

        for (Player player : playerList) {
            String teamName = teamNames.get(teamIndex);
            joinTeam(player, teamName);
            teamIndex = (teamIndex + 1) % teamNames.size();
        }
    }

    public void leaveAllTeams() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            leaveTeam(player);
        }
    }

    public Map<String, TeamData> getTeams() {
        return teams;
    }

    private void updatePlayerScoreboard(Player player, TeamData teamData) {
        Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();
        Team team = scoreboard.getTeam(teamData.getName());
        if (team == null) {
            team = scoreboard.registerNewTeam(teamData.getName());
            team.setAllowFriendlyFire(false);
            team.setColor(teamData.getColor());
        }
        team.addEntry(player.getName());
    }

    private void removePlayerFromScoreboardTeam(Player player) {
        Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();
        Team team = scoreboard.getEntryTeam(player.getName());
        if (team != null) {
            team.removeEntry(player.getName());
        }
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
