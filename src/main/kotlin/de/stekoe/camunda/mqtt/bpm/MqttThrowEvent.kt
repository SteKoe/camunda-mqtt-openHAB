package de.stekoe.camunda.mqtt.bpm

import de.stekoe.camunda.mqtt.logger
import de.stekoe.camunda.mqtt.service.MqttService
import org.camunda.bpm.engine.delegate.DelegateExecution
import org.camunda.bpm.engine.delegate.JavaDelegate
import org.camunda.bpm.model.bpmn.instance.Message
import org.camunda.bpm.model.bpmn.instance.MessageEventDefinition
import org.camunda.bpm.model.bpmn.instance.ThrowEvent
import org.springframework.stereotype.Component

@Component
class MqttThrowEvent(private val mqttService: MqttService) : JavaDelegate {
    override fun execute(execution: DelegateExecution) {
        val topic = getTopic(execution)
        if (topic == null) {
            logger().error("Not throwing any event in {}. Could not determine topic.", execution.currentActivityName)
            return
        }

        getMqttMessagePayload(execution)?.let { message ->
            logger().info("Throwing event topic {} with value {}", topic, message)
            mqttService.sendMessage(topic, message)
        }
    }

    private fun getTopic(execution: DelegateExecution): String? {
        return getBpmnMessageName(execution)?.split(":command:".toRegex())?.get(0)
    }

    private fun getMqttMessagePayload(execution: DelegateExecution): String? {
        return getBpmnMessageName(execution)?.split(":command:".toRegex())?.last()
    }

    private fun getBpmnMessage(execution: DelegateExecution): Message {
        val event = execution.bpmnModelElementInstance as ThrowEvent
        val definition = event.eventDefinitions.iterator().next() as MessageEventDefinition
        return definition.message
    }

    private fun getBpmnMessageName(execution: DelegateExecution): String? {
        return try {
            val bpmnMessage = getBpmnMessage(execution)
            bpmnMessage.name
        } catch (e: Exception) {
            logger().info("Cannot get the Message Name.", e)
            null
        }
    }
}