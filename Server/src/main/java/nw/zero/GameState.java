package nw.zero;

import java.util.ArrayList;
import java.util.List;

public class GameState {

    public enum Phase {
        WAITING,
        BETTING,
        PLAYING,
        FINISHED
    }

    public static class Card {
        private int value;
        private boolean played;

        public Card(int value) {
            this.value = value;
            this.played = false;
        }

        public int getValue()     { return value; }
        public boolean isPlayed() { return played; }
        public void setPlayed(boolean played) { this.played = played; }
    }

    public static class Player {
        private String name;
        private String sessionId;
        private List<Card> cards;
        private boolean folded;
        private boolean allIn;
        private boolean hasActed;
        private int balance;
        private int currentBet;

        public Player(String name, String sessionId) {
            this.name = name;
            this.sessionId = sessionId;
            this.folded = false;
            this.allIn = false;
            this.hasActed = false;
            this.balance = 100;
            this.currentBet = 0;
            this.cards = new ArrayList<>();

            for (int i = 0; i < 4; i++) {
                this.cards.add(new Card(i));
            }
        }

        public String getName()        { return name; }
        public String getSessionId()   { return sessionId; }
        public List<Card> getCards()   { return cards; }
        public boolean isFolded()      { return folded; }
        public boolean isAllIn()       { return allIn; }
        public boolean getHasActed()   { return hasActed; }
        public int getBalance()        { return balance; }
        public int getCurrentBet()     { return currentBet; }

        public void setFolded(boolean folded)     { this.folded = folded; }
        public void setAllIn(boolean allIn)       { this.allIn = allIn; }
        public void setHasActed(boolean hasActed) { this.hasActed = hasActed; }
        public void setBalance(int balance)       { this.balance = balance; }
        public void setCurrentBet(int currentBet) { this.currentBet = currentBet; }
        public void setCards(List<Card> cards)    { this.cards = cards; }

        public int getPlayedValue() {
            int total = 0;
            for (Card card : cards) {
                if (card.isPlayed()) {
                    total += card.getValue();
                }
            }
            return total;
        }

        public boolean hasUnplayedCards() {
            for (Card card : cards) {
                if (!card.isPlayed()) return true;
            }
            return false;
        }
    }

    private int gameId;
    private String yourName;
    private String currentTurn;
    private Phase phase;
    private int round;
    private final int maxRounds = 4;
    private int bet;
    private List<Player> players;

    public GameState(int gameId) {
        this.gameId = gameId;
        this.phase = Phase.WAITING;
        this.round = 1;
        this.bet = 0;
        this.players = new ArrayList<>();
    }

    public void setGameId(int gameId) {
        this.gameId = gameId;
    }

    public GameState masked(String viewerSessionId) {
        GameState view = new GameState(this.gameId);

        view.setGameId(this.gameId);

        view.yourName = this.yourName;
        view.currentTurn = this.currentTurn;
        view.phase = this.phase;
        view.round = this.round;
        view.bet = this.bet;

        List<Player> maskedPlayers = new ArrayList<>();

        for (Player p : this.players) {
            Player mp = new Player(p.getName(), p.getSessionId());
            mp.setFolded(p.isFolded());
            mp.setAllIn(p.isAllIn());
            mp.setHasActed(p.getHasActed());
            mp.setBalance(p.getBalance());
            mp.setCurrentBet(p.getCurrentBet());

            List<Card> maskedCards = new ArrayList<>();

            for (Card c : p.getCards()) {
                boolean isOwner = p.getSessionId().equals(viewerSessionId);
                boolean reveal = isOwner && this.phase != Phase.WAITING;

                int displayValue = (reveal || c.isPlayed()) ? c.getValue() : -1;

                Card mc = new Card(displayValue);
                mc.setPlayed(c.isPlayed());
                maskedCards.add(mc);
            }

            mp.setCards(maskedCards);
            maskedPlayers.add(mp);
        }

        view.setPlayers(maskedPlayers);
        return view;
    }

    public int getTotalPlayedValue() {
        int total = 0;
        for (Player player : players) {
            total += player.getPlayedValue();
        }
        return total;
    }

    public Player getPlayerBySessionId(String sessionId) {
        for (Player player : players) {
            if (player.getSessionId().equals(sessionId)) {
                return player;
            }
        }
        return null;
    }

    public boolean isGameOver() {
        return round >= maxRounds;
    }

    public int getGameId()           { return gameId; }
    public String getYourName()      { return yourName; }
    public String getCurrentTurn()   { return currentTurn; }
    public Phase getPhase()          { return phase; }
    public int getRound()            { return round; }
    public int getMaxRounds()        { return maxRounds; }
    public int getBet()              { return bet; }
    public List<Player> getPlayers() { return players; }
    private long turnDeadline;
    public long getTurnDeadline() { return turnDeadline; }
    public void setTurnDeadline(long turnDeadline) { this.turnDeadline = turnDeadline; }

    public void setYourName(String yourName)     { this.yourName = yourName; }
    public void setCurrentTurn(String t)         { this.currentTurn = t; }
    public void setPhase(Phase phase)            { this.phase = phase; }
    public void setRound(int round)              { this.round = round; }
    public void setBet(int bet)                  { this.bet = bet; }
    public void setPlayers(List<Player> players) { this.players = players; }
}