#include "Embedded.hpp"

#include <windows.h>

#include <cstddef>
#include <cstdint>
#include <cstring>
#include <vector>

#include "EmbeddedScripts.generated.hpp"

namespace cold::injector::embedded {
namespace {

bool resourceBytes(int id, const std::uint8_t*& data, std::size_t& size) {
    HRSRC found = FindResourceW(nullptr, MAKEINTRESOURCEW(id), RT_RCDATA);
    if (found == nullptr) {
        return false;
    }
    HGLOBAL handle = LoadResource(nullptr, found);
    if (handle == nullptr) {
        return false;
    }
    const void* locked = LockResource(handle);
    const DWORD bytes = SizeofResource(nullptr, found);
    if (locked == nullptr || bytes == 0) {
        return false;
    }
    data = static_cast<const std::uint8_t*>(locked);
    size = static_cast<std::size_t>(bytes);
    return true;
}

bool writeWholeFile(const std::wstring& path, const std::uint8_t* data, std::size_t size) {
    HANDLE file = CreateFileW(path.c_str(), GENERIC_WRITE, FILE_SHARE_READ, nullptr,
                              CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, nullptr);
    if (file == INVALID_HANDLE_VALUE) {
        return false;
    }
    std::size_t written = 0;
    bool ok = true;
    while (written < size) {
        DWORD chunk = size - written > 0x100000 ? 0x100000 : static_cast<DWORD>(size - written);
        DWORD done = 0;
        if (WriteFile(file, data + written, chunk, &done, nullptr) == FALSE || done == 0) {
            ok = false;
            break;
        }
        written += done;
    }
    CloseHandle(file);
    return ok;
}

bool fileMatches(const std::wstring& path, const std::uint8_t* data, std::size_t size) {
    HANDLE file = CreateFileW(path.c_str(), GENERIC_READ, FILE_SHARE_READ | FILE_SHARE_WRITE,
                              nullptr, OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, nullptr);
    if (file == INVALID_HANDLE_VALUE) {
        return false;
    }
    LARGE_INTEGER length;
    bool same = false;
    if (GetFileSizeEx(file, &length) && length.QuadPart == static_cast<LONGLONG>(size)) {
        std::vector<std::uint8_t> existing(size);
        std::size_t total = 0;
        bool ok = true;
        while (total < size) {
            DWORD want =
                size - total > 0x100000 ? 0x100000 : static_cast<DWORD>(size - total);
            DWORD got = 0;
            if (ReadFile(file, existing.data() + total, want, &got, nullptr) == FALSE ||
                got == 0) {
                ok = false;
                break;
            }
            total += got;
        }
        if (ok && total == size && memcmp(existing.data(), data, size) == 0) {
            same = true;
        }
    }
    CloseHandle(file);
    return same;
}

std::wstring parentDirectory(const std::wstring& filePath) {
    const std::size_t slash = filePath.find_last_of(L"\\/");
    return slash == std::wstring::npos ? L"." : filePath.substr(0, slash);
}

std::wstring tempPayloadPath() {
    wchar_t temp[MAX_PATH] = {};
    DWORD length = GetTempPathW(MAX_PATH, temp);
    if (length == 0 || length >= MAX_PATH) {
        return L"";
    }
    std::wstring dir = temp;
    while (!dir.empty() && (dir.back() == L'\\' || dir.back() == L'/')) {
        dir.pop_back();
    }
    dir += L"\\stone";
    CreateDirectoryW(dir.c_str(), nullptr);
    return dir + L"\\stone.dll";
}

std::wstring loaderDirectory() {
    wchar_t self[MAX_PATH] = {};
    GetModuleFileNameW(nullptr, self, MAX_PATH);
    return parentDirectory(self);
}

}

std::wstring ensurePayloadOnDisk() {
    const std::uint8_t* data = nullptr;
    std::size_t size = 0;
    if (!resourceBytes(kPayloadResourceId, data, size)) {
        return L"";
    }
    const std::wstring beside = loaderDirectory() + L"\\stone.dll";
    if (fileMatches(beside, data, size)) {
        return beside;
    }
    if (writeWholeFile(beside, data, size)) {
        return beside;
    }

    HANDLE probe = CreateFileW(beside.c_str(), GENERIC_READ, FILE_SHARE_READ, nullptr,
                               OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, nullptr);
    const bool exists = probe != INVALID_HANDLE_VALUE;
    if (exists) {
        CloseHandle(probe);
    }
    const std::wstring fallback = tempPayloadPath();
    if (!fallback.empty() &&
        (fileMatches(fallback, data, size) || writeWholeFile(fallback, data, size))) {
        return fallback;
    }
    return exists ? beside : L"";
}

void ensureScriptsOnDisk(const std::wstring& dllPath) {
    if (dllPath.empty()) {
        return;
    }
    const std::wstring dir = parentDirectory(dllPath) + L"\\scripts";
    CreateDirectoryW(dir.c_str(), nullptr);
    for (int index = 0; index < kEmbeddedScriptCount; ++index) {
        const EmbeddedScript& entry = kEmbeddedScripts[index];
        const std::uint8_t* data = nullptr;
        std::size_t size = 0;
        if (!resourceBytes(entry.id, data, size)) {
            continue;
        }
        wchar_t name[MAX_PATH] = {};
        MultiByteToWideChar(CP_UTF8, 0, entry.name, -1, name, MAX_PATH);
        const std::wstring target = dir + L"\\" + name;
        HANDLE probe = CreateFileW(target.c_str(), GENERIC_READ, FILE_SHARE_READ, nullptr,
                                   OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, nullptr);
        if (probe != INVALID_HANDLE_VALUE) {
            CloseHandle(probe);
            continue;
        }
        writeWholeFile(target, data, size);
    }
}

}
