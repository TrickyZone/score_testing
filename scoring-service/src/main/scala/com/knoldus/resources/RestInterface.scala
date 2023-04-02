package com.knoldus.resources

import cats.effect.IO
import com.knoldus.common.{Configuration, DeliverableScoreConfig, ScoreConfig}
import com.knoldus.infrastructure.db.DatabaseConnector
import com.knoldus.infrastructure.messaging.RabbitMQConnection
import com.knoldus.service.PessoaService
import com.knoldus.service.scoring.ScoringServiceImpl
import com.knoldus.swagger.SwaggerDocService
import doobie.util.transactor.Transactor

import scala.concurrent.ExecutionContext

trait RestInterface extends Resources {

  implicit def executionContext: ExecutionContext
  implicit def db: DatabaseConnector
  implicit def rabbitMq: RabbitMQConnection
  implicit def xa: Transactor[IO] = db.getTransactor(Configuration.serviceConf.dbConfig)
  implicit def rabbitMQConnection = rabbitMq.getConnection(Configuration.serviceConf.rabbitMqConfig)
  implicit def scoringConfig : ScoreConfig = Configuration.serviceConf.scoreConfig

  implicit def deliverabletypescoringConfig : DeliverableScoreConfig = Configuration.serviceConf.deliverableScoreConfig

  lazy val pessoaService = new PessoaService()

  lazy val scoringService = new ScoringServiceImpl()

  val routes = pessoaRoutes ~ scoringRoutes ~ SwaggerDocService.routes

}

trait Resources extends PessoaResource with ContributionResource // with SwaggerSite