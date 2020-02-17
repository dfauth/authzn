package com.github.dfauth.authzn.avro.transformations;

import io.vavr.control.Try;
import org.apache.avro.specific.SpecificRecord;

import java.util.function.Function;


public interface ResponseTransformations<V extends SpecificRecord,O> {

    Function<V, Try<O>> fromAvro();

    Function<Try<O>,V> toAvro();
}
