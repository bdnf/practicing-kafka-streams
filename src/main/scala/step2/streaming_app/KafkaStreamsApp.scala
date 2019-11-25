package step2.streaming_app

import java.time.Duration
import java.util.Properties

import models.{Purchase, PurchasePattern, RewardAccumulator}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.kstream.{ForeachAction, KeyValueMapper, Produced}
import org.apache.kafka.streams.{KafkaStreams, StreamsConfig}
import org.apache.kafka.streams.processor.WallclockTimestampExtractor
import org.apache.kafka.streams.scala.{Serdes, StreamsBuilder}
import org.apache.kafka.streams.scala.kstream.{Consumed, KStream}
import utils.DB
import utils.SerDes._

class KafkaStreamsApp {

  val config: Properties = {
    val p = new Properties()
    p.put(StreamsConfig.CLIENT_ID_CONFIG, "First-Kafka-Streams-Client")
    p.put(ConsumerConfig.GROUP_ID_CONFIG, "purchases")
    p.put(StreamsConfig.APPLICATION_ID_CONFIG, "First-Kafka-Streams-App")
    val bootstrapServers = "localhost:9092"
    p.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers)
    //p.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, 1)
    p.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, classOf[WallclockTimestampExtractor])
    p
  }

  val purchaseSerde: Serde[Purchase] = Serdes.fromFn(
    p => serialize(p),
    arr => Some(deserialize[Purchase](arr))
  )

  val purchasePatternSerde: Serde[PurchasePattern] = Serdes.fromFn(
    p => serialize(p),
    arr => Some(deserialize[PurchasePattern](arr))
  )

  val rewardAccumulatorSerde: Serde[RewardAccumulator] = Serdes.fromFn(
    p => serialize(p),
    arr => Some(deserialize[RewardAccumulator](arr))
  )

  implicit val consumedPurchase: Consumed[String,Purchase] = Consumed.`with`(Serdes.String, purchaseSerde)
  implicit val producedPurchase: Produced[String, Purchase] = Produced.`with`(Serdes.String, purchaseSerde)
  implicit val purchases: Produced[Long, Purchase] = Produced.`with`(Serdes.Long, purchaseSerde)

  implicit val producedPatternPurchase: Produced[String, PurchasePattern] = Produced.`with`(Serdes.String, purchasePatternSerde)
  implicit val rewardPurchase: Produced[String, RewardAccumulator] = Produced.`with`(Serdes.String, rewardAccumulatorSerde)

  val builder = new StreamsBuilder()

  //input topic
  val purchaseKStream: KStream[String, Purchase] = builder.stream[String, Purchase]("transactions")
    .mapValues(p => p.maskCreditCard)
  purchaseKStream.to("purchases")

  val purchasePatternKStream = purchaseKStream.mapValues(p => PurchasePattern(p))
  purchasePatternKStream.to("patterns")

  val rewardAccumulator: KStream[String, RewardAccumulator] = purchaseKStream.mapValues(p => RewardAccumulator(p))
  rewardAccumulator.to("rewards")

  // More involved actions
  val purchaseDateAsKey: (String, Purchase) => Long = (key: String, purchase: Purchase) => purchase.purchaseDate
  val filteredKStream = purchaseKStream.filter((key: String, purchase: Purchase) => purchase.price > 5.00).selectKey(purchaseDateAsKey)
  filteredKStream.to("purchases")
  // branching stream for separating out purchases in new departments to their own topics
  val isCoffee = (key: String, purchase: Purchase) => purchase.department.contains("coffee")
  val isElectronics = (key: String, purchase: Purchase) => purchase.department.contains("electronics")

  val branchedStream: Array[KStream[String, Purchase]] = purchaseKStream.branch(isCoffee, isElectronics)
  val coffee = 0
  val electronics = 1
  branchedStream(coffee).to("coffee_transactions")
  branchedStream(electronics).to("electronics_ransactions")

  // filter into DB
  val purchaseForeachAction = (key: String, purchase: Purchase) => DB.save(purchase.purchaseDate, purchase.employeeId, purchase.itemPurchased)
  purchaseKStream.filter((key: String, purchase: Purchase) => purchase.employeeId.equals("000000")).foreach(purchaseForeachAction)
  // End more involved actions

  val streams: KafkaStreams = new KafkaStreams(builder.build(), config)
  streams.cleanUp()

  streams.start()

  sys.ShutdownHookThread {
    streams.close(Duration.ofSeconds(10))
  }

}
