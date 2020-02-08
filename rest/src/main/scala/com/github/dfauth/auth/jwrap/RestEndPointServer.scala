package com.github.dfauth.auth.jwrap

import akka.http.scaladsl.server.Route

class RestEndPointServer(route: Route, hostname:String, port:Int) extends com.github.dfauth.auth.RestEndPointServer(route, hostname, port) {

  def start() = super.start(None)
}
