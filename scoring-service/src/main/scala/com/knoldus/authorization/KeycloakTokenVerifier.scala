package com.knoldus.authorization

import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.unmarshalling.Unmarshal
import com.knoldus.QuickstartApp.system
import org.keycloak.RSATokenVerifier
import org.keycloak.adapters.KeycloakDeployment
import org.keycloak.jose.jws.AlgorithmType
import org.keycloak.representations.AccessToken
import spray.json.{DefaultJsonProtocol, RootJsonFormat}

import java.math.BigInteger
import java.security.spec.{RSAPublicKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, PublicKey}
import java.util.Base64
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class KeycloakTokenVerifier(keycloakDeployment: KeycloakDeployment) extends TokenVerifier
  with SprayJsonSupport with DefaultJsonProtocol {

  final case class Keys(keys: Seq[KeyData])
  final case class KeyData(kid: String, n: String, e: String)

  implicit val keyDataFormat: RootJsonFormat[KeyData] = jsonFormat3(KeyData)
  implicit val keysFormat: RootJsonFormat[Keys] = jsonFormat1(Keys)
  val jwksUrl: String = keycloakDeployment.getJwksUrl
  lazy val publicKeys: Future[Map[String, PublicKey]] =
    Http().singleRequest(HttpRequest(uri = jwksUrl)).flatMap(
      response => {
        Unmarshal(response).to[Keys].map(_.keys.map(k => (k.kid, generateKey(k))).toMap)
    })


  /**
   * Generate authentication Key.
   *
   * @param keyData key for authentication.
   * @return a public key token to verify user.
   */
  private def generateKey(keyData: KeyData): PublicKey = {
    val keyFactory = KeyFactory.getInstance(AlgorithmType.RSA.toString)
    val urlDecoder = Base64.getUrlDecoder
    val modulus = new BigInteger(1, urlDecoder.decode(keyData.n))
    val publicExponent = new BigInteger(1, urlDecoder.decode(keyData.e))
    keyFactory.generatePublic(new RSAPublicKeySpec(modulus, publicExponent))
  }


  /**
   * Generate Access Token.
   *
   * @param token to verify and give access.
   * @return  a access Token.
   */
  @Deprecated
  def verifyToken(token: String): Future[AccessToken] = {
    Future {
      RSATokenVerifier.verifyToken(
        token,
        decodePublicKey(pemToDer(
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtx42AZgdKo2AkvoX9CNb1x8zMyAVp4UrEBc740EmLFWBCBt2GldXw4SQtfBNotCVA3YzOTKSnzoSrVvMsN/6hnCOXtZdURreK67xi+pY8lgHbJ3qFENjCh5gRlmCHVXvB8kg5+yMgedg33JYXFhvnae2J03VMyPb3WZaoGF0gyCWn6dcNtwfzHqyqMUsgU9NX8wpWTKq+F1H9tQFpU8Oua3eJ6K/eqYINE3zCk9kikRl6fAg8XldEYb8d0QBWmmi6SAq5orF63p1j4XMJLeLm4pyE5W1r4Ww3Fg4L+4PWKShC01T/KEJECXlkNLQoKU5nzVdYPkLAka5Z/pVVdgfTQIDAQAB")),
        keycloakDeployment.getRealmInfoUrl
      )
    }
  }

  def pemToDer(pem: String): Array[Byte] = Base64.getDecoder.decode(stripBeginEnd(pem))

  /**
   * Striping and replacing.
   *
   * @param pem as a input.
   * @return response entity in the form of string.
   */
  def stripBeginEnd(pem: String): String = {
    var stripped = pem.replaceAll("-----BEGIN (.*)-----", "")
    stripped = stripped.replaceAll("-----END (.*)----", "")
    stripped = stripped.replaceAll("\r\n", "")
    stripped = stripped.replaceAll("\n", "")
    stripped.trim
  }

  def decodePublicKey(der: Array[Byte]): PublicKey = {
    val spec = new X509EncodedKeySpec(der)
    val kf = KeyFactory.getInstance("RSA")
    kf.generatePublic(spec)
  }
}