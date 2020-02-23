package com.github.dfauth.jwt.scala

import com.github.dfauth.authzn.Role.role
import com.github.dfauth.authzn.User
import com.github.dfauth.jwt.{JWTBuilder, KeyPairFactory}
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.testng.TestNGSuite
import org.testng.Assert._
import org.testng.annotations.Test

import scala.util.{Success, Try}

class TestCase extends TestNGSuite with LazyLogging {

  @Test
  def testIt(): Unit = {
    val testKeyPair = KeyPairFactory.createKeyPair("RSA", 2048)
    val issuer = "me"
    val jwtBuilder = new JWTBuilder(issuer, testKeyPair.getPrivate)
    val user = User.of("fred", "flintstone", role("test:admin"), role("test:user"))
    val token = jwtBuilder.forSubject(user.getUserId).withClaim("roles", user.getRoles).build

    val jwtVerifier = JwtVerifier(testKeyPair.getPublic, issuer)
    val result:Try[User] = jwtVerifier.authenticate(token)
    assertTrue(result.isSuccess)
  }

}
