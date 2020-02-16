package com.github.dfauth.kafka.proxy;

import org.apache.avro.specific.SpecificRecord;

import java.util.function.Function;


public interface RequestTransformations<I,U extends SpecificRecord> {

    Function<I,U> toAvro();

    Function<U,I> fromAvro();
}
