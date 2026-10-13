#include "LoaderUi.hpp"

#include <d3d11.h>
#include <dwmapi.h>
#include <dxgi.h>

#include <cmath>

#include "imgui.h"
#include "imgui_impl_dx11.h"
#include "imgui_impl_win32.h"
#include "imgui_internal.h"

#ifndef DWMWA_WINDOW_CORNER_PREFERENCE
#define DWMWA_WINDOW_CORNER_PREFERENCE 33
#endif

extern IMGUI_IMPL_API LRESULT ImGui_ImplWin32_WndProcHandler(HWND window, UINT message,
                                                             WPARAM wParam, LPARAM lParam);

namespace cold::injector::ui {
namespace {

struct Instance {
    DWORD id = 0;
    std::string title;
    std::string detail;
};

HWND g_window = nullptr;
ID3D11Device* g_device = nullptr;
ID3D11DeviceContext* g_context = nullptr;
IDXGISwapChain* g_swapChain = nullptr;
ID3D11RenderTargetView* g_renderTarget = nullptr;
bool g_initialized = false;
bool g_shouldClose = false;

View g_view = View::Picking;
std::vector<Instance> g_instances;
std::string g_error;
DWORD g_pick = 0;

float g_time = 0.0f;
float g_viewTime = 0.0f;

ImFont* g_fontBody = nullptr;
ImFont* g_fontStrong = nullptr;
ImFont* g_fontSmall = nullptr;

const ImU32 kBackdrop = IM_COL32(16, 17, 17, 176);
const ImU32 kText = IM_COL32(235, 235, 232, 255);
const ImU32 kTextSoft = IM_COL32(163, 164, 160, 255);
const ImU32 kTextFaint = IM_COL32(103, 105, 102, 255);
const ImU32 kField = IM_COL32(255, 255, 255, 10);
const ImU32 kFieldActive = IM_COL32(255, 255, 255, 14);
const ImU32 kRule = IM_COL32(255, 255, 255, 26);
const ImU32 kButton = IM_COL32(231, 231, 227, 255);
const ImU32 kButtonDown = IM_COL32(205, 205, 201, 255);
const ImU32 kButtonText = IM_COL32(19, 20, 20, 255);

constexpr int kWidth = 500;
constexpr int kHeight = 320;
constexpr float kMargin = 52.0f;

std::string utf8(const std::wstring& text) {
    const int bytes = WideCharToMultiByte(CP_UTF8, 0, text.c_str(), static_cast<int>(text.size()),
                                          nullptr, 0, nullptr, nullptr);
    std::string result(static_cast<std::size_t>(bytes), '\0');
    WideCharToMultiByte(CP_UTF8, 0, text.c_str(), static_cast<int>(text.size()), result.data(),
                        bytes, nullptr, nullptr);
    return result;
}

float clamp01(float value) {
    return ImClamp(value, 0.0f, 1.0f);
}

float ease(float value) {
    return 1.0f - powf(1.0f - clamp01(value), 4.0f);
}

ImU32 alpha(ImU32 color, float amount) {
    ImVec4 value = ImGui::ColorConvertU32ToFloat4(color);
    value.w *= clamp01(amount);
    return ImGui::ColorConvertFloat4ToU32(value);
}

struct AccentPolicy {
    int accentState;
    int accentFlags;
    int tintColor;
    int animationId;
};

struct WindowCompositionAttribData {
    int attrib;
    PVOID data;
    SIZE_T size;
};

using SetWindowCompositionAttributeFn = BOOL(WINAPI*)(HWND, WindowCompositionAttribData*);

void enableBackdropBlur(HWND window) {
    HRGN clip = CreateRectRgn(0, 0, kWidth, kHeight);
    if (clip != nullptr && !SetWindowRgn(window, clip, TRUE)) {
        DeleteObject(clip);
    }

    DWMNCRENDERINGPOLICY policy = DWMNCRP_DISABLED;
    DwmSetWindowAttribute(window, DWMWA_NCRENDERING_POLICY, &policy, sizeof(policy));
    DWORD squareCorners = 1;
    DwmSetWindowAttribute(window, DWMWA_WINDOW_CORNER_PREFERENCE, &squareCorners,
                          sizeof(squareCorners));

    auto setComposition = reinterpret_cast<SetWindowCompositionAttributeFn>(
        GetProcAddress(GetModuleHandleW(L"user32.dll"), "SetWindowCompositionAttribute"));
    if (setComposition != nullptr) {
        AccentPolicy accent = {3, 0, 0x00111212, 0};
        WindowCompositionAttribData data = {19, &accent, sizeof(accent)};
        if (setComposition(window, &data)) {
            return;
        }
    }

    HRGN region = CreateRectRgn(0, 0, kWidth, kHeight);
    DWM_BLURBEHIND blur = {};
    blur.dwFlags = DWM_BB_ENABLE | DWM_BB_BLURREGION;
    blur.fEnable = TRUE;
    blur.hRgnBlur = region;
    DwmEnableBlurBehindWindow(window, &blur);
    if (region != nullptr) {
        DeleteObject(region);
    }
}

void drawRingSweep(ImDrawList* draw, ImVec2 center, float radius, float opacity) {
    const float tau = 6.28318530718f;
    const float head = fmodf(g_time * 3.15f, tau);
    const float trail = 2.18f;
    const int segments = 44;

    draw->AddCircle(center, radius, alpha(kText, opacity * 0.075f), 96, 1.2f);
    for (int index = 0; index < segments; ++index) {
        const float t0 = static_cast<float>(index) / segments;
        const float t1 = static_cast<float>(index + 1) / segments;
        const float a0 = head - trail + trail * t0;
        const float a1 = head - trail + trail * t1;
        const float strength = powf(t1, 2.55f);
        const ImVec2 p0(center.x + cosf(a0) * radius, center.y + sinf(a0) * radius);
        const ImVec2 p1(center.x + cosf(a1) * radius, center.y + sinf(a1) * radius);
        draw->AddLine(p0, p1, alpha(kText, opacity * strength * 0.055f), 7.0f);
        draw->AddLine(p0, p1, alpha(kText, opacity * strength * 0.14f), 3.8f);
        draw->AddLine(p0, p1, alpha(kText, opacity * (0.18f + strength * 0.82f)), 1.45f);
    }
}

void drawSmallTextCentered(ImDrawList* draw, ImVec2 center, const char* text, float opacity) {
    ImGui::PushFont(g_fontSmall);
    const ImVec2 size = ImGui::CalcTextSize(text);
    draw->AddText(ImVec2(center.x - size.x * 0.5f, center.y), alpha(kTextSoft, opacity), text);
    ImGui::PopFont();
}

bool drawButton(ImDrawList* draw, ImVec2 min, ImVec2 max, const char* label) {
    const ImVec2 mouse = ImGui::GetIO().MousePos;
    const bool hit = mouse.x >= min.x && mouse.x <= max.x && mouse.y >= min.y && mouse.y <= max.y;
    draw->AddRectFilled(min, max, hit && ImGui::IsMouseDown(0) ? kButtonDown : kButton);
    ImGui::PushFont(g_fontStrong);
    const ImVec2 size = ImGui::CalcTextSize(label);
    draw->AddText(ImVec2((min.x + max.x - size.x) * 0.5f, (min.y + max.y - size.y) * 0.5f),
                  kButtonText, label);
    ImGui::PopFont();
    return hit && ImGui::IsMouseClicked(0);
}

void drawSpinnerView(ImDrawList* draw, ImVec2 size, const char* caption, float opacity) {
    const float in = ease(g_viewTime / 0.14f);
    const ImVec2 center(size.x * 0.5f, size.y * 0.5f - 12.0f);
    drawRingSweep(draw, center, 18.0f, opacity * in);
    drawSmallTextCentered(draw, ImVec2(center.x, center.y + 34.0f), caption, opacity * in * 0.82f);
}

void drawPickView(ImDrawList* draw, ImVec2 size, float opacity) {
    if (g_instances.empty()) {
        drawSpinnerView(draw, size, "Open Minecraft", opacity);
        return;
    }

    const float in = ease(g_viewTime / 0.16f);
    const float width = size.x - kMargin * 2.0f;

    ImGui::PushFont(g_fontStrong);
    draw->AddText(ImVec2(kMargin, 34.0f), alpha(kText, opacity * in), "Stone");
    ImGui::PopFont();
    ImGui::PushFont(g_fontSmall);
    const char* hint = "Select Minecraft";
    draw->AddText(ImVec2(kMargin + width - ImGui::CalcTextSize(hint).x, 36.0f),
                  alpha(kTextFaint, opacity * in), hint);
    ImGui::PopFont();

    const float rowHeight = 44.0f;
    ImGui::SetCursorPos(ImVec2(kMargin, 72.0f));
    ImGui::PushStyleVar(ImGuiStyleVar_ItemSpacing, ImVec2(0.0f, 6.0f));
    ImGui::BeginChild("##instances", ImVec2(width, size.y - 72.0f - 24.0f), ImGuiChildFlags_None,
                      ImGuiWindowFlags_NoBackground);
    ImDrawList* rows = ImGui::GetWindowDrawList();
    for (const Instance& instance : g_instances) {
        const ImVec2 min = ImGui::GetCursorScreenPos();
        const ImVec2 max(min.x + width, min.y + rowHeight);
        ImGui::PushID(static_cast<int>(instance.id));
        if (ImGui::InvisibleButton("##row", ImVec2(width, rowHeight))) {
            g_pick = instance.id;
        }
        const bool hovered = ImGui::IsItemHovered();
        ImGui::PopID();

        rows->AddRectFilled(min, max, hovered ? kFieldActive : kField);
        rows->AddLine(ImVec2(min.x, max.y), max,
                      hovered ? alpha(kText, 0.72f * opacity * in) : kRule, 1.0f);
        rows->PushClipRect(min, ImVec2(max.x - 12.0f, max.y), true);
        rows->AddText(g_fontBody, g_fontBody->FontSize, ImVec2(min.x + 12.0f, min.y + 6.0f),
                      alpha(kText, opacity * in), instance.title.c_str());
        rows->AddText(g_fontSmall, g_fontSmall->FontSize, ImVec2(min.x + 12.0f, min.y + 24.0f),
                      alpha(kTextFaint, opacity * in), instance.detail.c_str());
        rows->PopClipRect();
    }
    ImGui::EndChild();
    ImGui::PopStyleVar();
}

void drawErrorView(ImDrawList* draw, ImVec2 size, float opacity) {
    const float in = ease(g_viewTime / 0.14f);
    const float width = ImMin(330.0f, size.x - 96.0f);
    const float x = (size.x - width) * 0.5f;

    ImGui::PushFont(g_fontBody);
    const ImVec2 textSize = ImGui::CalcTextSize(g_error.c_str(), nullptr, false, width);
    draw->AddText(g_fontBody, g_fontBody->FontSize, ImVec2(x, 110.0f), alpha(kText, opacity * in),
                  g_error.c_str(), nullptr, width);
    ImGui::PopFont();

    const float buttonY = ImMax(170.0f, 126.0f + textSize.y);
    if (drawButton(draw, ImVec2(x, buttonY), ImVec2(x + width, buttonY + 36.0f), "Back")) {
        setView(View::Picking);
    }
}

void drawSuccessView(ImDrawList* draw, ImVec2 size, float opacity) {
    const float resolve = ease(g_viewTime / 0.20f);
    const float fade = 1.0f - ease((g_viewTime - 0.24f) / 0.18f);
    const float amount = opacity * resolve * fade;
    const ImVec2 center(size.x * 0.5f, size.y * 0.5f);
    draw->AddCircle(center, 18.0f, alpha(kText, amount * 0.10f), 96, 6.0f);
    draw->AddCircle(center, 18.0f, alpha(kText, amount), 96, 1.45f);
    if (g_viewTime > 0.46f) {
        g_shouldClose = true;
    }
}

void createRenderTarget() {
    ID3D11Texture2D* backBuffer = nullptr;
    g_swapChain->GetBuffer(0, IID_PPV_ARGS(&backBuffer));
    g_device->CreateRenderTargetView(backBuffer, nullptr, &g_renderTarget);
    if (backBuffer != nullptr) {
        backBuffer->Release();
    }
}

LRESULT WINAPI windowProc(HWND window, UINT message, WPARAM wParam, LPARAM lParam) {
    if (ImGui_ImplWin32_WndProcHandler(window, message, wParam, lParam)) {
        return TRUE;
    }
    switch (message) {
        case WM_NCHITTEST: {
            POINT point = {static_cast<short>(LOWORD(lParam)), static_cast<short>(HIWORD(lParam))};
            ScreenToClient(window, &point);
            return point.y >= 0 && point.y < 30 ? HTCAPTION : HTCLIENT;
        }
        case WM_KEYDOWN:
            if (wParam == VK_ESCAPE) {
                g_shouldClose = true;
                return 0;
            }
            break;
        case WM_ERASEBKGND:
            return 1;
        case WM_CLOSE:
            g_shouldClose = true;
            return 0;
        case WM_DESTROY:
            PostQuitMessage(0);
            return 0;
    }
    return DefWindowProcW(window, message, wParam, lParam);
}

ImFont* loadFont(ImGuiIO& io, const char* primary, const char* fallback, float size) {
    ImFont* font = io.Fonts->AddFontFromFileTTF(primary, size);
    if (font == nullptr && fallback != nullptr) {
        font = io.Fonts->AddFontFromFileTTF(fallback, size);
    }
    return font != nullptr ? font : io.Fonts->AddFontDefault();
}

}

bool init() {
    SetProcessDPIAware();

    WNDCLASSEXW windowClass = {};
    windowClass.cbSize = sizeof(windowClass);
    windowClass.style = CS_OWNDC;
    windowClass.lpfnWndProc = windowProc;
    windowClass.hInstance = GetModuleHandleW(nullptr);
    windowClass.lpszClassName = L"StoneLoader";
    windowClass.hCursor = LoadCursorW(nullptr, IDC_ARROW);
    RegisterClassExW(&windowClass);

    const int screenWidth = GetSystemMetrics(SM_CXSCREEN);
    const int screenHeight = GetSystemMetrics(SM_CYSCREEN);
    g_window = CreateWindowExW(WS_EX_APPWINDOW, L"StoneLoader", L"Stone", WS_POPUP,
                               (screenWidth - kWidth) / 2, (screenHeight - kHeight) / 2, kWidth,
                               kHeight, nullptr, nullptr, windowClass.hInstance, nullptr);
    if (g_window == nullptr) {
        return false;
    }
    enableBackdropBlur(g_window);

    DXGI_SWAP_CHAIN_DESC description = {};
    description.BufferCount = 2;
    description.BufferDesc.Width = kWidth;
    description.BufferDesc.Height = kHeight;
    description.BufferDesc.Format = DXGI_FORMAT_R8G8B8A8_UNORM;
    description.BufferUsage = DXGI_USAGE_RENDER_TARGET_OUTPUT;
    description.OutputWindow = g_window;
    description.SampleDesc.Count = 1;
    description.Windowed = TRUE;
    description.SwapEffect = DXGI_SWAP_EFFECT_DISCARD;

    const D3D_FEATURE_LEVEL levels[] = {D3D_FEATURE_LEVEL_11_0, D3D_FEATURE_LEVEL_10_0};
    D3D_FEATURE_LEVEL level;
    if (FAILED(D3D11CreateDeviceAndSwapChain(nullptr, D3D_DRIVER_TYPE_HARDWARE, nullptr, 0, levels,
                                             2, D3D11_SDK_VERSION, &description, &g_swapChain,
                                             &g_device, &level, &g_context))) {
        DestroyWindow(g_window);
        return false;
    }
    createRenderTarget();

    IMGUI_CHECKVERSION();
    ImGui::CreateContext();
    ImGuiIO& io = ImGui::GetIO();
    io.IniFilename = nullptr;
    g_fontBody = loadFont(io, "C:\\Windows\\Fonts\\segoeui.ttf", nullptr, 14.0f);
    g_fontStrong =
        loadFont(io, "C:\\Windows\\Fonts\\seguisb.ttf", "C:\\Windows\\Fonts\\segoeuib.ttf", 14.0f);
    g_fontSmall = loadFont(io, "C:\\Windows\\Fonts\\segoeui.ttf", nullptr, 12.0f);

    ImGuiStyle& style = ImGui::GetStyle();
    style.WindowPadding = ImVec2(0, 0);
    style.WindowBorderSize = 0.0f;
    style.WindowRounding = 0.0f;
    style.ScrollbarSize = 3.0f;
    style.ScrollbarRounding = 0.0f;
    style.Colors[ImGuiCol_WindowBg] = ImVec4(0, 0, 0, 0);
    style.Colors[ImGuiCol_ScrollbarBg] = ImVec4(0, 0, 0, 0);
    style.Colors[ImGuiCol_ScrollbarGrab] = ImGui::ColorConvertU32ToFloat4(kRule);
    style.Colors[ImGuiCol_ScrollbarGrabHovered] = ImGui::ColorConvertU32ToFloat4(kTextFaint);
    style.Colors[ImGuiCol_ScrollbarGrabActive] = ImGui::ColorConvertU32ToFloat4(kTextSoft);

    ImGui_ImplWin32_Init(g_window);
    ImGui_ImplDX11_Init(g_device, g_context);
    g_initialized = true;
    return true;
}

void show() {
    ShowWindow(g_window, SW_SHOW);
}

void shutdown() {
    if (!g_initialized) {
        return;
    }
    ImGui_ImplDX11_Shutdown();
    ImGui_ImplWin32_Shutdown();
    ImGui::DestroyContext();
    g_renderTarget->Release();
    g_swapChain->Release();
    g_context->Release();
    g_device->Release();
    DestroyWindow(g_window);
    g_initialized = false;
}

bool render() {
    if (g_shouldClose) {
        return false;
    }
    MSG message;
    while (PeekMessageW(&message, nullptr, 0, 0, PM_REMOVE)) {
        TranslateMessage(&message);
        DispatchMessageW(&message);
        if (message.message == WM_QUIT) {
            return false;
        }
    }

    ImGui_ImplDX11_NewFrame();
    ImGui_ImplWin32_NewFrame();
    ImGui::NewFrame();

    ImGuiIO& io = ImGui::GetIO();
    const float dt = ImMin(io.DeltaTime, 0.05f);
    g_time += dt;
    g_viewTime += dt;

    ImGui::SetNextWindowPos(ImVec2(0, 0));
    ImGui::SetNextWindowSize(io.DisplaySize);
    ImGui::Begin("##main", nullptr,
                 ImGuiWindowFlags_NoDecoration | ImGuiWindowFlags_NoMove |
                     ImGuiWindowFlags_NoSavedSettings | ImGuiWindowFlags_NoBackground);
    ImDrawList* draw = ImGui::GetWindowDrawList();
    const ImVec2 size = io.DisplaySize;
    draw->AddRectFilled(ImVec2(0.0f, 0.0f), size, kBackdrop);

    const float opacity = ease(g_viewTime / 0.12f);
    switch (g_view) {
        case View::Picking:
            drawPickView(draw, size, opacity);
            break;
        case View::Injecting:
            drawSpinnerView(draw, size, "Loading Stone", opacity);
            break;
        case View::Success:
            drawSuccessView(draw, size, opacity);
            break;
        case View::Error:
            drawErrorView(draw, size, opacity);
            break;
    }
    ImGui::End();
    ImGui::Render();

    const float clear[4] = {0.0f, 0.0f, 0.0f, 0.0f};
    g_context->OMSetRenderTargets(1, &g_renderTarget, nullptr);
    g_context->ClearRenderTargetView(g_renderTarget, clear);
    ImGui_ImplDX11_RenderDrawData(ImGui::GetDrawData());
    g_swapChain->Present(1, 0);
    return !g_shouldClose;
}

View view() {
    return g_view;
}

void setView(View next) {
    if (g_view != next) {
        g_view = next;
        g_viewTime = 0.0f;
    }
}

void setInstances(const std::vector<ProcessEntry>& entries) {
    const bool wasEmpty = g_instances.empty();
    g_instances.clear();
    for (const ProcessEntry& entry : entries) {
        Instance instance;
        instance.id = entry.id;
        instance.title = utf8(entry.title);
        instance.detail = "pid " + std::to_string(entry.id) + "  ·  " + utf8(entry.executable);
        if (!entry.sixtyFourBit) {
            instance.detail += "  ·  32-bit";
        }
        g_instances.push_back(std::move(instance));
    }

    if (wasEmpty != g_instances.empty()) {
        g_viewTime = 0.0f;
    }
}

void showError(const std::wstring& message) {
    g_error = utf8(message);
    setView(View::Error);
}

DWORD takePick() {
    const DWORD pick = g_pick;
    g_pick = 0;
    return pick;
}

}
