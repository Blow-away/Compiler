//
// Created by zhao chenyang on 2021/1/12.
//

#ifndef COMPILER_COMMON_H
#define COMPILER_COMMON_H

#include <stdbool.h>
#include <stddef.h>
#include <stdint.h>
#include <stdlib.h>

#define NAN_BOXING
#define DEBUG_PRINT_CODE
#define DEBUG_TRACE_EXECUTION

// GC 的压力测试
#define DEBUG_STRESS_GC
// GC debug log
#define DEBUG_LOG_GC

#define UINT8_COUNT (UINT8_MAX + 1)

#endif //COMPILER_COMMON_H
