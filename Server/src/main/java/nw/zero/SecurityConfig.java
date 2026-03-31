package nw.zero;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final NetworkHandler networkHandler;

    public SecurityConfig(NetworkHandler networkHandler) {
        this.networkHandler = networkHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf().disable()
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/me", "/set-username", "/login**",
                                "/*.html", "/*.js", "/*.css").permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(new OauthHandler(networkHandler))
                );

        return http.build();
    }
}