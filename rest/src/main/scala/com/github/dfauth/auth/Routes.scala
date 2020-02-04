package com.github.dfauth.auth

import java.security.KeyPair

import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import com.github.dfauth.jwt.{JWTBuilder, JWTVerifier, KeyPairFactory}
import com.github.dfauth.auth.Directives._
import com.typesafe.scalalogging.LazyLogging

object Routes extends LazyLogging {

  val keyPair: KeyPair = KeyPairFactory.createKeyPair("RSA", 2048)
  val jwtBuilder = new JWTBuilder("me",keyPair.getPrivate)
  val jwtVerifier = new JWTVerifier(keyPair.getPublic)


  def hello(jwtVerifier: JWTVerifier) =
    path("hello") {
      get {
        authenticate(jwtVerifier) { user =>
          complete(HttpEntity(ContentTypes.`application/json`, s"""{"say": "hello to authenticated ${user.payload}"}"""))
        }
      }
    }

}
