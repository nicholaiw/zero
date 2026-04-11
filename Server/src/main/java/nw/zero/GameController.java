package nw.zero;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.security.Principal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Controller
public class GameController {

    private final GameManager gameManager;
    private final SimpMessagingTemplate messaging;
    private final Users users;
    private final Map<String, String> sessionUsernames = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final Map<Integer, ScheduledFuture<?>> timers = new ConcurrentHashMap<>();

    public GameController(GameManager gameManager, SimpMessagingTemplate messaging, Users users) {
        this.gameManager = gameManager;
        this.messaging = messaging;
        this.users = users;
    }

    public void registerSession(String sessionId, String username) {
        sessionUsernames.put(sessionId, username);
    }

    @MessageMapping("/join")
    public void join(Principal principal) {
        String sessionId = principal.getName();
        String username = sessionUsernames.getOrDefault(sessionId, "Unknown");
        int balance = users.findByUsername(username).map(User::getBalance).orElse(100);
        GameState game = gameManager.enterGame(sessionId, username, balance);
        broadcastState(game);
        startTimer(game);
    }

    @MessageMapping("/game/{gameId}/action")
    public void action(@DestinationVariable int gameId, Principal principal, GameAction action) {
        String sessionId = principal.getName();
        cancelTimer(gameId);
        GameState game;
        if (action.getType() == GameAction.Type.PLAY_CARD) {
            game = gameManager.handleCardPlay(gameId, sessionId, action);
        } else {
            game = gameManager.handleBet(gameId, sessionId, action);
        }
        if (game != null) {
            if (game.getPhase() == GameState.Phase.FINISHED) {
                persistBalances(game);
            }
            broadcastState(game);
            startTimer(game);
        }
    }

    private void startTimer(GameState game) {
        if (game.getPhase() == GameState.Phase.FINISHED || game.getPhase() == GameState.Phase.WAITING) return;

        int gameId = game.getGameId();
        String currentTurn = game.getCurrentTurn();

        GameState.Player currentPlayer = null;
        for (GameState.Player p : game.getPlayers()) {
            if (p.getName().equals(currentTurn)) { currentPlayer = p; break; }
        }
        if (currentPlayer == null) return;

        String sessionId = currentPlayer.getSessionId();
        long deadline = System.currentTimeMillis() + 30_000;
        game.setTurnDeadline(deadline);

        ScheduledFuture<?> timer = scheduler.schedule(() -> {
            GameState result = gameManager.handleTimeout(gameId, sessionId);
            if (result != null) {
                if (result.getPhase() == GameState.Phase.FINISHED) persistBalances(result);
                broadcastState(result);
                startTimer(result);
            }
        }, 5, TimeUnit.SECONDS);

        timers.put(gameId, timer);
    }

    private void cancelTimer(int gameId) {
        ScheduledFuture<?> timer = timers.remove(gameId);
        if (timer != null) timer.cancel(false);
    }

    private void persistBalances(GameState game) {
        for (GameState.Player player : game.getPlayers()) {
            users.findByUsername(player.getName()).ifPresent(user -> {
                user.setBalance(player.getBalance());
                users.save(user);
            });
        }
    }

    private void broadcastState(GameState game) {
        for (GameState.Player player : game.getPlayers()) {
            GameState masked = game.masked(player.getSessionId());
            masked.setYourName(player.getName());
            messaging.convertAndSendToUser(player.getSessionId(), "/queue/game", masked);
        }
    }
}