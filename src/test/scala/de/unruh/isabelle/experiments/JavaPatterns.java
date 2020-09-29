package de.unruh.isabelle.experiments;

import de.unruh.isabelle.pure.Const;
import de.unruh.isabelle.pure.Const$;
import de.unruh.isabelle.pure.Term;
import de.unruh.isabelle.pure.Term$;
import scala.*;

import static de.unruh.isabelle.experiments.PairPattern.*;

final class Tuple0 {}

public class JavaPatterns {

    interface Case<In,Return> {
        Option<Return> apply(In in);
    }

    public static <T> Pattern1<T,T> Value() {
        return value -> new Some<>(new Tuple1<>(value));
    }

    public static <T1,T2> Option<Tuple2<T1,T2>> unapply_Pair(Tuple2<T1,T2> value) {
        return new Some<>(value);
    }
    public static <T1,T2,T3> Option<Tuple3<T1,T2,T3>> unapply_Triple(Tuple3<T1,T2,T3> value) {
        return new Some<>(value);
    }
    public static <T1,T2,T3,T4> Option<Tuple4<T1,T2,T3,T4>> unapply_Tuple4(Tuple4<T1,T2,T3,T4> value) {
        return new Some<>(value);
    }

    /*public static <T1,T2,Out1,Out2,Out3,Out4> Pattern4<Tuple2<T1,T2>,Out1,Out2,Out3,Out4>
    Pair(Pattern2<T1,Out1,Out2> pattern1, Pattern2<T2,Out3,Out4> pattern2) {
        return value -> {
            Option<Tuple2<T1,T2>> match = unapply_Pair(value);
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

    public static <T1,T2,Out1,Out2> Pattern2<Tuple2<T1,T2>,Out1,Out2>
    Pair(Pattern1<T1,Out1> pattern1, Pattern1<T2,Out2> pattern2) {
        return value -> {
            Option<Tuple2<T1,T2>> match = unapply_Pair(value);
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
*/

    public static <T,X,U1,U2,U3,U4> Case<T,X> Case(Pattern4<T,U1,U2,U3,U4> pattern,
                                                   Function4<U1,U2,U3,U4,X> function) {
        return t -> {
            Option<Tuple4<U1, U2, U3, U4>> match = pattern.apply(t);
            if (match.isEmpty())
                return Option.empty();
            Tuple4<U1, U2, U3, U4> match2 = match.get();
            return new Some<>(function.apply(match2._1(), match2._2(), match2._3(), match2._4()));
        };
    }

    @SafeVarargs
    public static <T,X> X Match(T value, Case<T,X> ... cases) {
        for (Case<T,X> cas : cases) {
            Option<X> result = cas.apply(value);
            if (result.nonEmpty())
                return result.get();
        }
        throw new MatchError(value);
    }

    public static void main(String[] args) {
        Tuple2<Tuple2<Integer, Integer>, Tuple2<Integer, Integer>> testValue =
                new Tuple2<>(new Tuple2<>(1, 2), new Tuple2<>(3, 4));

        Integer result = Match(testValue,
                Case(Pair(Pair(Value(), Value()), Pair(Value(), Value())),
                        (x, y, z, w) -> x + y + z + w)
        );

        System.out.println(result);
    }

}






