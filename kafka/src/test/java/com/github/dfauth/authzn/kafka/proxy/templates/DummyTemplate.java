package com.github.dfauth.authzn.kafka.proxy.templates;

import com.github.dfauth.authzn.avro.AvroSerialization;
import com.github.dfauth.authzn.avro.EnvelopeHandler;
import com.github.dfauth.authzn.avro.SampleTransformations;
import com.github.dfauth.authzn.avro.transformations.RequestTransformations;
import com.github.dfauth.authzn.avro.transformations.ResponseTransformations;
import com.github.dfauth.authzn.kafka.SampleRequest;
import com.github.dfauth.authzn.kafka.SampleResponse;
import com.github.dfauth.kafka.proxy.EnvelopeHandlers;
import com.github.dfauth.kafka.proxy.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyTemplate implements Template<SampleRequest, SampleResponse, com.github.dfauth.avro.authzn.SampleRequest, com.github.dfauth.avro.authzn.SampleResponse> {

    private static final Logger logger = LoggerFactory.getLogger(DummyTemplate.class);

    @Override
    public EnvelopeHandlers<com.github.dfauth.avro.authzn.SampleRequest, com.github.dfauth.avro.authzn.SampleResponse> envelopeHandlers() {
        return new EnvelopeHandlers<com.github.dfauth.avro.authzn.SampleRequest, com.github.dfauth.avro.authzn.SampleResponse>() {
            @Override
            public EnvelopeHandler<com.github.dfauth.avro.authzn.SampleRequest> inbound(AvroSerialization avroSerialization) {
                return EnvelopeHandler.of(avroSerialization, com.github.dfauth.avro.authzn.SampleRequest.class);
            }

            @Override
            public EnvelopeHandler<com.github.dfauth.avro.authzn.SampleResponse> outbound(AvroSerialization avroSerialization) {
                return EnvelopeHandler.of(avroSerialization, com.github.dfauth.avro.authzn.SampleResponse.class);
            }
        };
    }

    @Override
    public RequestTransformations<SampleRequest, com.github.dfauth.avro.authzn.SampleRequest> requestTransformations() {
        return new SampleTransformations.SampleRequestTransformations();
    }

    @Override
    public ResponseTransformations<com.github.dfauth.avro.authzn.SampleResponse, SampleResponse> responseTransformations() {
        return new SampleTransformations.SampleResponseTransformations();
    }
}
