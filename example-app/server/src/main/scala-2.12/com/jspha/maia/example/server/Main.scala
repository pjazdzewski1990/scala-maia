/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

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
      .mountService(ApiService.service, "/api")
      .serve
  }

}
