package com.github.dfauth.authzn.kafka;

import com.github.dfauth.authzn.Directive;
import com.github.dfauth.kafka.EmbeddedKafkaTest;
import org.testng.annotations.Test;

import java.util.stream.Stream;

import static com.github.dfauth.authzn.PrincipalType.ROLE;
import static com.github.dfauth.authzn.avro.Transformations.toAvro;

public class TestCase extends EmbeddedKafkaTest {

    @Test
    public void testIt() {
        withEmbeddedKafka(p -> {
                Stream.of(Directive.builder().withPrincipal(ROLE.of("user")).build()).map(toAvro);
                return null;
        });

    }
}
