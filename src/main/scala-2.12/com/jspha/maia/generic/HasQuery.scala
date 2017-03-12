/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package com.jspha.maia.generic

import scala.language.higherKinds
import com.jspha.maia._
import shapeless._
import shapeless.labelled._
import shapeless.ops.record._
import cats.data.Validated

import scala.collection.immutable.HashMap

trait HasQuery[A[_ <: Mode]] {
  val query: Query[A]
}

object HasQuery {

  implicit def HasQueryGeneric[Api[_ <: Mode], Repr <: HList](
    implicit gen: LabelledGeneric.Aux[Query[Api], Repr],
    worker: Worker[Repr]
  ): HasQuery[Api] =
    new HasQuery[Api] {
      val query: Query[Api] = gen.from(worker.query)
    }

  trait Worker[T <: HList] {
    val query: T
  }

  object Worker {

    implicit val WorkerHNil: Worker[HNil] =
      new Worker[HNil] {
        val query: HNil = HNil
      }

    def buildWorkerRecurAtom[K <: Symbol,
                             ReqRepr <: HList,
                             RespRepr <: HList,
                             Api[_ <: Mode],
                             A,
                             E,
                             T <: HList](
      implicit recur: Lazy[Worker[T]],
      buildNullRequest: NullRequest[Api],
      reqRepr: LabelledGeneric.Aux[Request[Api], ReqRepr],
      updater: Updater.Aux[ReqRepr,
                           FieldType[K, Mode.Request#AtomE[E, A]],
                           ReqRepr],
      respRepr: LabelledGeneric.Aux[Response[Api], RespRepr],
      selector: Selector.Aux[RespRepr, K, Mode.Response.AtomE[E, A]],
      kWitness: Witness.Aux[K]
    ): Worker[FieldType[K, Mode.Query[Api]#AtomE[E, A]] :: T] =
      new Worker[FieldType[K, Mode.Query[Api]#AtomE[E, A]] :: T] {

        val request: Request[Api] =
          reqRepr.from(
            updater(reqRepr.to(buildNullRequest.request), field[K](true))
          )

        def handleResponse(resp: Response[Api]): Validated[LookupError[E], A] = {
          selector(respRepr.to(resp)) match {
            case None =>
              Validated.invalid(
                LookupError.Unexpected(LookupError.UnexpectedError
                  .ServerShouldHaveResponded(kWitness.value)))
            case Some(Left(err)) =>
              Validated.invalid(LookupError.Domain(err))
            case Some(Right(a)) =>
              Validated.valid(a)
          }
        }

        val lookup: Lookup[Api, E, A] =
          Lookup[Api, E, A](request, handleResponse)

        val query: FieldType[K, Mode.Query[Api]#AtomE[E, A]] :: T =
          field[K](lookup) :: recur.value.query

      }

    implicit def WorkerRecurAtomNothing[K <: Symbol,
                                        ReqRepr <: HList,
                                        RespRepr <: HList,
                                        Api[_ <: Mode],
                                        A,
                                        T <: HList](
      implicit recur: Lazy[Worker[T]],
      buildNullRequest: NullRequest[Api],
      reqRepr: LabelledGeneric.Aux[Request[Api], ReqRepr],
      updater: Updater.Aux[ReqRepr,
                           FieldType[K, Mode.Request#AtomE[Nothing, A]],
                           ReqRepr],
      respRepr: LabelledGeneric.Aux[Response[Api], RespRepr],
      selector: Selector.Aux[RespRepr, K, Mode.Response.AtomE[Nothing, A]],
      kWitness: Witness.Aux[K]
    ): Worker[FieldType[K, Mode.Query[Api]#AtomE[Nothing, A]] :: T] =
      buildWorkerRecurAtom[K, ReqRepr, RespRepr, Api, A, Nothing, T]

    implicit def WorkerRecurAtomGeneral[K <: Symbol,
                                        ReqRepr <: HList,
                                        RespRepr <: HList,
                                        Api[_ <: Mode],
                                        A,
                                        E,
                                        T <: HList](
      implicit recur: Lazy[Worker[T]],
      buildNullRequest: NullRequest[Api],
      reqRepr: LabelledGeneric.Aux[Request[Api], ReqRepr],
      updater: Updater.Aux[ReqRepr,
                           FieldType[K, Mode.Request#AtomE[E, A]],
                           ReqRepr],
      respRepr: LabelledGeneric.Aux[Response[Api], RespRepr],
      selector: Selector.Aux[RespRepr, K, Mode.Response.AtomE[E, A]],
      kWitness: Witness.Aux[K]
    ): Worker[FieldType[K, Mode.Query[Api]#AtomE[E, A]] :: T] =
      buildWorkerRecurAtom[K, ReqRepr, RespRepr, Api, A, E, T]

    implicit def WorkerRecurIndexedAtom[K <: Symbol,
                                        ReqRepr <: HList,
                                        RespRepr <: HList,
                                        Api[_ <: Mode],
                                        A,
                                        I,
                                        T <: HList](
      implicit recur: Lazy[Worker[T]],
      kWitness: Witness.Aux[K],
      buildNullRequest: NullRequest[Api],
      reqRepr: LabelledGeneric.Aux[Request[Api], ReqRepr],
      updater: Updater.Aux[ReqRepr,
                           FieldType[K, Mode.Request#IAtom[I, A]],
                           ReqRepr],
      respRepr: LabelledGeneric.Aux[Response[Api], RespRepr],
      selector: Selector.Aux[RespRepr, K, Mode.Response.IAtom[I, A]]
    ): Worker[FieldType[K, Mode.Query[Api]#IAtom[I, A]] :: T] =
      new Worker[FieldType[K, Mode.Query[Api]#IAtom[I, A]] :: T] {

        def lookup(ix: I): Lookup[Api, Nothing, A] = {

          val request: Request[Api] =
            reqRepr.from(
              updater(reqRepr.to(buildNullRequest.request), field[K](Set(ix)))
            )

          def handleResponse(
            resp: Response[Api]): Validated[LookupError[Nothing], A] = {
            selector(respRepr.to(resp)).get(ix) match {
              case None =>
                Validated.invalid(
                  LookupError.Unexpected(LookupError.UnexpectedError
                    .ServerShouldHaveResponded(kWitness.value)))
              case Some(a) =>
                Validated.valid(a)
            }
          }

          Lookup[Api, Nothing, A](request, handleResponse)
        }

        val query: FieldType[K, Mode.Query[Api]#IAtom[I, A]] :: T =
          field[K](lookup _) :: recur.value.query

      }

    implicit def WorkerRecurObjM[K <: Symbol,
                                 ReqRepr <: HList,
                                 RespRepr <: HList,
                                 Api[_ <: Mode],
                                 A[_ <: Mode],
                                 M <: Cardinality,
                                 T <: HList](
      implicit recur: Lazy[Worker[T]],
      kWitness: Witness.Aux[K],
      buildNullRequest: NullRequest[Api],
      reqRepr: LabelledGeneric.Aux[Request[Api], ReqRepr],
      updater: Updater.Aux[ReqRepr,
                           FieldType[K, Mode.Request#Obj[M, A]],
                           ReqRepr],
      respRepr: LabelledGeneric.Aux[Response[Api], RespRepr],
      selector: Selector.Aux[RespRepr, K, Mode.Response.Obj[M, A]],
      multOps: Cardinality.Ops[M],
      recurQuery: Lazy[HasQuery[A]]
    ): Worker[FieldType[K, Mode.Query[Api]#Obj[M, A]] :: T] =
      new Worker[FieldType[K, Mode.Query[Api]#Obj[M, A]] :: T] {

        val qm: Mode.Query[Api] = new Mode.Query[Api]

        val query: FieldType[K, Mode.Query[Api]#Obj[M, A]] :: T = {

          val obj = new qm.Obj[M, A] {

            def apply[R](cont: Query[A] => Lookup[A, Nothing, R])
              : Lookup[Api, Nothing, M#Coll[R]] = {

              val subLookup: Lookup[A, Nothing, R] = cont(
                recurQuery.value.query)

              val request: Request[Api] =
                reqRepr.from(
                  updater(
                    reqRepr.to(buildNullRequest.request),
                    field[K](Option(subLookup.request))
                  ))

              def doResp(resp: Response[Api])
                : Validated[LookupError[Nothing], M#Coll[R]] =
                selector(respRepr.to(resp)) match {
                  case None =>
                    Validated.Invalid(
                      LookupError.Unexpected(LookupError.UnexpectedError
                        .ServerShouldHaveResponded(kWitness.value)))
                  case Some(respA) =>
                    multOps.traversable
                      .traverse[Validated[LookupError[Nothing], ?],
                                Response[A],
                                R](respA)(subLookup.handleResponse)
                      // We mark the lower errors with an "object group
                      // name" forming a trie of errors
                      .leftMap(LookupError.Object(kWitness.value, _))
                }

              Lookup[Api, Nothing, M#Coll[R]](request, doResp)
            }
          }

          field[K](obj) :: recur.value.query
        }
      }

    implicit def WorkerRecurIndexedMultiObj[K <: Symbol,
                                            ReqRepr <: HList,
                                            RespRepr <: HList,
                                            Api[_ <: Mode],
                                            A[_ <: Mode],
                                            I,
                                            M <: Cardinality,
                                            T <: HList](
      implicit recur: Lazy[Worker[T]],
      kWitness: Witness.Aux[K],
      buildNullRequest: NullRequest[Api],
      reqRepr: LabelledGeneric.Aux[Request[Api], ReqRepr],
      updater: Updater.Aux[ReqRepr,
                           FieldType[K, Mode.Request#IObj[I, M, A]],
                           ReqRepr],
      respRepr: LabelledGeneric.Aux[Response[Api], RespRepr],
      selector: Selector.Aux[RespRepr, K, Mode.Response.IObj[I, M, A]],
      multOps: Cardinality.Ops[M],
      recurQuery: Lazy[HasQuery[A]]
    ): Worker[FieldType[K, Mode.Query[Api]#IObj[I, M, A]] :: T] =
      new Worker[FieldType[K, Mode.Query[Api]#IObj[I, M, A]] :: T] {

        val qm: Mode.Query[Api] = new Mode.Query[Api]

        val query: FieldType[K, Mode.Query[Api]#IObj[I, M, A]] :: T = {

          val obj = new qm.IObj[I, M, A] {

            def apply[R](ix: I)(cont: Query[A] => Lookup[A, Nothing, R])
              : Lookup[Api, Nothing, M#Coll[R]] = {

              val subLookup: Lookup[A, Nothing, R] =
                cont(recurQuery.value.query)

              val request: Request[Api] =
                reqRepr.from(
                  updater(
                    reqRepr.to(buildNullRequest.request),
                    field[K](HashMap(ix -> subLookup.request))
                  ))

              def doResp(resp: Response[Api])
                : Validated[LookupError[Nothing], M#Coll[R]] =
                selector(respRepr.to(resp)).get(ix) match {
                  case None =>
                    Validated.Invalid(
                      LookupError.Unexpected(LookupError.UnexpectedError
                        .ServerShouldHaveResponded(kWitness.value)))
                  case Some(respA) =>
                    multOps.traversable
                      .traverse[Validated[LookupError[Nothing], ?],
                                Response[A],
                                R](respA)(subLookup.handleResponse)
                      // We mark the lower errors with an "object group
                      // name" forming a trie of errors
                      .leftMap(LookupError.Object(kWitness.value, _))
                }

              Lookup[Api, Nothing, M#Coll[R]](request, doResp)
            }
          }

          field[K](obj) :: recur.value.query
        }
      }

  }

}