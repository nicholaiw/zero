package nw.zero;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface Users extends JpaRepository<User, Long> {
	Optional<User> findByGithubID(String githubID);
	Optional<User> findByUsername(String username);
}
