[toc]

# Summary

- Lox 的C编译器

## 前端

### 词法分析

- 这是初始工作，在`scanner.c`中，将文本分割为一个个token

### 语法&语义分析

- 在`compiler.c`中，采用one pass的方式，进行遍历token流
- 遇到不同的token采取不同的方案
- 语义分析根据`rules`数组来处理，避免语义错误
- 常量通过hashtable来存放

#### function

- 每一个函数都有自己的一个`compiler`,以提供一个函数的可调用指针
- 函数分为4种，最外层的script也视为一个函数

#### closure

- closure的实现通过upvalue，每个函数的complier都存有其内可以使用的上层函数的upvalue

## 后端

- 后端的实现采用bytecode
- 经过语法和语义分析之后，每一个操作都要生成对应的字节码
- 在`vm.c`中，通过虚拟机来执行这些字节码
- vm使用stack来管理，每个function的调用会生成一个函数调用帧
- 所有局部变量都存放在stack中，通过stack来直接取用
- 对于不同的bytecode，采用不同的操作

### GC

- 对于GC，采取的是标记清扫模式，初始时每个内存块都是**白的**
- 将一些情况下的stack中的值设为root（**涂黑**），将与root相连的可到达的内存都标记为black
- 遍历所有内存块，判断是否mark，没有mark就sweep
- 什么时候执行GC：设置一个GC阈值，当分配的内存超过GC的阈值时，进行GC，GC过程中调整GC阈值为新的（比已分配的内存大的）阈值

## 一些优化

- 由于hash表的capacity都是2的n次方，因此可以把%变为&来加快hash函数的执行速度
- NaN boxing
  - 用一个64bit的数来表示几种不同的Value，以提高性能

