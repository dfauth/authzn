package com.github.dfauth.authzn.kafka.proxy.templates;

import com.github.dfauth.authzn.avro.AvroSerialization;
import com.github.dfauth.authzn.avro.EnvelopeHandler;
import com.github.dfauth.authzn.avro.UserInfoTransformations;
import com.github.dfauth.authzn.avro.transformations.RequestTransformations;
import com.github.dfauth.authzn.avro.transformations.ResponseTransformations;
import com.github.dfauth.authzn.domain.NoOp;
import com.github.dfauth.authzn.domain.UserInfoResponse;
import com.github.dfauth.kafka.proxy.EnvelopeHandlers;
import com.github.dfauth.kafka.proxy.TransformationTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserInfoTemplate implements TransformationTemplate<NoOp, UserInfoResponse, com.github.dfauth.avro.authzn.NoOp, com.github.dfauth.avro.authzn.UserInfoResponse> {

    private static final Logger logger = LoggerFactory.getLogger(UserInfoTemplate.class);

    @Override
    public EnvelopeHandlers<com.github.dfauth.avro.authzn.NoOp, com.github.dfauth.avro.authzn.UserInfoResponse> envelopeHandlers() {
        return new EnvelopeHandlers<com.github.dfauth.avro.authzn.NoOp, com.github.dfauth.avro.authzn.UserInfoResponse>() {
            @Override
            public EnvelopeHandler<com.github.dfauth.avro.authzn.NoOp> inbound(AvroSerialization avroSerialization) {
                return EnvelopeHandler.of(avroSerialization, com.github.dfauth.avro.authzn.NoOp.class);
            }

            @Override
            public EnvelopeHandler<com.github.dfauth.avro.authzn.UserInfoResponse> outbound(AvroSerialization avroSerialization) {
                return EnvelopeHandler.of(avroSerialization, com.github.dfauth.avro.authzn.UserInfoResponse.class);
            }
        };
    }

    @Override
    public RequestTransformations<NoOp, com.github.dfauth.avro.authzn.NoOp> requestTransformations() {
        return new UserInfoTransformations.NoOpTransformations();
    }

    @Override
    public ResponseTransformations<com.github.dfauth.avro.authzn.UserInfoResponse, UserInfoResponse> responseTransformations() {
        return new UserInfoTransformations.UserInfoResponseTransformations();
    }
}
