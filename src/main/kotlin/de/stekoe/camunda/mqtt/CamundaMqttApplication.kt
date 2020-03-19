package de.stekoe.camunda.mqtt

import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttException
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import java.util.*

@SpringBootApplication
class CamundaMqttApplication {
    @Value("\${mqtt.broker}")
    private val broker: String? = null

    @Bean(destroyMethod = "disconnect")
    fun mqttClient(): MqttClient? {
        var client: MqttClient? = null
        try {
            client = MqttClient(broker, UUID.randomUUID().toString(), MemoryPersistence())
            val connOpts = MqttConnectOptions()
            connOpts.isCleanSession = true
            client.connect(connOpts)
            logger().info("Connected to broker: {}...", broker)
        } catch (e: MqttException) {
            logger().error("Error establishing connection", e)
        }
        return client
    }
}

fun main(args: Array<String>) {
    runApplication<CamundaMqttApplication>(*args)
}

inline fun <reified T : Any> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)