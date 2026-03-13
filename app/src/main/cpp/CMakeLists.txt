cmake_minimum_required(VERSION 3.22.1)
project("p7zip" LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_C_STANDARD 11)

set(SEVENZIP_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/7zip")

# ─── Compile definitions (equivalent to 7zip_gcc.mak / Arc_gcc.mak) ──────────
add_compile_definitions(
    NDEBUG
    _REENTRANT
    _FILE_OFFSET_BITS=64
    _LARGEFILE_SOURCE
    ENV_UNIX
    UNICODE
    _UNICODE
    Z7_PROG_VARIANT_Z   # Alone2 / 7zz variant
)

add_compile_options(
    -fexceptions
    -fvisibility=hidden
    -fPIC
    -O2
    -w          # suppress warnings from third-party source
    # Force-include Android compatibility header before all source files.
    # Undefines TIME_UTC so 7-Zip uses clock_gettime() instead of the
    # unavailable timespec_get() on API < 29.
    -include ${CMAKE_CURRENT_SOURCE_DIR}/compat/android_compat.h
)

# ─── Include paths ────────────────────────────────────────────────────────────
include_directories(
    ${SEVENZIP_ROOT}/CPP
    ${SEVENZIP_ROOT}/CPP/Common
    ${SEVENZIP_ROOT}/C
    ${CMAKE_CURRENT_SOURCE_DIR}
)

# ─── C sources (C/) — all files are cross-platform ───────────────────────────
file(GLOB C_SRCS "${SEVENZIP_ROOT}/C/*.c")
# No assembly stubs live in C/*.c, but guard anyway
list(FILTER C_SRCS EXCLUDE REGEX ".*\\.asm$")

# ─── CPP/Common ───────────────────────────────────────────────────────────────
file(GLOB CPP_COMMON "${SEVENZIP_ROOT}/CPP/Common/*.cpp")
list(FILTER CPP_COMMON EXCLUDE REGEX ".*(StdAfx).*")

# ─── CPP/Windows — only the subset that has Unix implementations ──────────────
# Based on WIN_OBJS + WIN_OBJS_2 + SYS_OBJS from Alone2/makefile.gcc
set(CPP_WINDOWS
    "${SEVENZIP_ROOT}/CPP/Windows/ErrorMsg.cpp"
    "${SEVENZIP_ROOT}/CPP/Windows/FileDir.cpp"
    "${SEVENZIP_ROOT}/CPP/Windows/FileFind.cpp"
    "${SEVENZIP_ROOT}/CPP/Windows/FileIO.cpp"
    "${SEVENZIP_ROOT}/CPP/Windows/FileLink.cpp"
    "${SEVENZIP_ROOT}/CPP/Windows/FileName.cpp"
    "${SEVENZIP_ROOT}/CPP/Windows/PropVariant.cpp"
    "${SEVENZIP_ROOT}/CPP/Windows/PropVariantConv.cpp"
    "${SEVENZIP_ROOT}/CPP/Windows/PropVariantUtils.cpp"
    "${SEVENZIP_ROOT}/CPP/Windows/Synchronization.cpp"
    "${SEVENZIP_ROOT}/CPP/Windows/System.cpp"
    "${SEVENZIP_ROOT}/CPP/Windows/SystemInfo.cpp"
    "${SEVENZIP_ROOT}/CPP/Windows/TimeUtils.cpp"
)

# ─── CPP/7zip/Common ─────────────────────────────────────────────────────────
file(GLOB CPP_7Z_COMMON "${SEVENZIP_ROOT}/CPP/7zip/Common/*.cpp")
list(FILTER CPP_7Z_COMMON EXCLUDE REGEX ".*(StdAfx).*")

# ─── CPP/7zip/Archive (all format handlers, recursive) ───────────────────────
file(GLOB_RECURSE CPP_7Z_ARCHIVE "${SEVENZIP_ROOT}/CPP/7zip/Archive/*.cpp")
list(FILTER CPP_7Z_ARCHIVE EXCLUDE REGEX ".*(StdAfx|DllExports|ArchiveExports).*")

# ─── CPP/7zip/Compress ────────────────────────────────────────────────────────
file(GLOB_RECURSE CPP_7Z_COMPRESS "${SEVENZIP_ROOT}/CPP/7zip/Compress/*.cpp")
list(FILTER CPP_7Z_COMPRESS EXCLUDE REGEX ".*(StdAfx|DllExports).*")

# ─── CPP/7zip/Crypto ─────────────────────────────────────────────────────────
file(GLOB CPP_7Z_CRYPTO "${SEVENZIP_ROOT}/CPP/7zip/Crypto/*.cpp")
list(FILTER CPP_7Z_CRYPTO EXCLUDE REGEX ".*(StdAfx).*")

# ─── CPP/7zip/UI/Common — only the subset used by the Alone2 console build ───
# Based on UI_COMMON_OBJS from Alone2/makefile.gcc.
# GUI/Windows files (ArchiveName, CompressCall, WorkDir, ZipRegistry) excluded.
set(CPP_UI_COMMON
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Common/ArchiveCommandLine.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Common/ArchiveExtractCallback.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Common/ArchiveOpenCallback.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Common/Bench.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Common/DefaultName.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Common/EnumDirItems.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Common/Extract.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Common/ExtractingFilePath.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Common/HashCalc.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Common/LoadCodecs.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Common/OpenArchive.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Common/PropIDUtils.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Common/SetProperties.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Common/SortUtils.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Common/TempFiles.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Common/Update.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Common/UpdateAction.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Common/UpdateCallback.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Common/UpdatePair.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Common/UpdateProduce.cpp"
)

# ─── CPP/7zip/UI/Console — console entry point + handlers ────────────────────
# Based on CONSOLE_OBJS from Alone2/makefile.gcc.
set(CPP_UI_CONSOLE
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Console/BenchCon.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Console/ConsoleClose.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Console/ExtractCallbackConsole.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Console/HashCon.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Console/List.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Console/Main.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Console/MainAr.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Console/OpenCallbackConsole.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Console/PercentPrinter.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Console/UpdateCallbackConsole.cpp"
    "${SEVENZIP_ROOT}/CPP/7zip/UI/Console/UserInputUtils.cpp"
)

# ─── Assemble all 7-Zip sources ───────────────────────────────────────────────
set(SEVENZIP_SRCS
    ${C_SRCS}
    ${CPP_COMMON}
    ${CPP_WINDOWS}
    ${CPP_7Z_COMMON}
    ${CPP_7Z_ARCHIVE}
    ${CPP_7Z_COMPRESS}
    ${CPP_7Z_CRYPTO}
    ${CPP_UI_COMMON}
    ${CPP_UI_CONSOLE}
)

# ─── JNI bridge + command-string parser ──────────────────────────────────────
set(JNI_SRCS
    jni_bridge.cpp
    cmd/command.cpp
)

add_library(p7zip SHARED ${SEVENZIP_SRCS} ${JNI_SRCS})

# ─── 16 KB ELF page alignment — fixes Play Console 16 KB page size error ─────
target_link_options(p7zip PRIVATE "-Wl,-z,max-page-size=16384")

target_link_libraries(p7zip log)
