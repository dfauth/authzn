package com.github.dfauth.kafka.proxy;

import org.apache.avro.specific.SpecificRecord;


public interface Template<I,O,U extends SpecificRecord, V extends SpecificRecord> {

    EnvelopeHandlers<U,V> envelopeHandlers();

    RequestTransformations<I,U> requestTransformations();

    ResponseTransformations<V,O> responseTransformations();
}
