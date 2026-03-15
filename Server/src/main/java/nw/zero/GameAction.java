package nw.zero;

public class GameAction {

    public enum Type {
        RAISE,
        CALL,
        FOLD,
        PLAY_CARD
    }

    private Type type;
    private int cardIndex;

    public Type getType()      { return type; }
    public int getCardIndex()  { return cardIndex; }

    public void setType(Type type)          { this.type = type; }
    public void setCardIndex(int cardIndex) { this.cardIndex = cardIndex; }
}