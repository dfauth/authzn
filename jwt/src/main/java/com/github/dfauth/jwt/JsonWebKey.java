package com.github.dfauth.jwt;

import com.github.dfauth.authzn.common.Builder;
import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;


public class JsonWebKey {

    private static final Logger logger = LoggerFactory.getLogger(JsonWebKey.class);

    private String kty;
    private String kid;
    private String n;
    private String e;
    private String alg;
    private String use = "sig";

    public JsonWebKey(String kty, String kid, String n, String e, String alg, String use) {
        this.kty = kty;
        this.kid = kid;
        this.n = n;
        this.e = e;
        this.alg = alg;
        this.use = use;
    }

    public static JsonWebKeyBuilder builder() {
        return new JsonWebKeyBuilder();
    }

    public static Map<String, Object> keys(JsonWebKey webKey) {
        return Collections.singletonMap("keys", Collections.singletonList(webKey));
    }

    public String getKty() {
        return kty;
    }

    public String getKid() {
        return kid;
    }

    public String getN() {
        return n;
    }

    public String getE() {
        return e;
    }

    public String getAlg() {
        return alg;
    }

    public String getUse() {
        return use;
    }

    public Map<String, Object> asMap() {
        return ImmutableMap.<String, Object>builder()
                .put("kty", kty)
                .put("kid", kid)
                .put("n", n)
                .put("e", e)
                .put("alg", alg)
                .put("use", use)
                .build();
    }

    public static class JsonWebKeyBuilder implements Builder<JsonWebKey> {
        private String kty;
        private String kid;
        private String n;
        private String e;
        private String alg;
        private String use = "sig";

        @Override
        public JsonWebKey build() {
            return new JsonWebKey(kty, kid, n, e, alg, use);
        }

        public JsonWebKeyBuilder withKty(String kty) {
            this.kty = kty;
            return this;
        }

        public JsonWebKeyBuilder withKid(String kid) {
            this.kid = kid;
            return this;
        }

        public JsonWebKeyBuilder withN(String n) {
            this.n = n;
            return this;
        }

        public JsonWebKeyBuilder withE(String e) {
            this.e = e;
            return this;
        }

        public JsonWebKeyBuilder withAlg(String alg) {
            this.alg = alg;
            return this;
        }
    }
}
