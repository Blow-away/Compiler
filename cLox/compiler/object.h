//
// Created by zhao chenyang on 2021/1/19.
//

#ifndef COMPILER_OBJECT_H
#define COMPILER_OBJECT_H

#include "common.h"
#include "table.h"
#include "value.h"
#include "chunk.h"

#define OBJ_TYPE(value)        (AS_OBJ(value)->type)

#define IS_BOUND_METHOD(value) isObjType(value, OBJ_BOUND_METHOD)
#define IS_CLASS(value)        isObjType(value, OBJ_CLASS)
#define IS_CLOSURE(value)      isObjType(value, OBJ_CLOSURE)
#define IS_FUNCTION(value)     isObjType(value, OBJ_FUNCTION)
#define IS_INSTANCE(value)     isObjType(value, OBJ_INSTANCE)
#define IS_NATIVE(value)       isObjType(value, OBJ_NATIVE)
#define IS_STRING(value)       isObjType(value, OBJ_STRING)

#define AS_BOUND_METHOD(value) ((ObjBoundMethod*)AS_OBJ(value))
#define AS_CLASS(value)        ((ObjClass*)AS_OBJ(value))
#define AS_CLOSURE(value)      ((ObjClosure*)AS_OBJ(value))
#define AS_FUNCTION(value)     ((ObjFunction*)AS_OBJ(value))
#define AS_INSTANCE(value)     ((ObjInstance*)AS_OBJ(value))

#define AS_NATIVE(value) \
    (((ObjNative*)AS_OBJ(value))->function)
#define AS_STRING(value)       ((ObjString*)AS_OBJ(value))
#define AS_CSTRING(value)      (((ObjString*)AS_OBJ(value))->chars)

typedef enum {
    OBJ_BOUND_METHOD,
    OBJ_CLASS,
    OBJ_CLOSURE,
    OBJ_FUNCTION,
    OBJ_INSTANCE,
    OBJ_NATIVE,
    OBJ_STRING,
    OBJ_UPVALUE,
} ObjType;

// 类似于基类
struct Obj {
    ObjType type;
    bool isMarked;
    struct Obj* next;
};

typedef struct {
    Obj obj;
    int arity; // 参数个数
    int upvalueCount;
    Chunk chunk; // function body
    ObjString* name;
}ObjFunction;


// 本机函数，直接调用C代码
typedef Value (*NativeFn)(int argCount, Value* args);

typedef struct {
    Obj obj;
    NativeFn function;
} ObjNative;

// Obj子类：string
struct ObjString {
    // 这样排布，可以使任何ObjString* 转化为Obj* 来用
    Obj obj;
    int length;
    char *chars;
    uint32_t hash; // string 的 hash 值
};

typedef struct ObjUpvalue {
    Obj obj;
    Value* location;
    Value closed;
    struct ObjUpvalue* next;
} ObjUpvalue;

typedef struct {
    Obj obj;
    ObjFunction *function;
    ObjUpvalue **upvalues; // upvalue array
    int upvalueCount;
}ObjClosure;

typedef struct {
    Obj obj;
    ObjString* name;
    Table methods;
}ObjClass;

typedef struct {
    Obj obj;
    ObjClass* klass;
    Table fields;
} ObjInstance;

typedef struct {
    Obj obj;
    Value receiver; // class instance
    ObjClosure* method; // 函数Closure
} ObjBoundMethod;


ObjBoundMethod* newBoundMethod(Value receiver,
                               ObjClosure* method);

ObjClass* newClass(ObjString* name);

ObjClosure* newClosure(ObjFunction* function);

ObjFunction* newFunction();

ObjInstance* newInstance(ObjClass* klass);

ObjNative* newNative(NativeFn function);

ObjString* takeString(char* chars, int length);

ObjString *copyString(const char *chars, int length);

ObjUpvalue* newUpvalue(Value* slot);

void printObject(Value value);

static inline bool isObjType(Value value, ObjType type) {
    // 不用宏是因为用了两次value，可能会在某些时候产生难以发现的bug，如isObjType(pop())，如果是宏的话就会pop两次
    return IS_OBJ(value) && AS_OBJ(value)->type == type;
}

#endif //COMPILER_OBJECT_H
