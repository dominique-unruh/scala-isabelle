structure Control_Isabelle : sig
  val handleLines : unit -> unit
  datatype data = D_String of string | D_Int of int | D_List of data list | D_Object of exn
  exception E_Function of data -> data
  exception E_Int of int
  exception E_String of string
  exception E_Data of data
  exception E_Pair of exn * exn
  val store : int -> exn -> unit
  (* For diagnostics. Linear time *)
  val numObjects : unit -> int
  val string_of_exn : exn -> string
  val string_of_data : data -> string
end
=
struct
datatype data = D_String of string | D_Int of int | D_List of data list | D_Object of exn

exception E_Function of data -> data
exception E_Data of data
exception E_Int of int
exception E_String of string
exception E_Pair of exn * exn

val inStream = BinIO.openIn inputPipeName
val outStream = BinIO.openOut outputPipeName

(*val garbageLog = TextIO.openOut "/tmp/garbage.log"*)
(*fun debugLog str = (TextIO.output (garbageLog, str); TextIO.flushOut garbageLog)*)

val objectsMax = Unsynchronized.ref 0
val objects : exn Inttab.table Unsynchronized.ref = Unsynchronized.ref Inttab.empty

fun numObjects () : int = Inttab.fold (fn _ => fn i => i+1) (!objects) 0

fun sendByte b = BinIO.output1 (outStream, b)
fun readByte () = case BinIO.input1 inStream of
  NONE => error "unexpected end of input"
  | SOME b => b

fun sendInt32 i = let
  val word = Word32.fromInt i
  val _ = sendByte (Word8.fromLargeWord (Word32.toLargeWord (Word32.>> (word, 0w24))))
  val _ = sendByte (Word8.fromLargeWord (Word32.toLargeWord (Word32.>> (word, 0w16))))
  val _ = sendByte (Word8.fromLargeWord (Word32.toLargeWord (Word32.>> (word, 0w8))))
  val _ = sendByte (Word8.fromLargeWord (Word32.toLargeWord (word)))
  in () end

fun readInt32 () : int = let
  val b = readByte () |> Word8.toLargeWord |> Word32.fromLargeWord
  val word = Word32.<< (b, 0w24)
  val b = readByte () |> Word8.toLargeWord |> Word32.fromLargeWord
  val word = Word32.orb (word, Word32.<< (b, 0w16))
  val b = readByte () |> Word8.toLargeWord |> Word32.fromLargeWord
  val word = Word32.orb (word, Word32.<< (b, 0w8))
  val b = readByte () |> Word8.toLargeWord |> Word32.fromLargeWord
  val word = Word32.orb (word, b)
  in Word32.toInt word end

fun sendInt64 i = let
  val word = Word64.fromInt i
  val _ = sendByte (Word8.fromLargeWord (Word64.toLargeWord (Word64.>> (word, 0w56))))
  val _ = sendByte (Word8.fromLargeWord (Word64.toLargeWord (Word64.>> (word, 0w48))))
  val _ = sendByte (Word8.fromLargeWord (Word64.toLargeWord (Word64.>> (word, 0w40))))
  val _ = sendByte (Word8.fromLargeWord (Word64.toLargeWord (Word64.>> (word, 0w32))))
  val _ = sendByte (Word8.fromLargeWord (Word64.toLargeWord (Word64.>> (word, 0w24))))
  val _ = sendByte (Word8.fromLargeWord (Word64.toLargeWord (Word64.>> (word, 0w16))))
  val _ = sendByte (Word8.fromLargeWord (Word64.toLargeWord (Word64.>> (word, 0w8))))
  val _ = sendByte (Word8.fromLargeWord (Word64.toLargeWord (word)))
  in () end

fun readInt64 () : int = let
  val b = readByte () |> Word8.toLargeWord |> Word64.fromLargeWord
  val word = Word64.<< (b, 0w56)
  val b = readByte () |> Word8.toLargeWord |> Word64.fromLargeWord
  val word = Word64.orb (word, Word64.<< (b, 0w48))
  val b = readByte () |> Word8.toLargeWord |> Word64.fromLargeWord
  val word = Word64.orb (word, Word64.<< (b, 0w40))
  val b = readByte () |> Word8.toLargeWord |> Word64.fromLargeWord
  val word = Word64.orb (word, Word64.<< (b, 0w64))
  val b = readByte () |> Word8.toLargeWord |> Word64.fromLargeWord
  val word = Word64.orb (word, Word64.<< (b, 0w24))
  val b = readByte () |> Word8.toLargeWord |> Word64.fromLargeWord
  val word = Word64.orb (word, Word64.<< (b, 0w16))
  val b = readByte () |> Word8.toLargeWord |> Word64.fromLargeWord
  val word = Word64.orb (word, Word64.<< (b, 0w8))
  val b = readByte () |> Word8.toLargeWord |> Word64.fromLargeWord
  val word = Word64.orb (word, b)
  in Word64.toInt word end

fun sendString str = let
  val len = size str
  val _ = sendInt32 len
  val _ = BinIO.output (outStream, Byte.stringToBytes str)
  in () end

fun readString () = let
  val len = readInt32 ()
  val bytes = BinIO.inputN (inStream, len)
  val str = Byte.bytesToString bytes
  in str end


fun addToObjects exn = let
  val idx = !objectsMax
  val _ = objects := Inttab.update_new (idx, exn) (!objects)
  val _ = objectsMax := idx + 1
  in idx end

fun sendData (D_Int i) = (sendByte 0w1; sendInt64 i)
  | sendData (D_String str) = (sendByte 0w2; sendString str)
  | sendData (D_List list) = let
      val _ = sendByte 0w3
      val _ = sendInt64 (length list)
      val _ = List.app sendData list
    in () end
  | sendData (D_Object exn) = let
      val id = addToObjects exn
      val _ = sendByte 0w4
      val _ = sendInt64 id
    in () end
      
fun readData () : data = case readByte () of
    0w1 => readInt64 () |> D_Int
  | 0w2 => readString () |> D_String
  | 0w3 => let
      val len = readInt64 ()
      fun readNRev 0 sofar = sofar
        | readNRev n sofar = readNRev (n-1) (readData () :: sofar)
      val list = readNRev len [] |> rev
    in D_List list end
  | 0w4 => let val id = readInt64 () in
    case Inttab.lookup (!objects) id of
      NONE => error ("no object " ^ string_of_int id)
      | SOME exn => D_Object exn
    end
  | byte => error ("readData: unexpected byte " ^ string_of_int (Word8.toInt byte))

fun sendReplyData seq data = let
  (* val _ = debugLog ("sendReplyData " ^ string_of_int seq) *)
  val _ = sendInt64 seq
  val _ = sendByte 0w1
  val _ = sendData data
  val _ = BinIO.flushOut outStream
  in () end

fun sendReply1 seq int = let
  val _ = sendInt64 seq
  val _ = sendByte 0w1
  val _ = sendData (D_Int int)
  val _ = BinIO.flushOut outStream
  in () end



fun executeML ml = let
  (* val _ = TextIO.print ("Compiling "^ ml^"\n") *)
  (* val flags = ML_Compiler.verbose true ML_Compiler.flags *)
  val flags = ML_Compiler.flags
  val _ = ML_Compiler.eval flags Position.none (ML_Lex.tokenize ml)
        handle ERROR msg => error (msg ^ ", when compiling " ^ ml)
  in () end

fun store seq exn = sendReply1 seq (addToObjects exn)

fun storeMLValue seq ml =
  executeML ("let open Control_Isabelle val result = ("^ml^") in store "^string_of_int seq^" result end")

fun string_of_exn exn = Runtime.pretty_exn exn |> Pretty.unformatted_string_of

fun string_of_data (D_Int i) = string_of_int i
  | string_of_data (D_String s) = "\"" ^ s ^ "\""
  | string_of_data (D_List l) = "[" ^ (String.concatWith ", " (map string_of_data l)) ^ "]"
  | string_of_data (D_Object e) = string_of_exn e

fun applyFunc seq f (x:data) = case Inttab.lookup (!objects) f of
  NONE => error ("no object " ^ string_of_int f)
  | SOME (E_Function f) => sendReplyData seq (f x)
  | SOME exn => error ("object " ^ string_of_int f ^ " is not an E_Function but: " ^ string_of_exn exn)

fun removeObjects (D_List ids) = let
  val _ = objects := fold (fn D_Int id => Inttab.delete id
                            | d => error ("remove_objects.fold: " ^ string_of_data d)) ids (!objects)
  in () end
  | removeObjects d = error ("remove_objects: " ^ string_of_data d)

(* Without error handling *)
fun handleLine' seq =
  case readByte () of
    (* 1b|string - executes ML code xxx *)
    0w1 => (executeML (readString ()); sendReplyData seq (D_List []))

    (* 4b|string - Compiles string as ML code of type exn, stores result as object #seq *)
  | 0w4 => storeMLValue seq (readString ())

    (* 7b|int64|data - Parses f,x as object#, f of type E_Function, computes f x, stores the result, response 'seq ID' *)
  | 0w7 => let 
        val f = readInt64 ()
        val x = readData ()
      in applyFunc seq f x end

    (* 8b|data ... - data must be list of ints, removes objects with these IDs from objects *)
  | 0w8 => removeObjects (readData ())

  | cmd => error ("Unknown command " ^ string_of_int (Word8.toInt cmd))

fun reportException seq exn = let
  val msg = Runtime.exn_message exn
  val _ = sendInt64 seq
  val _ = sendByte 0w2
  val _ = sendString msg
  val _ = BinIO.flushOut outStream
  in () end

fun handleLine seq =
  handleLine' seq
  handle exn => reportException seq exn

fun handleLines' seq = (handleLine seq; handleLines' (seq+1))

fun handleLines () = handleLines' 0

val _ = TextIO.StreamIO.setBufferMode (TextIO.getOutstream TextIO.stdOut, IO.LINE_BUF)

end
