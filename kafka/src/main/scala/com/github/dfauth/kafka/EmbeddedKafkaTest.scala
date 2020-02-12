package com.github.dfauth.kafka

import java.util.Properties

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import net.manub.embeddedkafka.{EmbeddedKafka, EmbeddedKafkaConfig}

class EmbeddedKafkaTest extends EmbeddedKafka with LazyLogging {

  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()

  def withEmbeddedKafka[T](f: Properties => T):Unit = {
    withEmbeddedKafka(f, 9092, 2181)
  }

  def withEmbeddedKafka[T](f: Properties => T, kafkaPort:Int, zooKeeperPort:Int):Unit = {
    try {
      withRunningKafkaOnFoundPort(EmbeddedKafkaConfig(kafkaPort, zooKeeperPort)) { implicit config =>
        f(connectionProperties(config))
      }
    } finally {
      EmbeddedKafka.stop()
    }
  }

  def connectionProperties(config: EmbeddedKafkaConfig):Properties = {
    val props = new Properties()
    props.setProperty("zookeeperConnectString", s"localhost:${config.zooKeeperPort}")
    props.setProperty("bootstrap.servers", s"localhost:${config.kafkaPort}")
    props
  }
}
