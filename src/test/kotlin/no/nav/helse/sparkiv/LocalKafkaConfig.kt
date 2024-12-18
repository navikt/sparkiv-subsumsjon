package no.nav.helse.sparkiv

import com.github.navikt.tbd_libs.kafka.Config
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import java.util.*

internal class LocalKafkaConfig(private val connectionProperties: Properties) : Config {
    override fun producerConfig(properties: Properties) = properties.apply {
        putAll(connectionProperties)
        put(ProducerConfig.ACKS_CONFIG, "all")
        put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, "1")
        put(ProducerConfig.LINGER_MS_CONFIG, "0")
        put(ProducerConfig.RETRIES_CONFIG, "0")
    }

    override fun consumerConfig(groupId: String, properties: Properties) = properties.apply {
        putAll(connectionProperties)
        put(ConsumerConfig.GROUP_ID_CONFIG, groupId)
        put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")
    }

    override fun adminConfig(properties: Properties) = properties.apply {
        putAll(connectionProperties)
    }
}

