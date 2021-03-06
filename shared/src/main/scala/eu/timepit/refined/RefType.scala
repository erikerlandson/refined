package eu.timepit.refined

import eu.timepit.refined.internal.{ RefineAux, RefineMAux }
import shapeless.tag.@@

import scala.reflect.macros.blackbox

trait RefType[F[_, _]] extends Serializable {

  def unsafeWrap[T, P](t: T): F[T, P]

  def unwrap[T, P](tp: F[T, P]): T

  def unsafeWrapM[T: c.WeakTypeTag, P: c.WeakTypeTag](c: blackbox.Context)(t: c.Expr[T]): c.Expr[F[T, P]]

  def unsafeRewrapM[T: c.WeakTypeTag, A: c.WeakTypeTag, B: c.WeakTypeTag](c: blackbox.Context)(ta: c.Expr[F[T, A]]): c.Expr[F[T, B]]

  def refine[P]: RefineAux[F, P] =
    new RefineAux(this)

  def refineM[P]: RefineMAux[F, P] =
    new RefineMAux

  def mapRefine[T, P, U](tp: F[T, P])(f: T => U)(implicit p: Predicate[P, U]): Either[String, F[U, P]] =
    refine(f(unwrap(tp)))
}

object RefType {

  def apply[F[_, _]](implicit rt: RefType[F]): RefType[F] = rt

  implicit val refinedRefType: RefType[Refined] =
    new RefType[Refined] {
      override def unsafeWrap[T, P](t: T): Refined[T, P] =
        Refined.unsafeApply(t)

      override def unwrap[T, P](tp: Refined[T, P]): T =
        tp.get

      override def unsafeWrapM[T: c.WeakTypeTag, P: c.WeakTypeTag](c: blackbox.Context)(t: c.Expr[T]): c.Expr[Refined[T, P]] =
        c.universe.reify(Refined.unsafeApply(t.splice))

      override def unsafeRewrapM[T: c.WeakTypeTag, A: c.WeakTypeTag, B: c.WeakTypeTag](c: blackbox.Context)(ta: c.Expr[Refined[T, A]]): c.Expr[Refined[T, B]] =
        c.universe.reify(ta.splice.asInstanceOf[Refined[T, B]])
    }

  implicit val tagRefType: RefType[@@] =
    new RefType[@@] {
      override def unsafeWrap[T, P](t: T): T @@ P =
        t.asInstanceOf[T @@ P]

      override def unwrap[T, P](tp: T @@ P): T =
        tp

      override def unsafeWrapM[T: c.WeakTypeTag, P: c.WeakTypeTag](c: blackbox.Context)(t: c.Expr[T]): c.Expr[T @@ P] =
        c.universe.reify(t.splice.asInstanceOf[T @@ P])

      override def unsafeRewrapM[T: c.WeakTypeTag, A: c.WeakTypeTag, B: c.WeakTypeTag](c: blackbox.Context)(ta: c.Expr[T @@ A]): c.Expr[T @@ B] =
        c.universe.reify(ta.splice.asInstanceOf[T @@ B])
    }

  final class RefTypeOps[F[_, _], T, P](tp: F[T, P])(implicit F: RefType[F]) {

    def unwrap: T =
      F.unwrap(tp)

    def mapRefine[U](f: T => U)(implicit p: Predicate[P, U]): Either[String, F[U, P]] =
      F.mapRefine(tp)(f)
  }

  object ops {

    implicit def toRefTypeOps[F[_, _]: RefType, T, P](tp: F[T, P]): RefTypeOps[F, T, P] =
      new RefTypeOps(tp)
  }
}
