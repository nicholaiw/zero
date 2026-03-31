package nw.zero;

import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.http.ResponseEntity;

@RestController
public class UserController {

    private final Users users;

    public UserController(Users users) {
        this.users = users;
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(HttpSession session) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body("Not logged in");

        return ResponseEntity.ok(Map.of(
                "loggedIn", true,
                "needsUsername", user.getUsername() == null
        ));
    }

    @PostMapping("/set-username")
    public ResponseEntity<?> setUsername(HttpSession session, @RequestBody Map<String, String> body) {
        User user = (User) session.getAttribute("user");
        if (user == null) return ResponseEntity.status(401).body("Not logged in");

        String newUsername = body.get("username").trim();
        if (newUsername.isEmpty()) return ResponseEntity.badRequest().body("Username cannot be empty");

        if (users.findByUsername(newUsername).isPresent()) {
            return ResponseEntity.status(409).body("Username taken");
        }

        user.setUsername(newUsername);
        users.save(user);
        session.setAttribute("user", user);
        return ResponseEntity.ok("ok");
    }
}