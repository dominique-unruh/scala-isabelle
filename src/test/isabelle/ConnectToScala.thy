theory ConnectToScala
  imports Main
  keywords "scala_diag" :: diag
begin

ML \<open>
fun system string = if OS.Process.system string |> OS.Process.isSuccess then () else
  error ("Command " ^ string ^ " returned non-zero error code")
val random = string_of_int (Random.random_range 0 100000000000) ^ serial_string ()
val inputPipeName = "/tmp/input-pipe" ^ random
val outputPipeName = "/tmp/output-pipe" ^ random
val _ = system ("/home/unruh/svn/qrhl-tool/scala-isabelle/scripts/connect-to-running-isabelle.py "
          ^ inputPipeName ^ " " ^ outputPipeName ^ " " ^ "/tmp/scala-isabelle.log")
\<close>

ML_file "../../main/resources/de/unruh/isabelle/control/control_isabelle.ml"

ML \<open>
val control_isabelle_struct =
  #allStruct ML_Env.name_space () |> find_first (fn (n,_) => n = "Control_Isabelle") |> Option.valOf |> snd
val params : Standard_Thread.params = {name="scala-isabelle protocol", stack_limit=NONE, interrupts=false}
val thread = Standard_Thread.fork params (fn () =>
  (#enterStruct ML_Env.name_space ("Control_Isabelle", control_isabelle_struct);
   Control_Isabelle.handleLines ()))
\<close>

ML \<open>
local
open Control_Isabelle 
fun string_of_attribs [] = ""
  | string_of_attribs attribs = "[" ^ String.concatWith ", " (map (String.concat o map Token.content_of) attribs) ^ "]"

fun string_of_thmref (fact : Facts.ref, attribs) : string =
  Facts.string_of_ref fact ^ string_of_attribs attribs

val scala_source = Parse.input (Parse.group (fn () => "Scala source") Parse.text)
val scala_arg_types : (string * ((Proof.context -> exn) parser)) Symtab.table = Symtab.make
  [("String", ("String", Parse.embedded >> (fn string => fn _ => E_String string))),
   ("Thms", ("List[Thm]", Parse.thm >> (fn thmref => fn ctxt => E_List (map E_Thm (Attrib.eval_thms ctxt [thmref]))))),
   ("Thm", ("Thm", Parse.thm >> (fn thmref => fn ctxt => case Attrib.eval_thms ctxt [thmref]
        of [th] => E_Thm th | thms => error (string_of_thmref thmref ^ " should be a single theorem (it evaluates to " ^ string_of_int (length thms) ^ " theorems")))),
   ("Term", ("Term", Parse.term >> (fn term => fn ctxt => E_Term (Syntax.read_term ctxt term))))]

val scala_arg : (string(* varName *) * string(* scalaType *) * (Proof.context -> exn)(* value as exn *)) parser
  = (Parse.short_ident(* argType *) -- Parse.short_ident(* varName *) --| Parse.$$$ "=") :|--
    (fn (argType, varName) => case Symtab.lookup scala_arg_types argType of
      NONE => error ("Unknown argument type " ^ argType ^ ". Allowed types are " ^
                     String.concatWith ", " (Symtab.keys scala_arg_types) ^ ".")
    | SOME (scalaType, parser) => parser >> (fn exn => (varName, scalaType, exn)))
      
val scala_diag_parser = scala_source --
  Scan.optional (Parse.$$$ "for" |-- Parse.and_list1 scala_arg) []

val imports = "import de.unruh.isabelle.pure._\nimport de.unruh.isabelle.mlvalue._\nimport de.unruh.isabelle.pure.Implicits._\nimport de.unruh.isabelle.mlvalue.Implicits._\nimport de.unruh.isabelle.control.Isabelle._\n"
fun retrieve varName scalaType = "val " ^ varName ^ " = MLValue.unsafeFromId[" ^ scalaType ^ "](" ^ varName ^ "$id).retrieveNow\n"

fun scala_diag_command (scala:string) (args:(string*string*(Proof.context->exn)) list) = Toplevel.keep (fn state => let
  val ctxt = Toplevel.context_of state
  val args = ("context", "Context", E_Context) :: args
  val scala' = [imports]
  val scala' = scala' @ ["val DList(" ^ (String.concatWith ", " (map (fn (varName,_,_) => "DObject("^varName^"$id)") args)) ^ ") = data\n"]
  val scala' = scala' @ map (fn (varName,scalaType,_) => retrieve varName scalaType) args
  val scala' = scala' @ [scala]
  val data = DList (map (fn (_,_,exn) => DObject (exn ctxt)) args)

  val scala' = String.concat scala'
  val _ = tracing scala'
  val _ = \<^print> data
  val _ = Control_Isabelle.sendToScala (DList [DString scala', data])
  in () end)
in
val _ = Outer_Syntax.command \<^command_keyword>\<open>scala_diag\<close> "Invokes Scala code"
  (scala_diag_parser >> (fn (source,args) => scala_diag_command (source |> Input.source_content |> fst) args))
end
\<close>

scala_diag "println(context, t.pretty(context), th.pretty(context), s)" 
  for Term t = "x + y" and String s = "hi" and Thm th = list_induct2

end
