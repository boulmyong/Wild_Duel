package com.wildduel.commands;

import com.wildduel.gui.AdminGUI;
import com.wildduel.gui.TeamAdminGUI;
import com.wildduel.game.GameManager;
import com.wildduel.game.TeamManager;
import com.wildduel.game.TpaManager;
import com.wildduel.game.TeamAdminManager;
import com.wildduel.gui.StartItemGUI;
import com.wildduel.WildDuel;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class WildDuelCommand implements CommandExecutor {

    private final GameManager gameManager;
    private final TeamManager teamManager;
    private final TeamAdminManager teamAdminManager;
    private final TpaManager tpaManager;

    public WildDuelCommand(GameManager gameManager, TeamManager teamManager, TeamAdminManager teamAdminManager, TpaManager tpaManager) {
        this.gameManager = gameManager;
        this.teamManager = teamManager;
        this.teamAdminManager = teamAdminManager;
        this.tpaManager = tpaManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        // Non-player specific commands can be handled here if any

        if (!(sender instanceof Player)) {
            // Handle commands that can be run by console if any, otherwise return
            // For now, all commands require a player
        }

        switch (args[0].toLowerCase()) {
            case "help":
                sendHelpMessage(sender);
                break;
            case "set":
            case "sp":
            case "start":
            case "preptime":
            case "st":
            case "team":
            case "randomteam":
            case "admin":
            case "autosmelt":
            case "setinventory":
                handlePlayerCommands(sender, args);
                break;
            case "reset":
                handleResetCommand(sender, args);
                break;
            case "tparefresh":
            case "tpastatus":
                handleAdminTpaCommands(sender, args);
                break;
            default:
                sendHelpMessage(sender);
                break;
        }

        return true;
    }

    private void handleResetCommand(CommandSender sender, String[] args) {
        if (!sender.isOp() && !sender.hasPermission("wildduel.admin")) {
            sender.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
            return;
        }

        if (args.length > 1 && args[1].equalsIgnoreCase("confirm")) {
            resetGameAndWorld(sender);
            return;
        }

        sender.sendMessage("§6=============================================");
        sender.sendMessage("§e⚠️ 정말로 게임과 월드를 완전히 초기화하시겠습니까?");
        sender.sendMessage("§c이 작업은 되돌릴 수 없으며, 현재 게임 월드를 삭제합니다.");
        sender.sendMessage("§e계속하려면 §l/wd reset confirm§e 을 입력하세요.");
        sender.sendMessage("§6=============================================");
    }

    private void resetGameAndWorld(CommandSender sender) {
        Bukkit.broadcastMessage("§c⚠️ 관리자에 의해 전체 게임 초기화가 시작되었습니다. 잠시 렉이 발생할 수 있습니다.");
        sender.sendMessage("§a[WD] 초기화 작업을 시작합니다...");

        // 1. Reset internal game systems
        sender.sendMessage("§a[WD] 게임 상태, 팀, TPA 정보를 초기화합니다...");
        gameManager.resetGame();
        teamManager.resetTeams();
        tpaManager.resetTpa();

        // 2. Evacuate players before touching worlds
        sender.sendMessage("§a[WD] 모든 플레이어를 로비로 이동시킵니다...");
        World lobbyWorld = Bukkit.getWorld("wildduel_world");
        if (lobbyWorld == null) {
            sender.sendMessage("§c[WD] 치명적 오류: 로비 월드를 찾을 수 없습니다! 초기화를 중단합니다.");
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(lobbyWorld.getSpawnLocation());
        }

        // 3. Recreate the game world using the centralized method
        // This correctly targets 'wildduel_game' and handles unload/delete/create
        WildDuel.getInstance().recreateGameWorld();

        // 4. Finalize and notify
        gameManager.initializeWorlds(); // Re-initialize world references in GameManager
        Bukkit.broadcastMessage("§6✅ 게임과 월드가 성공적으로 초기화되었습니다!");
        sender.sendMessage("§b[WD] 초기화 완료.");
    }

    private void deleteWorldFolder(java.io.File path) throws Exception {
        if (path.exists()) {
            java.io.File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteWorldFolder(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        path.delete();
    }

    private void handlePlayerCommands(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("This command can only be used by a player.");
            return;
        }
        Player player = (Player) sender;
        if (!player.isOp()) {
            player.sendMessage("You do not have permission to use this command.");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "start":
                if (!sender.isOp()) {
                    sender.sendMessage("You do not have permission.");
                    return;
                }
                gameManager.startGame();
                sender.sendMessage("Game sequence initiated!");
                break;
            case "team":
                new TeamAdminGUI(teamAdminManager, teamManager).open((Player) sender);
                break;
            case "randomteam":
                if (!sender.isOp()) {
                    sender.sendMessage("You do not have permission.");
                    return;
                }
                teamManager.assignRandomTeams();
                sender.sendMessage("All players have been assigned to random teams.");
                break;
            case "admin":
                if (!sender.isOp()) {
                    sender.sendMessage("You do not have permission.");
                    return;
                }
                new AdminGUI(gameManager, teamManager, tpaManager, teamAdminManager).open((Player) sender);
                break;
            case "st":
                if (!sender.isOp()) {
                    sender.sendMessage("You do not have permission.");
                    return;
                }
                if (args.length > 1) {
                    try {
                        int seconds = Integer.parseInt(args[1]);
                        gameManager.addTime(seconds);
                        sender.sendMessage("§bRemaining time adjusted by " + seconds + " seconds.");
                    } catch (NumberFormatException e) {
                        sender.sendMessage("§cInvalid number format.");
                    }
                } else {
                    sender.sendMessage("§cUsage: /wd st <seconds>");
                }
                break;
            case "autosmelt":
                if (!sender.isOp()) {
                    sender.sendMessage("You do not have permission.");
                    return;
                }
                if (args.length > 1) {
                    if (args[1].equalsIgnoreCase("on")) {
                        gameManager.setAutoSmelt(true);
                        sender.sendMessage("Auto-smelt enabled.");
                    } else if (args[1].equalsIgnoreCase("off")) {
                        gameManager.setAutoSmelt(false);
                        sender.sendMessage("Auto-smelt disabled.");
                    } else {
                        sender.sendMessage("Usage: /wd autosmelt <on|off>");
                    }
                } else {
                    sender.sendMessage("Usage: /wd autosmelt <on|off>");
                }
                break;
            case "setinventory":
                if (!player.isOp() && !player.hasPermission("wildduel.admin")) {
                    player.sendMessage("§c이 명령어를 사용할 권한이 없습니다.");
                    return;
                }
                new StartItemGUI(WildDuel.getInstance().getDefaultStartInventory()).open(player);
                player.sendMessage("§a시작 아이템 설정 GUI를 엽니다.");
                break;
        }
    }

    private void handleAdminTpaCommands(CommandSender sender, String[] args) {
        if (!sender.isOp()) {
            sender.sendMessage("You do not have permission to use this command.");
            return;
        }

        switch (args[0].toLowerCase()) {
            case "tparefresh":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /wd tparefresh <player|all>");
                    return;
                }
                if (args[1].equalsIgnoreCase("all")) {
                    tpaManager.refreshAllCooldowns();
                    sender.sendMessage("§a[TPA] 전체 플레이어의 쿨타임을 초기화했습니다.");
                } else {
                    Player target = Bukkit.getPlayer(args[1]);
                    if (target == null) {
                        sender.sendMessage("§c[TPA] 해당 플레이어는 온라인이 아닙니다.");
                        return;
                    }
                    if (tpaManager.refreshCooldown(target)) {
                        sender.sendMessage("§a[TPA] " + target.getName() + "님의 쿨타임을 초기화했습니다.");
                    } else {
                        sender.sendMessage("§c[TPA] " + target.getName() + "님은 현재 쿨타임이 없습니다.");
                    }
                }
                break;
            case "tpastatus":
                if (args.length < 2) {
                    sender.sendMessage("§cUsage: /wd tpastatus <player>");
                    return;
                }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage("§c[TPA] 해당 플레이어는 온라인이 아닙니다.");
                    return;
                }
                long remaining = tpaManager.getRemainingCooldown(target);
                if (remaining > 0) {
                    long minutes = remaining / 60;
                    long seconds = remaining % 60;
                    sender.sendMessage(String.format("§e[TPA] %s님의 남은 쿨타임: %d분 %d초", target.getName(), minutes, seconds));
                } else {
                    sender.sendMessage("§a[TPA] " + target.getName() + "님은 현재 쿨타임이 없습니다.");
                }
                break;
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage("§a--- 와일드 듀얼 명령어 목록 ---");
        sender.sendMessage("§e/wd help §7- §f이 도움말을 표시합니다.");
        sender.sendMessage("§e/wd admin §7- §f(추천) 모든 설정을 관리하는 GUI를 엽니다.");
        sender.sendMessage("§e/wd setinventory §7- §f(추천) GUI를 통해 시작 아이템 인벤토리를 설정합니다.");
        sender.sendMessage("§e/wd start §7- §f게임을 즉시 시작합니다.");
        sender.sendMessage("§e/wd team §7- §f팀 관리 GUI를 엽니다.");
        sender.sendMessage("§e/wd randomteam §7- §f모든 플레이어를 랜덤 팀에 배정합니다.");
        sender.sendMessage("§e/wd st <seconds> §7- §f파밍 시간을 조절합니다 (양수/음수 가능).");
        sender.sendMessage("§e/wd autosmelt <on|off> §7- §f자동 제련 기능을 켜거나 끕니다.");
        sender.sendMessage("§e/wd reset [confirm] §7- §f게임과 월드를 초기화합니다. (confirm 필요)");
        sender.sendMessage("§e/wd tparefresh <player|all> §7- §fTPA 쿨타임을 초기화합니다.");
        sender.sendMessage("§e/wd tpastatus <player> §7- §fTPA 쿨타임 상태를 확인합니다.");
        sender.sendMessage("§7- 상세 설정(스폰, 파밍 시간 등)은 /wd admin 패널을 이용해주세요.");
    }
}