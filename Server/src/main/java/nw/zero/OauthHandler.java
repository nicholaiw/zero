package nw.zero;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String githubID = oauthUser.getAttribute("id").toString();

        User dbUser = networkHandler.findPendingUser(githubID);
        request.getSession().setAttribute("user", dbUser);

        response.sendRedirect("/");
    }
}