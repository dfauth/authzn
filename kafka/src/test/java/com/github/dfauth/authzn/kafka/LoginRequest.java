package com.github.dfauth.authzn.kafka;

import com.github.dfauth.authzn.Builder;

public class LoginRequest {

    private String username;
    private String passwordHash;
    private String random;

    public LoginRequest(String username, String passwordHash, String random) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.random = random;
    }

    public static _Builder builder() {
        return new _Builder();
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getRandom() {
        return random;
    }

    static class _Builder implements Builder<LoginRequest> {

        private String random;
        private String passwordHash;
        private String username;

        @Override
        public LoginRequest build() {
            return new LoginRequest(username, passwordHash, random);
        }

        public _Builder withRandom(String random) {
            this.random = random;
            return this;
        }

        public _Builder withPasswordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public _Builder withUsername(String username) {
            this.username = username;
            return this;
        }
    }

}
