package jspha.comms

import cats.data.Xor

import scala.collection.immutable.HashMap

/**
  * A Comms wire response is a subtree of an Api with values adorned on the
  * leaves.
  *
  * Due to the types forgotten inside of a Response it's not possible to
  * serialize them directly. Instead, they must be serialized against an Api.
  */
case class Response(here: HashMap[Key.Dyn, MSet[Dyn] Xor MSet[Response]]) {

  def apply(key: Key.Dyn): Option[MSet[Dyn] Xor MSet[Response]] =
    here.get(key)

  def nest(key: Key.Dyn): Response =
    Response(key -> Xor.right(MSet.one(this)))

  def ++(other: Response): Response =
    Response(here.merged(other.here) {
      case ((k1, v1), (k2, v2)) => (k1, v1)
    })

  def +(pair: (Key.Dyn, MSet[Dyn] Xor MSet[Response])): Response =
    Response(here + pair)

}

object Response {

  val zero: Response = Response()

  def apply(pairs: (Key.Dyn, MSet[Dyn] Xor MSet[Response])*): Response =
    Response(HashMap(pairs:_*))

}
