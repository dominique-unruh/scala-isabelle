(*
This theory is for opening \<open>control_isabelle.ml\<close> for editing. Load it in Isabelle and 
control-click on "\<dots>/control_isabelle.ml" below
*)

theory Control_Isabelle
  imports HOL.Set Main
begin

ML \<open>
val COMMUNICATION_STREAMS = (BinIO.openIn "/dev/null", BinIO.openOut "/dev/null")
val SECRETS = (0,0)
\<close>

ML_file "../../main/resources/de/unruh/isabelle/control/control_isabelle.ml"

end
