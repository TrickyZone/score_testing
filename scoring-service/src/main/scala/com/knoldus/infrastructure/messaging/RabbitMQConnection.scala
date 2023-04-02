package com.knoldus.infrastructure.messaging

import com.knoldus.common.RabbitMqConfig
import com.rabbitmq.client.{Connection, ConnectionFactory}

class RabbitMQConnection {

  private val connection: Connection = null;

  /**
   * Return a connection if one doesn't exist. Else create
   * @param rabbitMQConfig to connect with rabbit MQ server.
   *
   * @return connection if credential is valid.
   */

  def getConnection(rabbitMQConfig : RabbitMqConfig): Connection = {
    connection match {
      case null => {
        val factory = new ConnectionFactory()
        factory.setHost(rabbitMQConfig.host)
        factory.setPort(rabbitMQConfig.port);
        rabbitMQConfig.user map  {
          username => factory.setUsername(username)
        }
        rabbitMQConfig.password.map {
          password => factory.setPassword(password)
        }
        factory.newConnection();
      }
      case _ => connection
    }
  }

}

