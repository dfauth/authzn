package com.github.dfauth.authzn.kafka;

import com.github.dfauth.kafka.proxy.RequestTransformations;
import com.github.dfauth.kafka.proxy.ResponseTransformations;

import java.util.function.Function;


public class TestTransformations {

    static class LoginRequestTransformations implements RequestTransformations<LoginRequest, com.github.dfauth.avro.authzn.LoginRequest> {

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

        @Override
        public Function<LoginRequest, com.github.dfauth.avro.authzn.LoginRequest> toAvro() {
            return toAvro;
        }

        @Override
        public Function<com.github.dfauth.avro.authzn.LoginRequest, LoginRequest> fromAvro() {
            return fromAvro;
        }
    }

    public static class LoginResponseTransformations implements ResponseTransformations<com.github.dfauth.avro.authzn.LoginResponse, LoginResponse> {

        public static Function<LoginResponse, com.github.dfauth.avro.authzn.LoginResponse> toAvro = d ->
                com.github.dfauth.avro.authzn.LoginResponse.newBuilder()
                        .setToken(d.getToken())
                        .build();

        public static Function<com.github.dfauth.avro.authzn.LoginResponse, LoginResponse> fromAvro = avro ->
                new LoginResponse(avro.getToken());

        @Override
        public Function<com.github.dfauth.avro.authzn.LoginResponse, LoginResponse> fromAvro() {
            return fromAvro;
        }

        @Override
        public Function<LoginResponse, com.github.dfauth.avro.authzn.LoginResponse> toAvro() {
            return toAvro;
        }
    }
}
