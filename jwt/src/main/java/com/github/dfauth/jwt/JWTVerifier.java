package com.github.dfauth.jwt;

import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.github.dfauth.authzn.User;
import io.jsonwebtoken.*;
import io.vavr.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Key;
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

    private final String issuer;
    private SigningKeyResolver keyResolver;

    public JWTVerifier(PublicKey publicKey, String issuer) {
        this.keyResolver = new SigningKeyResolver() {
            @Override
            public Key resolveSigningKey(JwsHeader jwsHeader, Claims claims) {
                return publicKey;
            }

            @Override
            public Key resolveSigningKey(JwsHeader jwsHeader, String s) {
                return publicKey;
            }
        };
        this.issuer = issuer;
    }

    private static ClaimsSigningKeyResolver wrap(JwkProvider jwkProvider) {
        return (h, c) -> {
            try {
                return jwkProvider.get((String)c.get("kid")).getPublicKey();
            } catch (JwkException e) {
                logger.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
        };
    }

    public JWTVerifier(JwkProvider jwkProvider, String issuer) {
        this.keyResolver = wrap(jwkProvider);
        this.issuer = issuer;
    }

    public JWTVerifier(SigningKeyResolver resolver, String issuer) {
        this.keyResolver = resolver;
        this.issuer = issuer;
    }

    public Try<User> authenticateToken(String token) {
        return authenticateToken(token, asUser);
    }

    public <T> Try<T> authenticateToken(String token, Function<Jws<Claims>, T> f) {
        try {
            Jws<Claims> claims = Jwts.parser()
                    .setSigningKeyResolver(keyResolver)
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

    public interface ClaimsSigningKeyResolver<T extends JwsHeader<T>> extends SigningKeyResolver {
        @Override
        default Key resolveSigningKey(JwsHeader header, Claims claims) {
            return resolveSigningKeyT(header, claims);
        }

        Key resolveSigningKeyT(JwsHeader<T> header, Claims claims);

        @Override
        default Key resolveSigningKey(JwsHeader header, String plaintext) {
            throw new UnsupportedOperationException("key resolution based on plaintext not supported, use "+PlaintextSigningKeyResolver.class.getCanonicalName());
        }
    }

    public interface PlaintextSigningKeyResolver extends SigningKeyResolver {
        @Override
        default Key resolveSigningKey(JwsHeader header, Claims claims) {
            throw new UnsupportedOperationException("key resolution based on claims not supported, use "+ClaimsSigningKeyResolver.class.getCanonicalName());
        }
    }
}

