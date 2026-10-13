#pragma once

#include <windows.h>

#include <string>
#include <vector>

namespace cold::injector {

struct ProcessEntry {
    DWORD id = 0;
    std::wstring executable;
    std::wstring title;
    bool sixtyFourBit = true;
};

std::vector<ProcessEntry> enumerateJavaProcesses();

}
