package jspha.comms.qs.wire

import cats.Monoid
import cats.data.Xor
import io.circe.Decoder.Result

import scala.collection.immutable.HashMap
import io.circe._

// NOTE: We use HashMaps here so that we can use the .merged method
case class Request(
    here: HashMap[Symbol, Set[String] Xor HashMap[String, Request]]
) {

  /**
    * Recursively merge requests so that all the query paths in each
    * component request are reflected in their combination. Does the best it
    * can do to resolve a disagreement about whether a given key is atomic or
    * nested (assumes nested with empty subrequests), but this is clearly a
    * library error.
    */
  def ++(other: Request): Request =
    Request(here.merged(other.here) {
      case ((n1, Xor.Left(m1)), (n2, Xor.Right(m2))) =>
        (n1, Xor.Right(m1.foldLeft(m2) {
          case (map, key) => map + (key -> Request())
        }))
      case ((n1, Xor.Right(m1)), (n2, Xor.Left(m2))) =>
        (n1, Xor.Right(m2.foldLeft(m1) {
          case (map, key) => map + (key -> Request())
        }))
      case ((n1, Xor.Left(m1)), (n2, Xor.Left(m2))) =>
        (n1, Xor.Left(m1 ++ m2))
      case ((n1, Xor.Right(m1)), (n2, Xor.Right(m2))) =>
        (n1, Xor.Right(m1.merged(m2) {
          case ((p1, r1), (p2, r2)) => (p1, r1 ++ r2)
        }))
    })

}

object Request {

  type Value = Set[String] Xor HashMap[String, Request]

  def apply(pairs: (Symbol, Value)*): Request =
    Request(HashMap(pairs: _*))

  val zero: Request =
    Request()

  def unit[P: KeyEncoder](name: Symbol, keys: Set[P]): Request = {
    val localSet: Value = Xor.left(keys.map(KeyEncoder[P].apply))
    Request(name -> localSet)
  }

  def unit[P: KeyEncoder](name: Symbol, requests: HashMap[P, Request]) = {
    val localRequests: HashMap[String, Request] =
      requests.map(pair => (KeyEncoder[P].apply(pair._1), pair._2))
    val localXor: Value = Xor.right(localRequests)
    Request(name -> localXor)
  }

  private def lazyEncoder[A](delegate: => Encoder[A]): Encoder[A] =
    new Encoder[A] {
      lazy val _delegate: Encoder[A] = delegate
      def apply(a: A): Json = _delegate(a)
    }

  private def lazyDecoder[A](delegate: => Decoder[A]): Decoder[A] =
    new Decoder[A] {
      lazy val _delegate: Decoder[A] = delegate
      def apply(c: HCursor): Result[A] = _delegate(c)
    }

  implicit lazy val RequestEncoder: Encoder[Request] =
    lazyEncoder {
      val hashMapEnc: Encoder[HashMap[String, Request]] =
        Encoder.encodeMapLike(implicitly, RequestEncoder)
      val xorEnc: Encoder[Value] =
        Encoder.encodeXor("Atomic", "Nested")(implicitly, hashMapEnc)
      val mapEnc: Encoder[HashMap[Symbol, Value]] =
        Encoder.encodeMapLike(implicitly, xorEnc)
      mapEnc.contramap(_.here)
    }

  implicit lazy val RequestDecoder: Decoder[Request] =
    lazyDecoder {
      val hashMapDec: Decoder[HashMap[String, Request]] =
        Decoder.decodeMapLike(implicitly, RequestDecoder, implicitly)
      val xorDec: Decoder[Value] =
        Decoder.decodeXor("Atomic", "Nested")(implicitly, hashMapDec)
      val mapDec: Decoder[HashMap[Symbol, Value]] =
        Decoder.decodeMapLike(implicitly, xorDec, implicitly)
      mapDec.map(Request(_))
    }

  implicit val RequestIsMonoid: Monoid[Request] = new Monoid[Request] {
    def empty: Request = zero
    def combine(x: Request, y: Request): Request = x ++ y
  }

}
