package com.github.dfauth.jwt.scala

import java.security.{Key, PublicKey}

import com.auth0.jwk.JwkProvider
import com.github.dfauth.authzn.User
import com.github.dfauth.jwt.JWTVerifier.ClaimsSigningKeyResolver
import io.jsonwebtoken.{Claims, Jws, JwsHeader, Jwts, SigningKeyResolver}

import scala.util.Try

object JwtVerifier {

  def claimsSigningKeyResolver[T <: JwsHeader[T]](f:(JwsHeader[T], Claims) => Key):ClaimsSigningKeyResolver[T] = (header: JwsHeader[T], claims: Claims) => f(header, claims)

  def apply[T <: JwsHeader[T]](key: PublicKey, issuer: String): JwtVerifier[T] = new JwtVerifier[T](claimsSigningKeyResolver((_:JwsHeader[T],_:Claims) => key), issuer)
  def apply[T <: JwsHeader[T]](jwkProvider: JwkProvider, issuer: String): JwtVerifier[T] = new JwtVerifier[T](claimsSigningKeyResolver((h:JwsHeader[T], c:Claims) => {
    jwkProvider.get(c.get("kid", classOf[String])).getPublicKey
  }), issuer)
}

class JwtVerifier[T <: JwsHeader[T]](keyResolver:SigningKeyResolver, issuer:String) extends com.github.dfauth.jwt.JWTVerifier(keyResolver, issuer) {

  def authenticate(token:String): Try[User] = {
    authenticate(token, j => asUser.apply(j))
  }

  def authenticate[T](token:String, f: Jws[Claims] => T): Try[T] = {
    val claims = Jwts.parser
      .setSigningKeyResolver(keyResolver)
      .requireIssuer(issuer)
      .parseClaimsJws(token)
    return Try(f(claims))
  }
}
