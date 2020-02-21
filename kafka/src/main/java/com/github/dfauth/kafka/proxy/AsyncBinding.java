package com.github.dfauth.kafka.proxy;

import akka.actor.ActorSystem;
import akka.kafka.ConsumerSettings;
import akka.kafka.Subscriptions;
import akka.kafka.javadsl.Consumer;
import akka.stream.Materializer;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import com.github.dfauth.authzn.avro.*;
import com.github.dfauth.authzn.utils.CloseableProcessor;
import com.github.dfauth.avro.authzn.Envelope;
import com.github.dfauth.kafka.KafkaSubscriber;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.vavr.control.Try;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Properties;


public class AsyncBinding<I,O,U extends SpecificRecord,V extends SpecificRecord> extends ServiceProxy<I,O,U,V> {

    private final String groupId;
    private final CloseableProcessor<MetadataEnvelope<I>, MetadataEnvelope<Try<O>>> processor;
    private Consumer.Control control;
    private KafkaSubscriber<Envelope> sink;

    public AsyncBinding(ActorSystem system, Materializer materializer, Properties props, String brokerList, AvroSerialization avroSerialization, String topic, TransformationTemplate<I,O,U,V> template, CloseableProcessor<MetadataEnvelope<I>, MetadataEnvelope<Try<O>>> processor, String groupId) {
        super(system, materializer, props, brokerList, avroSerialization, topic, template);
        this.processor = processor;
        this.groupId = groupId;
        bind(template);
    }

    public void close() {
        if(processor != null) {
            processor.close();
        }
        if(this.sink != null) {
            this.sink.close();
        }
        if(control != null) {
            this.control.shutdown();
        }
    }

    private <U extends SpecificRecord, V extends SpecificRecord> void bind(TransformationTemplate<I,O,U,V> template) {
        Config consumerConfig = ConfigFactory.load().getConfig("akka.kafka.consumer");

        SpecificRecordDeserializer<Envelope> envelopeDeserializer = avroSerialization.deserializer(Envelope.class);
        SpecificRecordSerializer<Envelope> envelopeSerializer = avroSerialization.serializer(Envelope.class);

        EnvelopeHandler<U> inEnvelopeHandler = template.envelopeHandlers().inbound(avroSerialization);

        EnvelopeHandler<V> outEnvelopeHandler = template.envelopeHandlers().outbound(avroSerialization);

        sink = new KafkaSubscriber<>(topic, props, envelopeSerializer);

        ConsumerSettings<String, Envelope> consumerSettings = ConsumerSettings.apply(system, new StringDeserializer(), envelopeDeserializer)
                .withBootstrapServers(brokerList)
                .withGroupId(groupId)
                .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, consumerConfig.getString("auto.offset.reset"));

        control = Consumer.plainSource(consumerSettings, Subscriptions.assignment(new TopicPartition(topic, 0)))
                .map(r -> r.value())
                .map(e -> inEnvelopeHandler.extractRecordWithMetadata(e))
                .filter(m -> m.isInbound())
                .map(m -> m.mapPayload(template.requestTransformations().fromAvro()))
                .to(Sink.fromSubscriber(processor))
                .run(materializer);

        Source.fromPublisher(processor)
                .map(m -> m.outbound())
                .map(m -> m.mapPayload(template.responseTransformations().toAvro()))
                .map(e -> outEnvelopeHandler.envelope(e))
                .to(Sink.fromSubscriber(sink))
                .run(materializer);

    }
}
