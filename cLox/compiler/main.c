#include <stdio.h>
#include "vm.h"

static void repl() {
    char line[1024];
    for (;;) {
        _sleep(200);
        printf(">>");

        if (!fgets(line, sizeof(line), stdin)) {
            printf("\n");
            break;
        }

        interpret(line);
    }
}

// F:\Github\Compiler\cLox\compiler\test.txt
static char *readFile(const char *path) {
    FILE *file = fopen(path, "rb");
    if (file == NULL){
        fprintf(stderr,"Could not open the file \"%s\".\n",path);
        exit(74);
    }
    // 定位至 file end + 0
    fseek(file, 0L, SEEK_END);
    // 上一行把定位调至末尾，现在通过ftell()就可以得到文件长度
    size_t fileSize = ftell(file);
    // 重置定位符
    rewind(file);

    char *buffer = (char *) malloc(fileSize + 1);
    if (buffer == NULL) {
        fprintf(stderr, "Not enough memory to read \"%s\".\n", path);
        exit(74);
    }
    size_t bytesRead = fread(buffer, sizeof(char), fileSize, file);
    // fread() can be fail
    if (bytesRead < fileSize) {
        fprintf(stderr, "Could not read file \"%s\".\n", path);
        exit(74);
    }
    buffer[bytesRead] = '\0';

    fclose(file);
    return buffer;
}

static void runFile(const char *path) {
    char *source = readFile(path);
    InterpretResult result = interpret(source);
    // 必须要编译完了才能free，因为token中使用了source
    free(source);

    if (result == INTERPRET_COMPILE_ERROR) {
        // 编译错误
        fprintf(stderr, "Compiler Error!\n");
        exit(65);
    }
    if (result == INTERPRET_RUNTIME_ERROR) {
        // 运行错误
        fprintf(stderr, "Compiler Error!\n");
        exit(70);
    }
}


int main(int argc, const char *argv[]) {
    initVM();

    // argc只有一个现在运行的文件名，就进入交互式编译器
    if (argc == 1) {
        repl();
    } else if (argc == 2) {
        // 否则进入编译文件
        runFile(argv[1]);
    } else {
        // 格式错误
        fprintf(stderr, "usage: clox [path]\n");
        exit(64);
    }

    freeVM();
    return 0;
}
