#include <stdio.h>
#include <stdlib.h>

int main(int argc, char** argv) {
    if (argc < 2) {
        return 0;
    }
    printf("%x\n", atoi(argv[1]));
}
