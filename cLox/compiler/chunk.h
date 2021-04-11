//
// Created by zhao chenyang on 2021/1/12.
//

#ifndef COMPILER_CHUNK_H
#define COMPILER_CHUNK_H

#include "common.h"
#include "value.h"

// 一字节的操作码
typedef enum {
    OP_CONSTANT, //  从栈中取出常数
    OP_NOT, //  !
    OP_NEGATE, //  取负
    OP_NIL, //  从栈中取出NIL
    OP_TRUE, // 从栈中取出TRUE
    OP_FALSE, // 从栈中取出FALSE
    OP_POP, // pop stack
    OP_GET_LOCAL, //  获取 局部变量的值
    OP_SET_LOCAL, //  修改 局部变量的值
    OP_GET_GLOBAL, // 获取 全局变量的值
    OP_SET_GLOBAL, // 修改 全局变量的值
    OP_GET_UPVALUE,
    OP_SET_UPVALUE,
    OP_DEFINE_GLOBAL, // define global variable
    OP_GET_PROPERTY,
    OP_SET_PROPERTY,
    OP_GET_SUPER, // 从super的函数table中获得函数
    OP_EQUAL, //  =
    OP_GREATER, //  >
    OP_LESS,//  <
    OP_ADD, //  +
    OP_SUBTRACT, //  -
    OP_MULTIPLY, //  *
    OP_DIVIDE, //  /
    OP_PRINT, //  print
    OP_JUMP, // jump else
    OP_JUMP_IF_FALSE, // jump if
    OP_LOOP,
    OP_CALL, //  function call
    OP_INVOKE,
    OP_SUPER_INVOKE,
    OP_CLOSURE, // 函数闭包，获取运行时周边变量
    OP_CLOSE_UPVALUE,
    OP_RETURN, //  函数返回
    OP_CLASS,
    OP_INHERIT,
    OP_METHOD,
} OpCode;

// 一个字节码的动态数组
typedef struct {
    int count;
    int capacity;
    // point to codes
    uint8_t *code;
    int *lines; // 指令对应的源代码的行数
    ValueArray constants;
} Chunk;

// 初始化动态数组
void initChunk(Chunk *chunk);

// 释放动态数组
void freeChunk(Chunk *chunk);

// 添加字节码
void writeChunk(Chunk *chunk, uint8_t byte, int line);

// 添加常量
int addConstant(Chunk *chunk, Value value);

#endif //COMPILER_CHUNK_H
