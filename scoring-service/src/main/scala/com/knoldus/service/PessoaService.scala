package com.knoldus.service

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.knoldus.model.Pessoa
import com.knoldus.statement.Table
import doobie._
import doobie.implicits._
import doobie.util.query.Query0
import doobie.util.transactor.Transactor

import scala.concurrent.{ExecutionContext, Future}

class PessoaService(implicit xa: Transactor[IO], ec: ExecutionContext) extends Table[Pessoa] with Service[Long, Pessoa] {

  override val tableName: String = "pessoa"

  override def select(pk: Long): Future[Option[Either[String, Pessoa]]] = {
    for {
      result <- Query0[Pessoa](selectEntitySQL(pk)).unique.transact(xa).attemptSomeSqlState {
        case _ => "Pessoa Selected"
      }.unsafeToFuture()
    } yield Option(result)
  }

  override def list: Future[Option[Either[List[Pessoa], List[Pessoa]]]] = {
    for {
      result <- Query0[Pessoa](listEntitySQL).to[List].transact(xa).attemptSomeSqlState {
        case _ => List.empty[Pessoa]
      }.unsafeToFuture()
    } yield Option(result)
  }

  override def upsert(e: Pessoa): Future[Int] = {
    Update[Pessoa](insertEntitySQL).run(e).transact(xa).unsafeToFuture()
  }

  override def remove(pk: Long): Future[Int] = {
    Query0[Pessoa](removeEntitySQL(pk)).toFragment.update.run.transact(xa).unsafeToFuture()
  }
}
