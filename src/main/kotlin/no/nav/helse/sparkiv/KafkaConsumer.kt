package no.nav.helse.sparkiv

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.github.navikt.tbd_libs.kafka.poll
import org.apache.kafka.common.errors.WakeupException
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class KafkaConsumer(
    groupId: String,
    private val topic: String,
    properties: Properties,
    factory: ConsumerProducerFactory
) {
    private val running = AtomicBoolean(false)
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
                        val fødselsnummer = jsonNode["fodselsnummer"]?.asText() ?: return@forEach
                        val id = jsonNode["id"].asUuid()
                        val eventName = jsonNode["event_name"]?.asText() ?: "ukjent"
                        val tidsstempel = jsonNode["tidsstempel"].asLocalDateTime()
                        meldingRepository.lagreMelding(fødselsnummer, id, tidsstempel, eventName, record.value())
                    }
                }
            } catch (err: WakeupException) {
                logger.info("Exiting consumer after ${if (!running.get()) "receiving shutdown signal" else "being interrupted" }")
            }
        }
    }

    fun stop() {
        if (!running.getAndSet(false)) return logger.info("Already in process of shutting down")
        logger.info("Received shutdown signal. Waiting 10 seconds for app to shutdown gracefully")
        consumer.wakeup()
    }

    private fun JsonNode.asLocalDateTime() = LocalDateTime.parse(asText())
    private fun JsonNode.asUuid() = UUID.fromString(asText())
}
