//
// Created by zhao chenyang on 2021/1/13.
//
// virtual machine to run bytecode

#ifndef COMPILER_VM_H
#define COMPILER_VM_H

#include "chunk.h"
#include "table.h"
#include "object.h"

#define FRAMES_MAX 64
#define STACK_MAX (FRAMES_MAX * UINT8_COUNT)

typedef struct {
    ObjClosure* closure;
    uint8_t* ip;    // 返回地址
    Value* slots;   // 函数的第一个局部变量
} CallFrame;

typedef struct {
    CallFrame frames[FRAMES_MAX];
    int frameCount;

    // 存指令的Chunk
    Chunk *chunk;
    // 指向当前指令，等于指令集的程序计数器 PC
    // x86, x64, and the CLR call it “IP”.
    // 68k, PowerPC, ARM, p-code, and the JVM call it “PC”.
    uint8_t *ip;
    // 存值的栈，vm基于这个栈运行
    Value stack[STACK_MAX];
    Value *stackTop; //不用index是因为解引用更快
    Table globals; //将所有global 变量存在这个hash表中
    Table strings; // 将所有字符串都存进一个Table中，且这个Table是以Set方式运作，这样对于字符串比较就能直接用 == 来进行判断地址
    ObjString* initString;
    ObjUpvalue* openUpvalues;

    size_t bytesAllocated; // 已分配的内存byte数
    size_t nextGC; // GC的byte阈值（初值为1024*1024

    Obj* objects; // 指向objects的指针，用于在freeVm时将所有Obj free

    int grayCount;
    int grayCapacity;
    Obj** grayStack;
} VM;

// 唯一的一个虚拟机对象
extern VM vm;

typedef enum {
    INTERPRET_OK,
    INTERPRET_COMPILE_ERROR,
    INTERPRET_RUNTIME_ERROR
} InterpretResult;

void initVM();

void freeVM();

// run bytecode
InterpretResult interpret(const char* source);

void push(Value value);
Value pop();

#endif //COMPILER_VM_H
