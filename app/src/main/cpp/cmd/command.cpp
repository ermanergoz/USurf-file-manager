#include "command.h"

#include <cstdlib>
#include <cstring>
#include <vector>

static std::vector<char *> parseTokens(const char *command) {
    std::vector<char *> tokens;
    const char *p = command;
    while (*p) {
        while (*p == ' ' || *p == '\t') ++p;
        if (!*p) break;
        std::vector<char> token;
        bool inSingleQuote = false;
        bool inDoubleQuote = false;
        while (*p && (inSingleQuote || inDoubleQuote || (*p != ' ' && *p != '\t'))) {
            if (*p == '\'' && !inDoubleQuote) {
                inSingleQuote = !inSingleQuote;
                ++p;
            } else if (*p == '"' && !inSingleQuote) {
                inDoubleQuote = !inDoubleQuote;
                ++p;
            } else if (*p == '\\' && !inSingleQuote && *(p + 1)) {
                ++p;
                token.push_back(*p++);
            } else {
                token.push_back(*p++);
            }
        }
        token.push_back('\0');
        char *copy = static_cast<char *>(malloc(token.size()));
        memcpy(copy, token.data(), token.size());
        tokens.push_back(copy);
    }
    return tokens;
}

char **CommandToArgs(const char *command, int *argc) {
    std::vector<char *> tokens = parseTokens(command);
    *argc = static_cast<int>(tokens.size());
    char **argv = static_cast<char **>(malloc((*argc + 1) * sizeof(char *)));
    for (int i = 0; i < *argc; ++i) {
        argv[i] = tokens[i];
    }
    argv[*argc] = nullptr;
    return argv;
}

void FreeArgs(int argc, char **argv) {
    for (int i = 0; i < argc; ++i) {
        free(argv[i]);
    }
    free(argv);
}
