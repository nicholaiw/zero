package nw.zero;

import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class GameManager {

    private static final int MAX_PLAYERS = 4;

    private final Map<Integer, GameState> games = new ConcurrentHashMap<>();
    private final AtomicInteger nextGameId = new AtomicInteger(0);

    public synchronized GameState enterGame(String sessionId, String username, int balance) {
        GameState game = findOpenGame();
        if (game == null) game = createGame();

        GameState.Player player = new GameState.Player(username, sessionId);
        player.setBalance(balance);
        game.getPlayers().add(player);

        if (game.getPlayers().size() == MAX_PLAYERS) startGame(game);

        return game;
    }

    /** Lets the controller check whose turn it is without touching game state,
     *  so it can avoid resetting the turn timer on out-of-turn/garbage actions. */
    public boolean isCurrentTurn(int gameId, String sessionId) {
        GameState game = games.get(gameId);
        if (game == null || game.getCurrentTurn() == null) return false;

        GameState.Player player = game.getPlayerBySessionId(sessionId);
        return player != null && game.getCurrentTurn().equals(player.getName());
    }

    public GameState handleTimeout(int gameId, String sessionId) {
        GameState game = games.get(gameId);
        if (game == null) return null;

        GameState.Player player = game.getPlayerBySessionId(sessionId);
        if (player == null) return null;
        if (!game.getCurrentTurn().equals(player.getName())) return null;

        if (game.getPhase() == GameState.Phase.BETTING) {
            GameAction action = new GameAction();
            action.setType(GameAction.Type.FOLD);
            return handleBet(gameId, sessionId, action);
        }

        if (game.getPhase() == GameState.Phase.PLAYING) {
            for (int i = 0; i < player.getCards().size(); i++) {
                if (!player.getCards().get(i).isPlayed()) {
                    GameAction action = new GameAction();
                    action.setType(GameAction.Type.PLAY_CARD);
                    action.setCardIndex(i);
                    return handleCardPlay(gameId, sessionId, action);
                }
            }
        }

        return null;
    }

    public GameState handleBet(int gameId, String sessionId, GameAction action) {
        GameState game = games.get(gameId);
        if (game == null) return null;
        if (game.getPhase() != GameState.Phase.BETTING) return game;

        GameState.Player player = game.getPlayerBySessionId(sessionId);
        if (!isValidTurn(game, player)) return game;

        if (action.getType() == GameAction.Type.FOLD) {
            player.setFolded(true);
            player.setHasActed(true);
            if (countActivePlayers(game) == 1) {
                endRound(game, null);
                return game;
            }
        }

        if (action.getType() == GameAction.Type.CALL) {
            int highest = getHighestBet(game);
            int difference = highest - player.getCurrentBet();
            if (player.getBalance() >= difference) {
                player.setBalance(player.getBalance() - difference);
                player.setCurrentBet(highest);
                player.setHasActed(true);
            }
        }

        if (action.getType() == GameAction.Type.RAISE) {
            int highest = getHighestBet(game);
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
            if (newBet > game.getBet()) game.setBet(newBet);
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

        if (countActivePlayers(game) == 1) {
            endRound(game, null);
            return game;
        }

        if (allCardsPlayed(game)) {
            endRound(game, null);
            return game;
        }

        advancePlayTurn(game);
        return game;
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

        if (winners.isEmpty() && loser != null) {
            loser.setBalance(loser.getBalance() + pot);
        } else if (!winners.isEmpty()) {
            // Floor-divide, then hand out the remainder one chip at a time
            // instead of rounding every winner up (which was minting chips).
            int share = pot / winners.size();
            int remainder = pot % winners.size();
            for (int i = 0; i < winners.size(); i++) {
                int amount = share + (i < remainder ? 1 : 0);
                GameState.Player w = winners.get(i);
                w.setBalance(w.getBalance() + amount);
            }
        }

        game.setRound(game.getRound() + 1);

        if (game.isGameOver()) {
            game.setPhase(GameState.Phase.FINISHED);
            games.remove(game.getGameId());
            return;
        }

        setupRound(game);
    }

    private void setupRound(GameState game) {
        game.setBet(3);
        int startIndex = (game.getRound() - 1) % game.getPlayers().size();

        List<GameState.Card> deck = new ArrayList<>();
        for (int value = 0; value <= 3; value++) {
            for (int i = 0; i < 10; i++) {
                deck.add(new GameState.Card(value));
            }
        }
        Collections.shuffle(deck);

        int cardIndex = 0;
        for (GameState.Player p : game.getPlayers()) {
            p.setFolded(false);
            p.setAllIn(false);
            p.setHasActed(false);
            p.setCurrentBet(3);
            p.setBalance(p.getBalance() - 3);

            List<GameState.Card> cards = new ArrayList<>();
            for (int i = 0; i < 4; i++) {
                cards.add(deck.get(cardIndex++));
            }
            p.setCards(cards);
        }

        game.setPhase(GameState.Phase.BETTING);
        game.setCurrentTurn(game.getPlayers().get(startIndex).getName());
    }

    private void startPlayingPhase(GameState game) {
        game.setPhase(GameState.Phase.PLAYING);

        if (countActivePlayers(game) == 1) {
            endRound(game, null);
            return;
        }

        int startIndex = (game.getRound() - 1) % game.getPlayers().size();
        for (int i = 0; i < game.getPlayers().size(); i++) {
            GameState.Player p = game.getPlayers().get((startIndex + i) % game.getPlayers().size());
            if (!p.isFolded()) {
                game.setCurrentTurn(p.getName());
                return;
            }
        }
    }

    private void advanceTurn(GameState game) {
        List<GameState.Player> players = game.getPlayers();
        int index = getCurrentPlayerIndex(game, players);

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
        int index = getCurrentPlayerIndex(game, players);

        for (int i = 1; i <= players.size(); i++) {
            GameState.Player next = players.get((index + i) % players.size());
            if (!next.isFolded()) {
                game.setCurrentTurn(next.getName());
                return;
            }
        }
    }

    private int getCurrentPlayerIndex(GameState game, List<GameState.Player> players) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getName().equals(game.getCurrentTurn())) return i;
        }
        return -1;
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

    private boolean allCardsPlayed(GameState game) {
        for (GameState.Player p : game.getPlayers()) {
            if (!p.isFolded() && p.hasUnplayedCards()) return false;
        }
        return true;
    }

    private boolean isValidTurn(GameState game, GameState.Player player) {
        if (player == null) return false;
        return game.getCurrentTurn().equals(player.getName());
    }

    private int countActivePlayers(GameState game) {
        int count = 0;
        for (GameState.Player p : game.getPlayers()) {
            if (!p.isFolded()) count++;
        }
        return count;
    }

    private int getHighestBet(GameState game) {
        int highest = 0;
        for (GameState.Player p : game.getPlayers()) {
            if (p.getCurrentBet() > highest) highest = p.getCurrentBet();
        }
        return highest;
    }

    private GameState findOpenGame() {
        for (GameState g : games.values()) {
            if (g.getPhase() == GameState.Phase.WAITING && g.getPlayers().size() < MAX_PLAYERS) return g;
        }
        return null;
    }

    private GameState createGame() {
        int id = nextGameId.getAndIncrement();
        GameState game = new GameState(id);
        games.put(id, game);
        return game;
    }

    private void startGame(GameState game) {
        game.setRound(1);
        setupRound(game);
    }
}