package com.knoldus.service

import com.knoldus.QuickstartApp.xa
import com.knoldus.dbTest.TestEmbeddedPostgres
import com.knoldus.model.Pessoa
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class PessoaServiceTest extends AnyFlatSpec with Matchers with TestEmbeddedPostgres {

  val pessoa = Pessoa(1L, "Bhavya", "bhavya@knoldus.com")
  val pessoaLong = 1L

  it should "select Pessoa" in {
    val executorService = Executors.newFixedThreadPool(1)
    implicit val ec = ExecutionContext.fromExecutor(executorService)
    val pessoaService = new PessoaService
    pessoaService.select(pessoaLong).onComplete {
      case Success(result) =>
        logger.error(s"########################## select Pessoa test x = $result")
        result shouldBe Some(Left("Pessoa Selected"))
      case Failure(exception) =>
        logger.error(s"########################## select Pessoa test failed = $exception")
        logger.error(exception.getMessage)
    }
    Thread.sleep(3000)
  }

  it should "list Pessoa" in {
    val executorService = Executors.newFixedThreadPool(1)
    implicit val ec = ExecutionContext.fromExecutor(executorService)
    val pessoaService = new PessoaService
    pessoaService.list.onComplete {
      case Success(result) =>
        logger.error(s"########################## list Pessoa test x = $result")
        result shouldBe Some(Left(pessoa))
      case Failure(exception) =>
        logger.error(s"########################## list Pessoa test failed = $exception")
        logger.error(exception.getMessage)
    }
    Thread.sleep(3000)
  }

  it should "upsert Pessoa" in {
    val executorService = Executors.newFixedThreadPool(1)
    implicit val ec = ExecutionContext.fromExecutor(executorService)
    val pessoaService = new PessoaService
    pessoaService.upsert(pessoa).onComplete {
      case Success(result) =>
        logger.error(s"########################## upsert Pessoa test x = $result")
        result shouldBe 1
      case Failure(exception) =>
        logger.error(s"########################## upsert Pessoa test failed = $exception")
        logger.error(exception.getMessage)
    }
    Thread.sleep(3000)
  }

  it should "Remove Pessoa" in {
    val executorService = Executors.newFixedThreadPool(1)
    implicit val ec = ExecutionContext.fromExecutor(executorService)
    val pessoaService = new PessoaService
    pessoaService.remove(pessoaLong).onComplete {
      case Success(result) =>
        logger.error(s"########################## Remove Pessoa test x = $result")
        result shouldBe 1
      case Failure(exception) =>
        logger.error(s"########################## Remove Pessoa test failed = $exception")
        logger.error(exception.getMessage)
    }
    Thread.sleep(3000)
  }
}
