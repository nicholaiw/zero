package nw.zero;

import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GameManager {

    private final Map<Integer, GameState> games = new HashMap<>();
    private int nextGameId = 0;

    public GameState enterGame(String sessionId) {
        GameState game = findOpenGame();
        if (game == null) {
            game = createGame();
        }
        int playerNumber = game.getPlayers().size() + 1;
        game.getPlayers().add(new GameState.Player("Player " + playerNumber, sessionId));
        if (game.getPlayers().size() == 4) {
            startGame(game);
        }
        return game;
    }

    public GameState handleBet(int gameId, String sessionId, GameAction action) {
        GameState game = games.get(gameId);
        if (game == null) return null;
        if (game.getPhase() != GameState.Phase.BETTING) return game;

        GameState.Player player = game.getPlayerBySessionId(sessionId);
        if (player == null) return game;
        if (!game.getCurrentTurn().equals(player.getName())) return game;

        int highest = getHighestBet(game);

        if (action.getType() == GameAction.Type.FOLD) {
            player.setFolded(true);
        }

        if (action.getType() == GameAction.Type.CALL) {
            int difference = highest - player.getCurrentBet();
            if (player.getBalance() >= difference) {
                player.setCurrentBet(highest);
                player.setBalance(player.getBalance() - difference);
            }
        }

        if (action.getType() == GameAction.Type.RAISE) {
            int newBet = highest + 3;
            int difference = newBet - player.getCurrentBet();
            if (player.getBalance() >= difference) {
                player.setCurrentBet(newBet);
                player.setBalance(player.getBalance() - difference);
                game.setBet(newBet);
            }
        }

        if (action.getType() == GameAction.Type.ALL_IN) {
            player.setCurrentBet(player.getCurrentBet() + player.getBalance());
            player.setBalance(0);
            player.setAllIn(true);
            if (player.getCurrentBet() > game.getBet()) {
                game.setBet(player.getCurrentBet());
            }
        }

        if (bettingOver(game)) {
            startPlayingPhase(game);
            return game;
        }

        advanceTurn(game);
        return game;
    }

    private boolean bettingOver(GameState game) {
        int highest = getHighestBet(game);
        for (GameState.Player player : game.getPlayers()) {
            if (player.isFolded() || player.isAllIn()) {
                continue;
            }
            if (player.getCurrentBet() < highest) {
                return false;
            }
        }
        return true;
    }

    private int getHighestBet(GameState game) {
        int highest = 0;
        for (GameState.Player player : game.getPlayers()) {
            if (player.getCurrentBet() > highest) {
                highest = player.getCurrentBet();
            }
        }
        return highest;
    }

    private List<GameState.Player> getActivePlayers(GameState game) {
        List<GameState.Player> active = new ArrayList<>();
        for (GameState.Player player : game.getPlayers()) {
            if (!player.isFolded()) {
                active.add(player);
            }
        }
        return active;
    }

    private void advanceTurn(GameState game) {
        List<GameState.Player> players = game.getPlayers();
        int currentIndex = -1;
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getName().equals(game.getCurrentTurn())) {
                currentIndex = i;
                break;
            }
        }
        for (int i = 1; i <= players.size(); i++) {
            int nextIndex = (currentIndex + i) % players.size();
            GameState.Player next = players.get(nextIndex);
            if (!next.isFolded() && !next.isAllIn()) {
                game.setCurrentTurn(next.getName());
                return;
            }
        }
        startPlayingPhase(game);
    }

    private void startPlayingPhase(GameState game) {
        game.setPhase(GameState.Phase.PLAYING);
        for (GameState.Player player : game.getPlayers()) {
            if (!player.isFolded()) {
                game.setCurrentTurn(player.getName());
                return;
            }
        }
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
            player.setAllIn(false);
            player.setCurrentBet(3);
            player.setBalance(player.getBalance() - 3);
        }

        game.setCurrentTurn(game.getPlayers().get(0).getName());
    }
}