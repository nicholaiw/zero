package nw.zero;

import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
public class GameManager {

    private final Map<Integer, GameState> games = new HashMap<>();
    private int nextGameId = 0;

    public GameState enterGame(String playerName) {
        GameState game = findOpenGame();
        if (game == null) {
            game = createGame();
        }
        game.getPlayers().add(new GameState.Player(playerName));
        if (game.getPlayers().size() == 4) {
            startGame(game);
        }
        return game;
    }

    private GameState findOpenGame() {
        for (GameState game : games.values()) {
            if (game.getPhase() == GameState.Phase.WAITING && game.getPlayers().size() < 4) {
                return game;
            }
        }
        return null;
    }

    private GameState createGame() {
        GameState game = new GameState(nextGameId);
        games.put(nextGameId, game);
        nextGameId++;
        return game;
    }

    private void startGame(GameState game) {
        game.setPhase(GameState.Phase.BETTING);
        game.setRound(1);
        game.setBet(3);

        for (GameState.Player player : game.getPlayers()) {
            player.setFolded(false);
            player.setCurrentBet(3);
            player.setBalance(player.getBalance() - 3);
            if (player.getBalance() < 0) {
                player.setBalance(3);
            }
        }

        game.setCurrentTurn(game.getPlayers().get(0).getName());
    }
}