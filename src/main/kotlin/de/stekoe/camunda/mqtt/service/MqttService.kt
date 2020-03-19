package de.stekoe.camunda.mqtt.service

import de.stekoe.camunda.mqtt.logger
import org.camunda.bpm.engine.ProcessEngineException
import org.camunda.bpm.engine.RuntimeService
import org.eclipse.paho.client.mqttv3.*
import org.springframework.stereotype.Service
import java.util.*
import javax.annotation.PostConstruct

const val PAYLOAD_NAME_PATTERN = "%s_payload"
const val TOPIC_NAME_PATTERN = "%s_topic"

@Service
class MqttService(private val mqttClient: MqttClient, private val runtimeService: RuntimeService) : MqttCallback {
    @PostConstruct
    fun init() {
        mqttClient.setCallback(this)

        if (mqttClient.isConnected) {
            subscribeAllTopics()
        }
    }

    private fun subscribeAllTopics() {
        try {
            mqttClient.subscribe("#", 1)
        } catch (e: MqttException) {
            logger().error("Error subscribing topics", e)
        }
    }

    override fun messageArrived(topic: String, message: MqttMessage) {
        logger().info("Message arrived at '{}' with payload {}", topic, message)

        val values: MutableMap<String, Any> = HashMap()
        values[String.format(PAYLOAD_NAME_PATTERN, topic.replace("/", "_"))] = String(message.payload)
        values[String.format(TOPIC_NAME_PATTERN, topic.replace("/", "_"))] = topic

        try {
            runtimeService.signalEventReceived(topic, values)
        } catch (e: ProcessEngineException) {
            logger().warn("Signal processing failed due to: ${e.message}")
        }

        logger().info("Throwing BPMN signal '{}' width value {}", topic, values)
    }

    fun sendMessage(topic: String?, content: String) {
        logger().trace("Publishing message to topic {}: {}", topic, content)
        val message = MqttMessage(content.toByteArray())
        message.qos = 1
        try {
            mqttClient.publish(topic, message)
        } catch (e: MqttException) {
            logger().error("Error sending message", e)
        }
    }

    override fun deliveryComplete(iMqttDeliveryToken: IMqttDeliveryToken) {
        logger().info("MqttSevice deliveryComplete.")
    }

    override fun connectionLost(throwable: Throwable) {
        logger().error("MqttSevice lost connection.", throwable)
    }
}