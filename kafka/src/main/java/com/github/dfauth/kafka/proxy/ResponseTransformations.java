package com.github.dfauth.kafka.proxy;

import org.apache.avro.specific.SpecificRecord;

import java.util.function.Function;


public interface ResponseTransformations<V extends SpecificRecord,O> {

    Function<V,O> fromAvro();

    Function<O,V> toAvro();
}
