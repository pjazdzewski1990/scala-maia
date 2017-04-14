/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.jspha.maia.example.server

import com.jspha.maia._
import com.jspha.maia.example.api.Root
import com.jspha.maia.generic.{Interprets, RequestDecoder, ResponseEncoder}
import fs2.Task
import fs2.interop.cats._
import io.circe._
import org.http4s
import org.http4s.{Request => _, Response => _, _}
import org.http4s.circe._
import org.http4s.dsl._

object ApiService {

  val reqDecoder: Decoder[Request[Root]] =
    implicitly[RequestDecoder[Root]]

  val respEncoder: Encoder[Response[Root]] =
    implicitly[ResponseEncoder[Root]]

  implicit val reqEntityDecoder: EntityDecoder[Request[Root]] =
    jsonOf(reqDecoder)

  implicit val respEntityEncoder: EntityEncoder[Response[Root]] =
    jsonEncoderOf(respEncoder)

  val rootFetcher: Fetcher[Task, Root] =
    Root[Fields.Fetcher[Task]](
      getCount = Task.now(Right(0))
    )

  val interpretsRoot: Interprets[Task, Root] =
    Interprets[Task, Root]

  val service = HttpService {
    case req @ POST -> http4s.dsl.Root =>
      for {
        maiaReq <- req.as[Request[Root]]
        maiaResp <- interpretsRoot(rootFetcher, maiaReq)
        http4sResp <- Ok(maiaResp)
      } yield http4sResp
  }

}
