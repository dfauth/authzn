package com.github.dfauth.kafka;

import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.github.dfauth.authzn.avro.*;
import com.github.dfauth.avro.authzn.Envelope;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
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
    private String outTopic;
    private String inTopic;

    public ServiceProxy(ActorSystem system, Materializer materializer) {
        this.system = system;
        this.materializer = materializer;
    }

    public Client createClient(Properties props, String brokerList, String groupId, AvroSerialization avroSerialization, String inTopic, String outTopic) {
        this.props = props;
        this.brokerList = brokerList;
        this.avroSerialization = avroSerialization;
        this.inTopic = inTopic;
        this.outTopic = outTopic;
        return new Client(this, groupId);
    }

    public <I,O> ServiceProxy.Service createService(Processor<MetadataEnvelope<I>, MetadataEnvelope<O>> processor, Properties props, String brokerList, String groupId, AvroSerialization avroSerialization, String inTopic, String outTopic) {
        this.props = props;
        this.brokerList = brokerList;
        this.avroSerialization = avroSerialization;
        this.inTopic = inTopic;
        this.outTopic = outTopic;
        return new Service(this, groupId, processor);
    }

    public static class Service<I,O> {

        private final String groupId;
        private final ServiceProxy serviceProxy;
        private final Processor<MetadataEnvelope<I>, MetadataEnvelope<O>> processor;

        public Service(ServiceProxy serviceProxy, String groupId, Processor<MetadataEnvelope<I>, MetadataEnvelope<O>> processor) {
            this.serviceProxy = serviceProxy;
            this.groupId = groupId;
            this.processor = processor;
        }

        public <U extends SpecificRecord, V extends SpecificRecord> void bindToKafka(Template<I,O,U,V> template) {
            Config consumerConfig = ConfigFactory.load().getConfig("akka.kafka.consumer");

            SpecificRecordDeserializer<Envelope> envelopeDeserializer = serviceProxy.avroSerialization.deserializer(Envelope.class);
            SpecificRecordSerializer<Envelope> envelopeSerializer = serviceProxy.avroSerialization.serializer(Envelope.class);

            EnvelopeHandler<U> inEnvelopeHandler = template.envelopeHandlers().inbound(serviceProxy.avroSerialization); //EnvelopeHandler.of(serviceProxy.avroSerialization, com.github.dfauth.avro.authzn.LoginRequest.class);

            EnvelopeHandler<V> outEnvelopeHandler = template.envelopeHandlers().outbound(serviceProxy.avroSerialization); // EnvelopeHandler.of(serviceProxy.avroSerialization, com.github.dfauth.avro.authzn.LoginResponse.class);

            ConsumerSettings<String, Envelope> consumerSettings = ConsumerSettings.apply(serviceProxy.system, new StringDeserializer(), envelopeDeserializer)
                    .withBootstrapServers(serviceProxy.brokerList)
                    .withGroupId(groupId)
                    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerConfig.getString("auto.offset.reset"));

            Consumer.plainSource(consumerSettings, Subscriptions.assignment(new TopicPartition(serviceProxy.inTopic, 0)))
                    .map(r -> r.value())
                    .map(e -> inEnvelopeHandler.extractRecordWithMetadata(e))
                    .map(m -> m.mapPayload(template.requestTransformations().fromAvro()))
                    .to(Sink.fromSubscriber(processor))
                    .run(serviceProxy.materializer);

            Source.fromPublisher(processor)
                    .map(m -> m.mapPayload(template.responseTransformations().toAvro()))
                    .map(e -> outEnvelopeHandler.envelope(e))
                    .to(KafkaSink.createSink(serviceProxy.outTopic, serviceProxy.props, envelopeSerializer))
                    .run(serviceProxy.materializer);

        }
    }

    public static class Client<I,O> {

        private final String groupId;
        private final ServiceProxy serviceProxy;

        public Client(ServiceProxy serviceProxy, String groupId) {
            this.serviceProxy = serviceProxy;
            this.groupId = groupId;
        }

        public <U extends SpecificRecord, V extends SpecificRecord> Function<MetadataEnvelope<I>, CompletableFuture<MetadataEnvelope<O>>> asyncProxy(Template<I,O,U,V> template) {
            Config consumerConfig = ConfigFactory.load().getConfig("akka.kafka.consumer");

            SpecificRecordDeserializer<Envelope> envelopeDeserializer = serviceProxy.avroSerialization.deserializer(Envelope.class);
            SpecificRecordSerializer<Envelope> envelopeSerializer = serviceProxy.avroSerialization.serializer(Envelope.class);

            EnvelopeHandler<U> inEnvelopeHandler = template.envelopeHandlers().inbound(serviceProxy.avroSerialization); //.of(serviceProxy.avroSerialization,com.github.dfauth.avro.authzn.LoginRequest.class);

            EnvelopeHandler<V> outEnvelopeHandler = template.envelopeHandlers().outbound(serviceProxy.avroSerialization); //EnvelopeHandler.of(serviceProxy.avroSerialization, com.github.dfauth.avro.authzn.LoginResponse.class);

            ConsumerSettings<String, Envelope> consumerSettings = ConsumerSettings.apply(serviceProxy.system, new StringDeserializer(), envelopeDeserializer)
                    .withBootstrapServers(serviceProxy.brokerList)
                    .withGroupId(groupId)
                    .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerConfig.getString("auto.offset.reset"));

            return i -> {

                AtomicReference<String> correlationId = new AtomicReference<>();

                CompletableFuture<MetadataEnvelope<O>> f = new CompletableFuture<>();

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
                        .to(KafkaSink.createSink(serviceProxy.inTopic, serviceProxy.props, envelopeSerializer))
                        .run(serviceProxy.materializer);

                Consumer.plainSource(consumerSettings, Subscriptions.assignment(new TopicPartition(serviceProxy.outTopic, 0)))
                        .map(r -> r.value())
                        .map(e -> outEnvelopeHandler.extractRecordWithMetadata(e))
                        .filter(m -> m.correlationId().map(j -> correlationId.get().equals(j)).orElse(false))
                        .map(m -> m.mapPayload(template.responseTransformations().fromAvro()))
                        .to(Sink.foreach(r -> {
                            f.complete(r);
                        }))
                        .run(serviceProxy.materializer);

                return f;
            };
        }
    }

    public interface Template<I,O,U extends SpecificRecord, V extends SpecificRecord> {

        EnvelopeHandlers<U,V> envelopeHandlers();

        RequestTransformations<I,U> requestTransformations();

        ResponseTransformations<V,O> responseTransformations();
    }

    public interface ResponseTransformations<V extends SpecificRecord,O> {

        Function<V,O> fromAvro();

        Function<O,V> toAvro();
    }

    public interface RequestTransformations<I,U extends SpecificRecord> {

        Function<I,U> toAvro();

        Function<U,I> fromAvro();
    }

    public interface EnvelopeHandlers<U extends SpecificRecord, V extends SpecificRecord> {

        EnvelopeHandler<U> inbound(AvroSerialization avroSerialization);

        EnvelopeHandler<V> outbound(AvroSerialization avroSerialization);
    }
}
