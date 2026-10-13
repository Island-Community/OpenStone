#pragma once

#include <windows.h>

#include <string>

namespace cold::injector {

struct InjectResult {
    bool ok = false;
    std::wstring message;
};

InjectResult injectLibrary(DWORD processId, const std::wstring& libraryPath);

bool isLibraryLoaded(DWORD processId, const std::wstring& libraryPath);

}
