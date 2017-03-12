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

    implicit def WorkerRecurAtom[K <: Symbol,
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
      WorkerRecurAtom[K, ReqRepr, RespRepr, Api, A, Nothing, T]

    implicit def WorkerRecurIndexedAtom[K <: Symbol,
                                        ReqRepr <: HList,
                                        RespRepr <: HList,
                                        Api[_ <: Mode],
                                        A,
                                        E,
                                        I,
                                        T <: HList](
      implicit recur: Lazy[Worker[T]],
      kWitness: Witness.Aux[K],
      buildNullRequest: NullRequest[Api],
      reqRepr: LabelledGeneric.Aux[Request[Api], ReqRepr],
      updater: Updater.Aux[ReqRepr,
                           FieldType[K, Mode.Request#IAtomE[I, E, A]],
                           ReqRepr],
      respRepr: LabelledGeneric.Aux[Response[Api], RespRepr],
      selector: Selector.Aux[RespRepr, K, Mode.Response.IAtomE[I, E, A]]
    ): Worker[FieldType[K, Mode.Query[Api]#IAtomE[I, E, A]] :: T] =
      new Worker[FieldType[K, Mode.Query[Api]#IAtomE[I, E, A]] :: T] {

        def lookup(ix: I): Lookup[Api, E, A] = {

          val request: Request[Api] =
            reqRepr.from(
              updater(reqRepr.to(buildNullRequest.request), field[K](Set(ix)))
            )

          def handleResponse(
            resp: Response[Api]): Validated[LookupError[E], A] = {
            selector(respRepr.to(resp)).get(ix) match {
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

          Lookup[Api, E, A](request, handleResponse)
        }

        val query: FieldType[K, Mode.Query[Api]#IAtomE[I, E, A]] :: T =
          field[K](lookup _) :: recur.value.query

      }

    implicit def WorkerRecurIndexedAtomNothing[K <: Symbol,
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
                           FieldType[K, Mode.Request#IAtomE[I, Nothing, A]],
                           ReqRepr],
      respRepr: LabelledGeneric.Aux[Response[Api], RespRepr],
      selector: Selector.Aux[RespRepr, K, Mode.Response.IAtomE[I, Nothing, A]]
    ): Worker[FieldType[K, Mode.Query[Api]#IAtomE[I, Nothing, A]] :: T] =
      WorkerRecurIndexedAtom[K, ReqRepr, RespRepr, Api, A, Nothing, I, T]

    implicit def WorkerRecurObjM[K <: Symbol,
                                 ReqRepr <: HList,
                                 RespRepr <: HList,
                                 Api[_ <: Mode],
                                 A[_ <: Mode],
                                 E,
                                 M <: Cardinality,
                                 T <: HList](
      implicit recur: Lazy[Worker[T]],
      kWitness: Witness.Aux[K],
      buildNullRequest: NullRequest[Api],
      reqRepr: LabelledGeneric.Aux[Request[Api], ReqRepr],
      updater: Updater.Aux[ReqRepr,
                           FieldType[K, Mode.Request#ObjE[M, E, A]],
                           ReqRepr],
      respRepr: LabelledGeneric.Aux[Response[Api], RespRepr],
      selector: Selector.Aux[RespRepr, K, Mode.Response.ObjE[M, E, A]],
      multOps: Cardinality.Ops[M],
      recurQuery: Lazy[HasQuery[A]]
    ): Worker[FieldType[K, Mode.Query[Api]#ObjE[M, E, A]] :: T] =
      new Worker[FieldType[K, Mode.Query[Api]#ObjE[M, E, A]] :: T] {

        val qm: Mode.Query[Api] = new Mode.Query[Api]

        val query: FieldType[K, Mode.Query[Api]#ObjE[M, E, A]] :: T = {

          val obj = new qm.ObjE[M, E, A] {

            def apply[R](
              cont: Query[A] => Lookup[A, E, R]): Lookup[Api, E, M#Coll[R]] = {

              val subLookup: Lookup[A, E, R] = cont(recurQuery.value.query)

              val request: Request[Api] =
                reqRepr.from(
                  updater(
                    reqRepr.to(buildNullRequest.request),
                    field[K](Option(subLookup.request))
                  ))

              def doResp(
                resp: Response[Api]): Validated[LookupError[E], M#Coll[R]] =
                selector(respRepr.to(resp)) match {
                  case None =>
                    Validated.invalid(
                      LookupError.Unexpected(LookupError.UnexpectedError
                        .ServerShouldHaveResponded(kWitness.value)))
                  case Some(Left(err)) =>
                    Validated.invalid(LookupError.Domain(err))
                  case Some(Right(respA)) =>
                    multOps.traversable
                      .traverse[Validated[LookupError[E], ?], Response[A], R](
                        respA)(subLookup.handleResponse)
                      // We mark the lower errors with an "object group
                      // name" forming a trie of errors
                      .leftMap(LookupError.Object(kWitness.value, _))
                }

              Lookup[Api, E, M#Coll[R]](request, doResp)
            }
          }

          field[K](obj) :: recur.value.query
        }
      }

    implicit def WorkerRecurObjMNothing[K <: Symbol,
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
                           FieldType[K, Mode.Request#ObjE[M, Nothing, A]],
                           ReqRepr],
      respRepr: LabelledGeneric.Aux[Response[Api], RespRepr],
      selector: Selector.Aux[RespRepr, K, Mode.Response.ObjE[M, Nothing, A]],
      multOps: Cardinality.Ops[M],
      recurQuery: Lazy[HasQuery[A]]
    ): Worker[FieldType[K, Mode.Query[Api]#ObjE[M, Nothing, A]] :: T] =
      WorkerRecurObjM[K, ReqRepr, RespRepr, Api, A, Nothing, M, T]

    implicit def WorkerRecurIndexedMultiObj[K <: Symbol,
                                            ReqRepr <: HList,
                                            RespRepr <: HList,
                                            Api[_ <: Mode],
                                            A[_ <: Mode],
                                            E,
                                            I,
                                            M <: Cardinality,
                                            T <: HList](
      implicit recur: Lazy[Worker[T]],
      kWitness: Witness.Aux[K],
      buildNullRequest: NullRequest[Api],
      reqRepr: LabelledGeneric.Aux[Request[Api], ReqRepr],
      updater: Updater.Aux[ReqRepr,
                           FieldType[K, Mode.Request#IObjE[I, M, E, A]],
                           ReqRepr],
      respRepr: LabelledGeneric.Aux[Response[Api], RespRepr],
      selector: Selector.Aux[RespRepr, K, Mode.Response.IObjE[I, M, E, A]],
      multOps: Cardinality.Ops[M],
      recurQuery: Lazy[HasQuery[A]]
    ): Worker[FieldType[K, Mode.Query[Api]#IObjE[I, M, E, A]] :: T] =
      new Worker[FieldType[K, Mode.Query[Api]#IObjE[I, M, E, A]] :: T] {

        val qm: Mode.Query[Api] = new Mode.Query[Api]

        val query: FieldType[K, Mode.Query[Api]#IObjE[I, M, E, A]] :: T = {

          val obj = new qm.IObjE[I, M, E, A] {

            def apply[R](ix: I)(
              cont: Query[A] => Lookup[A, E, R]): Lookup[Api, E, M#Coll[R]] = {

              val subLookup: Lookup[A, E, R] =
                cont(recurQuery.value.query)

              val request: Request[Api] =
                reqRepr.from(
                  updater(
                    reqRepr.to(buildNullRequest.request),
                    field[K](HashMap(ix -> subLookup.request))
                  ))

              def doResp(
                resp: Response[Api]): Validated[LookupError[E], M#Coll[R]] =
                selector(respRepr.to(resp)).get(ix) match {
                  case None =>
                    Validated.invalid(
                      LookupError.Unexpected(LookupError.UnexpectedError
                        .ServerShouldHaveResponded(kWitness.value)))
                  case Some(Left(err)) =>
                    Validated.invalid(LookupError.Domain(err))
                  case Some(Right(respA)) =>
                    multOps.traversable
                      .traverse[Validated[LookupError[E], ?], Response[A], R](
                        respA)(subLookup.handleResponse)
                      // We mark the lower errors with an "object group
                      // name" forming a trie of errors
                      .leftMap(LookupError.Object(kWitness.value, _))
                }

              Lookup[Api, E, M#Coll[R]](request, doResp)
            }
          }

          field[K](obj) :: recur.value.query
        }
      }

    implicit def WorkerRecurIndexedMultiObjNothing[K <: Symbol,
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
                           FieldType[K, Mode.Request#IObjE[I, M, Nothing, A]],
                           ReqRepr],
      respRepr: LabelledGeneric.Aux[Response[Api], RespRepr],
      selector: Selector.Aux[RespRepr,
                             K,
                             Mode.Response.IObjE[I, M, Nothing, A]],
      multOps: Cardinality.Ops[M],
      recurQuery: Lazy[HasQuery[A]]
    ): Worker[FieldType[K, Mode.Query[Api]#IObjE[I, M, Nothing, A]] :: T] =
      WorkerRecurIndexedMultiObj[K,
                                 ReqRepr,
                                 RespRepr,
                                 Api,
                                 A,
                                 Nothing,
                                 I,
                                 M,
                                 T]
  }

}
