package no.nav.helse.sparkiv

import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.github.navikt.tbd_libs.kafka.poll
import org.apache.kafka.common.errors.WakeupException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class KafkaConsumer(
    groupId: String,
    properties: Properties,
    factory: ConsumerProducerFactory
) {
    private val running = AtomicBoolean(false)
    private val consumer = factory.createConsumer(groupId, properties)

    fun consume() {
        consumer.use {
            consumer.subscribe(listOf("flex.omrade-helse-etterlevelse"))

            try {
                consumer.poll(running::get) { records ->
                    records.forEach { record ->
                        // gjør noe greier med meldingen
                    }
                }
            } catch (err: WakeupException) {
                logger.info("Exiting consumer after ${if (!running.get()) "receiving shutdown signal" else "being interrupted by someone" }")
            }
        }
    }

    fun stop() {
        if (!running.getAndSet(false)) return logger.info("Already in process of shutting down")
        logger.info("Received shutdown signal. Waiting 10 seconds for app to shutdown gracefully")
        consumer.wakeup()
    }
}
