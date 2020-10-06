theory ScalaKeywords
  imports Pure
  keywords "scala" :: diag and "preamble" :: diag
begin

ML \<open>
structure ScalaKeywords : sig
datatype scala_command = Preamble of string | Code of string | Empty
(* val initialize_data : Toplevel.state -> Toplevel.state *)
val retrieve_command : Toplevel.state -> scala_command
exception E_ScalaCommand of scala_command
end = struct

datatype scala_command = Preamble of string | Code of string | Empty
exception E_ScalaCommand of scala_command

structure Data = Theory_Data(
  type T = scala_command Unsynchronized.ref option
  fun fresh () = SOME (Unsynchronized.ref Empty)
  val empty = fresh ()
  fun merge _ = fresh ()
  fun extend _ = fresh ()
)

fun put_command cmd = Toplevel.keep (fn st => let
  val thy = Toplevel.theory_of st
  val cmd_ref = case Data.get thy of SOME r => r | NONE => error "This command only works when executed with Scala support"
  val _ = case !cmd_ref of Empty => () | _ => error "Scala command reference not empty"
  val _ = cmd_ref := cmd
in () end)

fun retrieve_command st = 
  if Toplevel.is_toplevel st then Empty (* Not inside the theory *)
  else let
   val thy = Toplevel.theory_of st
  val cmd_ref = case Data.get thy of SOME r => r | NONE => error "This command only works when executed with Scala support"
  val cmd = !cmd_ref
  val _ = cmd_ref := Empty
  in cmd end

(* val initialize_data = let
  val thy_fun = Data.put (SOME (Unsynchronized.ref Empty))
  val trans = Toplevel.theory thy_fun Toplevel.empty
  in Toplevel.command_exception false trans end *)

val scala_source = Parse.input (Parse.group (fn () => "Scala source") Parse.text);

val _ = Outer_Syntax.command \<^command_keyword>\<open>scala\<close> "Invokes Scala code"
  (scala_source >> (fn source => put_command (Code (source |> Input.source_content |> fst))))
val _ = Outer_Syntax.command \<^command_keyword>\<open>preamble\<close> "Adds to Scala preamble code"
  (scala_source >> (fn source => put_command (Preamble (source |> Input.source_content |> fst))))
end
\<close>

end
