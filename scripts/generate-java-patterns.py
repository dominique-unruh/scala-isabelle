#!/usr/bin/python3

import argparse, argcomplete

# PYTHON_ARGCOMPLETE_OK

parser = argparse.ArgumentParser(description="Generate pattern matchers for Java")
parser.add_argument('--package', help="Package name for generated source")
parser.add_argument('--interfaces', action='store_true', help="Generate interfaces")
parser.add_argument('--tuple-size', type=int, help="Maximum number of captured variables", default=22)
parser.add_argument('--unapply', type=str, metavar='UNAPPLY', help="Generate pattern matcher from UNAPPLY")
parser.add_argument('--matcher', type=str, metavar='MATCHER', help="Name of generated matcher function")
parser.add_argument('--unapply-typeparams', nargs='+', metavar='TYPE', help="Type parameters of UNAPPLY")
parser.add_argument('--unapply-outputs', nargs='+', metavar='OUTPUTS', help="Output types of UNAPPLY (i.e., UNAPPLY returns TupleN<OUTPUTS>)")
parser.add_argument('--unapply-input', type=str, metavar='INPUT', help="Type of UNAPPLY's input")
parser.add_argument('--matcher-class', type=str, metavar='CLASS', help="Class that should contain MATCHER")

args = parser.parse_args()

def generate_pattern_interfaces():
    print(f"package {args.package};")
    print("import scala.*;")
    print()

    print("interface Pattern0<In> { Option<Tuple0> apply(In value); }")
    for i in range(1,args.tuple_size+1):
        out = ",".join(f"Out{j}" for j in range(1,i+1))
        print(f"interface Pattern{i}<In,{out}> {{ Option<Tuple{i}<{out}>> apply(In value); }}")

def arglist(args): return ",".join(args)
def arglistSpace(args): return ", ".join(args)

def mkTupleT(args):
    args = list(args)
    if not args: return "Tuple0"
    return f"Tuple{len(args)}<{arglist(args)}>"

def mkTuple(args):
    args = list(args)
    if not args: return "new Tuple0()"
    return f"new Tuple{len(args)}<>({arglistSpace(args)})"


def generate_pattern_function(unapply, name, outs, typeArgs, inType, outTypes):
    # n = sum(len(o) for o in outs)
    outArgs = list(f"Out{i}" for o in outs for i in o)
    n = len(outArgs)

    subpatternIndex = 1
    subpatterns = []
    for (out,outType) in zip(outs,outTypes):
        patternTypeArgs = [outType] + list(f"Out{i}" for i in out)
        subpatterns.append(f"Pattern{len(out)}<{arglist(patternTypeArgs)}> pattern{subpatternIndex}")
        subpatternIndex += 1
    outType = mkTupleT(outTypes)

    print(f"public static <{arglist(typeArgs+outArgs)}> Pattern{n}<{arglist([inType]+outArgs)}>")
    print(f"  {name}({arglistSpace(subpatterns)}) {{")
    print( "  return value -> {")
    print(f"    Option<{outType}> match = {unapply}(value);")
    print( "    if (match.isEmpty()) return Option.empty();")
    print(f"    {outType} matchX = match.get();")
    subpatternIndex = 1
    args = []
    for out in outs:
        subpatternOutType = mkTupleT(f'Out{o}' for o in out)
        print(f"    Option<{subpatternOutType}> match{subpatternIndex} = pattern{subpatternIndex}.apply(matchX._{subpatternIndex}());")
        print(f"    if (match{subpatternIndex}.isEmpty()) return Option.empty();")
        print(f"    {subpatternOutType} match{subpatternIndex}X = match{subpatternIndex}.get();")
        tupleIndex = 1
        for i in out:
            args.append(f"match{subpatternIndex}X._{tupleIndex}()")
            tupleIndex += 1
        subpatternIndex += 1
    print(f"    return new Some<>({mkTuple(args)});")
    print( "  };")
    print( "}")

def generate_pattern_functions(unapply, name, typeArgs, inType, outTypes):
    def partition(n,numOuts):
        if n==0:
            if numOuts==0:
                return [[]]
            else:
                return []
        results = []
        for i in range(0,numOuts+1):
            suffix = list(range(numOuts-i+1, numOuts+1))
            for p in partition(n-1, numOuts-i):
                results.append(p + [suffix])
        return results

    print(f"package {args.package};")
    print(f"import scala.*;")
    print(f"public class {args.matcher_class} {{")
    print()
    for numOuts in range(0,args.tuple_size+1):
        for out in partition(len(outTypes),numOuts):
            # print(out)
            generate_pattern_function(unapply, name, out, typeArgs, inType, outTypes)
            print()
    print("}")

if args.interfaces:
    generate_pattern_interfaces()

if args.unapply is not None:
    generate_pattern_functions(unapply=args.unapply, name=args.matcher, inType=args.unapply_input,
                               typeArgs=args.unapply_typeparams, outTypes=args.unapply_outputs)

#generate_pattern_functions("Pair", ["T1","T2"], "Tuple2<T1,T2>", ["T1","T2"])