(* A variant of control_isabelle.ml that logs all queries to Isabelle in a theory file.
 For debugging only.

 To activate the use of this variant, set the environment variable SCALA_ISABELLE_LOG_QUERIES (to true or 1).

 The resulting log is stored in the temporary directory, file Scala_Isabelle_Log.thy.
 This file is an Isabelle theory that documents and replays all queries sent from scala-isabelle to the Isabelle process.
 (Replaying succeeds under the assumption that all queries give deterministic results.)

*)

structure Control_Isabelle : sig
  (* Only for scala-isabelle internal use. Should only be called once, to initialize the communication protocol *)
  val handleLines : unit -> unit
  (* Updates the ML namespace context (atomically). Use with care (since the context is globally used) *)
  val update_ml_compilation_context : (Context.generic -> Context.generic) -> unit

  datatype data = DString of string | DInt of int | DList of data list | DObject of exn
  
  exception E_Function of data -> data
  exception E_Context of Proof.context
  exception E_List of exn list
  exception E_Bool of bool
  exception E_Option of exn option
  exception E_Int of int
  exception E_String of string
  exception E_Pair of exn * exn
  exception E_Term of term
  exception E_Cterm of cterm
  exception E_Theory of theory
  exception E_Thm of thm
  exception E_Typ of typ
  exception E_Ctyp of ctyp
  exception E_Path of Path.T
  exception E_TheoryHeader of Thy_Header.header
  exception E_Position of Position.T
  exception E_ToplevelState of Toplevel.state
  exception E_Transition of Toplevel.transition
  exception E_Keywords of Thy_Header.keywords
  exception E_Mutex of mcp_mutex
  exception E_Proofterm of Proofterm.proof
  exception E_Data of data

  val store : int -> exn -> unit
  (* For diagnostics. Linear time *)
  val numObjects : unit -> int
  val string_of_exn : exn -> string
  val message_of_exn : Proof.context option -> exn -> string
  val string_of_data : data -> string
  val sendToScala : data -> unit
  type mutex = mcp_mutex
end
=
struct
datatype data = DString of string | DInt of int | DList of data list | DObject of exn
datatype pre_data = PDString of string | PDInt of int | PDList of pre_data list | PDObject of int | PDWildcard

exception E_Function of data -> data
exception E_Context of Proof.context
exception E_List of exn list
exception E_Bool of bool
exception E_Option of exn option
exception E_Int of int
exception E_String of string
exception E_Pair of exn * exn
exception E_Term of term
exception E_Cterm of cterm
exception E_Theory of theory
exception E_Thm of thm
exception E_Typ of typ
exception E_Ctyp of ctyp
exception E_Path of Path.T
exception E_TheoryHeader of Thy_Header.header
exception E_Position of Position.T
exception E_ToplevelState of Toplevel.state
exception E_Transition of Toplevel.transition
exception E_Keywords of Thy_Header.keywords
exception E_Mutex of mcp_mutex
exception E_Proofterm of Proofterm.proof
exception E_Data of data

val (inStream, outStream) = COMMUNICATION_STREAMS
val (inSecret, outSecret) = SECRETS

fun string_of_exn exn =
  Runtime.pretty_exn exn |> Pretty.unformatted_string_of
  handle Size => "<exn description too long>"

fun message_of_exn ctxt exn =
  exn |> Runtime.exn_context (SOME (the_default \<^context> ctxt)) |> Runtime.exn_message |> YXML.parse_body |> XML.content_of
  handle Size => "<exn message too long>"

fun string_of_data (DInt i) = string_of_int i
  | string_of_data (DString s) = ("\"" ^ s ^ "\""
        handle Size => "<data description too long>")
  | string_of_data (DList l) = ("[" ^ (String.concatWith ", " (map string_of_data l)) ^ "]"
        handle Size => "<data description too long>")
  | string_of_data (DObject e) = string_of_exn e

val logFile = TextIO.openOut (File.platform_path (Path.basic "Scala_Isabelle_Log.thy"))

val _ = TextIO.output (logFile, "theory Scala_Isabelle_Log imports Pure begin\n\nML_file \<open>control_isabelle_log.ml\<close>\n\n")

fun log str = TextIO.output (logFile, str)

fun logStore seq id =
  let val _ = log ("ML \<open>val obj_" ^ string_of_int id ^ " = seq_" ^ string_of_int seq ^ "\<close>\n\n")
      val _ = TextIO.flushOut logFile
  in () end      

fun logQuery_executeML seq ml = 
  let val _ = log ("ML (* execute ML *) \<open>" ^ ml ^ "\<close>\n")
      val _ = log ("ML \<open>val seq_" ^ string_of_int seq ^ " = Control_Isabelle.DList []\<close>\n\n")
      val _ = TextIO.flushOut logFile
  in () end

fun logQuery_storeMLValue seq ml = 
  let val _ = log ("ML (* store value *) \<open>val seq_" ^ string_of_int seq ^ " = let open Control_Isabelle in\n")
      val _ = log ml
      val _ = log "\nend\<close>\n\n"
      val _ = TextIO.flushOut logFile
  in () end

fun log_data (PDInt i) = log ("DInt " ^ string_of_int i)
  | log_data (PDString s) = log ("DString \"" ^ s ^ "\"")
  | log_data (PDList l) = 
      (log "DList ["; log_data_list l; log "]")
  | log_data PDWildcard = log "_"
  | log_data (PDObject id) = log ("DObject obj_" ^ string_of_int id)
and log_data_list [] = ()
  | log_data_list [x] = log_data x
  | log_data_list (x::xs) = (log_data x; log ", "; log_data_list xs)

fun data_wildcard (PDInt _) = PDWildcard
  | data_wildcard (PDString _) = PDWildcard
  | data_wildcard (PDObject id) = PDObject id
  | data_wildcard (PDList l) = let
      val l2 = map data_wildcard l
      in if List.all (fn d => d=PDWildcard) l2 then PDWildcard
         else PDList l2
      end

fun logQuery_applyFunc seq f (x:pre_data) =  
  let val _ = log ("ML (* apply function *) \<open>val seq_" ^ string_of_int seq ^ " = let open Control_Isabelle\n  val E_Function f = obj_" ^ string_of_int f ^ "\n  val x = ")
      val _  = log_data x
      val _ = log "\nin f x end\<close>\n\n"
      val _ = TextIO.flushOut logFile
  in () end

fun logQuery_removeObjects seq (DList objs) = 
  let val _ = log ("(* Garbage collecting (seq " ^ string_of_int seq ^ "):")
      val _ = List.app (fn DInt i => log (" obj_" ^ string_of_int i)) objs
      val _ = log "\n\n"
      val _ = TextIO.flushOut logFile
  in () end

fun logReportException seq exn id =
  let val _ = log ("(**** Exception in seq_" ^ string_of_int seq ^ ": ")
      val _ = log (message_of_exn NONE exn)
      val _ = log " ****)\n\n"
      val _ = log ("ML \<open>val obj_" ^ string_of_int id ^ " = ERROR \"fake exception\"" ^ "\<close>\n\n")
      val _ = TextIO.flushOut logFile
  in () end

fun logSendReplyData seq data =
  let val _ = log "ML (* sending reply data *) \<open>local open Control_Isabelle in val "
      val _ = log_data (data_wildcard data)
      val _ = log (" = seq_" ^ string_of_int seq ^ " end\<close>\n\n")
      val _ = TextIO.flushOut logFile
  in () end

(* val (inStream, outStream) = Socket_IO.open_streams (host ^ ":" ^ string_of_int port) *)

(* val inStream = BinIO.openIn inputPipeName *)
(* val outStream = BinIO.openOut outputPipeName *)

(* Any sending of data, and any adding of data to the object store must use this mutex *)
val mutex = mcp_mutex ()
fun withMutex f x = let
  val _ = mcp_mutex_lock mutex
  val result = f x handle e => (mcp_mutex_unlock mutex; Exn.reraise e)
  val _ = mcp_mutex_unlock mutex
in result end

val objectsMax = Unsynchronized.ref 0

(* To write: use mutex. To read: be on the main thread *)
val objects : exn Inttab.table Unsynchronized.ref = Unsynchronized.ref Inttab.empty

fun numObjects () : int = Inttab.fold (fn _ => fn i => i+1) (!objects) 0

(* Only with mutex *)
fun sendByte b = BinIO.output1 (outStream, b)

fun readByte () = case BinIO.input1 inStream of
  NONE => error "unexpected end of input"
  | SOME b => b

(* Only with mutex *)
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
  in Word32.toIntX word end

(* Only with mutex *)
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
  val word = Word64.orb (word, Word64.<< (b, 0w32))
  val b = readByte () |> Word8.toLargeWord |> Word64.fromLargeWord
  val word = Word64.orb (word, Word64.<< (b, 0w24))
  val b = readByte () |> Word8.toLargeWord |> Word64.fromLargeWord
  val word = Word64.orb (word, Word64.<< (b, 0w16))
  val b = readByte () |> Word8.toLargeWord |> Word64.fromLargeWord
  val word = Word64.orb (word, Word64.<< (b, 0w8))
  val b = readByte () |> Word8.toLargeWord |> Word64.fromLargeWord
  val word = Word64.orb (word, b)
  in Word64.toIntX word end

(* Only with mutex *)
fun sendString str = let
  val len = size str
  val _ = sendInt32 len
  val _ = BinIO.output (outStream, Byte.stringToBytes str)
  in () end

fun discardNBytes (n:int) =
  if n <= 0 then ()
  else (readByte(); discardNBytes (n-1))

fun readString () = let
  val len = readInt32 ()
  val bytes = BinIO.inputN (inStream, len)
              handle Size => (discardNBytes len;
                error ("Received string longer than ML can handle ("^string_of_int len^" bytes)"))
  val str = Byte.bytesToString bytes
  in str end

(* Only with mutex *)
fun addToObjects exn = let
  val idx = !objectsMax
  val _ = objects := Inttab.update_new (idx, exn) (!objects)
  val _ = objectsMax := idx + 1
  in idx end

(* Only with mutex *)
fun sendData (PDInt i) = (sendByte 0w1; sendInt64 i)
  | sendData (PDString str) = (sendByte 0w2; sendString str)
  | sendData (PDList list) = let
      val _ = sendByte 0w3
      val _ = sendInt64 (length list)
      val _ = List.app sendData list
    in () end
  | sendData (PDObject id) = let
      val _ = sendByte 0w4
      val _ = sendInt64 id
    in () end

fun dataToPredata (DInt i) = PDInt i
  | dataToPredata (DString i) = PDString i
  | dataToPredata (DList l) = map dataToPredata l |> PDList
  | dataToPredata (DObject exn) = addToObjects exn |> PDObject

fun sendData' d = sendData (dataToPredata d)

fun readData () : pre_data = case readByte () of
    0w1 => readInt64 () |> PDInt
  | 0w2 => readString () |> PDString
  | 0w3 => let
      val len = readInt64 ()
      fun readNRev 0 sofar = sofar
        | readNRev n sofar = readNRev (n-1) (readData () :: sofar)
      val list = readNRev len [] |> rev
    in PDList list end
  | 0w4 => readInt64 () |> PDObject
  | byte => error ("readData: unexpected byte " ^ string_of_int (Word8.toInt byte))

fun predataToData (PDInt i) = DInt i
  | predataToData (PDString s) = DString s
  | predataToData (PDList l) = map predataToData l |> DList
  | predataToData (PDObject id) = 
      case Inttab.lookup (!objects) id of
        NONE => error ("no object " ^ string_of_int id)
        | SOME exn => DObject exn

(* Takes mutex *)
fun sendReplyData seq = withMutex (fn data => let
  val _ = sendInt64 seq
  val _ = sendByte 0w1
  val predata = dataToPredata data
  val _ = logSendReplyData seq predata
  val _ = sendData predata
  val _ = BinIO.flushOut outStream
  in () end)

(* Only with mutex *)
fun sendReply1 seq int = let
  val _ = sendInt64 seq
  val _ = sendByte 0w1
  val _ = sendData' (DInt int)
  val _ = BinIO.flushOut outStream
  in () end

(* Takes mutex *)
val sendToScala = withMutex (fn data => let
  val _ = sendInt64 0
  val _ = sendByte 0w3
  val _ = sendData' data
  val _ = log "(* WARNING: logging sendToScala not implemented. Some obj_... may be missing. *)"
  val _ = BinIO.flushOut outStream
  in () end)

(* Takes mutex *)
fun reportException seq = withMutex (fn exn => let
  val id = addToObjects exn
  val _ = logReportException seq exn id
  val _ = sendInt64 seq
  val _ = sendByte 0w5
  val _ = sendInt64 id
  val _ = BinIO.flushOut outStream
  in () end)

fun withErrorReporting seq f = 
  f () handle e => reportException seq e

val asyncMode = true

val asyncGroup = Future.new_group NONE
val asyncParams = {name = "scala-isabelle", group = SOME asyncGroup, deps = [], pri = 0, interrupts = false}

fun runAsync seq f = 
  if asyncMode then
    (Future.forks asyncParams [(fn () => withErrorReporting seq f)]; ())
  else
    f ()

(* fun runAsyncDep deps seq f = 
  if asyncMode then
    (Future.forks {name = "scala-isabelle", group = SOME asyncGroup, deps = deps, pri = 0, interrupts = false} [(fn () => withErrorReporting seq f)]; ())
  else
    f () *)

(* Context for compiling ML code in. Can be mutated when declaring new ML symbols *)
val ml_compilation_context = Unsynchronized.ref (Context.Theory \<^theory>)
(* Mutex for updating the context above *)
val ml_compilation_mutex = mcp_mutex ()
fun update_ml_compilation_context f = let
  val _ = mcp_mutex_lock ml_compilation_mutex
  val _ = (ml_compilation_context := f (!ml_compilation_context))
             handle e => (mcp_mutex_unlock ml_compilation_mutex; Exn.reraise e)
  val _ = mcp_mutex_unlock ml_compilation_mutex
  in () end
(* Executes ML code in the namespace of context, and updates that namespace (side effect) *)
fun executeML_update (ml:string) = let
  fun run_ml () = ML_Context.eval ML_Compiler.flags Position.none (ML_Lex.read ml)
                handle ERROR msg => error (msg ^ ", when compiling " ^ ml)
  val _ = update_ml_compilation_context (ML_Context.exec run_ml)
  in () end

(* Executes ML code in the namespace of context, and updates that namespace (side effect) *)
fun executeML ml = let
  val _ = Context.setmp_generic_context (SOME (!ml_compilation_context))
          (fn _ => ML_Context.eval ML_Compiler.flags Position.none (ML_Lex.read ml)) ()
        handle ERROR msg => error (msg ^ ", when compiling " ^ ml)
  in () end

(* Takes mutex *)
fun store seq = withMutex (fn exn => 
  let val id = addToObjects exn
      val _ = logStore seq id
  in sendReply1 seq id end)

(* Asynchronous *)
fun storeMLValue seq ml = runAsync seq (fn () =>
  executeML ("let open Control_Isabelle val result = ("^ml^") in store "^string_of_int seq^" result end"))

(* Asynchronous *)
fun applyFunc seq f (x:data) = 
  case Inttab.lookup (!objects) f of (* Must be on main thread otherwise f might be GC'd before we fetch it *)
    NONE => error ("no object " ^ string_of_int f)
  | SOME (E_Function f) => runAsync seq (fn () => sendReplyData seq (f x))
  | SOME exn => error ("object " ^ string_of_int f ^ " is not an E_Function but: " ^ string_of_exn exn)

(* Takes mutex *)
fun removeObjects seq (DList ids) = runAsync seq (withMutex (fn () => let
  val _ = objects := fold (fn DInt id => Inttab.delete id
                            | d => error ("remove_objects.fold: " ^ string_of_data d)) ids (!objects)
  in () end))
  | removeObjects _ d = error ("remove_objects: " ^ string_of_data d)

fun handleLine seq = withErrorReporting seq (fn () =>
  case readByte () of
    (* 1b|string - executes ML code xxx, updates the name space *)
    0w1 => let val ml = readString ()
               val _ = logQuery_executeML seq ml in
           runAsync seq (fn () => (executeML_update ml; sendReplyData seq (DList []))) end

    (* 4b|string - Compiles string as ML code of type exn, stores result as object #seq *)
  | 0w4 => let val ml = readString ()
               val _ = logQuery_storeMLValue seq ml
           in storeMLValue seq ml end

    (* 7b|int64|data - Parses f,x as object#, f of type E_Function, computes f x, stores the result, response 'seq ID' *)
  | 0w7 => let val f = readInt64 ()
               val x = readData ()
               val _ = logQuery_applyFunc seq f x
           in applyFunc seq f (predataToData x) end

    (* 8b|data ... - data must be list of ints, removes objects with these IDs from objects *)
  | 0w8 => let val data = readData () |> predataToData
               val _ = logQuery_removeObjects seq data
           in removeObjects seq data end

  | cmd => (log ("(* ERROR: Unknown command " ^ string_of_int (Word8.toInt cmd) ^")\n\n"); 
            error ("Unknown command " ^ string_of_int (Word8.toInt cmd))))

(* fun handleLine seq =
  handleLine' seq
  handle exn => reportException seq exn *)

fun checkSecret () = let
  val inSecret' = readInt64 ()
  val _ = if inSecret' = inSecret then ()
          else error "Input secret incorrect"
  val _ = sendInt64 outSecret
  val _ = BinIO.flushOut outStream
  in () end

fun handleLines' seq = (handleLine seq; handleLines' (seq+1))

fun handleLines () = (checkSecret (); handleLines' 0)

val _ = TextIO.StreamIO.setBufferMode (TextIO.getOutstream TextIO.stdOut, IO.LINE_BUF)

(* This provides a name for Thread.Mutex.mutex / Mutex.mutex that is independent of the Isabelle version *)
type mutex = mcp_mutex

end
