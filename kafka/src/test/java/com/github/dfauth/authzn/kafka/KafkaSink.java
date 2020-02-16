package com.github.dfauth.authzn.kafka;

import akka.NotUsed;
import akka.stream.javadsl.Sink;
import com.github.dfauth.kafka.ReactiveKafkaProducer;
import org.apache.kafka.common.serialization.Serializer;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Properties;

import static com.github.dfauth.authzn.utils.TryCatchUtils.tryCatch;


public class KafkaSink<T> implements Subscriber<T> {

    private static final Logger logger = LoggerFactory.getLogger(KafkaSink.class);
    private final Properties props;
    private final Serializer<T> serializer;
    private Optional<Subscription> subscription = Optional.empty();
    private Optional<ReactiveKafkaProducer<T>> producer = Optional.empty();
    private final String topic;

    public KafkaSink(String topic, Properties props, Serializer<T> serializer) {
        this.topic = topic;
        this.props = props;
        this.serializer = serializer;
    }

    public static <T> Sink<T, NotUsed> createSink(String topic, Properties props, Serializer<T> serializer) {
        return Sink.fromSubscriber(new KafkaSink<>(topic, props, serializer));
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        this.subscription = Optional.of(subscription);
        this.producer = Optional.of(new ReactiveKafkaProducer<T>(props, this.topic, serializer));
        subscription.request(Integer.MAX_VALUE);
    }

    @Override
    public void onNext(T t) {
        this.producer
                .map(p -> tryCatch(() -> p.send(t)))
                .map(e -> e.thenAccept(r -> logger.info(String.format("metadata: %s %d %d"+r.topic(),
                        r.partition(),
                        r.offset()))));
    }

    @Override
    public void onError(Throwable t) {
        logger.info(t.getMessage(), t);
    }

    @Override
    public void onComplete() {
        this.subscription.ifPresent(s -> s.cancel());
    }
}
