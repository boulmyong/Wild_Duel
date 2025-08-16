package com.wildduel.game;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TeamManager {

    public static final String SPECTATOR_TEAM_NAME = "Spectator"; // 관전자 팀 이름 상수
    private final Map<String, TeamData> teams = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerTeams = new ConcurrentHashMap<>();
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
        createTeam(SPECTATOR_TEAM_NAME, ChatColor.GRAY); // 관전자 팀 추가
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

        String oldTeamName = getPlayerTeam(player); // Get old team BEFORE leaving

        leaveTeam(player); // This will remove the player from the old team and set their gamemode to ADVENTURE if they were a spectator.

        TeamData teamData = teams.get(teamName);
        teamData.getPlayers().add(player);
        playerTeams.put(player.getUniqueId(), teamName);

        updatePlayerScoreboard(player, teamData);
        player.sendMessage(teamData.getColor() + "당신은 " + teamData.getName() + " 팀에 합류했습니다.");

        boolean wasSpectator = oldTeamName != null && oldTeamName.equals(SPECTATOR_TEAM_NAME);
        boolean isNowSpectator = teamName.equals(SPECTATOR_TEAM_NAME);

        if (isNowSpectator) {
            player.setGameMode(GameMode.SPECTATOR);
        } else {
            // The gamemode is already set to ADVENTURE by leaveTeam if they were a spectator.
            if (wasSpectator) {
                player.teleport(player.getWorld().getSpawnLocation());
                // You might want to add a configurable message for this in messages.yml
                player.sendMessage(ChatColor.AQUA + "팀에 합류하여 월드 스폰 지점으로 이동했습니다.");
            }
        }
        return true;
    }

    public void leaveTeam(Player player) {
        if (playerTeams.containsKey(player.getUniqueId())) {
            String teamName = playerTeams.remove(player.getUniqueId());
            if (teams.containsKey(teamName)) {
                teams.get(teamName).getPlayers().remove(player);
            }
            removePlayerFromScoreboardTeam(player);

            // 팀을 떠날 때, 만약 관전자였다면 게임 모드를 ADVENTURE로 변경
            if (player.getGameMode() == GameMode.SPECTATOR) {
                player.setGameMode(GameMode.ADVENTURE);
            }
        }
    }

    public String getPlayerTeam(Player player) {
        return playerTeams.get(player.getUniqueId());
    }

    // 새로운 isSpectator 메소드
    public boolean isSpectator(Player player) {
        String teamName = getPlayerTeam(player);
        return teamName != null && teamName.equals(SPECTATOR_TEAM_NAME);
    }

    public void assignRandomTeams() {
        leaveAllTeams();
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(onlinePlayers);

        // 관전자 팀을 제외한 팀 목록 가져오기
        List<String> teamNames = new ArrayList<>(teams.keySet());
        teamNames.remove(SPECTATOR_TEAM_NAME);

        if (teamNames.isEmpty()) return; // Assignable teams don't exist

        int teamIndex = 0;

        for (Player player : onlinePlayers) {
            // 이미 관전자 팀인 플레이어는 무작위 팀 배정에서 제외
            if (isSpectator(player)) {
                continue;
            }
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
        private final Set<Player> players = Collections.synchronizedSet(new HashSet<>());

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
