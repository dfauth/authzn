package com.github.dfauth.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class ReactiveKafkaProducer<T> extends KafkaProducer<String, T> {

    private final String topic;

    public ReactiveKafkaProducer(Properties props, String topic, Serializer<T> serializer) {
        super(props, new StringSerializer(), serializer);
        this.topic = topic;
    }

    public CompletionStage<RecordMetadata> send(T t) {
        ProducerRecord<String, T> record = new ProducerRecord<>(topic, t);
        CompletableFuture<RecordMetadata> future = new CompletableFuture<>();
        super.send(record,(m,e) -> {
            if(m != null) {
                future.complete(m);
            } else {
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}
