package com.github.dfauth.authzn.kafka;

import java.util.function.Function;


public class TestTransformations {

    static class LoginRequestTransformations {

        public static Function<LoginRequest, com.github.dfauth.avro.authzn.LoginRequest> toAvro = d ->
                com.github.dfauth.avro.authzn.LoginRequest.newBuilder()
                        .setUsername(d.getUsername())
                        .setPasswordHash(d.getPasswordHash())
                        .setRandom(d.getRandom())
                        .build();

        public static Function<com.github.dfauth.avro.authzn.LoginRequest, LoginRequest> fromAvro = avro ->
                LoginRequest.builder()
                        .withUsername(avro.getUsername())
                        .withPasswordHash(avro.getPasswordHash())
                        .withRandom(avro.getRandom())
                        .build();
    }

    public static class LoginResponseTransformations {

        public static Function<LoginResponse, com.github.dfauth.avro.authzn.LoginResponse> toAvro = d ->
                com.github.dfauth.avro.authzn.LoginResponse.newBuilder()
                        .setToken(d.getToken())
                        .build();

        public static Function<com.github.dfauth.avro.authzn.LoginResponse, LoginResponse> fromAvro = avro ->
                new LoginResponse(avro.getToken());
    }
}
