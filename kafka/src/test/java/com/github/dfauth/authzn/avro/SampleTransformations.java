package com.github.dfauth.authzn.avro;

import com.github.dfauth.authzn.avro.transformations.RequestTransformations;
import com.github.dfauth.authzn.avro.transformations.ResponseTransformations;
import com.github.dfauth.authzn.kafka.SampleRequest;
import com.github.dfauth.authzn.kafka.SampleResponse;
import com.github.dfauth.avro.authzn.SampleResponseSuccess;
import io.vavr.control.Try;

import java.util.function.Function;

import static com.github.dfauth.authzn.avro.AvroUtils.match;

public class SampleTransformations {

    public static class SampleRequestTransformations implements RequestTransformations<SampleRequest, com.github.dfauth.avro.authzn.SampleRequest> {

        @Override
        public Function<SampleRequest, com.github.dfauth.avro.authzn.SampleRequest> toAvro() {
            return s -> com.github.dfauth.avro.authzn.SampleRequest.newBuilder().setPayload(s.getPayload()).build();
        }

        @Override
        public Function<com.github.dfauth.avro.authzn.SampleRequest, SampleRequest> fromAvro() {
            return avro -> new SampleRequest(avro.getPayload());
        }
    }

    public static class SampleResponseTransformations implements ResponseTransformations<com.github.dfauth.avro.authzn.SampleResponse, SampleResponse> {
        @Override
        public Function<com.github.dfauth.avro.authzn.SampleResponse, Try<SampleResponse>> fromAvro() {
            return avro -> match(avro.getPayload(), SampleResponseSuccess.class, s -> new SampleResponse(s.getPayload()));
        }

        @Override
        public Function<Try<SampleResponse>, com.github.dfauth.avro.authzn.SampleResponse> toAvro() {
            return t -> t.map(r -> com.github.dfauth.avro.authzn.SampleResponse.newBuilder()
                    .setPayload(r)
                    .build())
                    .getOrElseGet(x -> com.github.dfauth.avro.authzn.SampleResponse.newBuilder()
                            .setPayload(com.github.dfauth.avro.authzn.Exception.newBuilder()
                                    .setMessage(x.getMessage())
                                    .build())
                            .build());

        }
    }
}
