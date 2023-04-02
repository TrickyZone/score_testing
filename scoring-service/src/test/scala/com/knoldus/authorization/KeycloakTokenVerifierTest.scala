package com.knoldus.authorization

import com.knoldus.dbTest.TestEmbeddedPostgres
import org.keycloak.adapters.KeycloakDeployment
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.security.PublicKey
import java.util.concurrent.{ExecutorService, Executors}
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}
import scala.util.{Failure, Success}

class KeycloakTokenVerifierTest extends AnyFlatSpec with Matchers with TestEmbeddedPostgres {

  val keycloakDeployment = new KeycloakDeployment
  val keycloakTokenVerifier = new KeycloakTokenVerifier(keycloakDeployment)
  val executorService: ExecutorService = Executors.newFixedThreadPool(1)
  implicit val ec: ExecutionContextExecutor = ExecutionContext.fromExecutor(executorService)

  it should "verify token" in {
    val token = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJhRWZOOVNPeWlrOWQ3RGtnQ21Qd3JScl95WHB5NjlMQ0pENUs5aGNqS1JNIn0.eyJleHAiOjE2NDkwNTMwNzYsImlhdCI6MTY0OTA1Mjc3NiwiYXV0aF90aW1lIjoxNjQ5MDUyNzc1LCJqdGkiOiI2NDk0ZjIwMS00M2Q0LTRlMGQtYjQxNy04NjVlNGU3NzNjYjAiLCJpc3MiOiJodHRwOi8vMTkyLjE2OC41MC40Mjo4MDgwL2F1dGgvcmVhbG1zL0thZmthTW9uaXRvciIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiI5YjU2MzQ1NS1kY2U2LTQyMGItYWEzNy1kOWYyNTlmMmFlZjkiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJrbW9uLXVpLWRldiIsIm5vbmNlIjoiMzA3ZWEzMzQtNjZkYS00OWFlLThkOTMtNWNkZjZjMzIxNmUwIiwic2Vzc2lvbl9zdGF0ZSI6IjlkM2U4ZjFkLTE5MTQtNDZkYi04ZDk4LTM0MTVkMWUzYjNkYyIsImFjciI6IjEiLCJhbGxvd2VkLW9yaWdpbnMiOlsiaHR0cDovLzE5Mi4xNjguNTAuNDI6ODA4MSJdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoib3BlbmlkIGVtYWlsIHByb2ZpbGUiLCJzdWIiOiJvd2xlciIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwicHJlZmVycmVkX3VzZXJuYW1lIjoib3dsZXIifQ.cA_NEsQZkxAopTWkpkbRZtV4CN2iepguUoMyydmPB805m88AqxQ3ZMKCFaNKWoYFMrwRo_jN1rvCoiFmAdGUkSohbQu416cqeWfB0i9EPElcSjzPFq_gRoUmPn61ZfRbz8KgIiWFYdAGbrv-cfRGeHf_EJrsRoYW05DYObBrPywc59msV7J-tLyIRFhftDcsW2CITnV2YHW-exwd2AiYfYeZ-1UhOzgiu3wZGaCEwr-P7dqVLJ0xXwH6pe-8_j9aYnfnuAAv-cgm_UE_AX-mA5Zb-lNfGJnGE7Z_kN71hUDD9Vd5Mg5X6VKtVXlF9h3FZFpNXFiSh48HtGJF5Q0dwA"
    keycloakTokenVerifier.verifyToken(token).onComplete {
      case Success(verifiedToken) => verifiedToken
      case Failure(exception) =>  exception.getMessage
    }
    Thread.sleep(2000)
  }

  it should "stripBeginEnd" in {
    val result = keycloakTokenVerifier.stripBeginEnd("pem\n")
    assert(result === "pem")
  }

  it should "decodePublicKey" in {
    val pem = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCkiAouwPJBEZl0XvsreDFHLA8d\nNBsbJDGKWU7dQ7yts9Kuo42ol83zGieUTb/3PfZiGWiFBHGfyWegcVQMu/mPxF+0\nKLODttpTxuWioe2yqrDo4S3sDw+pUIDYF3WmPGMDBvm3i+p3h4eMPj5+tbmytJZU\na54hE38Iwye32ojigwIDAQAB"
    val stripBeginEnd = keycloakTokenVerifier.stripBeginEnd(pem)
    val pemToDer = keycloakTokenVerifier.pemToDer(stripBeginEnd)
    val result = keycloakTokenVerifier.decodePublicKey(pemToDer)
    assert(result.isInstanceOf[PublicKey])
  }

  it should "pemToDer" in {
    val pem = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCkiAouwPJBEZl0XvsreDFHLA8d\nNBsbJDGKWU7dQ7yts9Kuo42ol83zGieUTb/3PfZiGWiFBHGfyWegcVQMu/mPxF+0\nKLODttpTxuWioe2yqrDo4S3sDw+pUIDYF3WmPGMDBvm3i+p3h4eMPj5+tbmytJZU\na54hE38Iwye32ojigwIDAQAB"
    val result = keycloakTokenVerifier.pemToDer(pem)
    assert(result.isInstanceOf[Array[Byte]])
  }
}
