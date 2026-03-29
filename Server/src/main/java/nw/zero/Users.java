package nw.zero;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface Users extends JpaRepository<User, Long> {
	Optional<User> findByGoogleId(String googleId);
	Optional<User> findByUsername(String username);
}