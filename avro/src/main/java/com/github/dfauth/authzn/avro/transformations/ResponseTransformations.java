package com.github.dfauth.authzn.avro.transformations;

import org.apache.avro.specific.SpecificRecord;

import java.util.function.Function;


public interface ResponseTransformations<V extends SpecificRecord,O> {

    Function<V,O> fromAvro();

    Function<O,V> toAvro();
}
