/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.jspha.maia.example.ui

import scala.scalajs.js.JSApp

import fr.hmil.roshttp.HttpRequest
import monix.execution.Scheduler.Implicits.global
import scala.util.{Failure, Success}
import fr.hmil.roshttp.response.SimpleHttpResponse
import fr.hmil.roshttp.Protocol.HTTP
import fr.hmil.roshttp.body.Implicits._
import fr.hmil.roshttp.body.JSONBody._

object Runtime extends JSApp {

  val request =
    HttpRequest()
      .withProtocol(HTTP)
      .withHost("localhost")
      .withPort(8080)
      .withPath("/api")

  def main(): Unit = {
    println("Loaded!!!")
    request
      .post(
        JSONObject(
          "getCount" -> new JSONValue {
            override def toString() = "true"
          }
        ))
      .onComplete({
        case res: Success[SimpleHttpResponse] => println(res.get.body)
        case _: Failure[SimpleHttpResponse] =>
          println("Huston, we got a problem!")
      })
  }
}
