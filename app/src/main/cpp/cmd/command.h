#pragma once

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Splits a single command string (e.g. "7z a -tzip /out.zip /file") into a
 * null-terminated argv array. The first element is always the program name
 * ("7z") so that 7-Zip's argument parser sees the expected layout.
 *
 * @param command  UTF-8 command string (must not be null)
 * @param argc     out-param: number of arguments written to argv
 * @return heap-allocated argv array; free with FreeArgs()
 */
char **CommandToArgs(const char *command, int *argc);

/**
 * Frees an argv array returned by CommandToArgs().
 */
void FreeArgs(int argc, char **argv);

#ifdef __cplusplus
}
#endif
