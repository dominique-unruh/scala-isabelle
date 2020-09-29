package de.unruh.isabelle.experiments;
import scala.*;
public class PairPattern {

public static <T1,T2> Pattern0<Tuple2<T1,T2>>
  Pair(Pattern0<T1> pattern1, Pattern0<T2> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple0> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple0 match1X = match1.get();
    Option<Tuple0> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple0 match2X = match2.get();
    return new Some<>(new Tuple0());
  };
}

public static <T1,T2,Out1> Pattern1<Tuple2<T1,T2>,Out1>
  Pair(Pattern1<T1,Out1> pattern1, Pattern0<T2> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple1<Out1>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple1<Out1> match1X = match1.get();
    Option<Tuple0> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple0 match2X = match2.get();
    return new Some<>(new Tuple1<>(match1X._1()));
  };
}

public static <T1,T2,Out1> Pattern1<Tuple2<T1,T2>,Out1>
  Pair(Pattern0<T1> pattern1, Pattern1<T2,Out1> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple0> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple0 match1X = match1.get();
    Option<Tuple1<Out1>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple1<Out1> match2X = match2.get();
    return new Some<>(new Tuple1<>(match2X._1()));
  };
}

public static <T1,T2,Out1,Out2> Pattern2<Tuple2<T1,T2>,Out1,Out2>
  Pair(Pattern2<T1,Out1,Out2> pattern1, Pattern0<T2> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple2<Out1,Out2>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple2<Out1,Out2> match1X = match1.get();
    Option<Tuple0> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple0 match2X = match2.get();
    return new Some<>(new Tuple2<>(match1X._1(), match1X._2()));
  };
}

public static <T1,T2,Out1,Out2> Pattern2<Tuple2<T1,T2>,Out1,Out2>
  Pair(Pattern1<T1,Out1> pattern1, Pattern1<T2,Out2> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple1<Out1>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple1<Out1> match1X = match1.get();
    Option<Tuple1<Out2>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple1<Out2> match2X = match2.get();
    return new Some<>(new Tuple2<>(match1X._1(), match2X._1()));
  };
}

public static <T1,T2,Out1,Out2> Pattern2<Tuple2<T1,T2>,Out1,Out2>
  Pair(Pattern0<T1> pattern1, Pattern2<T2,Out1,Out2> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple0> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple0 match1X = match1.get();
    Option<Tuple2<Out1,Out2>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple2<Out1,Out2> match2X = match2.get();
    return new Some<>(new Tuple2<>(match2X._1(), match2X._2()));
  };
}

public static <T1,T2,Out1,Out2,Out3> Pattern3<Tuple2<T1,T2>,Out1,Out2,Out3>
  Pair(Pattern3<T1,Out1,Out2,Out3> pattern1, Pattern0<T2> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple3<Out1,Out2,Out3>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple3<Out1,Out2,Out3> match1X = match1.get();
    Option<Tuple0> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple0 match2X = match2.get();
    return new Some<>(new Tuple3<>(match1X._1(), match1X._2(), match1X._3()));
  };
}

public static <T1,T2,Out1,Out2,Out3> Pattern3<Tuple2<T1,T2>,Out1,Out2,Out3>
  Pair(Pattern2<T1,Out1,Out2> pattern1, Pattern1<T2,Out3> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple2<Out1,Out2>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple2<Out1,Out2> match1X = match1.get();
    Option<Tuple1<Out3>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple1<Out3> match2X = match2.get();
    return new Some<>(new Tuple3<>(match1X._1(), match1X._2(), match2X._1()));
  };
}

public static <T1,T2,Out1,Out2,Out3> Pattern3<Tuple2<T1,T2>,Out1,Out2,Out3>
  Pair(Pattern1<T1,Out1> pattern1, Pattern2<T2,Out2,Out3> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple1<Out1>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple1<Out1> match1X = match1.get();
    Option<Tuple2<Out2,Out3>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple2<Out2,Out3> match2X = match2.get();
    return new Some<>(new Tuple3<>(match1X._1(), match2X._1(), match2X._2()));
  };
}

public static <T1,T2,Out1,Out2,Out3> Pattern3<Tuple2<T1,T2>,Out1,Out2,Out3>
  Pair(Pattern0<T1> pattern1, Pattern3<T2,Out1,Out2,Out3> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple0> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple0 match1X = match1.get();
    Option<Tuple3<Out1,Out2,Out3>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple3<Out1,Out2,Out3> match2X = match2.get();
    return new Some<>(new Tuple3<>(match2X._1(), match2X._2(), match2X._3()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4> Pattern4<Tuple2<T1,T2>,Out1,Out2,Out3,Out4>
  Pair(Pattern4<T1,Out1,Out2,Out3,Out4> pattern1, Pattern0<T2> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple4<Out1,Out2,Out3,Out4>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple4<Out1,Out2,Out3,Out4> match1X = match1.get();
    Option<Tuple0> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple0 match2X = match2.get();
    return new Some<>(new Tuple4<>(match1X._1(), match1X._2(), match1X._3(), match1X._4()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4> Pattern4<Tuple2<T1,T2>,Out1,Out2,Out3,Out4>
  Pair(Pattern3<T1,Out1,Out2,Out3> pattern1, Pattern1<T2,Out4> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple3<Out1,Out2,Out3>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple3<Out1,Out2,Out3> match1X = match1.get();
    Option<Tuple1<Out4>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple1<Out4> match2X = match2.get();
    return new Some<>(new Tuple4<>(match1X._1(), match1X._2(), match1X._3(), match2X._1()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4> Pattern4<Tuple2<T1,T2>,Out1,Out2,Out3,Out4>
  Pair(Pattern2<T1,Out1,Out2> pattern1, Pattern2<T2,Out3,Out4> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple2<Out1,Out2>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple2<Out1,Out2> match1X = match1.get();
    Option<Tuple2<Out3,Out4>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple2<Out3,Out4> match2X = match2.get();
    return new Some<>(new Tuple4<>(match1X._1(), match1X._2(), match2X._1(), match2X._2()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4> Pattern4<Tuple2<T1,T2>,Out1,Out2,Out3,Out4>
  Pair(Pattern1<T1,Out1> pattern1, Pattern3<T2,Out2,Out3,Out4> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple1<Out1>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple1<Out1> match1X = match1.get();
    Option<Tuple3<Out2,Out3,Out4>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple3<Out2,Out3,Out4> match2X = match2.get();
    return new Some<>(new Tuple4<>(match1X._1(), match2X._1(), match2X._2(), match2X._3()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4> Pattern4<Tuple2<T1,T2>,Out1,Out2,Out3,Out4>
  Pair(Pattern0<T1> pattern1, Pattern4<T2,Out1,Out2,Out3,Out4> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple0> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple0 match1X = match1.get();
    Option<Tuple4<Out1,Out2,Out3,Out4>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple4<Out1,Out2,Out3,Out4> match2X = match2.get();
    return new Some<>(new Tuple4<>(match2X._1(), match2X._2(), match2X._3(), match2X._4()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5> Pattern5<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5>
  Pair(Pattern5<T1,Out1,Out2,Out3,Out4,Out5> pattern1, Pattern0<T2> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple5<Out1,Out2,Out3,Out4,Out5>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple5<Out1,Out2,Out3,Out4,Out5> match1X = match1.get();
    Option<Tuple0> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple0 match2X = match2.get();
    return new Some<>(new Tuple5<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match1X._5()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5> Pattern5<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5>
  Pair(Pattern4<T1,Out1,Out2,Out3,Out4> pattern1, Pattern1<T2,Out5> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple4<Out1,Out2,Out3,Out4>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple4<Out1,Out2,Out3,Out4> match1X = match1.get();
    Option<Tuple1<Out5>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple1<Out5> match2X = match2.get();
    return new Some<>(new Tuple5<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match2X._1()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5> Pattern5<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5>
  Pair(Pattern3<T1,Out1,Out2,Out3> pattern1, Pattern2<T2,Out4,Out5> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple3<Out1,Out2,Out3>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple3<Out1,Out2,Out3> match1X = match1.get();
    Option<Tuple2<Out4,Out5>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple2<Out4,Out5> match2X = match2.get();
    return new Some<>(new Tuple5<>(match1X._1(), match1X._2(), match1X._3(), match2X._1(), match2X._2()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5> Pattern5<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5>
  Pair(Pattern2<T1,Out1,Out2> pattern1, Pattern3<T2,Out3,Out4,Out5> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple2<Out1,Out2>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple2<Out1,Out2> match1X = match1.get();
    Option<Tuple3<Out3,Out4,Out5>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple3<Out3,Out4,Out5> match2X = match2.get();
    return new Some<>(new Tuple5<>(match1X._1(), match1X._2(), match2X._1(), match2X._2(), match2X._3()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5> Pattern5<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5>
  Pair(Pattern1<T1,Out1> pattern1, Pattern4<T2,Out2,Out3,Out4,Out5> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple1<Out1>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple1<Out1> match1X = match1.get();
    Option<Tuple4<Out2,Out3,Out4,Out5>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple4<Out2,Out3,Out4,Out5> match2X = match2.get();
    return new Some<>(new Tuple5<>(match1X._1(), match2X._1(), match2X._2(), match2X._3(), match2X._4()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5> Pattern5<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5>
  Pair(Pattern0<T1> pattern1, Pattern5<T2,Out1,Out2,Out3,Out4,Out5> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple0> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple0 match1X = match1.get();
    Option<Tuple5<Out1,Out2,Out3,Out4,Out5>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple5<Out1,Out2,Out3,Out4,Out5> match2X = match2.get();
    return new Some<>(new Tuple5<>(match2X._1(), match2X._2(), match2X._3(), match2X._4(), match2X._5()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6> Pattern6<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6>
  Pair(Pattern6<T1,Out1,Out2,Out3,Out4,Out5,Out6> pattern1, Pattern0<T2> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple6<Out1,Out2,Out3,Out4,Out5,Out6>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple6<Out1,Out2,Out3,Out4,Out5,Out6> match1X = match1.get();
    Option<Tuple0> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple0 match2X = match2.get();
    return new Some<>(new Tuple6<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match1X._5(), match1X._6()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6> Pattern6<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6>
  Pair(Pattern5<T1,Out1,Out2,Out3,Out4,Out5> pattern1, Pattern1<T2,Out6> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple5<Out1,Out2,Out3,Out4,Out5>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple5<Out1,Out2,Out3,Out4,Out5> match1X = match1.get();
    Option<Tuple1<Out6>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple1<Out6> match2X = match2.get();
    return new Some<>(new Tuple6<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match1X._5(), match2X._1()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6> Pattern6<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6>
  Pair(Pattern4<T1,Out1,Out2,Out3,Out4> pattern1, Pattern2<T2,Out5,Out6> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple4<Out1,Out2,Out3,Out4>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple4<Out1,Out2,Out3,Out4> match1X = match1.get();
    Option<Tuple2<Out5,Out6>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple2<Out5,Out6> match2X = match2.get();
    return new Some<>(new Tuple6<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match2X._1(), match2X._2()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6> Pattern6<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6>
  Pair(Pattern3<T1,Out1,Out2,Out3> pattern1, Pattern3<T2,Out4,Out5,Out6> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple3<Out1,Out2,Out3>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple3<Out1,Out2,Out3> match1X = match1.get();
    Option<Tuple3<Out4,Out5,Out6>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple3<Out4,Out5,Out6> match2X = match2.get();
    return new Some<>(new Tuple6<>(match1X._1(), match1X._2(), match1X._3(), match2X._1(), match2X._2(), match2X._3()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6> Pattern6<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6>
  Pair(Pattern2<T1,Out1,Out2> pattern1, Pattern4<T2,Out3,Out4,Out5,Out6> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple2<Out1,Out2>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple2<Out1,Out2> match1X = match1.get();
    Option<Tuple4<Out3,Out4,Out5,Out6>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple4<Out3,Out4,Out5,Out6> match2X = match2.get();
    return new Some<>(new Tuple6<>(match1X._1(), match1X._2(), match2X._1(), match2X._2(), match2X._3(), match2X._4()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6> Pattern6<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6>
  Pair(Pattern1<T1,Out1> pattern1, Pattern5<T2,Out2,Out3,Out4,Out5,Out6> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple1<Out1>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple1<Out1> match1X = match1.get();
    Option<Tuple5<Out2,Out3,Out4,Out5,Out6>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple5<Out2,Out3,Out4,Out5,Out6> match2X = match2.get();
    return new Some<>(new Tuple6<>(match1X._1(), match2X._1(), match2X._2(), match2X._3(), match2X._4(), match2X._5()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6> Pattern6<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6>
  Pair(Pattern0<T1> pattern1, Pattern6<T2,Out1,Out2,Out3,Out4,Out5,Out6> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple0> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple0 match1X = match1.get();
    Option<Tuple6<Out1,Out2,Out3,Out4,Out5,Out6>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple6<Out1,Out2,Out3,Out4,Out5,Out6> match2X = match2.get();
    return new Some<>(new Tuple6<>(match2X._1(), match2X._2(), match2X._3(), match2X._4(), match2X._5(), match2X._6()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7> Pattern7<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7>
  Pair(Pattern7<T1,Out1,Out2,Out3,Out4,Out5,Out6,Out7> pattern1, Pattern0<T2> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple7<Out1,Out2,Out3,Out4,Out5,Out6,Out7>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple7<Out1,Out2,Out3,Out4,Out5,Out6,Out7> match1X = match1.get();
    Option<Tuple0> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple0 match2X = match2.get();
    return new Some<>(new Tuple7<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match1X._5(), match1X._6(), match1X._7()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7> Pattern7<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7>
  Pair(Pattern6<T1,Out1,Out2,Out3,Out4,Out5,Out6> pattern1, Pattern1<T2,Out7> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple6<Out1,Out2,Out3,Out4,Out5,Out6>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple6<Out1,Out2,Out3,Out4,Out5,Out6> match1X = match1.get();
    Option<Tuple1<Out7>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple1<Out7> match2X = match2.get();
    return new Some<>(new Tuple7<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match1X._5(), match1X._6(), match2X._1()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7> Pattern7<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7>
  Pair(Pattern5<T1,Out1,Out2,Out3,Out4,Out5> pattern1, Pattern2<T2,Out6,Out7> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple5<Out1,Out2,Out3,Out4,Out5>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple5<Out1,Out2,Out3,Out4,Out5> match1X = match1.get();
    Option<Tuple2<Out6,Out7>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple2<Out6,Out7> match2X = match2.get();
    return new Some<>(new Tuple7<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match1X._5(), match2X._1(), match2X._2()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7> Pattern7<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7>
  Pair(Pattern4<T1,Out1,Out2,Out3,Out4> pattern1, Pattern3<T2,Out5,Out6,Out7> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple4<Out1,Out2,Out3,Out4>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple4<Out1,Out2,Out3,Out4> match1X = match1.get();
    Option<Tuple3<Out5,Out6,Out7>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple3<Out5,Out6,Out7> match2X = match2.get();
    return new Some<>(new Tuple7<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match2X._1(), match2X._2(), match2X._3()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7> Pattern7<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7>
  Pair(Pattern3<T1,Out1,Out2,Out3> pattern1, Pattern4<T2,Out4,Out5,Out6,Out7> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple3<Out1,Out2,Out3>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple3<Out1,Out2,Out3> match1X = match1.get();
    Option<Tuple4<Out4,Out5,Out6,Out7>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple4<Out4,Out5,Out6,Out7> match2X = match2.get();
    return new Some<>(new Tuple7<>(match1X._1(), match1X._2(), match1X._3(), match2X._1(), match2X._2(), match2X._3(), match2X._4()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7> Pattern7<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7>
  Pair(Pattern2<T1,Out1,Out2> pattern1, Pattern5<T2,Out3,Out4,Out5,Out6,Out7> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple2<Out1,Out2>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple2<Out1,Out2> match1X = match1.get();
    Option<Tuple5<Out3,Out4,Out5,Out6,Out7>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple5<Out3,Out4,Out5,Out6,Out7> match2X = match2.get();
    return new Some<>(new Tuple7<>(match1X._1(), match1X._2(), match2X._1(), match2X._2(), match2X._3(), match2X._4(), match2X._5()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7> Pattern7<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7>
  Pair(Pattern1<T1,Out1> pattern1, Pattern6<T2,Out2,Out3,Out4,Out5,Out6,Out7> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple1<Out1>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple1<Out1> match1X = match1.get();
    Option<Tuple6<Out2,Out3,Out4,Out5,Out6,Out7>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple6<Out2,Out3,Out4,Out5,Out6,Out7> match2X = match2.get();
    return new Some<>(new Tuple7<>(match1X._1(), match2X._1(), match2X._2(), match2X._3(), match2X._4(), match2X._5(), match2X._6()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7> Pattern7<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7>
  Pair(Pattern0<T1> pattern1, Pattern7<T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple0> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple0 match1X = match1.get();
    Option<Tuple7<Out1,Out2,Out3,Out4,Out5,Out6,Out7>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple7<Out1,Out2,Out3,Out4,Out5,Out6,Out7> match2X = match2.get();
    return new Some<>(new Tuple7<>(match2X._1(), match2X._2(), match2X._3(), match2X._4(), match2X._5(), match2X._6(), match2X._7()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8> Pattern8<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8>
  Pair(Pattern8<T1,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8> pattern1, Pattern0<T2> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple8<Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple8<Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8> match1X = match1.get();
    Option<Tuple0> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple0 match2X = match2.get();
    return new Some<>(new Tuple8<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match1X._5(), match1X._6(), match1X._7(), match1X._8()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8> Pattern8<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8>
  Pair(Pattern7<T1,Out1,Out2,Out3,Out4,Out5,Out6,Out7> pattern1, Pattern1<T2,Out8> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple7<Out1,Out2,Out3,Out4,Out5,Out6,Out7>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple7<Out1,Out2,Out3,Out4,Out5,Out6,Out7> match1X = match1.get();
    Option<Tuple1<Out8>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple1<Out8> match2X = match2.get();
    return new Some<>(new Tuple8<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match1X._5(), match1X._6(), match1X._7(), match2X._1()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8> Pattern8<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8>
  Pair(Pattern6<T1,Out1,Out2,Out3,Out4,Out5,Out6> pattern1, Pattern2<T2,Out7,Out8> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple6<Out1,Out2,Out3,Out4,Out5,Out6>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple6<Out1,Out2,Out3,Out4,Out5,Out6> match1X = match1.get();
    Option<Tuple2<Out7,Out8>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple2<Out7,Out8> match2X = match2.get();
    return new Some<>(new Tuple8<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match1X._5(), match1X._6(), match2X._1(), match2X._2()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8> Pattern8<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8>
  Pair(Pattern5<T1,Out1,Out2,Out3,Out4,Out5> pattern1, Pattern3<T2,Out6,Out7,Out8> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple5<Out1,Out2,Out3,Out4,Out5>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple5<Out1,Out2,Out3,Out4,Out5> match1X = match1.get();
    Option<Tuple3<Out6,Out7,Out8>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple3<Out6,Out7,Out8> match2X = match2.get();
    return new Some<>(new Tuple8<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match1X._5(), match2X._1(), match2X._2(), match2X._3()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8> Pattern8<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8>
  Pair(Pattern4<T1,Out1,Out2,Out3,Out4> pattern1, Pattern4<T2,Out5,Out6,Out7,Out8> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple4<Out1,Out2,Out3,Out4>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple4<Out1,Out2,Out3,Out4> match1X = match1.get();
    Option<Tuple4<Out5,Out6,Out7,Out8>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple4<Out5,Out6,Out7,Out8> match2X = match2.get();
    return new Some<>(new Tuple8<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match2X._1(), match2X._2(), match2X._3(), match2X._4()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8> Pattern8<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8>
  Pair(Pattern3<T1,Out1,Out2,Out3> pattern1, Pattern5<T2,Out4,Out5,Out6,Out7,Out8> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple3<Out1,Out2,Out3>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple3<Out1,Out2,Out3> match1X = match1.get();
    Option<Tuple5<Out4,Out5,Out6,Out7,Out8>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple5<Out4,Out5,Out6,Out7,Out8> match2X = match2.get();
    return new Some<>(new Tuple8<>(match1X._1(), match1X._2(), match1X._3(), match2X._1(), match2X._2(), match2X._3(), match2X._4(), match2X._5()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8> Pattern8<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8>
  Pair(Pattern2<T1,Out1,Out2> pattern1, Pattern6<T2,Out3,Out4,Out5,Out6,Out7,Out8> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple2<Out1,Out2>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple2<Out1,Out2> match1X = match1.get();
    Option<Tuple6<Out3,Out4,Out5,Out6,Out7,Out8>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple6<Out3,Out4,Out5,Out6,Out7,Out8> match2X = match2.get();
    return new Some<>(new Tuple8<>(match1X._1(), match1X._2(), match2X._1(), match2X._2(), match2X._3(), match2X._4(), match2X._5(), match2X._6()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8> Pattern8<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8>
  Pair(Pattern1<T1,Out1> pattern1, Pattern7<T2,Out2,Out3,Out4,Out5,Out6,Out7,Out8> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple1<Out1>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple1<Out1> match1X = match1.get();
    Option<Tuple7<Out2,Out3,Out4,Out5,Out6,Out7,Out8>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple7<Out2,Out3,Out4,Out5,Out6,Out7,Out8> match2X = match2.get();
    return new Some<>(new Tuple8<>(match1X._1(), match2X._1(), match2X._2(), match2X._3(), match2X._4(), match2X._5(), match2X._6(), match2X._7()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8> Pattern8<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8>
  Pair(Pattern0<T1> pattern1, Pattern8<T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple0> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple0 match1X = match1.get();
    Option<Tuple8<Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple8<Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8> match2X = match2.get();
    return new Some<>(new Tuple8<>(match2X._1(), match2X._2(), match2X._3(), match2X._4(), match2X._5(), match2X._6(), match2X._7(), match2X._8()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9> Pattern9<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9>
  Pair(Pattern9<T1,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9> pattern1, Pattern0<T2> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple9<Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple9<Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9> match1X = match1.get();
    Option<Tuple0> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple0 match2X = match2.get();
    return new Some<>(new Tuple9<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match1X._5(), match1X._6(), match1X._7(), match1X._8(), match1X._9()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9> Pattern9<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9>
  Pair(Pattern8<T1,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8> pattern1, Pattern1<T2,Out9> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple8<Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple8<Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8> match1X = match1.get();
    Option<Tuple1<Out9>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple1<Out9> match2X = match2.get();
    return new Some<>(new Tuple9<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match1X._5(), match1X._6(), match1X._7(), match1X._8(), match2X._1()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9> Pattern9<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9>
  Pair(Pattern7<T1,Out1,Out2,Out3,Out4,Out5,Out6,Out7> pattern1, Pattern2<T2,Out8,Out9> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple7<Out1,Out2,Out3,Out4,Out5,Out6,Out7>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple7<Out1,Out2,Out3,Out4,Out5,Out6,Out7> match1X = match1.get();
    Option<Tuple2<Out8,Out9>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple2<Out8,Out9> match2X = match2.get();
    return new Some<>(new Tuple9<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match1X._5(), match1X._6(), match1X._7(), match2X._1(), match2X._2()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9> Pattern9<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9>
  Pair(Pattern6<T1,Out1,Out2,Out3,Out4,Out5,Out6> pattern1, Pattern3<T2,Out7,Out8,Out9> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple6<Out1,Out2,Out3,Out4,Out5,Out6>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple6<Out1,Out2,Out3,Out4,Out5,Out6> match1X = match1.get();
    Option<Tuple3<Out7,Out8,Out9>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple3<Out7,Out8,Out9> match2X = match2.get();
    return new Some<>(new Tuple9<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match1X._5(), match1X._6(), match2X._1(), match2X._2(), match2X._3()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9> Pattern9<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9>
  Pair(Pattern5<T1,Out1,Out2,Out3,Out4,Out5> pattern1, Pattern4<T2,Out6,Out7,Out8,Out9> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple5<Out1,Out2,Out3,Out4,Out5>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple5<Out1,Out2,Out3,Out4,Out5> match1X = match1.get();
    Option<Tuple4<Out6,Out7,Out8,Out9>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple4<Out6,Out7,Out8,Out9> match2X = match2.get();
    return new Some<>(new Tuple9<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match1X._5(), match2X._1(), match2X._2(), match2X._3(), match2X._4()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9> Pattern9<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9>
  Pair(Pattern4<T1,Out1,Out2,Out3,Out4> pattern1, Pattern5<T2,Out5,Out6,Out7,Out8,Out9> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple4<Out1,Out2,Out3,Out4>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple4<Out1,Out2,Out3,Out4> match1X = match1.get();
    Option<Tuple5<Out5,Out6,Out7,Out8,Out9>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple5<Out5,Out6,Out7,Out8,Out9> match2X = match2.get();
    return new Some<>(new Tuple9<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match2X._1(), match2X._2(), match2X._3(), match2X._4(), match2X._5()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9> Pattern9<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9>
  Pair(Pattern3<T1,Out1,Out2,Out3> pattern1, Pattern6<T2,Out4,Out5,Out6,Out7,Out8,Out9> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple3<Out1,Out2,Out3>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple3<Out1,Out2,Out3> match1X = match1.get();
    Option<Tuple6<Out4,Out5,Out6,Out7,Out8,Out9>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple6<Out4,Out5,Out6,Out7,Out8,Out9> match2X = match2.get();
    return new Some<>(new Tuple9<>(match1X._1(), match1X._2(), match1X._3(), match2X._1(), match2X._2(), match2X._3(), match2X._4(), match2X._5(), match2X._6()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9> Pattern9<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9>
  Pair(Pattern2<T1,Out1,Out2> pattern1, Pattern7<T2,Out3,Out4,Out5,Out6,Out7,Out8,Out9> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple2<Out1,Out2>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple2<Out1,Out2> match1X = match1.get();
    Option<Tuple7<Out3,Out4,Out5,Out6,Out7,Out8,Out9>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple7<Out3,Out4,Out5,Out6,Out7,Out8,Out9> match2X = match2.get();
    return new Some<>(new Tuple9<>(match1X._1(), match1X._2(), match2X._1(), match2X._2(), match2X._3(), match2X._4(), match2X._5(), match2X._6(), match2X._7()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9> Pattern9<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9>
  Pair(Pattern1<T1,Out1> pattern1, Pattern8<T2,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple1<Out1>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple1<Out1> match1X = match1.get();
    Option<Tuple8<Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple8<Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9> match2X = match2.get();
    return new Some<>(new Tuple9<>(match1X._1(), match2X._1(), match2X._2(), match2X._3(), match2X._4(), match2X._5(), match2X._6(), match2X._7(), match2X._8()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9> Pattern9<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9>
  Pair(Pattern0<T1> pattern1, Pattern9<T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple0> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple0 match1X = match1.get();
    Option<Tuple9<Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple9<Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9> match2X = match2.get();
    return new Some<>(new Tuple9<>(match2X._1(), match2X._2(), match2X._3(), match2X._4(), match2X._5(), match2X._6(), match2X._7(), match2X._8(), match2X._9()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10> Pattern10<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10>
  Pair(Pattern10<T1,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10> pattern1, Pattern0<T2> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple10<Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple10<Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10> match1X = match1.get();
    Option<Tuple0> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple0 match2X = match2.get();
    return new Some<>(new Tuple10<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match1X._5(), match1X._6(), match1X._7(), match1X._8(), match1X._9(), match1X._10()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10> Pattern10<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10>
  Pair(Pattern9<T1,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9> pattern1, Pattern1<T2,Out10> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple9<Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple9<Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9> match1X = match1.get();
    Option<Tuple1<Out10>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple1<Out10> match2X = match2.get();
    return new Some<>(new Tuple10<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match1X._5(), match1X._6(), match1X._7(), match1X._8(), match1X._9(), match2X._1()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10> Pattern10<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10>
  Pair(Pattern8<T1,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8> pattern1, Pattern2<T2,Out9,Out10> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple8<Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple8<Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8> match1X = match1.get();
    Option<Tuple2<Out9,Out10>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple2<Out9,Out10> match2X = match2.get();
    return new Some<>(new Tuple10<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match1X._5(), match1X._6(), match1X._7(), match1X._8(), match2X._1(), match2X._2()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10> Pattern10<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10>
  Pair(Pattern7<T1,Out1,Out2,Out3,Out4,Out5,Out6,Out7> pattern1, Pattern3<T2,Out8,Out9,Out10> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple7<Out1,Out2,Out3,Out4,Out5,Out6,Out7>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple7<Out1,Out2,Out3,Out4,Out5,Out6,Out7> match1X = match1.get();
    Option<Tuple3<Out8,Out9,Out10>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple3<Out8,Out9,Out10> match2X = match2.get();
    return new Some<>(new Tuple10<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match1X._5(), match1X._6(), match1X._7(), match2X._1(), match2X._2(), match2X._3()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10> Pattern10<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10>
  Pair(Pattern6<T1,Out1,Out2,Out3,Out4,Out5,Out6> pattern1, Pattern4<T2,Out7,Out8,Out9,Out10> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple6<Out1,Out2,Out3,Out4,Out5,Out6>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple6<Out1,Out2,Out3,Out4,Out5,Out6> match1X = match1.get();
    Option<Tuple4<Out7,Out8,Out9,Out10>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple4<Out7,Out8,Out9,Out10> match2X = match2.get();
    return new Some<>(new Tuple10<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match1X._5(), match1X._6(), match2X._1(), match2X._2(), match2X._3(), match2X._4()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10> Pattern10<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10>
  Pair(Pattern5<T1,Out1,Out2,Out3,Out4,Out5> pattern1, Pattern5<T2,Out6,Out7,Out8,Out9,Out10> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple5<Out1,Out2,Out3,Out4,Out5>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple5<Out1,Out2,Out3,Out4,Out5> match1X = match1.get();
    Option<Tuple5<Out6,Out7,Out8,Out9,Out10>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple5<Out6,Out7,Out8,Out9,Out10> match2X = match2.get();
    return new Some<>(new Tuple10<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match1X._5(), match2X._1(), match2X._2(), match2X._3(), match2X._4(), match2X._5()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10> Pattern10<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10>
  Pair(Pattern4<T1,Out1,Out2,Out3,Out4> pattern1, Pattern6<T2,Out5,Out6,Out7,Out8,Out9,Out10> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple4<Out1,Out2,Out3,Out4>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple4<Out1,Out2,Out3,Out4> match1X = match1.get();
    Option<Tuple6<Out5,Out6,Out7,Out8,Out9,Out10>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple6<Out5,Out6,Out7,Out8,Out9,Out10> match2X = match2.get();
    return new Some<>(new Tuple10<>(match1X._1(), match1X._2(), match1X._3(), match1X._4(), match2X._1(), match2X._2(), match2X._3(), match2X._4(), match2X._5(), match2X._6()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10> Pattern10<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10>
  Pair(Pattern3<T1,Out1,Out2,Out3> pattern1, Pattern7<T2,Out4,Out5,Out6,Out7,Out8,Out9,Out10> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple3<Out1,Out2,Out3>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple3<Out1,Out2,Out3> match1X = match1.get();
    Option<Tuple7<Out4,Out5,Out6,Out7,Out8,Out9,Out10>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple7<Out4,Out5,Out6,Out7,Out8,Out9,Out10> match2X = match2.get();
    return new Some<>(new Tuple10<>(match1X._1(), match1X._2(), match1X._3(), match2X._1(), match2X._2(), match2X._3(), match2X._4(), match2X._5(), match2X._6(), match2X._7()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10> Pattern10<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10>
  Pair(Pattern2<T1,Out1,Out2> pattern1, Pattern8<T2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple2<Out1,Out2>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple2<Out1,Out2> match1X = match1.get();
    Option<Tuple8<Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple8<Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10> match2X = match2.get();
    return new Some<>(new Tuple10<>(match1X._1(), match1X._2(), match2X._1(), match2X._2(), match2X._3(), match2X._4(), match2X._5(), match2X._6(), match2X._7(), match2X._8()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10> Pattern10<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10>
  Pair(Pattern1<T1,Out1> pattern1, Pattern9<T2,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple1<Out1>> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple1<Out1> match1X = match1.get();
    Option<Tuple9<Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple9<Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10> match2X = match2.get();
    return new Some<>(new Tuple10<>(match1X._1(), match2X._1(), match2X._2(), match2X._3(), match2X._4(), match2X._5(), match2X._6(), match2X._7(), match2X._8(), match2X._9()));
  };
}

public static <T1,T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10> Pattern10<Tuple2<T1,T2>,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10>
  Pair(Pattern0<T1> pattern1, Pattern10<T2,Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10> pattern2) {
  return value -> {
    Option<Tuple2<T1,T2>> match = JavaPatterns.unapply_Pair(value);
    if (match.isEmpty()) return Option.empty();
    Tuple2<T1,T2> matchX = match.get();
    Option<Tuple0> match1 = pattern1.apply(matchX._1());
    if (match1.isEmpty()) return Option.empty();
    Tuple0 match1X = match1.get();
    Option<Tuple10<Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10>> match2 = pattern2.apply(matchX._2());
    if (match2.isEmpty()) return Option.empty();
    Tuple10<Out1,Out2,Out3,Out4,Out5,Out6,Out7,Out8,Out9,Out10> match2X = match2.get();
    return new Some<>(new Tuple10<>(match2X._1(), match2X._2(), match2X._3(), match2X._4(), match2X._5(), match2X._6(), match2X._7(), match2X._8(), match2X._9(), match2X._10()));
  };
}

}
