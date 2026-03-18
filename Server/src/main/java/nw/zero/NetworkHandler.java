package nw.zero;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
public class NetworkHandler {

    @Autowired
    private SimpMessagingTemplate messaging;

    @Autowired
    private GameManager gameManager;

    @MessageMapping("/join")
    @SendTo("/type/game/lobby")
    public GameState join(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        System.out.println("join received, session: " + sessionId);
        GameState game = gameManager.enterGame(sessionId);
        System.out.println("game id: " + game.getGameId());
        game.setYourName(game.getPlayerBySessionId(sessionId).getName());
        return game;
    }

    @MessageMapping("/game/{gameId}/action")
    public void action(@DestinationVariable int gameId, GameAction action, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        GameState game;

        if (action.getType() == GameAction.Type.PLAY_CARD) {
            game = gameManager.handleCardPlay(gameId, sessionId, action);
        } else {
            game = gameManager.handleBet(gameId, sessionId, action);
        }

        if (game != null) {
            messaging.convertAndSend("/type/game/" + gameId, game);
        }
    }
}