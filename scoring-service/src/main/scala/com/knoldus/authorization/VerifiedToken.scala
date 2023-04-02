package com.knoldus.authorization

final case class VerifiedToken(token: String,
                         id: String,
                         name: String,
                         username: String,
                         email: String,
                         roles: Seq[String])