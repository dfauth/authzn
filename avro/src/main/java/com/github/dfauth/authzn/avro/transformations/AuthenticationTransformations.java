package com.github.dfauth.authzn.avro.transformations;

import com.github.dfauth.authzn.domain.LoginRequest;
import com.github.dfauth.authzn.domain.LoginResponse;
import com.github.dfauth.avro.authzn.LoginResponseSuccess;
import io.vavr.control.Try;

import java.util.function.Function;

import static com.github.dfauth.authzn.avro.AvroUtils.match;


public class AuthenticationTransformations {

    public static class LoginRequestTransformations implements RequestTransformations<LoginRequest, com.github.dfauth.avro.authzn.LoginRequest> {

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

        public static Function<Try<LoginResponse>, com.github.dfauth.avro.authzn.LoginResponse> toAvro = t ->
            t.map(r -> com.github.dfauth.avro.authzn.LoginResponse.newBuilder()
                    .setPayload(com.github.dfauth.avro.authzn.LoginResponseSuccess.newBuilder()
                            .setToken(r.getToken())
                            .build())
                    .build())
                    .getOrElseGet(x -> com.github.dfauth.avro.authzn.LoginResponse.newBuilder()
                            .setPayload(com.github.dfauth.avro.authzn.Exception.newBuilder()
                                .setMessage(x.getMessage())
                                .build())
                            .build());

        public static Function<com.github.dfauth.avro.authzn.LoginResponse, Try<LoginResponse>> fromAvro = avro ->
                match(avro.getPayload(), LoginResponseSuccess.class, s -> new LoginResponse(s.getToken()));

        @Override
        public Function<com.github.dfauth.avro.authzn.LoginResponse, Try<LoginResponse>> fromAvro() {
            return fromAvro;
        }

        @Override
        public Function<Try<LoginResponse>, com.github.dfauth.avro.authzn.LoginResponse> toAvro() {
            return toAvro;
        }
    }
}
