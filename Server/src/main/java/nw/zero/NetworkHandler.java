package nw.zero;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class NetworkHandler {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public User findPendingUser(String githubID) {
        User user = entityManager
                .createQuery("SELECT u FROM User u WHERE u.githubID = :gid", User.class)
                .setParameter("gid", githubID)
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (user != null) return user;

        user = new User();
        user.setGithubID(githubID);
        user.setUsername(null);
        entityManager.persist(user);
        return user;
    }
}