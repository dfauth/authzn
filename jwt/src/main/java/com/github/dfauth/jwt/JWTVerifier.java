package com.github.dfauth.jwt;

import com.github.dfauth.authzn.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.PublicKey;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class JWTVerifier {

    private static final Logger logger = LoggerFactory.getLogger(JWTVerifier.class);
    private static final Function<Map<String, Object>, RoleBuilder> RBM = t -> new RoleBuilder().withSystemId((String) (t.get("systemId"))).withRoleName((String) (t.get("rolename")));


    public Function<Jws<Claims>, User> asUser = claims -> {
        Set<RoleBuilder> roles = ((List<Map<String, Object>>) Optional.ofNullable(claims.getBody().get("roles", List.class)).orElse(Collections.emptyList())).stream().map(RBM).collect(Collectors.toSet());
        String companyId = claims.getBody().get("companyId", String.class);
        String userId = claims.getBody().getSubject();
        Date expiry = claims.getBody().getExpiration();
        return new UserBuilder().withUserId(userId).withCompanyId(companyId).withExpiry(expiry.toInstant()).withRoles(roles).build();
    };

    private final PublicKey publicKey;
    private final String issuer;

    public JWTVerifier(PublicKey publicKey, String issuer) {
        this.publicKey = publicKey;
        this.issuer = issuer;
    }

    public <T> Try<T> authenticateToken(String token, Function<Jws<Claims>, T> f) {
        try {
            Jws<Claims> claims = Jwts.parser()
                    .setSigningKey(publicKey)
                    .requireIssuer(issuer)
                    .parseClaimsJws(token);
            return Try.success(f.apply(claims));
        } catch (RuntimeException e) {
            logger.error(e.getMessage(), e);
            return Try.failure(e);
        }
    }

    public <T> TokenAuthenticator<T> tokenAuthenticator(Function<Jws<Claims>, T> f) {
        return (String t) -> authenticateToken(t, f);
    }

    public interface TokenAuthenticator<T> extends Function<String, Try<T>> {

        @Override
        default Try<T> apply(String t) {
            return parseToken(t);
        }

        Try<T> parseToken(String t);
    }
}

