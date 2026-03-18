package nw.zero;

import java.util.ArrayList;
import java.util.List;

public class GameState {

    public enum Phase {
        WAITING,
        BETTING,
        PLAYING
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
            for (int i = 0; i <= 3; i++) {
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

    public int getTotalPlayedValue() {
        int total = 0;
        for (Player player : players) {
            total += player.getPlayedValue();
        }
        return total;
    }

    public Player getPlayer(String name) {
        for (Player player : players) {
            if (player.getName().equals(name)) {
                return player;
            }
        }
        return null;
    }

    public Player getPlayerBySessionId(String sessionId) {
        for (Player player : players) {
            if (player.getSessionId().equals(sessionId)) {
                return player;
            }
        }
        return null;
    }

    public Player getCurrentPlayer() {
        return getPlayer(currentTurn);
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

    public void setYourName(String yourName)     { this.yourName = yourName; }
    public void setCurrentTurn(String t)         { this.currentTurn = t; }
    public void setPhase(Phase phase)            { this.phase = phase; }
    public void setRound(int round)              { this.round = round; }
    public void setBet(int bet)                  { this.bet = bet; }
    public void setPlayers(List<Player> players) { this.players = players; }
}