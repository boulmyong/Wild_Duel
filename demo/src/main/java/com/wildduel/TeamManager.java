package com.wildduel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class TeamManager {

    private final Map<String, TeamData> teams = new HashMap<>();
    private final Map<UUID, String> playerTeams = new HashMap<>();

    public TeamManager() {
        initializeTeams();
    }

    public void initializeTeams() {
        // Ensure teams are created on the main scoreboard
        createTeam("Red", ChatColor.RED);
        createTeam("Blue", ChatColor.BLUE);
    }

    public void createTeam(String name, ChatColor color) {
        teams.put(name, new TeamData(name, color));
        Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();
        Team team = scoreboard.getTeam(name);
        if (team == null) {
            team = scoreboard.registerNewTeam(name);
        }
        team.setAllowFriendlyFire(false);
        team.setColor(color);
        team.setPrefix(color + "[" + name + "] ");
    }

    public boolean joinTeam(Player player, String teamName) {
        if (!teams.containsKey(teamName)) {
            return false; // Team does not exist
        }

        leaveTeam(player); // Leave current team first

        TeamData teamData = teams.get(teamName);
        teamData.getPlayers().add(player);
        playerTeams.put(player.getUniqueId(), teamName);

        updatePlayerScoreboard(player, teamData);
        player.sendMessage(teamData.getColor() + "You have joined the " + teamData.getName() + " team.");
        return true;
    }

    public void leaveTeam(Player player) {
        if (playerTeams.containsKey(player.getUniqueId())) {
            String teamName = playerTeams.remove(player.getUniqueId());
            if (teams.containsKey(teamName)) {
                teams.get(teamName).getPlayers().remove(player);
            }
            removePlayerFromScoreboardTeam(player);
        }
    }

    public String getPlayerTeam(Player player) {
        return playerTeams.get(player.getUniqueId());
    }

    public void assignRandomTeams() {
        leaveAllTeams();
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(onlinePlayers);

        List<String> teamNames = new ArrayList<>(teams.keySet());
        int teamIndex = 0;

        for (Player player : onlinePlayers) {
            String teamName = teamNames.get(teamIndex);
            joinTeam(player, teamName);
            teamIndex = (teamIndex + 1) % teamNames.size();
        }
    }

    public void leaveAllTeams() {
        List<UUID> playerUuids = new ArrayList<>(playerTeams.keySet());
        for (UUID uuid : playerUuids) {
            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                leaveTeam(player);
            }
        }
    }

    public Collection<TeamData> getTeams() {
        return teams.values();
    }

    private void updatePlayerScoreboard(Player player, TeamData teamData) {
        Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();
        Team team = scoreboard.getTeam(teamData.getName());
        if (team == null) {
            createTeam(teamData.getName(), teamData.getColor());
            team = scoreboard.getTeam(teamData.getName());
        }
        player.setScoreboard(scoreboard);
        Objects.requireNonNull(team).addEntry(player.getName());
    }

    private void removePlayerFromScoreboardTeam(Player player) {
        Scoreboard scoreboard = Objects.requireNonNull(Bukkit.getScoreboardManager()).getMainScoreboard();
        Team team = scoreboard.getEntryTeam(player.getName());
        if (team != null) {
            team.removeEntry(player.getName());
        }
        player.setScoreboard(scoreboard);
    }

    public void applyTeamVisualsOnJoin(Player player) {
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        String teamName = getPlayerTeam(player);
        if (teamName != null) {
            TeamData teamData = teams.get(teamName);
            if (teamData != null) {
                updatePlayerScoreboard(player, teamData);
            }
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