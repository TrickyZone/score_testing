package com.knoldus.authorization

import org.keycloak.representations.AccessToken
import scala.concurrent.Future

trait TokenVerifier {

  /**
   * Verifies token.
   *
   * @param token to verify user.
   * @return response in the form of Access Token.
   */
  def verifyToken(token: String): Future[AccessToken]
}