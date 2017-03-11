/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.jspha.maia.examples.api1

import com.jspha.maia._
import cats._

final case class City[M <: Mode](
  name: M#Atom[String],
  location: M#Obj1[Location]
)

object City {

  def atlanta: Fetcher[Id, Err, City] =
    City[Mode.Fetcher[Id, Err]](
      name = Right("Atlanta"),
      location = Right(Location.fetchConst(33.7490, 84.3880))
    )

  val q: Query[Err, City] =
    implicitly[generic.HasQuery[Err, City]].query

  val i: generic.Interprets[Id, Err, City] =
    generic.Interprets[Id, Err, City]

}
