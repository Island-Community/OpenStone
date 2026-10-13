#pragma once

#include <windows.h>

#include <string>
#include <vector>

#include "ProcessList.hpp"

namespace cold::injector::ui {

enum class View { Picking, Injecting, Success, Error };

bool init();
void show();
void shutdown();

bool render();

View view();
void setView(View next);
void setInstances(const std::vector<ProcessEntry>& entries);
void showError(const std::wstring& message);

DWORD takePick();

}
