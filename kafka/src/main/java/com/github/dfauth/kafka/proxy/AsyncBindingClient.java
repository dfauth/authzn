package com.github.dfauth.kafka.proxy;

import akka.Done;
import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.github.dfauth.authzn.avro.*;
import com.github.dfauth.authzn.utils.AbstractSubscriber;
import com.github.dfauth.avro.authzn.Envelope;
import com.github.dfauth.kafka.KafkaSubscriber;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.vavr.control.Try;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.reactivestreams.Subscription;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;


public class AsyncBindingClient<I,O,U extends SpecificRecord, V extends SpecificRecord> extends ServiceProxy<I,O,U,V> {

    private final AtomicReference<String> correlationId;
    private final Timer timer;
    private final Config consumerConfig;
    private Consumer.Control control;
    private KafkaSubscriber<Envelope> sink;
    private AbstractSubscriber<MetadataEnvelope<Try<O>>> subscriber;
    private CompletableFuture<MetadataEnvelope<Try<O>>> f;
    private EnvelopeHandler inEnvelopeHandler;

    public AsyncBindingClient(ActorSystem system, Materializer materializer, Properties props, String brokerList, AvroSerialization avroSerialization, String topic, TransformationTemplate<I,O,U,V> template) {
        super(system, materializer, props, brokerList, avroSerialization, topic, template);
        correlationId = new AtomicReference<>(UUID.randomUUID().toString());
        timer = new Timer();
        consumerConfig = ConfigFactory.load().getConfig("akka.kafka.consumer");
        init();
    }

    public CompletionStage<Done> close() {
        this.subscriber.close();
        this.sink.close();
        return this.control.shutdown();
    }

    private void init() {
        SpecificRecordDeserializer<Envelope> envelopeDeserializer = avroSerialization.deserializer(Envelope.class);
        SpecificRecordSerializer<Envelope> envelopeSerializer = avroSerialization.serializer(Envelope.class);

        inEnvelopeHandler = template.envelopeHandlers().inbound(avroSerialization);

        EnvelopeHandler<V> outEnvelopeHandler = template.envelopeHandlers().outbound(avroSerialization);

        ConsumerSettings<String, Envelope> consumerSettings = ConsumerSettings.apply(system, new StringDeserializer(), envelopeDeserializer)
                .withBootstrapServers(brokerList)
                .withGroupId(correlationId.get())
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerConfig.getString("auto.offset.reset"));

        this.sink = new KafkaSubscriber<>(topic, props, envelopeSerializer);

        f = new CompletableFuture<>();
        subscriber = new AbstractSubscriber<MetadataEnvelope<Try<O>>>(){
            @Override
            protected void _onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(MetadataEnvelope<Try<O>> e) {
                e.getPayload()
                        .onSuccess(v -> f.complete(e))
                        .onFailure(t -> f.completeExceptionally(t));
            }
        };

        control = Consumer.plainSource(consumerSettings, Subscriptions.assignment(new TopicPartition(topic, 0)))
                .map(r -> r.value())
                .map(e -> outEnvelopeHandler.extractRecordWithMetadata(e))
                .filter(m -> m.isOutbound())
                .filter(m -> m.correlationId().map(j -> correlationId.get().equals(j)).orElse(false))
                .wireTap(m -> timer.cancel())
                .map(m -> m.mapPayload(template.responseTransformations().fromAvro()))
                .to(Sink.fromSubscriber(subscriber))
                .run(materializer);
/**
        return i -> {

            CompletableFuture<MetadataEnvelope<Try<O>>> f = new CompletableFuture<>();

            subscriber = new AbstractSubscriber<MetadataEnvelope<Try<O>>>(){
                @Override
                protected void _onSubscribe(Subscription s) {
                    s.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(MetadataEnvelope<Try<O>> e) {
                    e.getPayload()
                            .onSuccess(v -> f.complete(e))
                            .onFailure(t -> f.completeExceptionally(t));
                }
            };

            Source.single(i)
                    .wireTap(m -> {
                        TemporalAmount timeout = Optional.ofNullable(m.getMetadata().get(MetadataEnvelope.TIMEOUT))
                                .map(s -> (TemporalAmount) Duration.ofMillis(Long.parseLong(s)))
                                .orElse(consumerConfig.getTemporal("timeout"));
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                f.completeExceptionally(new TimeoutException("timed out after " + timeout));
                            }
                        }, timeout.get(ChronoUnit.SECONDS) * 1000);

                    })
                    .map(m -> i.correlationId().map(k -> {
                        correlationId.set(k);
                        return i;
                    }).orElseGet(() -> {
                        correlationId.set(groupId);
                        return m.withCorrelationId(correlationId.get());
                    }))
                    .map(m -> m.inbound())
                    .map(m -> m.mapPayload(template.requestTransformations().toAvro()))
                    .map(e -> inEnvelopeHandler.envelope(e))
                    .to(Sink.fromSubscriber(sink))
                    .run(materializer);


            return f;
        };
 **/
    }

    public CompletableFuture<MetadataEnvelope<Try<O>>> call(MetadataEnvelope<I> envelope) {
        Source.single(envelope)
                .wireTap(m -> {
                    TemporalAmount timeout = Optional.ofNullable(m.getMetadata().get(MetadataEnvelope.TIMEOUT))
                            .map(s -> (TemporalAmount) Duration.ofMillis(Long.parseLong(s)))
                            .orElse(consumerConfig.getTemporal("timeout"));
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            f.completeExceptionally(new TimeoutException("timed out after " + timeout));
                        }
                    }, timeout.get(ChronoUnit.SECONDS) * 1000);

                })
                .map(m -> m.correlationId().map(k -> {
                            correlationId.set(k);
                            return m;
                        }).orElseGet(() -> m.withCorrelationId(correlationId.get()))
                )
                .map(m -> m.inbound())
                .map(m -> m.mapPayload(template.requestTransformations().toAvro()))
                .map(e -> inEnvelopeHandler.envelope(e))
                .to(Sink.fromSubscriber(sink))
                .run(materializer);
        return f;
    }
}
