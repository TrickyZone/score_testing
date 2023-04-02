package com.knoldus.infrastructure.messaging

import akka.actor.Actor
import com.knoldus.resources.JsonSupport
import com.knoldus.service.scoring.ScoringService
import com.rabbitmq.client.{AMQP, Channel, DefaultConsumer, Envelope}
import com.typesafe.scalalogging.LazyLogging
import spray.json._

class ContributionConsumerActor(scoringService: ScoringService, channel: Channel, queue: String) extends Actor with JsonSupport with LazyLogging {


  // called on the initial run
  def receive: Receive = {
    case _ => startReceving
  }

  /**
   * Receives the API messages to store.
   *
   * @return response in the form of string.
   */
  def startReceving: String = {

    val consumer = new DefaultConsumer(channel) {
      override def handleDelivery(
                                   consumerTag: String,
                                   envelope: Envelope,
                                   properties: AMQP.BasicProperties,
                                   body: Array[Byte]): Unit = {

        val message = new String(body, "UTF-8")
        try {
          val jsValue: JsValue = message.parseJson
          val contribution = updateContributionFormat.read(jsValue)
          scoringService.calculateScore(contribution.contribution)(contribution.tenantId)
          logger.info(s"Consuming Message $message  = $contribution")
         // process the message
          // send the message to the provided callback function
          // and execute this in a subactor
        } catch {
          case exception: Exception => logger.error(exception.getMessage, exception)
        }
      }
    }
    channel.basicQos(100)
    channel.basicConsume(queue, true, consumer)
  }

}

