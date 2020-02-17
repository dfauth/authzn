package com.github.dfauth.kafka.proxy;

import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.github.dfauth.authzn.avro.*;
import com.github.dfauth.avro.authzn.Envelope;
import com.github.dfauth.kafka.KafkaSink;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.vavr.control.Try;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.reactivestreams.Processor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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

    public Client createClient(String groupId, String inTopic, String outTopic) {
        return new Client(this, groupId, inTopic, outTopic);
    }

    public <I,O> ServiceProxy.Service createService(Processor<MetadataEnvelope<I>, MetadataEnvelope<Try<O>>> processor, String groupId, String inTopic, String outTopic) {
        return new Service(this, processor, groupId, inTopic, outTopic);
    }

    public static class Service<I,O> {

        private final String groupId;
        private final ServiceProxy serviceProxy;
        private final Processor<MetadataEnvelope<I>, MetadataEnvelope<Try<O>>> processor;
        private String outTopic;
        private String inTopic;

        public Service(ServiceProxy serviceProxy, Processor<MetadataEnvelope<I>, MetadataEnvelope<Try<O>>> processor, String groupId, String inTopic, String outTopic) {
            this.serviceProxy = serviceProxy;
            this.processor = processor;
            this.groupId = groupId;
            this.inTopic = inTopic;
            this.outTopic = outTopic;
        }

        public <U extends SpecificRecord, V extends SpecificRecord> void bindToKafka(Template<I,O,U,V> template) {
            Config consumerConfig = ConfigFactory.load().getConfig("akka.kafka.consumer");

            SpecificRecordDeserializer<Envelope> envelopeDeserializer = serviceProxy.avroSerialization.deserializer(Envelope.class);
            SpecificRecordSerializer<Envelope> envelopeSerializer = serviceProxy.avroSerialization.serializer(Envelope.class);

            EnvelopeHandler<U> inEnvelopeHandler = template.envelopeHandlers().inbound(serviceProxy.avroSerialization);

            EnvelopeHandler<V> outEnvelopeHandler = template.envelopeHandlers().outbound(serviceProxy.avroSerialization);

            ConsumerSettings<String, Envelope> consumerSettings = ConsumerSettings.apply(serviceProxy.system, new StringDeserializer(), envelopeDeserializer)
                    .withBootstrapServers(serviceProxy.brokerList)
                    .withGroupId(groupId)
                    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerConfig.getString("auto.offset.reset"));

            Consumer.plainSource(consumerSettings, Subscriptions.assignment(new TopicPartition(inTopic, 0)))
                    .map(r -> r.value())
                    .map(e -> inEnvelopeHandler.extractRecordWithMetadata(e))
                    .map(m -> m.mapPayload(template.requestTransformations().fromAvro()))
                    .to(Sink.fromSubscriber(processor))
                    .run(serviceProxy.materializer);

            Source.fromPublisher(processor)
                    .map(m -> m.mapPayload(template.responseTransformations().toAvro()))
                    .map(e -> outEnvelopeHandler.envelope(e))
                    .to(KafkaSink.createSink(outTopic, serviceProxy.props, envelopeSerializer))
                    .run(serviceProxy.materializer);

        }
    }

    public static class Client<I,O> {

        private final String groupId;
        private final ServiceProxy serviceProxy;
        private String outTopic;
        private String inTopic;

        public Client(ServiceProxy serviceProxy, String groupId, String inTopic, String outTopic) {
            this.serviceProxy = serviceProxy;
            this.groupId = groupId;
            this.inTopic = inTopic;
            this.outTopic = outTopic;
        }

        public <U extends SpecificRecord, V extends SpecificRecord> Function<MetadataEnvelope<I>, CompletableFuture<MetadataEnvelope<Try<O>>>> asyncProxy(Template<I,O,U,V> template) {
            Config consumerConfig = ConfigFactory.load().getConfig("akka.kafka.consumer");

            SpecificRecordDeserializer<Envelope> envelopeDeserializer = serviceProxy.avroSerialization.deserializer(Envelope.class);
            SpecificRecordSerializer<Envelope> envelopeSerializer = serviceProxy.avroSerialization.serializer(Envelope.class);

            EnvelopeHandler<U> inEnvelopeHandler = template.envelopeHandlers().inbound(serviceProxy.avroSerialization);

            EnvelopeHandler<V> outEnvelopeHandler = template.envelopeHandlers().outbound(serviceProxy.avroSerialization);

            ConsumerSettings<String, Envelope> consumerSettings = ConsumerSettings.apply(serviceProxy.system, new StringDeserializer(), envelopeDeserializer)
                    .withBootstrapServers(serviceProxy.brokerList)
                    .withGroupId(groupId)
                    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerConfig.getString("auto.offset.reset"));

            return i -> {

                AtomicReference<String> correlationId = new AtomicReference<>();

                CompletableFuture<MetadataEnvelope<Try<O>>> f = new CompletableFuture<>();

                Source.single(i)
                        .wireTap(m -> {
                            TemporalAmount timeout = Optional.ofNullable(m.getMetadata().get(MetadataEnvelope.TIMEOUT))
                                    .map(s -> (TemporalAmount) Duration.ofMillis(Long.parseLong(s)))
                                    .orElse(consumerConfig.getTemporal("timeout"));
                            Timer t = new Timer();
                            t.schedule(new TimerTask() {
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
                            correlationId.set(UUID.randomUUID().toString());
                            return m.withCorrelationId(correlationId.get());
                        }))
                        .map(m -> m.mapPayload(template.requestTransformations().toAvro()))
                        .map(e -> inEnvelopeHandler.envelope(e))
                        .to(KafkaSink.createSink(inTopic, serviceProxy.props, envelopeSerializer))
                        .run(serviceProxy.materializer);

                Consumer.plainSource(consumerSettings, Subscriptions.assignment(new TopicPartition(outTopic, 0)))
                        .map(r -> r.value())
                        .map(e -> outEnvelopeHandler.extractRecordWithMetadata(e))
                        .filter(m -> m.correlationId().map(j -> correlationId.get().equals(j)).orElse(false))
                        .map(m -> m.mapPayload(template.responseTransformations().fromAvro()))
                        .to(Sink.foreach(e -> {
                            e.getPayload()
                                    .onSuccess(v -> f.complete(e))
                                    .onFailure(t -> f.completeExceptionally(t));
                        }))
                        .run(serviceProxy.materializer);

                return f;
            };
        }
    }

}
