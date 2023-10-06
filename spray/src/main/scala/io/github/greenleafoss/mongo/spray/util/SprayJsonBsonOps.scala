package io.github.greenleafoss.mongo.spray.util

import io.github.greenleafoss.mongo.core.util.GreenLeafJsonBsonOps
import io.github.greenleafoss.mongo.core.util.GreenLeafJsonBsonOps.JsonBsonErr
import io.github.greenleafoss.mongo.core.util.ZonedDateTimeOps
import io.github.greenleafoss.mongo.core.util.ZonedDateTimeOps.*
import io.github.greenleafoss.mongo.spray.bson.SprayBsonProtocol

import org.mongodb.scala.bson.BsonArray
import org.mongodb.scala.bson.BsonBoolean
import org.mongodb.scala.bson.BsonDateTime
import org.mongodb.scala.bson.BsonDecimal128
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.bson.BsonDouble
import org.mongodb.scala.bson.BsonInt32
import org.mongodb.scala.bson.BsonInt64
import org.mongodb.scala.bson.BsonNull
import org.mongodb.scala.bson.BsonObjectId
import org.mongodb.scala.bson.BsonString
import org.mongodb.scala.bson.BsonValue
import org.mongodb.scala.bson.ObjectId

import org.bson.json.JsonMode
import org.bson.json.JsonWriterSettings

import java.time.ZonedDateTime
import java.util.Date

import scala.jdk.CollectionConverters.*
import scala.language.implicitConversions
import scala.util.chaining.*

import spray.json.*
import spray.json.DefaultJsonProtocol.*

trait SprayJsonBsonOps extends GreenLeafJsonBsonOps:

  // **************************************************
  // FORMATS
  // **************************************************

  override type JsonFormat[E] = spray.json.JsonFormat[E]
  override type Json          = spray.json.JsValue

  // **************************************************
  // JSON
  // **************************************************

  extension (string: String) override def parseJson: Json          = spray.json.JsonParser(string)
  extension [E: JsonFormat](e: E) override def convertToJson: Json = e.toJson
  extension (json: Json) override def convertTo[E: JsonFormat]: E  = json.convertTo[E]

  import SprayBsonProtocol.*
  import SprayBsonProtocol.given
  override protected def convertJsonToBson(json: Json): BsonValue = json match
    case JsObject(x) if x.contains($oid)           => BsonObjectId(json.convertTo[ObjectId])
    case JsObject(x) if x.contains($date)          => BsonDateTime(json.convertTo[ZonedDateTime].toEpochMilli)
    case JsObject(x) if x.contains($numberDecimal) => BsonDecimal128(json.convertTo[BigDecimal])
    case JsObject(x) if x.contains($numberDouble)  => BsonDouble(json.convertTo[Double])
    case JsObject(x) if x.contains($numberLong)    => BsonInt64(json.convertTo[Long])
    case JsObject(x) if x.contains($numberInt)     => BsonInt32(json.convertTo[Int])
    case JsObject(x)                               => BsonDocument(x.map { case (k, v) => k -> convertJsonToBson(v) })
    case JsArray(x)                                => BsonArray.fromIterable(x.map(convertJsonToBson))
    case JsBoolean(x)                              => BsonBoolean(x)
    case JsString(x)                               => BsonString(x)
    case JsNull                                    => BsonNull()
    case null                                      => BsonNull()
    case _                                         => throw JsonBsonErr(s"Unknown input in JSON to BSON: $json")

  // **************************************************
  // BSON
  // **************************************************

  override protected def convertBsonToJson(bson: BsonValue): Json = bson match
    case x: BsonDocument   => x.toJson(jws).parseJson
    case x: BsonArray      => JsArray(x.getValues.iterator().asScala.map(convertBsonToJson).toVector)
    case x: BsonDateTime   => x.getValue.asZonedDateTime().convertToJson
    case x: BsonString     => x.getValue.convertToJson
    case x: BsonBoolean    => x.getValue.convertToJson
    case x: BsonObjectId   => x.getValue.convertToJson
    case x: BsonInt32      => x.getValue.convertToJson
    case x: BsonInt64      => x.getValue.convertToJson
    case x: BsonDouble     => x.getValue.convertToJson
    case x: BsonDecimal128 => BigDecimal(x.decimal128Value().bigDecimalValue()).convertToJson
    case _: BsonNull       => JsNull
    case _                 => throw JsonBsonErr(s"Unknown input in BSON to JSON: $bson")

object SprayJsonBsonOps extends SprayJsonBsonOps
