package com.github.dfauth.authzn.avro;

import com.github.dfauth.avro.authzn.Envelope;
import com.google.common.collect.ImmutableMap;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.common.errors.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static java.util.Objects.requireNonNull;


public class EnvelopeHandler<T extends SpecificRecord> {

    private static final Logger logger = LoggerFactory.getLogger(EnvelopeHandler.class);

    private final SpecificRecordSerializer<T> serializer;
    private final SpecificRecordDeserializer<T> deserializer;

    public static <T extends SpecificRecord> EnvelopeHandler<T> of(AvroSerialization avroSerialization, Class<T> targetClass) {
        return new EnvelopeHandler<>(
                avroSerialization.serializer(targetClass),
                avroSerialization.deserializer(targetClass)
        );
    }

    public EnvelopeHandler(SpecificRecordSerializer<T> serializer, SpecificRecordDeserializer<T> deserializer) {
        this.serializer = serializer;
        this.deserializer = deserializer;
    }

    public Envelope envelope(final T t) {
        return envelope(t, Collections.emptyMap());
    }

    public Envelope envelope(final T t, final Map<String, String> metadata) {
        try {
            final String canonicalName = requireNonNull(t, "null specificRecord").getClass().getCanonicalName();
            final byte[] serialize = serializer.serialize(canonicalName, t);
            final String id = UUID.randomUUID().toString();

            logger.info("Creating payload with id [{}], schemaRegistryTopic [{}]", id, canonicalName);

            return Envelope.newBuilder()
                    .setId(id)
                    .setPayload(ByteBuffer.wrap(serialize))
                    .setMetadata(metadata != null ? metadata : ImmutableMap.of())
                    .setSchemaRegistryTopic(canonicalName)
                    .build();
        } catch (Exception e) {
            final String schema = t == null ? "null" : t.getSchema().toString(true);
            final String exMsg = "Unable to create envelope for specificRecord with schema [" + schema + "]";
            logger.error(exMsg, e);
            throw new SerializationException(exMsg, e);
        }
    }

    public T extractRecord(Envelope e) {
        try {
            return (T) deserializer.deserialize(e.getSchemaRegistryTopic(), requireNonNull(e, "null envelopeDto").getPayload().array());
        } catch (Exception e1) {
            final String topic = (e == null) ? "null" : e.getSchemaRegistryTopic();
            final String message = "Unable to extract record from envelope with topic " + topic;
            logger.error(message, e);
            throw new SerializationException(message, e1);
        }
    }

    public MetadataEnvelope<T> extractRecordWithMetadata(Envelope e) {
        return new MetadataEnvelope<T>(extractRecord(e), e.getMetadata());
    }

    public Envelope envelope(MetadataEnvelope<T> e) {
        return envelope(e.getPayload(), e.getMetadata());
    }
}
