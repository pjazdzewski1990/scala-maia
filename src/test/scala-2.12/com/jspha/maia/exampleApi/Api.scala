/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.jspha.maia.exampleApi

import com.jspha.maia._
import cats._

final case class Api[M <: Mode](
  getUser: M#IndexedObj[User.Identity, User],
  getAllUsers: M#IndexedMultiObj[Int, Multiplicity.Collection, User]
)

object Api {

  type Fm = FetcherMode[Id]

  val fetcher: Fetcher[Id, Api] =
    Api[Fm](
      getUser = (id: User.Identity) => User.fetch(id),
      getAllUsers = (_: Int) =>
        List(
          User.fetch(User.Root),
          User.fetch(User.JosephAbrahamson)
      )
    )

  val q: Query[Api] =
    implicitly[props.HasQuery[Api]].query

  val i: props.Interprets[Id, Api] =
    props.Interprets[Id, Api]

}
