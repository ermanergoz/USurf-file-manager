#include <jni.h>
#include <android/log.h>
#include <string>
#include <vector>
#include "7zVersion.h"

#define LOG_TAG "p7zip"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Forward declaration — defined in CPP/7zip/UI/Console/Main.cpp
#ifndef _WIN32
extern int Main2(int numArgs, char *args[]);
#endif

namespace {

// 7-Zip's exit code for a fatal error (CPP/7zip/UI/Common/ExitCode.h).
constexpr int kFatalError = 2;

void appendUtf8(std::string &out, uint32_t codePoint) {
    if (codePoint < 0x80) {
        out += static_cast<char>(codePoint);
    } else if (codePoint < 0x800) {
        out += static_cast<char>(0xC0 | (codePoint >> 6));
        out += static_cast<char>(0x80 | (codePoint & 0x3F));
    } else if (codePoint < 0x10000) {
        out += static_cast<char>(0xE0 | (codePoint >> 12));
        out += static_cast<char>(0x80 | ((codePoint >> 6) & 0x3F));
        out += static_cast<char>(0x80 | (codePoint & 0x3F));
    } else {
        out += static_cast<char>(0xF0 | (codePoint >> 18));
        out += static_cast<char>(0x80 | ((codePoint >> 12) & 0x3F));
        out += static_cast<char>(0x80 | ((codePoint >> 6) & 0x3F));
        out += static_cast<char>(0x80 | (codePoint & 0x3F));
    }
}

// GetStringUTFChars returns modified UTF-8, which writes characters outside the BMP (emoji, for example) as two
// encoded surrogates; 7-Zip expects standard UTF-8, so the UTF-16 units are converted here.
std::string toUtf8(const jchar *units, jsize length) {
    std::string out;
    out.reserve(static_cast<size_t>(length));
    for (jsize i = 0; i < length; ++i) {
        uint32_t unit = units[i];
        if (unit >= 0xD800 && unit <= 0xDBFF && i + 1 < length && units[i + 1] >= 0xDC00 && units[i + 1] <= 0xDFFF) {
            unit = 0x10000 + ((unit - 0xD800) << 10) + (units[i + 1] - 0xDC00);
            ++i;
        } else if (unit >= 0xD800 && unit <= 0xDFFF) {
            unit = 0xFFFD;  // an unpaired surrogate has no UTF-8 form
        }
        appendUtf8(out, unit);
    }
    return out;
}

// Copies every element of the Java array into its own string, without splitting or unquoting anything.
bool toArguments(JNIEnv *env, jobjectArray array, std::vector<std::string> &arguments) {
    const jsize count = env->GetArrayLength(array);
    arguments.reserve(static_cast<size_t>(count));
    for (jsize i = 0; i < count; ++i) {
        auto element = static_cast<jstring>(env->GetObjectArrayElement(array, i));
        if (element == nullptr) return false;
        const jchar *units = env->GetStringChars(element, nullptr);
        if (units == nullptr) {
            env->DeleteLocalRef(element);
            return false;
        }
        arguments.push_back(toUtf8(units, env->GetStringLength(element)));
        env->ReleaseStringChars(element, units);
        env->DeleteLocalRef(element);
    }
    return true;
}

}  // namespace

// Logs the bundled 7-Zip version once, when the library is loaded.
extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM * /*vm*/, void * /*reserved*/) {
    LOGI("7-Zip %s", MY_VERSION_NUMBERS);
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_erman_usurf_directory_model_P7ZipApi_executeCommand(
        JNIEnv *env, jobject /*api*/, jobjectArray arguments_) {
    std::vector<std::string> arguments;
    if (!toArguments(env, arguments_, arguments) || arguments.empty()) {
        LOGE("Could not read the 7-Zip arguments");
        return kFatalError;
    }
    std::vector<char *> argv;
    argv.reserve(arguments.size() + 1);
    std::string command;
    for (std::string &argument : arguments) {
        argv.push_back(&argument[0]);
        command += (command.empty() ? "[" : " [") + argument + "]";
    }
    argv.push_back(nullptr);
    LOGI("CMD:%s", command.c_str());
    int ret = kFatalError;
#ifndef _WIN32
    ret = Main2(static_cast<int>(arguments.size()), argv.data());
#endif
    LOGI("EXIT:%d", ret);
    return static_cast<jint>(ret);
}
