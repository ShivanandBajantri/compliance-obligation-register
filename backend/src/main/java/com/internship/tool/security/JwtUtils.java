package com.internship.tool.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    // Minimum 32 bytes required for HMAC-SHA256; we enforce 32+ chars.
    // The default is intentionally long enough to be safe for dev/test.
    // In production, override with JWT_SECRET env var (64+ random chars).
    private static final String DEFAULT_SECRET =
            "compliance-register-dev-secret-key-32chars!!";
    private static final long EXPIRATION_MILLIS = 86_400_000L; // 24 hours

    private final Key key;

    public JwtUtils() {
        String secret = System.getenv("JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            logger.warn("JWT_SECRET env var not set — using default dev secret. " +
                        "Set a strong secret before deploying to production.");
            secret = DEFAULT_SECRET;
        } else if (secret.length() < 32) {
            logger.warn("JWT_SECRET is shorter than 32 characters — using default dev secret.");
            secret = DEFAULT_SECRET;
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(UserDetails userDetails) {
        String roles = userDetails.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.joining(","));

        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(userDetails.getUsername())
                .claim("roles", roles)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(EXPIRATION_MILLIS)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Returns true only if the token is structurally valid, correctly signed,
     * and not expired. Logs the specific failure reason at DEBUG level.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            logger.debug("JWT token expired: {}", e.getMessage());
        } catch (SignatureException e) {
            logger.debug("JWT signature invalid: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.debug("JWT token malformed: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.debug("JWT token unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.debug("JWT token empty or null: {}", e.getMessage());
        }
        return false;
    }

    public String getUsernameFromToken(String token) {
        return parseClaims(token).getSubject();
    }

    public List<String> getRolesFromToken(String token) {
        String roles = parseClaims(token).get("roles", String.class);
        if (roles == null || roles.isBlank()) {
            return List.of();
        }
        return List.of(roles.split(","));
    }

    private Claims parseClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
