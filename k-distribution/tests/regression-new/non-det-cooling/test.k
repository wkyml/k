// Copyright (c) K Team. All Rights Reserved.
module TEST-SYNTAX
  imports DOMAINS-SYNTAX
  syntax AExp  ::= Int
                 | Id
                 | "++" Id
                 | "(" AExp ")"    [bracket]
                 > AExp "/" AExp   [left, strict, division]
  syntax Stmt  ::= "int" Ids ";"
                 | Id "=" AExp ";" [strict(2)]
                 > Stmt Stmt       [left]
  syntax Ids   ::= List{Id,","}
endmodule


module TEST
  imports TEST-SYNTAX
  imports DOMAINS
  syntax KResult ::= Int

  configuration <T>
                  <k> $PGM:Stmt </k>
                  <env> .Map </env>
                  <store> .Map </store>
                </T>

  rule <k> X:Id => I ...</k>
       <env>... X |-> N ...</env>
       <store>... N |-> I ...</store>
  rule <k> ++X => I +Int 1 ...</k>
       <env>... X |-> N ...</env>
       <store>... N |-> (I => I +Int 1) ...</store>
  rule I1 / I2 => I1 /Int I2  when I2 =/=Int 0

  rule <k> X = I:Int; => . ...</k>
       <env>... X |-> N ...</env>
       <store>... N |-> (_ => I) ...</store>
  rule S1:Stmt S2:Stmt => S1 ~> S2  [structural]

  rule <k> int (X,Xs => Xs); ...</k>
       <env> Rho => Rho[X <- !N:Int] </env>
       <store>... .Map => !N |-> 0 ...</store>
  rule int .Ids; => .  [structural]
endmodule
