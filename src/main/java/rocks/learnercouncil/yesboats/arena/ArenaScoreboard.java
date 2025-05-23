package rocks.learnercouncil.yesboats.arena;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import static org.bukkit.ChatColor.*;

public class ArenaScoreboard {

    private final Scoreboard scoreboard;
    private Objective queue, queueIdle, game;
    private Team startsIn, timeLeft, lap;

    public ArenaScoreboard(Player player, Scoreboard scoreboard) {
        this.scoreboard = scoreboard;
        player.setScoreboard(scoreboard);
        initializeScores();
    }

    private void initializeScores() {
        queue = scoreboard.registerNewObjective("queue", Criteria.DUMMY, DARK_AQUA.toString() + BOLD + "YesBoats");
        queueIdle = scoreboard.registerNewObjective("queueIdle",
                Criteria.DUMMY,
                DARK_AQUA.toString() + BOLD + "YesBoats"
        );
        game = scoreboard.registerNewObjective("game", Criteria.DUMMY, DARK_AQUA.toString() + BOLD + "YesBoats");

        startsIn = scoreboard.registerNewTeam("YBSB_startsIn");
        timeLeft = scoreboard.registerNewTeam("YBSB_timeLeft");
        lap = scoreboard.registerNewTeam("YBSB_lap");

        queueIdle.getScore(" ").setScore(3);
        queueIdle.getScore(AQUA + "Waiting for players...").setScore(2);
        queueIdle.getScore("  ").setScore(1);
        queueIdle.getScore("   ").setScore(0);

        queue.getScore(" ").setScore(3);
        queue.getScore(AQUA + "Starts in: ").setScore(2);
        startsIn.addEntry(AQUA + "Starts in: ");
        queue.getScore("  ").setScore(1);
        queue.getScore("   ").setScore(0);

        game.getScore(" ").setScore(3);
        game.getScore(AQUA + "Time Left: ").setScore(2);
        timeLeft.addEntry(AQUA + "Time Left: ");
        game.getScore(AQUA + "Lap: ").setScore(1);
        lap.addEntry(AQUA + "Lap: ");
        game.getScore("  ").setScore(0);
    }

    public void updateScores(int seconds) {
        if (seconds == -1) {
            queueIdle.setDisplaySlot(DisplaySlot.SIDEBAR);
            return;
        }
        if (queue.getDisplaySlot() != DisplaySlot.SIDEBAR) queue.setDisplaySlot(DisplaySlot.SIDEBAR);
        startsIn.setSuffix(YELLOW + String.valueOf(seconds));
    }

    public void updateScores(int seconds, int currentLap, int maxLaps) {
        if (game.getDisplaySlot() != DisplaySlot.SIDEBAR) game.setDisplaySlot(DisplaySlot.SIDEBAR);
        timeLeft.setSuffix(String.format(YELLOW + "%d:%02d", seconds / 60, seconds % 60));
        lap.setSuffix(YELLOW.toString() + currentLap + "/" + maxLaps);
    }
}
