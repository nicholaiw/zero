package nw.zero;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.util.Map;

@Controller
public class GameController {

    private final GameManager gameManager;
    private final SimpMessagingTemplate messaging;
    private final Map<String, String> sessionUsernames = new java.util.concurrent.ConcurrentHashMap<>();

    public GameController(GameManager gameManager, SimpMessagingTemplate messaging) {
        this.gameManager = gameManager;
        this.messaging = messaging;
    }

    public void registerSession(String sessionId, String username) {
        sessionUsernames.put(sessionId, username);
    }

    @MessageMapping("/join")
    public void join(Principal principal) {
        String sessionId = principal.getName();
        String username = sessionUsernames.getOrDefault(sessionId, "Unknown");
        GameState game = gameManager.enterGame(sessionId, username);
        broadcastState(game);
    }

    @MessageMapping("/game/{gameId}/action")
    public void action(@DestinationVariable int gameId, Principal principal, GameAction action) {
        String sessionId = principal.getName();
        GameState game;
        if (action.getType() == GameAction.Type.PLAY_CARD) {
            game = gameManager.handleCardPlay(gameId, sessionId, action);
        } else {
            game = gameManager.handleBet(gameId, sessionId, action);
        }
        if (game != null) broadcastState(game);
    }

    private void broadcastState(GameState game) {
        for (GameState.Player player : game.getPlayers()) {
            GameState masked = game.masked(player.getSessionId());
            masked.setYourName(player.getName());
            messaging.convertAndSendToUser(player.getSessionId(), "/queue/game", masked);
        }
    }
}