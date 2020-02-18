package com.github.dfauth.kafka;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class ReactiveKafkaProducer<T> extends KafkaProducer<String, T> {

    private static final Logger logger = LoggerFactory.getLogger(ReactiveKafkaProducer.class);

    private final String topic;

    public ReactiveKafkaProducer(Properties props, String topic, Serializer<T> serializer) {
        super(props, new StringSerializer(), serializer);
        this.topic = topic;
    }

    public CompletableFuture<RecordMetadata> send(T t) {
        ProducerRecord<String, T> record = new ProducerRecord<>(topic, t);
        CompletableFuture<RecordMetadata> future = new CompletableFuture<>();
        super.send(record,(m,e) -> {
            if(m != null) {
                logger.debug(String.format("metadata: topic %s partition %d offset %d",m.topic(),m.partition(),m.offset()));
                future.complete(m);
            } else {
                logger.error(e.getMessage(), e);
                future.completeExceptionally(e);
            }
        });
        return future;
    }
}
