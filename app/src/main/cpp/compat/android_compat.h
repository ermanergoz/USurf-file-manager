/*
 * Workaround for Android NDK 29+:
 * The NDK headers define TIME_UTC unconditionally, but timespec_get()
 * is only available from API level 29. This causes 7-Zip's TimeUtils.cpp
 * to select the timespec_get() path which then fails to compile on
 * lower API targets.
 *
 * By including <time.h> and immediately undefining TIME_UTC, we force
 * the 7-Zip source to fall through to the clock_gettime() path, which
 * is available on all Android API levels >= 21.
 */
#pragma once

#include <time.h>
#undef TIME_UTC
