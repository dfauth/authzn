package com.github.dfauth.kafka.proxy;

import com.github.dfauth.authzn.avro.AvroSerialization;
import com.github.dfauth.authzn.avro.EnvelopeHandler;
import org.apache.avro.specific.SpecificRecord;


public interface EnvelopeHandlers<U extends SpecificRecord, V extends SpecificRecord> {

    EnvelopeHandler<U> inbound(AvroSerialization avroSerialization);

    EnvelopeHandler<V> outbound(AvroSerialization avroSerialization);
}
