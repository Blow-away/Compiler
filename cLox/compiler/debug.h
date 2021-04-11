//
// Created by zhao chenyang on 2021/1/12.
//

#ifndef COMPILER_DEBUG_H
#define COMPILER_DEBUG_H

#include "chunk.h"

// 反汇编一个Chunk中的所有指令
void disassembleChunk(Chunk *chunk, const char *name);

// 反汇编一条指令
int disassembleInstruction(Chunk *chunk, int offset);

#endif //COMPILER_DEBUG_H
