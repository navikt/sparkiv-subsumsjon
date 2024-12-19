package no.nav.helse.sparkiv

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.github.navikt.tbd_libs.kafka.poll
import org.apache.kafka.common.errors.WakeupException
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
                        meldingRepository.lagreMelding(fødselsnummer, record.value())
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
}
