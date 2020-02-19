package com.github.dfauth.authzn.avro;

import com.github.dfauth.authzn.avro.transformations.RequestTransformations;
import com.github.dfauth.authzn.avro.transformations.ResponseTransformations;
import com.github.dfauth.authzn.domain.NoOp;
import com.github.dfauth.authzn.domain.UserInfoRequest;
import com.github.dfauth.authzn.domain.UserInfoResponse;
import com.github.dfauth.avro.authzn.UserInfoResponseSuccess;
import io.vavr.control.Try;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.Function;

import static com.github.dfauth.authzn.avro.AvroUtils.match;

public class UserInfoTransformations {

    public static class NoOpTransformations implements RequestTransformations<NoOp, com.github.dfauth.avro.authzn.NoOp> {

        @Override
        public Function<NoOp, com.github.dfauth.avro.authzn.NoOp> toAvro() {
            return s -> com.github.dfauth.avro.authzn.NoOp.newBuilder().build();
        }

        @Override
        public Function<com.github.dfauth.avro.authzn.NoOp, NoOp> fromAvro() {
            return avro -> NoOp.noOp;
        }
    }

    public static class UserInfoRequestTransformations implements RequestTransformations<UserInfoRequest, com.github.dfauth.avro.authzn.UserInfoRequest> {

        @Override
        public Function<UserInfoRequest, com.github.dfauth.avro.authzn.UserInfoRequest> toAvro() {
            return s -> com.github.dfauth.avro.authzn.UserInfoRequest.newBuilder()
                    .setUserId(s.getUserId())
                    .build();
        }

        @Override
        public Function<com.github.dfauth.avro.authzn.UserInfoRequest, UserInfoRequest> fromAvro() {
            return avro -> new UserInfoRequest(avro.getUserId());
        }
    }

    public static class UserInfoResponseTransformations implements ResponseTransformations<com.github.dfauth.avro.authzn.UserInfoResponse, UserInfoResponse> {
        @Override
        public Function<com.github.dfauth.avro.authzn.UserInfoResponse, Try<UserInfoResponse>> fromAvro() {
            return avro -> match(avro.getPayload(), UserInfoResponseSuccess.class, s -> new UserInfoResponse(s.getUserId(), s.getCompanyId(), new HashSet<>(s.getRoles())));
        }

        @Override
        public Function<Try<UserInfoResponse>, com.github.dfauth.avro.authzn.UserInfoResponse> toAvro() {
            return t -> t.map(r -> com.github.dfauth.avro.authzn.UserInfoResponse.newBuilder()
                    .setPayload(UserInfoResponseSuccess.newBuilder()
                        .setUserId(r.getUserId())
                        .setCompanyId(r.getCompanyId())
                        .setRoles(new ArrayList(r.getRoles()))
                        .build())
                    .build())
                    .getOrElseGet(x -> com.github.dfauth.avro.authzn.UserInfoResponse.newBuilder()
                            .setPayload(com.github.dfauth.avro.authzn.Exception.newBuilder()
                                    .setMessage(x.getMessage())
                                    .build())
                            .build());

        }
    }
}
