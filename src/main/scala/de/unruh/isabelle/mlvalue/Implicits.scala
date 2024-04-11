package de.unruh.isabelle.mlvalue

import de.unruh.isabelle.mlvalue.MLValue.Converter

import scala.runtime.BoxedUnit

object Implicits {
  @inline implicit val booleanConverter: BooleanConverter.type = BooleanConverter
  /** In Java, [[booleanConverter]] does not have the right type. You may try to use this instead. */
  @inline val javaBooleanConverter: MLValue.Converter[java.lang.Boolean] = booleanConverter.asInstanceOf[MLValue.Converter[java.lang.Boolean]]
  @inline implicit val intConverter: IntConverter.type = IntConverter
  /** In Java, [[intConverter]] does not have the right type. You may try to use this instead. */
  @inline val javaIntConverter: MLValue.Converter[java.lang.Integer] = IntConverter.asInstanceOf[MLValue.Converter[java.lang.Integer]]
  @inline implicit val bigIntConverter: BigIntConverter.type = BigIntConverter
  @inline implicit val longConverter: LongConverter.type = LongConverter
  /** In Java, [[longConverter]] does not have the right type. You may try to use this instead. */
  @inline val javaLongConverter: MLValue.Converter[java.lang.Long] = LongConverter.asInstanceOf[MLValue.Converter[java.lang.Long]]
  @inline implicit val unitConverter: UnitConverter.type = UnitConverter
  @inline implicit val stringConverter: StringConverter.type = StringConverter
  @inline implicit val dataConverter: DataConverter.type = DataConverter
  @inline implicit def listConverter[A](implicit converter: Converter[A]): ListConverter[A] = new ListConverter()(converter)
  @inline implicit def optionConverter[A](implicit converter: Converter[A]): OptionConverter[A] = new OptionConverter()(converter)
  @inline implicit def tuple2Converter[A,B](implicit a: Converter[A], b: Converter[B]): Tuple2Converter[A, B] = new Tuple2Converter(a,b)
  @inline implicit def tuple3Converter[A,B,C](implicit a: Converter[A], b: Converter[B], c: Converter[C]): Tuple3Converter[A, B, C] = new Tuple3Converter(a,b,c)
  @inline implicit def tuple4Converter[A,B,C,D](implicit a: Converter[A], b: Converter[B], c: Converter[C], d: Converter[D]): Tuple4Converter[A, B, C, D] = new Tuple4Converter(a,b,c,d)
  @inline implicit def tuple5Converter[A,B,C,D,E](implicit a: Converter[A], b: Converter[B], c: Converter[C], d: Converter[D], e: Converter[E]): Tuple5Converter[A, B, C, D, E] = new Tuple5Converter(a,b,c,d,e)
  @inline implicit def tuple6Converter[A,B,C,D,E,F](implicit a: Converter[A], b: Converter[B], c: Converter[C], d: Converter[D], e: Converter[E], f: Converter[F]): Tuple6Converter[A, B, C, D, E, F] = new Tuple6Converter(a,b,c,d,e,f)
  @inline implicit def tuple7Converter[A,B,C,D,E,F,G](implicit a: Converter[A], b: Converter[B], c: Converter[C], d: Converter[D], e: Converter[E], f: Converter[F], g: Converter[G]): Tuple7Converter[A,B,C,D,E,F,G] = new Tuple7Converter(a,b,c,d,e,f,g)
  @inline implicit def mlValueConverter[A]: MLValueConverter[A] = new MLValueConverter[A]
  @inline implicit def functionConverter[D,R](implicit converterD: Converter[D], converterR: Converter[R]): FunctionConverter[D,R] =
    new FunctionConverter()(converterD, converterR)
}
