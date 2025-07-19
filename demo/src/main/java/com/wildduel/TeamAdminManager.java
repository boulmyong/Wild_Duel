package com.wildduel;

import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeamAdminManager {

    private final Map<UUID, PlayerSelection> adminSelections = new HashMap<>();

    public PlayerSelection getPlayerSelection(Player admin) {
        return adminSelections.computeIfAbsent(admin.getUniqueId(), k -> new PlayerSelection());
    }

    public static class PlayerSelection {
        private UUID selectedPlayer;
        private TeamType selectedTeam = TeamType.NONE;
        private int currentPage = 0;

        public UUID getSelectedPlayer() {
            return selectedPlayer;
        }

        public void setSelectedPlayer(UUID selectedPlayer) {
            this.selectedPlayer = selectedPlayer;
        }

        public TeamType getSelectedTeam() {
            return selectedTeam;
        }

        public void setSelectedTeam(TeamType selectedTeam) {
            this.selectedTeam = selectedTeam;
        }

        public int getCurrentPage() {
            return currentPage;
        }

        public void setCurrentPage(int currentPage) {
            this.currentPage = currentPage;
        }
    }
}
