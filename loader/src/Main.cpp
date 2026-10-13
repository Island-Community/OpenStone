#include <windows.h>

#include <future>
#include <string>

#include "Embedded.hpp"
#include "Inject.hpp"
#include "LoaderUi.hpp"
#include "ProcessList.hpp"

namespace ui = cold::injector::ui;

namespace {

constexpr ULONGLONG kRefreshMillis = 600;

}

int WINAPI wWinMain(HINSTANCE, HINSTANCE, PWSTR, int) {
    const std::wstring payload = cold::injector::embedded::ensurePayloadOnDisk();
    if (payload.empty()) {
        MessageBoxW(nullptr, L"Could not extract stone.dll beside the loader or into %TEMP%.",
                    L"Stone", MB_ICONERROR);
        return 1;
    }
    cold::injector::embedded::ensureScriptsOnDisk(payload);

    if (!ui::init()) {
        MessageBoxW(nullptr, L"Could not create the loader window (Direct3D 11 unavailable).",
                    L"Stone", MB_ICONERROR);
        return 1;
    }
    ui::show();

    std::future<cold::injector::InjectResult> pending;
    ULONGLONG nextRefresh = 0;

    bool autoPick = true;
    while (ui::render()) {
        if (ui::view() == ui::View::Picking) {
            DWORD pick = ui::takePick();
            const ULONGLONG now = GetTickCount64();
            if (now >= nextRefresh) {
                const std::vector<cold::injector::ProcessEntry> found =
                    cold::injector::enumerateJavaProcesses();
                ui::setInstances(found);
                nextRefresh = now + kRefreshMillis;
                if (found.size() > 1) {
                    autoPick = false;
                } else if (autoPick && found.size() == 1) {
                    pick = found.front().id;
                    autoPick = false;
                }
            }
            if (pick != 0) {
                if (cold::injector::isLibraryLoaded(pick, payload)) {
                    ui::showError(L"Stone is already loaded in that Minecraft.");
                } else {
                    ui::setView(ui::View::Injecting);
                    pending = std::async(std::launch::async, [pick, &payload] {
                        return cold::injector::injectLibrary(pick, payload);
                    });
                }
            }
        }
        if (pending.valid() &&
            pending.wait_for(std::chrono::seconds(0)) == std::future_status::ready) {
            const cold::injector::InjectResult result = pending.get();
            if (result.ok) {
                ui::setView(ui::View::Success);
            } else {
                ui::showError(result.message);
            }
        }
    }
    ui::shutdown();
    return 0;
}
