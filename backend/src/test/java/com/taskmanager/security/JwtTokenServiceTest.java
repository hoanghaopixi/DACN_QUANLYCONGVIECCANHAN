package com.taskmanager.security;

import com.taskmanager.entity.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenServiceTest {

    private static final String SECRET = "test-secret-with-at-least-32-bytes-long";

    @Test
    void issuesAndReadsSignedUserId() {
        JwtTokenService service = new JwtTokenService(SECRET, 60_000L);
        User user = User.builder().id(42L).email("user@example.com").build();

        String token = service.issueToken(user);

        assertEquals(42L, service.readUserId(token));
    }

    @Test
    void rejectsTokenSignedByAnotherKey() {
        JwtTokenService service = new JwtTokenService(SECRET, 60_000L);
        SecretKey otherKey = Keys.hmacShaKeyFor(
                "another-secret-with-at-least-32-bytes".getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("42")
                .signWith(otherKey)
                .compact();

        assertThrows(JwtException.class, () -> service.readUserId(token));
    }

    @Test
    void rejectsExpiredToken() {
        JwtTokenService service = new JwtTokenService(SECRET, 60_000L);
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .subject("42")
                .expiration(Date.from(Instant.now().minusSeconds(1)))
                .signWith(key)
                .compact();

        assertThrows(JwtException.class, () -> service.readUserId(token));
    }

    @Test
    void rejectsWeakSigningSecret() {
        assertThrows(IllegalStateException.class, () -> new JwtTokenService("too-short", 60_000L));
    }
}
