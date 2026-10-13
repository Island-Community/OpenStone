#pragma once

#include <string>

namespace cold::injector::embedded {

std::wstring ensurePayloadOnDisk();
void ensureScriptsOnDisk(const std::wstring& dllPath);

}
