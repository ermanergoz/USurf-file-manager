#include <jni.h>
#include <android/log.h>
#include "cmd/command.h"

#define LOG_TAG "p7zip"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Forward declaration — defined in CPP/7zip/UI/Console/Main.cpp
#ifndef _WIN32
extern int Main2(int numArgs, char *args[]);
#endif

extern "C" JNIEXPORT jint JNICALL
Java_com_erman_usurf_directory_model_P7ZipApi_executeCommand(
        JNIEnv *env, jclass /*type*/, jstring command_) {
    const char *command = env->GetStringUTFChars(command_, nullptr);
    LOGI("CMD:[%s]", command);
    int argc = 0;
    char **argv = CommandToArgs(command, &argc);
    int ret = -1;
#ifndef _WIN32
    ret = Main2(argc, argv);
#endif
    FreeArgs(argc, argv);
    env->ReleaseStringUTFChars(command_, command);
    LOGI("EXIT:%d", ret);
    return static_cast<jint>(ret);
}
