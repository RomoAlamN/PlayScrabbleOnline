package com.romoalamn.scrabble.parser

import java.nio.charset.MalformedInputException
import java.text.ParseException
import java.util.ArrayDeque

class ScrabbleParser(
    val delimiter: Char,
    val code: String,
    val operators: Array<Operator> = arrayOf(
        Operator.Add,
        Operator.Divide,
        Operator.Multiply,
        Operator.Subtract
    )
) {

    val tokens = ArrayList<Token>()
    val association = HashMap<String, Int>()

    init {
        var end = false
        var index = 0
        do {
            val char = code[index]
            if (char == delimiter) {
                val endIndex = code.indexOf(delimiter, startIndex = index+1)
                val name = code.substring(index+1, endIndex)
                tokens.add(Token(TokenType.Variable, name))
                index = endIndex+1
            }else if(char.isDigit()){
                val startIndex = index
                while( index < code.length && code[index++].isDigit() ){

                }
                val value = code.substring(startIndex, index)
                tokens.add(Token(TokenType.Value, value))
            }else {
                for(op in operators){
                    if(char == op.type){
                        tokens.add(Token(TokenType.Operator, op.type.toString()))
                        break
                    }
                }
            }
            index ++
            if(index>= code.length){
                end = true
            }
        } while (!end)
    }

    fun setAssociation(str: String, i: Int) {
        association[str] = i
    }

    fun getAssociation(str: String): Int {
        return association[str]!!
    }
    private fun getFor(t : Token) : Int{
        return when(t.type){
            TokenType.Value->{
                t.association.toInt()
            }
            TokenType.Variable ->{
                association[t.association]!!
            }
            TokenType.Operator ->{
                throw ParseException(code, tokens.indexOf(t))
            }
        }
    }
    fun parse(){
        val stack = ArrayDeque<Reference>()
        var index = 0
        while(index < tokens.size){
            val token = tokens[index]
            when(token.type){
                TokenType.Value->{
                    stack.push(Reference(token, token.association.toInt()))
                }
                TokenType.Variable ->{
                    stack.push(Reference(token, association[token.association]!!))
                }
                TokenType.Operator ->{
                    val op1 = stack.pop()
                    val op2token = tokens[++index]
                    val op2 = getFor(op2token)
                    val reference = if(op1.token.type == TokenType.Variable) op1.token else op2token
                    for(op in operators){
                        if(token.association[0] == op.type){
                            stack.push(Reference(reference, op.operation(op1.value, op2)))
                            break
                        }
                    }
                }
            }
            index++
        }
        while(!stack.isEmpty()){
            val ref = stack.pop()
            if(ref.token.type == TokenType.Variable){
                association[ref.token.association] = ref.value
            }
        }
    }

}
class Reference (val token : Token, val value : Int){
}
class Operator(val type: Char, val operation: (Int, Int) -> Int) {
    companion object {
        val Add = Operator('+', Int::plus)
        val Subtract = Operator('-', Int::minus)
        val Multiply = Operator('*', Int::times)
        val Divide = Operator('/', Int::div)
    }
}

class Token(val type: TokenType, val association: String) {
}

enum class TokenType {
    Value, Operator, Variable
}