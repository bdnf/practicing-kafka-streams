package utils

import java.time.Instant
import java.util.{Calendar, Date}

import com.sksamuel.avro4s.AvroSchema
import models.Purchase


object AvroSchemaGenertor extends App {
  val purchaseSchema = AvroSchema[Purchase]

  println(purchaseSchema)
}
