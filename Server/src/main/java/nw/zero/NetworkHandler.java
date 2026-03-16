package nw.zero;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
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
    public GameState join() {
        System.out.println("join received");
        GameState game = gameManager.enterGame("Player");
        System.out.println("game id: " + game.getGameId());
        return game;
    }
}