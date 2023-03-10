package rocks.learnercouncil.yesboats.arena;

import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.List;

import static org.bukkit.ChatColor.BOLD;
import static org.bukkit.ChatColor.GOLD;

public class ArenaScoreboard {

    private final Scoreboard scoreboard;
    private final Player player;

    private Objective gameObjective, queueObjective;

    private Team queueTime, raceTime;
    private Team[] positions = new Team[5];

    public ArenaScoreboard(ScoreboardManager manager, Player player) {
        this.scoreboard = manager.getNewScoreboard();
        this.player = player;
        initializeScoreboard();
        player.setScoreboard(scoreboard);
    }

    private void initializeScoreboard() {
        gameObjective = scoreboard.registerNewObjective("main", "dummy", GOLD.toString() + BOLD + "YesBoats");
        queueTime = scoreboard.registerNewTeam("YBSB_queueTime");
        raceTime = scoreboard.registerNewTeam("YBSB_raceTime");
        raceTime.addEntry("Time: ");
        positions[0] = scoreboard.registerNewTeam("YBSB_position1");
        positions[0].addEntry("#1 ");
        positions[1] = scoreboard.registerNewTeam("YBSB_position2");
        positions[1].addEntry("#2 ");
        positions[2] = scoreboard.registerNewTeam("YBSB_position3");
        positions[2].addEntry("#3 ");
        positions[3] = scoreboard.registerNewTeam("YBSB_position4");
        positions[3].addEntry("#4 ");
        positions[4] = scoreboard.registerNewTeam("YBSB_position5");
        positions[4].addEntry("#5 ");

        //Game Objective
        gameObjective.getScore("  ").setScore(0);
        gameObjective.getScore("Time: ").setScore(1);
        gameObjective.getScore("   ").setScore(2);
        gameObjective.getScore("#1 ").setScore(3);
        gameObjective.getScore("#2 ").setScore(4);
        gameObjective.getScore("#3 ").setScore(5);
    }

    public void update(int time, List<Player> players) {

        raceTime.setSuffix((time/60) + ":" + (time%60));

        positions[0].setSuffix(players.get(0).getName());

        positions[1].setSuffix(players.get(1).getName());

        int playerCount = players.size();
        int currentPosition = players.indexOf(player);

        if(playerCount >= 3)
            positions[2].setSuffix(players.get(2).getName());

        gameObjective.getScore("    ").setScore(6);

        if(currentPosition == 4 || currentPosition == 5) {
            positions[3].setSuffix(players.get(3).getName());
            gameObjective.getScore("#4 ").setScore(6);
            scoreboard.resetScores("...");
            gameObjective.getScore("    ").setScore(7);
            if(playerCount >= 5) {
                positions[4].setSuffix(players.get(4).getName());
                positions[4].setPrefix("#5 ");
                gameObjective.getScore(" ").setScore(7);
                gameObjective.getScore("    ").setScore(8);
            }
        } else if(currentPosition > 5) {
            gameObjective.getScore("...").setScore(6);
            scoreboard.resetScores("#4 ");

            positions[4].setSuffix(player.getName());
            positions[4].setPrefix("#" + currentPosition + " ");
            gameObjective.getScore("    ").setScore(8);
        }

    }

}
