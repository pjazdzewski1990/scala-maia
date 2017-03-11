/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.jspha.maia.props

import scala.language.higherKinds
import com.jspha.maia._
import shapeless._

import scala.collection.immutable.HashMap

// TODO: Implement this computation via implicits
trait MergeRequests[Api[_ <: Mode]] {
  def apply(a: Request[Api], b: Request[Api]): Request[Api]
}

object MergeRequests {

  implicit def MergeRequestsGeneric[Api[_ <: Mode], Repr <: HList](
    implicit gen: Generic.Aux[Request[Api], Repr],
    worker: Worker[Repr]
  ): MergeRequests[Api] =
    (a: Request[Api], b: Request[Api]) =>
      gen.from(worker(gen.to(a), gen.to(b)))

  trait Worker[Repr <: HList] {
    def apply(l: Repr, r: Repr): Repr
  }

  object Worker {

    implicit val WorkerHNil: Worker[HNil] = (l: HNil, r: HNil) => HNil

    implicit def WorkerRecurAtom[A, Tail <: HList](
      implicit recur: Worker[Tail]
    ): Worker[RequestMode.Atom[A] :: Tail] =
      (ll: RequestMode.Atom[A] :: Tail, rr: RequestMode.Atom[A] :: Tail) =>
        (ll, rr) match {
          case (l :: ls, r :: rs) => (l || r) :: recur(ls, rs)
      }

    implicit def WorkerRecurIndexedAtom[A, I, Tail <: HList](
      implicit recur: Worker[Tail]
    ): Worker[RequestMode.IndexedAtom[I, A] :: Tail] =
      (ll: RequestMode.IndexedAtom[I, A] :: Tail,
       rr: RequestMode.IndexedAtom[I, A] :: Tail) =>
        (ll, rr) match {
          case (l :: ls, r :: rs) => (l ++ r) :: recur(ls, rs)
      }

    implicit def WorkerRecurObj[A[_ <: Mode], Tail <: HList](
      implicit recur: Worker[Tail],
      recurObj: MergeRequests[A]
    ): Worker[RequestMode.Obj[A] :: Tail] =
      (ll: Option[Request[A]] :: Tail, rr: Option[Request[A]] :: Tail) =>
        (ll, rr) match {
          case (None :: ls, None :: rs) => None :: recur(ls, rs)
          case (Some(l) :: ls, None :: rs) => Some(l) :: recur(ls, rs)
          case (None :: ls, Some(r) :: rs) => Some(r) :: recur(ls, rs)
          case (Some(l) :: ls, Some(r) :: rs) =>
            Some(recurObj(l, r)) :: recur(ls, rs)
      }

    implicit def WorkerRecurObjM[A[_ <: Mode],
                                 M <: Multiplicity,
                                 Tail <: HList](
      implicit recur: Worker[Tail],
      recurObj: MergeRequests[A]
    ): Worker[RequestMode.MultiObj[M, A] :: Tail] =
      (ll: Option[Request[A]] :: Tail, rr: Option[Request[A]] :: Tail) =>
        (ll, rr) match {
          case (None :: ls, None :: rs) => None :: recur(ls, rs)
          case (Some(l) :: ls, None :: rs) => Some(l) :: recur(ls, rs)
          case (None :: ls, Some(r) :: rs) => Some(r) :: recur(ls, rs)
          case (Some(l) :: ls, Some(r) :: rs) =>
            Some(recurObj(l, r)) :: recur(ls, rs)
      }

    implicit def WorkerRecurIndexedObj[A[_ <: Mode], I, Tail <: HList](
      implicit recur: Worker[Tail],
      recurObj: MergeRequests[A]
    ): Worker[RequestMode.IndexedObj[I, A] :: Tail] =
      (ll: RequestMode.IndexedObj[I, A] :: Tail,
       rr: RequestMode.IndexedObj[I, A] :: Tail) =>
        (ll, rr) match {
          case (l :: ls, r :: rs) =>
            val here: HashMap[I, Request[A]] = l.merged(r) { (lt, rt) =>
              (lt._1, recurObj(lt._2, rt._2))
            }
            val there: Tail = recur(ls, rs)
            here :: there
      }

    implicit def WorkerRecurIndexedMultiObj[A[_ <: Mode],
                                            I,
                                            M <: Multiplicity,
                                            Tail <: HList](
      implicit recur: Worker[Tail],
      recurObj: MergeRequests[A]
    ): Worker[RequestMode.IndexedMultiObj[I, M, A] :: Tail] =
      (ll: RequestMode.IndexedMultiObj[I, M, A] :: Tail,
       rr: RequestMode.IndexedMultiObj[I, M, A] :: Tail) =>
        (ll, rr) match {
          case (l :: ls, r :: rs) =>
            val here: HashMap[I, Request[A]] = l.merged(r) { (lt, rt) =>
              (lt._1, recurObj(lt._2, rt._2))
            }
            val there: Tail = recur(ls, rs)
            here :: there
      }

  }

}
