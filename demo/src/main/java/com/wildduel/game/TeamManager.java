package com.wildduel.game;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class TeamManager {

    private final Map<String, TeamData> teams = new HashMap<>();
    private final Map<UUID, String> playerTeams = new HashMap<>();
    private final Scoreboard scoreboard;

    public TeamManager() {
        ScoreboardManager manager = Bukkit.getScoreboardManager();
        if (manager == null) {
            throw new IllegalStateException("Scoreboard Manager could not be accessed.");
        }
        this.scoreboard = manager.getMainScoreboard();
        initializeTeams();
    }

    public void initializeTeams() {
        // Ensure teams are created on the main scoreboard
        createTeam("Red", ChatColor.RED);
        createTeam("Blue", ChatColor.BLUE);
    }

    public void createTeam(String name, ChatColor color) {
        teams.put(name, new TeamData(name, color));
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
        player.sendMessage(teamData.getColor() + "당신은 " + teamData.getName() + " 팀에 합류했습니다.");
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

    public void resetTeams() {
        leaveAllTeams();
        for (String teamName : teams.keySet()) {
            Team team = scoreboard.getTeam(teamName);
            if (team != null) {
                team.unregister();
            }
        }
        teams.clear();
        playerTeams.clear();
        initializeTeams(); // Re-create default teams
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
        Team team = scoreboard.getTeam(teamData.getName());
        if (team == null) {
            // This should ideally not happen if teams are initialized correctly
            createTeam(teamData.getName(), teamData.getColor());
            team = scoreboard.getTeam(teamData.getName());
        }
        player.setScoreboard(scoreboard);
        if (team != null) {
            team.addEntry(player.getName());
        }
    }

    private void removePlayerFromScoreboardTeam(Player player) {
        Team team = scoreboard.getEntryTeam(player.getName());
        if (team != null) {
            team.removeEntry(player.getName());
        }
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

    public void handlePlayerQuit(Player player) {
        leaveTeam(player);
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