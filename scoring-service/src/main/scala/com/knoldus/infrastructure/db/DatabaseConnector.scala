package com.knoldus.infrastructure.db

import cats.effect.IO
import com.knoldus.common.DatabaseConfig
import doobie.util.transactor.Transactor
import doobie.util.transactor.Transactor.Aux

class DatabaseConnector {

  def getTransactor(cfg: DatabaseConfig): Aux[IO, Unit] =
    Transactor.fromDriverManager[IO](
      cfg.driver,
      cfg.url,
      cfg.user,
      cfg.password
    )

}
