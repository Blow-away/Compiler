//
// Created by zhao chenyang on 2021/1/19.
//

#ifndef COMPILER_TABLE_H
#define COMPILER_TABLE_H

#include "common.h"
#include "value.h"

typedef struct {
    ObjString *key;
    Value value;
} Entry;

// 存变量的hash表
typedef struct {
    int count;
    int capacity;
    Entry *entries;
} Table;

void initTable(Table *table);

void freeTable(Table *table);


bool tableGet(Table* table, ObjString* key, Value* value);

// 插入键值对，如果是新的key，返回true
bool tableSet(Table *table, ObjString *key, Value value);

bool tableDelete(Table* table, ObjString* key);

void tableAddAll(Table* from, Table* to);

ObjString* tableFindString(Table* table, const char* chars,
                           int length, uint32_t hash);


// string中的string的GC
void tableRemoveWhite(Table* table);

void markTable(Table* table);

#endif //COMPILER_TABLE_H
