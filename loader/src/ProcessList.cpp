#include "ProcessList.hpp"

#include <tlhelp32.h>

#include <algorithm>

namespace cold::injector {
namespace {

struct TitleSearch {
    std::vector<ProcessEntry>* entries;
};

bool isJavaExecutable(const wchar_t* name) {
    return _wcsicmp(name, L"java.exe") == 0 || _wcsicmp(name, L"javaw.exe") == 0;
}

BOOL CALLBACK captureTitle(HWND window, LPARAM parameter) {
    auto* search = reinterpret_cast<TitleSearch*>(parameter);
    if (!IsWindowVisible(window) || GetWindowTextLengthW(window) == 0) {
        return TRUE;
    }
    DWORD processId = 0;
    GetWindowThreadProcessId(window, &processId);
    if (processId == 0) {
        return TRUE;
    }
    wchar_t title[256] = {};
    if (GetWindowTextW(window, title, 256) == 0) {
        return TRUE;
    }
    for (ProcessEntry& entry : *search->entries) {
        if (entry.id == processId && entry.title.empty()) {
            entry.title = title;
            break;
        }
    }
    return TRUE;
}

bool detectSixtyFourBit(DWORD processId) {
    HANDLE process = OpenProcess(PROCESS_QUERY_LIMITED_INFORMATION, FALSE, processId);
    if (process == nullptr) {
        return true;
    }
    BOOL wow64 = FALSE;
    const bool ok = IsWow64Process(process, &wow64) != FALSE;
    CloseHandle(process);
    return ok ? wow64 == FALSE : true;
}

}

std::vector<ProcessEntry> enumerateJavaProcesses() {
    std::vector<ProcessEntry> entries;

    HANDLE snapshot = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if (snapshot == INVALID_HANDLE_VALUE) {
        return entries;
    }
    PROCESSENTRY32W process = {};
    process.dwSize = sizeof(process);
    if (Process32FirstW(snapshot, &process)) {
        do {
            if (!isJavaExecutable(process.szExeFile)) {
                continue;
            }
            ProcessEntry entry;
            entry.id = process.th32ProcessID;
            entry.executable = process.szExeFile;
            entries.push_back(std::move(entry));
        } while (Process32NextW(snapshot, &process));
    }
    CloseHandle(snapshot);

    TitleSearch search{&entries};
    EnumWindows(&captureTitle, reinterpret_cast<LPARAM>(&search));

    entries.erase(std::remove_if(entries.begin(), entries.end(),
                                 [](const ProcessEntry& entry) { return entry.title.empty(); }),
                  entries.end());
    for (ProcessEntry& entry : entries) {
        entry.sixtyFourBit = detectSixtyFourBit(entry.id);
    }
    std::sort(entries.begin(), entries.end(),
              [](const ProcessEntry& a, const ProcessEntry& b) { return a.id < b.id; });
    return entries;
}

}
