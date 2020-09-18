package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.control.Isabelle
import de.unruh.isabelle.control.Isabelle.{DObject, ID}
import de.unruh.isabelle.mlvalue.MLValue.{Converter, unsafeFromId}

import scala.concurrent.{ExecutionContext, Future}

import Implicits._

/** An [[MLValue]] that refers to an ML function in the Isabelle process.
 * This class extends `[[MLValue]][D => R]` (which by definition refers to an ML function) but provides
 * additional methods for invoking the function. Given an `[[MLValue]][D => R]`, you can convert it to
 * an [[MLFunction]] using [[MLValue.function]].
 * */
class MLFunction[D, R] protected(id: Future[ID]) extends MLValue[D => R](id) {
  /** Invokes the function on a value `arg` stored in the Isabelle process.
   *
   * Note that the result `r` of the computation happens in a future (inside an [[MLValue]]), so exceptions
   * in the ML code are not immediately thrown. To force exceptions, use, e.g.,
   * `r.[[MLValue.retrieveNow retrieveNow]]` or `r.[[MLValue.force force]]`.
   *
   *
   * @return the function result inside an [[MLValue]]
   * */
  def apply(arg: MLValue[D])
           (implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[R] = {
    MLValue.unsafeFromId(
      for (fVal <- this.id;
           xVal <- arg.id;
           DObject(fx) <- isabelle.applyFunction(fVal, DObject(xVal)))
        yield fx
    )
  }

  /** Same as [[apply(arg:de* apply(MLValue[D])]] but first converts `arg` into an [[MLValue]]
   * (i.e., transfers it to the Isabelle process). */
  def apply(arg: D)(implicit isabelle: Isabelle, ec: ExecutionContext, converter: Converter[D]): MLValue[R] =
    apply(MLValue(arg))
}

object MLFunction {
  def unsafeFromId[D,R](id: Future[ID]): MLFunction[D, R] = new MLFunction(id)
}

/** A refinement of [[MLFunction]] (see there). If the function takes a pair `(d1,d2)` as an argument, then
 * we can treat it as a function with two arguments `d1` and `d2`. Thus this class adds additional method
 * for invoking the function with two arguments instead of a tuple. An [[MLValue]] (or [[MLFunction]])
 * can be converted into an [[MLFunction2]] using [[MLValue.function2]].
 */
class MLFunction2[D1, D2, R] protected (id: Future[ID]) extends MLFunction[(D1, D2), R](id) {
  def apply(arg1: D1, arg2: D2)
           (implicit isabelle: Isabelle, ec: ExecutionContext, converter1: Converter[D1], converter2: Converter[D2]): MLValue[R] =
    apply((arg1,arg2))

  def apply(arg1: MLValue[D1], arg2: MLValue[D2])
           (implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[R] = {
    type C1[X] = Tuple2[X,MLValue[D2]]
    type C2[X] = Tuple2[D1,X]
    apply(MLValue((arg1, arg2)).removeMLValue[C1, D1].removeMLValue[C2, D2])
  }
}

object MLFunction2 {
  def unsafeFromId[D1, D2, R](id: Future[ID]): MLFunction2[D1, D2, R] = new MLFunction2(id)
}

/** Analogue to [[MLFunction2]] but for three arguments. See [[MLFunction2]]. */
class MLFunction3[D1, D2, D3, R] protected (id: Future[ID]) extends MLFunction[(D1, D2, D3), R](id) {
  def apply(arg1: D1, arg2: D2, arg3: D3)
           (implicit isabelle: Isabelle, ec: ExecutionContext,
            converter1: Converter[D1], converter2: Converter[D2], converter3: Converter[D3]): MLValue[R] =
    apply((arg1,arg2,arg3))
  def apply(arg1: MLValue[D1], arg2: MLValue[D2], arg3: MLValue[D3])
           (implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[R] = {
    apply(MLValue((arg1,arg2,arg3)).asInstanceOf[MLValue[(D1,D2,D3)]])
  }
}

object MLFunction3 {
  def unsafeFromId[D1, D2, D3, R](id: Future[ID]): MLFunction3[D1, D2, D3, R] =
    new MLFunction3(id)
}

/** Analogue to [[MLFunction2]] but for four arguments. See [[MLFunction2]]. */
class MLFunction4[D1, D2, D3, D4, R] protected (id: Future[ID]) extends MLFunction[(D1, D2, D3, D4), R](id) {
  def apply(arg1: D1, arg2: D2, arg3: D3, arg4: D4)
           (implicit isabelle: Isabelle, ec: ExecutionContext,
            converter1: Converter[D1], converter2: Converter[D2], converter3: Converter[D3], converter4: Converter[D4]): MLValue[R] =
    apply((arg1,arg2,arg3,arg4))
  def apply(arg1: MLValue[D1], arg2: MLValue[D2], arg3: MLValue[D3], arg4: MLValue[D4])
           (implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[R] = {
    apply(MLValue((arg1,arg2,arg3,arg4)).asInstanceOf[MLValue[(D1,D2,D3,D4)]])
  }
}

object MLFunction4 {
  def unsafeFromId[D1, D2, D3, D4, R](id: Future[ID]): MLFunction4[D1, D2, D3, D4, R] =
    new MLFunction4(id)
}

/** Analogue to [[MLFunction2]] but for five arguments. See [[MLFunction2]]. */
class MLFunction5[D1, D2, D3, D4, D5, R] protected (id: Future[ID]) extends MLFunction[(D1, D2, D3, D4, D5), R](id) {
  def apply(arg1: D1, arg2: D2, arg3: D3, arg4: D4, arg5: D5)
           (implicit isabelle: Isabelle, ec: ExecutionContext,
            converter1: Converter[D1], converter2: Converter[D2], converter3: Converter[D3], converter4: Converter[D4],
            converter5: Converter[D5]): MLValue[R] =
    apply((arg1,arg2,arg3,arg4,arg5))
  def apply(arg1: MLValue[D1], arg2: MLValue[D2], arg3: MLValue[D3], arg4: MLValue[D4], arg5: MLValue[D5])
           (implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[R] = {
    apply(MLValue((arg1,arg2,arg3,arg4,arg5)).asInstanceOf[MLValue[(D1,D2,D3,D4,D5)]])
  }
}

object MLFunction5 {
  def unsafeFromId[D1, D2, D3, D4, D5, R](id: Future[ID]): MLFunction5[D1, D2, D3, D4, D5, R] =
    new MLFunction5(id)
}

/** Analogue to [[MLFunction2]] but for six arguments. See [[MLFunction2]]. */
class MLFunction6[D1, D2, D3, D4, D5, D6, R] protected (id: Future[ID]) extends MLFunction[(D1, D2, D3, D4, D5, D6), R](id) {
  def apply(arg1: D1, arg2: D2, arg3: D3, arg4: D4, arg5: D5, arg6: D6)
           (implicit isabelle: Isabelle, ec: ExecutionContext,
            converter1: Converter[D1], converter2: Converter[D2], converter3: Converter[D3], converter4: Converter[D4],
            converter5: Converter[D5], converter6: Converter[D6]): MLValue[R] =
    apply((arg1,arg2,arg3,arg4,arg5,arg6))
  def apply(arg1: MLValue[D1], arg2: MLValue[D2], arg3: MLValue[D3], arg4: MLValue[D4], arg5: MLValue[D5], arg6: MLValue[D6])
           (implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[R] = {
    apply(MLValue((arg1,arg2,arg3,arg4,arg5,arg6)).asInstanceOf[MLValue[(D1,D2,D3,D4,D5,D6)]])
  }
}

object MLFunction6 {
  def unsafeFromId[D1, D2, D3, D4, D5, D6, R](id: Future[ID]): MLFunction6[D1, D2, D3, D4, D5, D6, R] =
    new MLFunction6(id)
}

/** Analogue to [[MLFunction2]] but for seven arguments. See [[MLFunction2]]. */
class MLFunction7[D1, D2, D3, D4, D5, D6, D7, R] protected (id: Future[ID]) extends MLFunction[(D1, D2, D3, D4, D5, D6, D7), R](id) {
  def apply(arg1: D1, arg2: D2, arg3: D3, arg4: D4, arg5: D5, arg6: D6, arg7: D7)
           (implicit isabelle: Isabelle, ec: ExecutionContext,
            converter1: Converter[D1], converter2: Converter[D2], converter3: Converter[D3], converter4: Converter[D4],
            converter5: Converter[D5], converter6: Converter[D6], converter7: Converter[D7]): MLValue[R] =
    apply((arg1,arg2,arg3,arg4,arg5,arg6,arg7))
  def apply(arg1: MLValue[D1], arg2: MLValue[D2], arg3: MLValue[D3], arg4: MLValue[D4], arg5: MLValue[D5], arg6: MLValue[D6], arg7: MLValue[D7])
           (implicit isabelle: Isabelle, ec: ExecutionContext): MLValue[R] = {
    apply(MLValue((arg1,arg2,arg3,arg4,arg5,arg6,arg7)).asInstanceOf[MLValue[(D1,D2,D3,D4,D5,D6,D7)]])
  }
}

object MLFunction7 {
  def unsafeFromId[D1, D2, D3, D4, D5, D6, D7, R](id: Future[ID]): MLFunction7[D1, D2, D3, D4, D5, D6, D7, R] =
    new MLFunction7(id)
}
