package com.github.dfauth.kafka.proxy.templates;

import com.github.dfauth.authzn.avro.AvroSerialization;
import com.github.dfauth.authzn.avro.EnvelopeHandler;
import com.github.dfauth.authzn.avro.transformations.AuthenticationTransformations;
import com.github.dfauth.authzn.avro.transformations.RequestTransformations;
import com.github.dfauth.authzn.avro.transformations.ResponseTransformations;
import com.github.dfauth.avro.authzn.LoginRequest;
import com.github.dfauth.avro.authzn.LoginResponse;
import com.github.dfauth.kafka.proxy.EnvelopeHandlers;
import com.github.dfauth.kafka.proxy.Template;


public class AuthenticationTemplate implements Template<com.github.dfauth.authzn.domain.LoginRequest,com.github.dfauth.authzn.domain.LoginResponse, LoginRequest, LoginResponse> {

        @Override
        public EnvelopeHandlers<LoginRequest, LoginResponse> envelopeHandlers() {
            return new EnvelopeHandlers<LoginRequest, LoginResponse>(){
                @Override
                public EnvelopeHandler<LoginRequest> inbound(AvroSerialization avroSerialization) {
                    return EnvelopeHandler.of(avroSerialization, com.github.dfauth.avro.authzn.LoginRequest.class);
                }

                @Override
                public EnvelopeHandler<LoginResponse> outbound(AvroSerialization avroSerialization) {
                    return EnvelopeHandler.of(avroSerialization, com.github.dfauth.avro.authzn.LoginResponse.class);
                }
            };
        }

        @Override
        public RequestTransformations<com.github.dfauth.authzn.domain.LoginRequest, LoginRequest> requestTransformations() {
            return new AuthenticationTransformations.LoginRequestTransformations();
        }

        @Override
        public ResponseTransformations<LoginResponse, com.github.dfauth.authzn.domain.LoginResponse> responseTransformations() {
            return new AuthenticationTransformations.LoginResponseTransformations();
        }
}
