/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.jspha.maia.examples.api1

import com.jspha.maia._
import shapeless._

final case class Nullo[F <: Dsl]()

object Nullo {

  def fetch: Handler[Id, Nullo] =
    Nullo[form.Handler[Id]]()

  val req0: Request[Nullo] =
    typelevel.NullRequest[Nullo]

  val q: QueriesAt[Nullo] =
    typelevel.GetQueriesAt[Nullo]

  def runner(req: Request[Nullo]): Response[Nullo] =
    typelevel.RunHandler[Id, Nullo](fetch, req)

}
