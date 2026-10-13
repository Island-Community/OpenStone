#include "Inject.hpp"

#include <tlhelp32.h>

namespace cold::injector {
namespace {

std::wstring describeLastError(const wchar_t* operation) {
    const DWORD error = GetLastError();
    wchar_t* text = nullptr;
    FormatMessageW(FORMAT_MESSAGE_ALLOCATE_BUFFER | FORMAT_MESSAGE_FROM_SYSTEM |
                       FORMAT_MESSAGE_IGNORE_INSERTS,
                   nullptr, error, 0, reinterpret_cast<wchar_t*>(&text), 0, nullptr);

    std::wstring message = operation;
    message += L" failed (";
    message += std::to_wstring(error);
    message += L"): ";
    message += text != nullptr ? text : L"unknown error";
    if (text != nullptr) {
        LocalFree(text);
    }
    while (!message.empty() && (message.back() == L'\n' || message.back() == L'\r')) {
        message.pop_back();
    }
    return message;
}

}

bool isLibraryLoaded(DWORD processId, const std::wstring& libraryPath) {
    HANDLE snapshot = CreateToolhelp32Snapshot(TH32CS_SNAPMODULE, processId);
    if (snapshot == INVALID_HANDLE_VALUE) {
        return false;
    }
    const std::size_t slash = libraryPath.find_last_of(L"\\/");
    const std::wstring wanted =
        slash == std::wstring::npos ? libraryPath : libraryPath.substr(slash + 1);
    MODULEENTRY32W module = {};
    module.dwSize = sizeof(module);
    bool found = false;
    if (Module32FirstW(snapshot, &module)) {
        do {
            if (_wcsicmp(module.szModule, wanted.c_str()) == 0) {
                found = true;
                break;
            }
        } while (Module32NextW(snapshot, &module));
    }
    CloseHandle(snapshot);
    return found;
}

InjectResult injectLibrary(DWORD processId, const std::wstring& libraryPath) {
    InjectResult result;

    HANDLE process = OpenProcess(PROCESS_CREATE_THREAD | PROCESS_QUERY_INFORMATION |
                                     PROCESS_VM_OPERATION | PROCESS_VM_WRITE | PROCESS_VM_READ,
                                 FALSE, processId);
    if (process == nullptr) {
        result.message = describeLastError(L"OpenProcess") +
                         L". Run the loader as the same (or an elevated) user as the game.";
        return result;
    }

    BOOL wow64 = FALSE;
    if (IsWow64Process(process, &wow64) && wow64) {
        CloseHandle(process);
        result.message = L"That Minecraft runs on a 32-bit Java; Stone is x64 only.";
        return result;
    }

    const SIZE_T bytes = (libraryPath.size() + 1) * sizeof(wchar_t);
    void* remote = VirtualAllocEx(process, nullptr, bytes, MEM_COMMIT | MEM_RESERVE, PAGE_READWRITE);
    if (remote == nullptr) {
        result.message = describeLastError(L"VirtualAllocEx");
        CloseHandle(process);
        return result;
    }
    if (!WriteProcessMemory(process, remote, libraryPath.c_str(), bytes, nullptr)) {
        result.message = describeLastError(L"WriteProcessMemory");
        VirtualFreeEx(process, remote, 0, MEM_RELEASE);
        CloseHandle(process);
        return result;
    }

    auto loadLibrary = reinterpret_cast<LPTHREAD_START_ROUTINE>(
        GetProcAddress(GetModuleHandleW(L"kernel32.dll"), "LoadLibraryW"));
    HANDLE thread = CreateRemoteThread(process, nullptr, 0, loadLibrary, remote, 0, nullptr);
    if (thread == nullptr) {
        result.message = describeLastError(L"CreateRemoteThread");
        VirtualFreeEx(process, remote, 0, MEM_RELEASE);
        CloseHandle(process);
        return result;
    }

    const ULONGLONG deadline = GetTickCount64() + 20000;
    DWORD wait = WAIT_TIMEOUT;
    DWORD targetExit = STILL_ACTIVE;
    while (GetTickCount64() < deadline) {
        wait = WaitForSingleObject(thread, 250);
        if (wait == WAIT_OBJECT_0 ||
            (GetExitCodeProcess(process, &targetExit) && targetExit != STILL_ACTIVE)) {
            break;
        }
    }
    DWORD exitCode = 0;
    GetExitCodeThread(thread, &exitCode);
    CloseHandle(thread);
    VirtualFreeEx(process, remote, 0, MEM_RELEASE);
    CloseHandle(process);

    if (targetExit != STILL_ACTIVE) {
        result.message = L"The game closed while Stone was loading.";
    } else if (wait != WAIT_OBJECT_0) {
        result.message = L"LoadLibraryW did not return within 20 seconds.";
    } else if (exitCode == 0) {
        result.message =
            L"LoadLibraryW returned NULL. An antivirus may be blocking stone.dll; "
            L"see %APPDATA%\\stone\\stone.log.";
    } else {
        result.ok = true;
    }
    return result;
}

}
