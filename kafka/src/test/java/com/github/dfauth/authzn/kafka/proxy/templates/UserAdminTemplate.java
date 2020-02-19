package com.github.dfauth.authzn.kafka.proxy.templates;

import com.github.dfauth.authzn.avro.AvroSerialization;
import com.github.dfauth.authzn.avro.EnvelopeHandler;
import com.github.dfauth.authzn.avro.UserInfoTransformations;
import com.github.dfauth.authzn.avro.transformations.RequestTransformations;
import com.github.dfauth.authzn.avro.transformations.ResponseTransformations;
import com.github.dfauth.authzn.domain.UserInfoRequest;
import com.github.dfauth.authzn.domain.UserInfoResponse;
import com.github.dfauth.kafka.proxy.EnvelopeHandlers;
import com.github.dfauth.kafka.proxy.TransformationTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserAdminTemplate implements TransformationTemplate<UserInfoRequest, UserInfoResponse, com.github.dfauth.avro.authzn.UserInfoRequest, com.github.dfauth.avro.authzn.UserInfoResponse> {

    private static final Logger logger = LoggerFactory.getLogger(UserAdminTemplate.class);

    @Override
    public EnvelopeHandlers<com.github.dfauth.avro.authzn.UserInfoRequest, com.github.dfauth.avro.authzn.UserInfoResponse> envelopeHandlers() {
        return new EnvelopeHandlers<com.github.dfauth.avro.authzn.UserInfoRequest, com.github.dfauth.avro.authzn.UserInfoResponse>() {
            @Override
            public EnvelopeHandler<com.github.dfauth.avro.authzn.UserInfoRequest> inbound(AvroSerialization avroSerialization) {
                return EnvelopeHandler.of(avroSerialization, com.github.dfauth.avro.authzn.UserInfoRequest.class);
            }

            @Override
            public EnvelopeHandler<com.github.dfauth.avro.authzn.UserInfoResponse> outbound(AvroSerialization avroSerialization) {
                return EnvelopeHandler.of(avroSerialization, com.github.dfauth.avro.authzn.UserInfoResponse.class);
            }
        };
    }

    @Override
    public RequestTransformations<UserInfoRequest, com.github.dfauth.avro.authzn.UserInfoRequest> requestTransformations() {
        return new UserInfoTransformations.UserInfoRequestTransformations();
    }

    @Override
    public ResponseTransformations<com.github.dfauth.avro.authzn.UserInfoResponse, UserInfoResponse> responseTransformations() {
        return new UserInfoTransformations.UserInfoResponseTransformations();
    }
}
