package com.github.dfauth.authzn.avro;

import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import org.apache.avro.specific.SpecificRecord;

public class AvroSerialization {

    private final SchemaRegistryClient schemaRegClient;
    private final String schemaRegUrl;
    private SpecificRecordSerializer.Builder serializerBuilder;
    private SpecificRecordDeserializer.Builder deserializerBuilder;

    public AvroSerialization(SchemaRegistryClient schemaRegClient, String schemaRegUrl) {
        this.schemaRegClient = schemaRegClient;
        this.schemaRegUrl = schemaRegUrl;
        this.serializerBuilder = SpecificRecordSerializer.Builder.builder()
                .withSchemaRegistryClient(schemaRegClient)
                .withSchemaRegistryURL(schemaRegUrl);
        this.deserializerBuilder = SpecificRecordDeserializer.Builder.builder()
                .withSchemaRegistryClient(schemaRegClient)
                .withSchemaRegistryURL(schemaRegUrl);
    }

    public static AvroSerialization of(SchemaRegistryClient schemaRegClient, String schemaRegUrl) {
        return new AvroSerialization(schemaRegClient, schemaRegUrl);
    }

    public <T extends SpecificRecord> SpecificRecordSerializer<T> serializer() {
        return serializerBuilder.build();
    }

    public <T extends SpecificRecord> SpecificRecordSerializer<T> serializer(Class<T> ignored) {
        return serializer();
    }

    public <T extends SpecificRecord> SpecificRecordDeserializer<T> deserializer() {
        return deserializerBuilder.build();
    }

    public <T extends SpecificRecord> SpecificRecordDeserializer<T> deserializer(Class<T> ignored) {
        return deserializer();
    }
}
