package nw.zero;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import jakarta.servlet.http.*;
import java.io.IOException;

public class OauthHandler implements AuthenticationSuccessHandler {

    private final NetworkHandler networkHandler;

    public OauthHandler(NetworkHandler networkHandler) {
        this.networkHandler = networkHandler;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User user = (OAuth2User) authentication.getPrincipal();

        String githubId = user.getAttribute("id").toString();
        String username = user.getAttribute("login");

        User dbUser = networkHandler.findOrCreateUser(githubId, username);

        request.getSession().setAttribute("user", dbUser);

        response.sendRedirect("/");
    }
}