package com.example.game24.model

enum class ParenMode(val label: String) {
    NONE("无"), AB("(1-2)"), BC("(2-3)"), CD("(3-4)")
}

enum class Op(val label: String) {
    ADD("+"), SUB("−"), MUL("×"), DIV("÷")
}

sealed class Tok {
    data class Num(val v: Frac) : Tok()
    data class Oper(val op: Op) : Tok()
    data object L : Tok()
    data object R : Tok()
}

sealed class RoundState {
    data object Playing : RoundState()
    data class Won(val expr: String) : RoundState()
    data object NoSolutionCorrect : RoundState()
    data class LostWrongNoSolution(val solution: String) : RoundState()
    data class Info(val message: String) : RoundState()
}
