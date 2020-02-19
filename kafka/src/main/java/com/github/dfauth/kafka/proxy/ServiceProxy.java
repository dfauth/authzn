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
import org.reactivestreams.Processor;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public class ServiceProxy {

    private static final Logger logger = LoggerFactory.getLogger(ServiceProxy.class);

    private final ActorSystem system;
    private final Materializer materializer;
    private Properties props;
    private String brokerList;
    private AvroSerialization avroSerialization;

    public ServiceProxy(ActorSystem system, Materializer materializer, Properties props, String brokerList, AvroSerialization avroSerialization) {
        this.system = system;
        this.materializer = materializer;
        this.props = props;
        this.brokerList = brokerList;
        this.avroSerialization = avroSerialization;
    }

    public Client createClient(String topic) {
        return new Client(this, topic);
    }

    public <I,O> ServiceProxy.Service createService(Processor<MetadataEnvelope<I>, MetadataEnvelope<Try<O>>> processor, String groupId, String topic) {
        return new Service(this, processor, groupId, topic);
    }

    public static class Service<I,O> {

        private final String groupId;
        private final ServiceProxy serviceProxy;
        private final Processor<MetadataEnvelope<I>, MetadataEnvelope<Try<O>>> processor;
        private String topic;
        private Consumer.Control control;
        private KafkaSubscriber<Envelope> sink;

        public Service(ServiceProxy serviceProxy, Processor<MetadataEnvelope<I>, MetadataEnvelope<Try<O>>> processor, String groupId, String topic) {
            this.serviceProxy = serviceProxy;
            this.processor = processor;
            this.groupId = groupId;
            this.topic = topic;
        }

        public void close() {
            if(this.sink != null) {
                this.sink.close();
            }
            if(control != null) {
                this.control.shutdown();
            }
        }

        public <U extends SpecificRecord, V extends SpecificRecord> void bindToKafka(TransformationTemplate<I,O,U,V> template) {
            Config consumerConfig = ConfigFactory.load().getConfig("akka.kafka.consumer");

            SpecificRecordDeserializer<Envelope> envelopeDeserializer = serviceProxy.avroSerialization.deserializer(Envelope.class);
            SpecificRecordSerializer<Envelope> envelopeSerializer = serviceProxy.avroSerialization.serializer(Envelope.class);

            EnvelopeHandler<U> inEnvelopeHandler = template.envelopeHandlers().inbound(serviceProxy.avroSerialization);

            EnvelopeHandler<V> outEnvelopeHandler = template.envelopeHandlers().outbound(serviceProxy.avroSerialization);

            sink = new KafkaSubscriber<>(topic, serviceProxy.props, envelopeSerializer);

            ConsumerSettings<String, Envelope> consumerSettings = ConsumerSettings.apply(serviceProxy.system, new StringDeserializer(), envelopeDeserializer)
                    .withBootstrapServers(serviceProxy.brokerList)
                    .withGroupId(groupId)
                    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerConfig.getString("auto.offset.reset"));

            control = Consumer.plainSource(consumerSettings, Subscriptions.assignment(new TopicPartition(topic, 0)))
                    .map(r -> r.value())
                    .map(e -> inEnvelopeHandler.extractRecordWithMetadata(e))
                    .filter(m -> m.isInbound())
                    .map(m -> m.mapPayload(template.requestTransformations().fromAvro()))
                    .to(Sink.fromSubscriber(processor))
                    .run(serviceProxy.materializer);

            Source.fromPublisher(processor)
                    .map(m -> m.outbound())
                    .map(m -> m.mapPayload(template.responseTransformations().toAvro()))
                    .map(e -> outEnvelopeHandler.envelope(e))
                    .to(Sink.fromSubscriber(sink))
                    .run(serviceProxy.materializer);

        }
    }

    public static class Client<I,O> {

        private final String groupId;
        private final ServiceProxy serviceProxy;
        private String topic;
        private Consumer.Control control;
        private KafkaSubscriber<Envelope> sink;
        private AbstractSubscriber<MetadataEnvelope<Try<O>>> subscriber;

        public Client(ServiceProxy serviceProxy, String topic) {
            this.serviceProxy = serviceProxy;
            this.groupId = UUID.randomUUID().toString();
            this.topic = topic;
        }

        public CompletionStage<Done> close() {
            this.subscriber.close();
            this.sink.close();
            return this.control.shutdown();
        }

        public <U extends SpecificRecord, V extends SpecificRecord> Function<MetadataEnvelope<I>, CompletableFuture<MetadataEnvelope<Try<O>>>> asyncProxy(TransformationTemplate<I,O,U,V> template) {
            Config consumerConfig = ConfigFactory.load().getConfig("akka.kafka.consumer");

            SpecificRecordDeserializer<Envelope> envelopeDeserializer = serviceProxy.avroSerialization.deserializer(Envelope.class);
            SpecificRecordSerializer<Envelope> envelopeSerializer = serviceProxy.avroSerialization.serializer(Envelope.class);

            EnvelopeHandler<U> inEnvelopeHandler = template.envelopeHandlers().inbound(serviceProxy.avroSerialization);

            EnvelopeHandler<V> outEnvelopeHandler = template.envelopeHandlers().outbound(serviceProxy.avroSerialization);

            ConsumerSettings<String, Envelope> consumerSettings = ConsumerSettings.apply(serviceProxy.system, new StringDeserializer(), envelopeDeserializer)
                    .withBootstrapServers(serviceProxy.brokerList)
                    .withGroupId(groupId)
                    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerConfig.getString("auto.offset.reset"));

            this.sink = new KafkaSubscriber<>(topic, serviceProxy.props, envelopeSerializer);

            return i -> {

                AtomicReference<String> correlationId = new AtomicReference<>();

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

                Timer timer = new Timer();
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
                        .run(serviceProxy.materializer);

                control = Consumer.plainSource(consumerSettings, Subscriptions.assignment(new TopicPartition(topic, 0)))
                        .mapAsync(1,r -> CompletableFuture.completedFuture(r.value()))
                        .wireTap(r -> logger.debug("value from kafka: "+r))
                        .map(e -> outEnvelopeHandler.extractRecordWithMetadata(e))
                        .filter(m -> m.isOutbound())
                        .filter(m -> m.correlationId().map(j -> correlationId.get().equals(j)).orElse(false))
                        .wireTap(m -> timer.cancel())
                        .map(m -> m.mapPayload(template.responseTransformations().fromAvro()))
                        .to(Sink.fromSubscriber(subscriber))
                        .run(serviceProxy.materializer);

                return f;
            };
        }
    }

}
