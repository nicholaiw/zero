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
        private List<Card> cards;
        private boolean folded;
        private int balance;
        private int currentBet;

        public Player(String name) {
            this.name = name;
            this.folded = false;
            this.balance = 30;
            this.currentBet = 0;
            this.cards = new ArrayList<>();
            for (int i = 0; i <= 3; i++) {
                this.cards.add(new Card(i));
            }
        }

        public String getName()      { return name; }
        public List<Card> getCards() { return cards; }
        public boolean isFolded()    { return folded; }
        public int getBalance()      { return balance; }
        public int getCurrentBet()   { return currentBet; }

        public void setFolded(boolean folded)     { this.folded = folded; }
        public void setBalance(int balance)       { this.balance = balance; }
        public void setCurrentBet(int currentBet) { this.currentBet = currentBet; }

        public int getPlayedValue() {
            return cards.stream()
                    .filter(Card::isPlayed)
                    .mapToInt(Card::getValue)
                    .sum();
        }
    }

    private int gameId;
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
        return players.stream()
                .mapToInt(Player::getPlayedValue)
                .sum();
    }

    public Player getPlayer(String name) {
        return players.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst()
                .orElse(null);
    }

    public Player getCurrentPlayer() {
        return getPlayer(currentTurn);
    }

    public boolean isGameOver() {
        return round > maxRounds;
    }

    public int getGameId()           { return gameId; }
    public String getCurrentTurn()   { return currentTurn; }
    public Phase getPhase()          { return phase; }
    public int getRound()            { return round; }
    public int getMaxRounds()        { return maxRounds; }
    public int getBet()              { return bet; }
    public List<Player> getPlayers() { return players; }

    public void setCurrentTurn(String t)         { this.currentTurn = t; }
    public void setPhase(Phase phase)            { this.phase = phase; }
    public void setRound(int round)              { this.round = round; }
    public void setBet(int bet)                  { this.bet = bet; }
    public void setPlayers(List<Player> players) { this.players = players; }
}