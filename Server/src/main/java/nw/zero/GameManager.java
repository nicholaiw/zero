package nw.zero;

import org.springframework.stereotype.Service;
import java.util.*;

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
        if (!isValidTurn(game, player)) return game;

        int highest = getHighestBet(game);

        if (action.getType() == GameAction.Type.FOLD) {
            player.setFolded(true);
            player.setHasActed(true);
        }

        if (action.getType() == GameAction.Type.CALL) {
            int difference = highest - player.getCurrentBet();

            if (player.getBalance() >= difference) {
                player.setBalance(player.getBalance() - difference);
                player.setCurrentBet(highest);
                player.setHasActed(true);
            }
        }

        if (action.getType() == GameAction.Type.RAISE) {
            int newBet = highest + 3;
            int difference = newBet - player.getCurrentBet();

            if (player.getBalance() >= difference) {
                player.setBalance(player.getBalance() - difference);
                player.setCurrentBet(newBet);
                game.setBet(newBet);
                player.setHasActed(true);

                for (GameState.Player p : game.getPlayers()) {
                    if (p != player && !p.isFolded() && !p.isAllIn()) {
                        p.setHasActed(false);
                    }
                }
            }
        }

        if (action.getType() == GameAction.Type.ALL_IN) {
            int newBet = player.getCurrentBet() + player.getBalance();

            player.setCurrentBet(newBet);
            player.setBalance(0);
            player.setAllIn(true);
            player.setHasActed(true);

            if (newBet > game.getBet()) {
                game.setBet(newBet);
            }
        }

        if (bettingOver(game)) {
            startPlayingPhase(game);
            return game;
        }

        advanceTurn(game);
        return game;
    }

    public GameState handleCardPlay(int gameId, String sessionId, GameAction action) {
        GameState game = games.get(gameId);
        if (game == null) return null;
        if (game.getPhase() != GameState.Phase.PLAYING) return game;

        GameState.Player player = game.getPlayerBySessionId(sessionId);
        if (!isValidTurn(game, player)) return game;

        int index = action.getCardIndex();
        if (index < 0 || index >= player.getCards().size()) return game;

        GameState.Card card = player.getCards().get(index);
        if (card.isPlayed()) return game;

        card.setPlayed(true);

        if (game.getTotalPlayedValue() > 9) {
            endRound(game, player);
            return game;
        }

        advancePlayTurn(game);
        return game;
    }

    private boolean isValidTurn(GameState game, GameState.Player player) {
        if (player == null) return false;
        if (!game.getCurrentTurn().equals(player.getName())) return false;
        return true;
    }

    private void endRound(GameState game, GameState.Player loser) {
        int pot = 0;

        for (GameState.Player p : game.getPlayers()) {
            pot += p.getCurrentBet();
        }

        List<GameState.Player> winners = new ArrayList<>();

        for (GameState.Player p : game.getPlayers()) {
            if (!p.isFolded() && p != loser) {
                winners.add(p);
            }
        }

        if (winners.isEmpty()) {
            loser.setBalance(loser.getBalance() + pot);
        } else {
            int share = pot / winners.size();

            for (GameState.Player p : winners) {
                p.setBalance(p.getBalance() + share);
            }
        }

        if (game.isGameOver()) {
            game.setPhase(GameState.Phase.WAITING);
            return;
        }

        game.setRound(game.getRound() + 1);
        setupRound(game);
    }

    private void setupRound(GameState game) {
        game.setBet(3);

        int startIndex = (game.getRound() - 1) % game.getPlayers().size();

        for (GameState.Player p : game.getPlayers()) {
            p.setFolded(false);
            p.setAllIn(false);
            p.setHasActed(false);
            p.setCurrentBet(3);
            p.setBalance(p.getBalance() - 3);

            List<GameState.Card> cards = new ArrayList<>();

            for (int i = 0; i < 4; i++) {
                cards.add(new GameState.Card(i));
            }

            p.setCards(cards);
        }

        game.setPhase(GameState.Phase.BETTING);
        game.setCurrentTurn(game.getPlayers().get(startIndex).getName());
    }

    private boolean bettingOver(GameState game) {
        int highest = getHighestBet(game);

        for (GameState.Player p : game.getPlayers()) {
            if (p.isFolded() || p.isAllIn()) continue;
            if (!p.getHasActed()) return false;
            if (p.getCurrentBet() < highest) return false;
        }

        return true;
    }

    private int getHighestBet(GameState game) {
        int highest = 0;

        for (GameState.Player p : game.getPlayers()) {
            if (p.getCurrentBet() > highest) {
                highest = p.getCurrentBet();
            }
        }

        return highest;
    }

    private void advanceTurn(GameState game) {
        List<GameState.Player> players = game.getPlayers();

        int index = -1;

        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getName().equals(game.getCurrentTurn())) {
                index = i;
                break;
            }
        }

        for (int i = 1; i <= players.size(); i++) {
            GameState.Player next = players.get((index + i) % players.size());

            if (!next.isFolded() && !next.isAllIn()) {
                game.setCurrentTurn(next.getName());
                return;
            }
        }

        startPlayingPhase(game);
    }

    private void advancePlayTurn(GameState game) {
        List<GameState.Player> players = game.getPlayers();

        int index = -1;

        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getName().equals(game.getCurrentTurn())) {
                index = i;
                break;
            }
        }

        for (int i = 1; i <= players.size(); i++) {
            GameState.Player next = players.get((index + i) % players.size());

            if (!next.isFolded()) {
                game.setCurrentTurn(next.getName());
                return;
            }
        }
    }

    private void startPlayingPhase(GameState game) {
        game.setPhase(GameState.Phase.PLAYING);

        int startIndex = (game.getRound() - 1) % game.getPlayers().size();

        for (int i = 0; i < game.getPlayers().size(); i++) {
            GameState.Player p = game.getPlayers().get((startIndex + i) % game.getPlayers().size());

            if (!p.isFolded()) {
                game.setCurrentTurn(p.getName());
                return;
            }
        }
    }

    private GameState findOpenGame() {
        for (GameState g : games.values()) {
            if (g.getPhase() == GameState.Phase.WAITING && g.getPlayers().size() < 4) {
                return g;
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
        game.setRound(1);
        setupRound(game);
    }
}