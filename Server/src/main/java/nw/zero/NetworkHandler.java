package nw.zero;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.Optional;

@Controller
public class NetworkHandler {

    @Autowired
    private SimpMessagingTemplate messaging;

    @Autowired
    private GameManager gameManager;

    @Autowired
    private Users users;

    @GetMapping("/test-user")
    @ResponseBody
    public String testUser() {
        User user = findOrCreateUser("google-123", "testplayer");
        return "Created/found user: " + user.getUsername();
    }

    @MessageMapping("/join")
    public void join(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        System.out.println("JOIN: " + sessionId);

        GameState game = gameManager.enterGame(sessionId);

        System.out.println("Assigned gameId: " + game.getGameId());

        broadcast(game);
    }

    @MessageMapping("/game/{gameId}/action")
    public void action(@DestinationVariable int gameId,
                       GameAction action,
                       SimpMessageHeaderAccessor headerAccessor) {

        String sessionId = headerAccessor.getSessionId();
        GameState game;

        if (action.getType() == GameAction.Type.PLAY_CARD) {
            game = gameManager.handleCardPlay(gameId, sessionId, action);
        } else {
            game = gameManager.handleBet(gameId, sessionId, action);
        }

        if (game != null) {
            broadcast(game);
        }
    }

    public User findOrCreateUser(String githubID, String username) {
        Optional<User> existing = users.findByGithubID(githubID);

        if (existing.isPresent()) {
            return existing.get();
        }

        User user = new User();
        user.setgithubID(githubID);
        user.setUsername(username);
        return users.save(user);
    }

    public boolean usernameExists(String username) {
        return users.findByUsername(username).isPresent();
    }

    private void broadcast(GameState game) {
        for (GameState.Player p : game.getPlayers()) {

            GameState view = game.masked(p.getSessionId());

            view.setYourName(p.getName());

            System.out.println(
                    "SEND: " + p.getName() + " gameId=" + view.getGameId() + " turn=" + view.getCurrentTurn() + " phase=" + view.getPhase()
            );

            messaging.convertAndSendToUser(
                    p.getSessionId(),
                    "/queue/game",
                    view,
                    buildHeaders(p.getSessionId())
            );
        }
    }

    private java.util.Map<String, Object> buildHeaders(String sessionId) {
        SimpMessageHeaderAccessor accessor =
                SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);

        accessor.setSessionId(sessionId);
        accessor.setLeaveMutable(true);

        return accessor.getMessageHeaders();
    }
}