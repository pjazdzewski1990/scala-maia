/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.jspha.maia.props

import scala.language.higherKinds
import com.jspha.maia._
import shapeless._
import shapeless.labelled._

import scala.collection.immutable.HashMap

trait NullRequest[Api[_ <: Mode]] {
  val request: Request[Api]
}

object NullRequest {

  implicit def NullRequestGeneric[Api[_ <: Mode], Repr <: HList](
    implicit gen: LabelledGeneric.Aux[Request[Api], Repr],
    worker: Worker[Repr]
  ): NullRequest[Api] =
    new NullRequest[Api] {
      val request: Request[Api] = gen.from(worker.request)
    }

  trait Worker[T <: HList] {
    val request: T
  }

  object Worker {

    implicit val WorkerHNil: Worker[HNil] = new Worker[HNil] {
      val request: HNil = HNil
    }

    implicit def WorkerRecurAtom[A, K <: Symbol, T <: HList](
      implicit recur: Worker[T]
    ): Worker[FieldType[K, RequestMode.Atom[A]] :: T] =
      new Worker[FieldType[K, RequestMode.Atom[A]] :: T] {
        val request: FieldType[K, RequestMode.Atom[A]] :: T =
          field[K](false) :: recur.request
      }

    implicit def WorkerRecurIndexedAtom[A, I, K <: Symbol, T <: HList](
      implicit recur: Worker[T]
    ): Worker[FieldType[K, RequestMode.IndexedAtom[I, A]] :: T] =
      new Worker[FieldType[K, RequestMode.IndexedAtom[I, A]] :: T] {
        val request: FieldType[K, RequestMode.IndexedAtom[I, A]] :: T =
          field[K](Set.empty[I]) :: recur.request
      }

    implicit def WorkerRecurObj[A[_ <: Mode], K <: Symbol, T <: HList](
      implicit recur: Worker[T]
    ): Worker[FieldType[K, RequestMode.Obj[A]] :: T] =
      new Worker[FieldType[K, RequestMode.Obj[A]] :: T] {
        val request: FieldType[K, RequestMode.Obj[A]] :: T =
          field[K](None) :: recur.request
      }

    implicit def WorkerRecurObjM[A[_ <: Mode],
                                 M <: Multiplicity,
                                 K <: Symbol,
                                 T <: HList](
      implicit recur: Worker[T]
    ): Worker[FieldType[K, RequestMode.ObjM[M, A]] :: T] =
      new Worker[FieldType[K, RequestMode.ObjM[M, A]] :: T] {
        val request: FieldType[K, RequestMode.ObjM[M, A]] :: T =
          field[K](None) :: recur.request
      }

    implicit def WorkerRecurIndexedObj[A[_ <: Mode],
                                       I,
                                       K <: Symbol,
                                       T <: HList](
      implicit recur: Worker[T]
    ): Worker[FieldType[K, RequestMode.IndexedObj[I, A]] :: T] =
      new Worker[FieldType[K, RequestMode.IndexedObj[I, A]] :: T] {
        val request: FieldType[K, RequestMode.IndexedObj[I, A]] :: T =
          field[K](HashMap.empty[I, Request[A]]) :: recur.request
      }

  }

}
