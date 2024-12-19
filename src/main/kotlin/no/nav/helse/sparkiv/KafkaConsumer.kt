package no.nav.helse.sparkiv

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.github.navikt.tbd_libs.kafka.poll
import org.apache.kafka.common.errors.WakeupException
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeParseException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class KafkaConsumer(
    groupId: String,
    private val topic: String,
    properties: Properties,
    factory: ConsumerProducerFactory,
) {
    private val running: AtomicBoolean = AtomicBoolean(false)
    private val consumer = factory.createConsumer(groupId, properties)

    fun consume(meldingRepository: MeldingRepository) {
        running.set(true)
        consumer.use {
            consumer.subscribe(listOf(topic))
            logger.info("Consuming messages")

            try {
                consumer.poll(running::get) { records ->
                    records.forEach { record ->
                        val jsonNode = jacksonObjectMapper().readTree(record.value())
                        val fødselsnummer = jsonNode["fodselsnummer"]?.asText()
                        val id = jsonNode["id"]?.asUuid()
                        val eventName = jsonNode["eventName"]?.asText()
                        val tidsstempel = jsonNode["tidsstempel"]?.asZonedDateTime()
                        if (fødselsnummer == null || id == null || eventName == null || tidsstempel == null) {
                            return@forEach meldingRepository.lagreMangelfullMelding(record.partition(), record.offset(), record.value())
                        }
                        meldingRepository.lagreMelding(fødselsnummer, id, tidsstempel, eventName, record.value())
                    }
                }
            } catch (err: WakeupException) {
                logger.info("Exiting consumer after ${if (!running.get()) "receiving stop signal" else "being interrupted" }")
            }
        }
    }

    fun stop() {
        if (!running.getAndSet(false)) return logger.info("Already in process of stopping consumer.")
        logger.info("Received stop signal. Stopping consumer.")
        consumer.wakeup()
    }

    private fun JsonNode.asZonedDateTime() = try {
        ZonedDateTime.parse(asText())
    } catch (err: DateTimeParseException) {
        LocalDateTime.parse(asText()).atZone(ZoneId.systemDefault())
    }
    private fun JsonNode.asUuid() = UUID.fromString(asText())
}
