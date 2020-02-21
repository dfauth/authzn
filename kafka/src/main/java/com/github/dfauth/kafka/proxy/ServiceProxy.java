package com.github.dfauth.kafka.proxy;

import akka.actor.ActorSystem;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;
import com.github.dfauth.authzn.avro.AvroSerialization;
import com.github.dfauth.authzn.avro.MetadataEnvelope;
import com.github.dfauth.authzn.utils.CloseableProcessor;
import io.vavr.control.Try;
import org.apache.avro.specific.SpecificRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class ServiceProxy<I,O,U extends SpecificRecord,V extends SpecificRecord> {

    private static final Logger logger = LoggerFactory.getLogger(ServiceProxy.class);

    protected final ActorSystem system;
    protected final Materializer materializer;
    protected final Properties props;
    protected final String brokerList;
    protected final AvroSerialization avroSerialization;
    protected final String topic;
    protected final TransformationTemplate<I, O, U, V> template;

    public ServiceProxy(ActorSystem system, Materializer materializer, Properties props, String brokerList, AvroSerialization avroSerialization, String topic, TransformationTemplate<I,O,U,V> template) {
        this.system = system;
        this.materializer = materializer;
        this.props = props;
        this.brokerList = brokerList;
        this.avroSerialization = avroSerialization;
        this.topic = topic;
        this.template = template;
    }

    public static ServiceProxy.Factory newFactory(ActorSystem system, ActorMaterializer materializer, Properties p, String brokerList, AvroSerialization avroSerialization) {
        return new Factory(system,materializer,p,brokerList,avroSerialization);
    }

    public AsyncBindingClient<I,O,U,V> createAsyncBindingClient() {
        return new AsyncBindingClient(this.system, this.materializer, this.props, this.brokerList, this.avroSerialization, topic, template);
    }

    public AsyncBinding<I,O,U,V> createAsyncBinding(CloseableProcessor<MetadataEnvelope<I>, MetadataEnvelope<Try<O>>> processor, String groupId) {
        return new AsyncBinding(this.system, this.materializer, this.props, this.brokerList, this.avroSerialization, topic, template, processor, groupId);
    }

    public static class Factory {

        private final ActorSystem system;
        private final Materializer materializer;
        private final Properties props;
        private final String brokerList;
        private final AvroSerialization avroSerialization;

        Factory(ActorSystem system, Materializer materializer, Properties props, String brokerList, AvroSerialization avroSerialization) {
            this.system = system;
            this.materializer = materializer;
            this.props = props;
            this.brokerList = brokerList;
            this.avroSerialization = avroSerialization;
        }

        public <I,O,U extends SpecificRecord,V extends SpecificRecord> ServiceProxy<I,O,U,V> createServiceProxy(String topic, TransformationTemplate<I,O,U,V> template) {
            return new ServiceProxy<>(system, materializer, props, brokerList, avroSerialization, topic, template);
        }
    }

}
