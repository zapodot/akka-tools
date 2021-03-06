package no.nextgentel.oss.akkatools.serializing

import java.io.IOException

import akka.serialization.Serializer
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.{SerializationFeature, ObjectMapper}
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import no.nextgentel.oss.akkatools.serializing.JacksonJsonSerializer._
import org.slf4j.LoggerFactory

object JacksonJsonSerializer {

  private var _objectMapper:Option[ObjectMapper] = None

  // Should only be used during testing
  // When true, all objects being serialized are also deserialized and compared
  private var verifySerialization: Boolean = false

  def init(m:ObjectMapper, verifySerialization:Boolean = false): Unit = {
    _objectMapper = Some(configureObjectMapper(m.copy()))
    this.verifySerialization = verifySerialization
    if (verifySerialization) {
      val logger = LoggerFactory.getLogger(getClass)
      logger.warn("*** Performance-warning: All objects being serialized are also deserialized and compared. Should only be used during testing")
    }
  }

  def configureObjectMapper(mapper:ObjectMapper):ObjectMapper = {
    mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
    mapper.registerModule(new DefaultScalaModule)
    mapper
  }

  protected def objectMapper():ObjectMapper = {
    _objectMapper.getOrElse(throw new Exception(getClass().toString + " has not been with an initialized with an objectMapper. You must call init(objectMapper) before using the serializer"))
  }
}

class JacksonJsonSerializerVerificationFailed(errorMsg:String) extends RuntimeException(errorMsg)

class JacksonJsonSerializer extends Serializer {
  val logger = LoggerFactory.getLogger(getClass)
  import JacksonJsonSerializer._



  // The serializer id has to have this exact value to be equal to the old original implementation
  override def identifier: Int = 67567521

  override def includeManifest: Boolean = true

  override def fromBinary(bytes: Array[Byte], manifest: Option[Class[_]]): AnyRef = {
    val clazz:Class[_] = manifest.get
    if (logger.isDebugEnabled) logger.debug("fromBinaryJava: " + clazz)

    val o = objectMapper().readValue(bytes, clazz).asInstanceOf[AnyRef]

    o match {
      case d:DepricatedTypeWithMigrationInfo =>
        val m = d.convertToMigratedType()
        if (logger.isDebugEnabled) logger.debug("fromBinaryJava: " + clazz + " was migrated to " + m.getClass)
        m
      case d:JacksonJsonSerializableButNotDeserializable =>
        throw new Exception("The type " + o.getClass + " is not supposed to be deserializable since it extends JacksonJsonSerializableButNotDeserializable")
      case x:AnyRef => x
    }
  }

  override def toBinary(o: AnyRef): Array[Byte] = {
    if (logger.isDebugEnabled) {
      logger.debug("toBinary: " + o.getClass)
    }
    val bytes: Array[Byte] = objectMapper().writeValueAsBytes(o)
    if (verifySerialization) {
      doVerifySerialization(o, bytes)
    }
    return bytes
  }

  private def doVerifySerialization(originalObject: AnyRef, bytes: Array[Byte]):Unit = {
    if (originalObject.isInstanceOf[JacksonJsonSerializableButNotDeserializable]) {
      if (logger.isDebugEnabled) {
        logger.debug("Skipping doVerifySerialization: " + originalObject.getClass)
      }
      return ;
    }
    if (logger.isDebugEnabled) {
      logger.debug("doVerifySerialization: " + originalObject.getClass)
    }
    val deserializedObject: AnyRef = fromBinary(bytes, originalObject.getClass)
    if (!(originalObject == deserializedObject)) {
      throw new JacksonJsonSerializerVerificationFailed("Serialization-verification failed.\n" + "original:     " + originalObject.toString + "\n" + "deserialized: " + deserializedObject.toString)
    }
  }
}
