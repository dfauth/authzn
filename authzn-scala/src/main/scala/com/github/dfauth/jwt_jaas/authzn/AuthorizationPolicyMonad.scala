package com.github.dfauth.jwt_jaas.authzn

import com.typesafe.scalalogging.LazyLogging

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

trait AuthorizationPolicyMonad {
  val policy:AuthorizationPolicy
  def permit[T](subject: Subject, permission: Permission)(codeBlock : => T):Try[T]
}

case class AuthorizationPolicyMonadImpl(directives:Directive*) extends AuthorizationPolicyMonad with LazyLogging {

  val policy:AuthorizationPolicy = new AuthorizationPolicyImpl(directives.asJava)

  def permit[T](subject: Subject, permission: Permission)(codeBlock : => T):Try[T] = {
    try {
      Success(policy.permit(subject, permission).run[T](() => codeBlock))
    } catch {
      case t:SecurityException => {
        logger.info(t.getMessage, t)
        Failure[T](t)
      }
      case t:Throwable => {
        logger.info(t.getMessage, t)
        Failure[T](t)
      }
    }
  }

}
