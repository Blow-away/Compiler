//
// Created by zhao chenyang on 2021/1/14.
//

#ifndef COMPILER_COMPILER_H
#define COMPILER_COMPILER_H

#include "chunk.h"
#include "object.h"
#include "vm.h"

ObjFunction *compile(const char *source);
void markCompilerRoots();

#endif //COMPILER_COMPILER_H
