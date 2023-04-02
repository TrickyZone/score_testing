package com.knoldus.common

import pureconfig.ConfigSource
import pureconfig.generic.auto._


case class DatabaseConfig(driver: String, url: String, user: String, password: String)

case class Config(host: String, port : Int, dbConfig : DatabaseConfig, scoreConfig : ScoreConfig,
                  deliverableScoreConfig: DeliverableScoreConfig,
                  rabbitMqConfig: RabbitMqConfig, swaggerConf: SwaggerConf)

case class ScoreConfig(scorePerBlog: Int, scorePerWebinar: Int, scorePerKnolx: Int, scorePerTechhub: Int,
                       scorePerOsContribution: Int, scorePerConference: Int, scorePerBook: Int,
                       scorePerResearchPaper: Int, scorePerMeetup: Int, scorePerPodcast : Int, scorePerPmoTemplate: Int,
                       scorePerProcessedDocument: Int, scorePerProposal: Int, scorePerCertification: Int,
                       scoreKnolxAttendee: Int, scoreKnolxSpotlightMember: Int, scorePerOther : Int)

case class DeliverableScoreConfig(scorePerVideos: Int, scorePerPresentation: Int, scorePerDocument: Int, scorePerPodcast: Int)

case class RabbitMqConfig(host: String, port: Int, queue: String, exchange: String,
                          user: Option[String], password : Option[String])

case class SwaggerConf(url: String, scheme : String)

object Configuration {

  val serviceConf: Config = ConfigSource.default.load[Config] match {
    case Right(conf) => conf
    case Left(error) => throw new Exception(error.toString())
  }

}
