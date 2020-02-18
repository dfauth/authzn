package com.github.dfauth.kafka.proxy;

import com.github.dfauth.authzn.avro.transformations.RequestTransformations;
import com.github.dfauth.authzn.avro.transformations.ResponseTransformations;
import org.apache.avro.specific.SpecificRecord;


public interface TransformationTemplate<I,O,U extends SpecificRecord, V extends SpecificRecord> {

    EnvelopeHandlers<U,V> envelopeHandlers();

    RequestTransformations<I,U> requestTransformations();

    ResponseTransformations<V,O> responseTransformations();
}
