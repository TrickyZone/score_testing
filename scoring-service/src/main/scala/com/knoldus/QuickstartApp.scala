package com.knoldus


import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.knoldus.common.Configuration
import com.knoldus.infrastructure.db.DatabaseConnector
import com.knoldus.infrastructure.messaging.{ContributionConsumerActor, RabbitMQConnection}
import com.knoldus.resources.RestInterface
import com.knoldus.utils.FlywayService
import com.rabbitmq.client.Channel

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps

//#main-class
object QuickstartApp extends App with RestInterface {

  //#start-http-server
  implicit val system: ActorSystem =  ActorSystem("ScoringService")
  implicit val executionContext = system.dispatcher
  implicit val materializer = ActorMaterializer()

  implicit val timeout = Timeout(10 seconds)

  val config = Configuration.serviceConf
  val api: Route = routes
  val port = config.port
  val host = config.host

  // Flyway Service for DB Initialization
  val flyWayService = new FlywayService(config.dbConfig)
  flyWayService.migrateDatabaseSchema()
  system.log.info("In the Start HTTP Server Method")

  Http().newServerAt(host,port).bind(api).map{
    binding =>
      println(s"REST interface bound to ${binding.localAddress}")
  } recover { case ex =>
    println(s"REST interface could not bind to $host:$port", ex.getMessage)
  }
  val queue = Configuration.serviceConf.rabbitMqConfig.queue

  val listeningChannel = rabbitMQConnection.createChannel();
  // make sure the queue exists we want to send to
  listeningChannel.queueDeclare(queue, false, false, false, null);

  setupListener(rabbitMQConnection.createChannel(), queue)

  override implicit def db: DatabaseConnector = new DatabaseConnector

  override implicit def rabbitMq: RabbitMQConnection = new RabbitMQConnection


  private def setupListener(receivingChannel: Channel, queue: String): Unit = {
    system.scheduler.scheduleOnce(2 seconds,
      system.actorOf(Props(new ContributionConsumerActor(scoringService, receivingChannel, queue))), "");
  }


}


//#main-class
