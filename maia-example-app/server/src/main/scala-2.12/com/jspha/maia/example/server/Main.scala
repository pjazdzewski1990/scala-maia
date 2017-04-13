package com.jspha.maia.example.server

import org.http4s._
import org.http4s.dsl._
import org.http4s.server.blaze._
import org.http4s.util.StreamApp
import fs2.{Stream, Task}

object Main extends StreamApp {

  val indexService = HttpService {
    case req @ GET -> Root =>
      StaticFile.fromResource("/index.html", Some(req)) match {
        case None => NotFound()
        case Some(f) => Task.now(f)
      }
  }

  val staticResourcesService = HttpService {
    case req @ GET -> path => {
      StaticFile.fromResource(path.toString, Some(req)) match {
        case None => NotFound()
        case Some(f) => Task.now(f)
      }
    }
  }

  def main(args: List[String]): Stream[Task, Unit] = {
    BlazeBuilder
      .bindHttp(8080, "localhost")
      .mountService(indexService, "/")
      .mountService(staticResourcesService, "/static")
      .serve
  }

}
