package com.vlife.api.service;

import com.vlife.shared.jdbc.dao.UserDao;
import com.vlife.shared.jdbc.entity.User;
import io.micronaut.security.authentication.Authentication;
import io.micronaut.security.token.jwt.generator.JwtTokenGenerator;
import jakarta.inject.Singleton;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Singleton
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserDao userDao;
    private final JwtTokenGenerator jwtTokenGenerator;

    public AuthService(UserDao userDao,
                       JwtTokenGenerator jwtTokenGenerator) {
        this.userDao = userDao;
        this.jwtTokenGenerator = jwtTokenGenerator;
    }

    public Optional<String> login(String email, String password) {
        Optional<User> userOpt = userDao.findByEmail(email);
        log.info("email={}", email);

        if (userOpt.isEmpty()) {
            log.info("user not found");
            return Optional.empty();
        }

        User user = userOpt.get();

        if (user.getStatus() == null || user.getStatus() != 1) {
            log.info("user inactive, userId={}", user.getId());
            return Optional.empty();
        }

        if (user.getPassword() == null || !BCrypt.checkpw(password, user.getPassword())) {
            log.info("password fail, userId={}", user.getId());
            return Optional.empty();
        }

        log.info("login success, userId={}", user.getId());
        return Optional.of(generateTokenForUser(user));
    }

    private String generateTokenForUser(User user) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", user.getEmail());
        attributes.put("name", user.getName());

        Authentication authentication = Authentication.build(
                String.valueOf(user.getId()),
                List.of(),
                attributes
        );

        return jwtTokenGenerator.generateToken(authentication, null)
                .orElseThrow(() -> new IllegalStateException("Cannot generate jwt token"));
    }
}