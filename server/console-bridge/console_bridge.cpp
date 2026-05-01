/**
 * BTELO Console Bridge - Node.js N-API Addon
 * 
 * 直接读写 Windows 控制台缓冲区，实现与 Claude Code 终端的双向通信
 * 
 * 编译方式:
 *   npm install
 *   npx node-gyp rebuild
 * 
 */

#include <node_api.h>
#include <windows.h>
#include <string>
#include <vector>
#include <algorithm>

// ============================================================================
// 常量定义
// ============================================================================

// P0 #1: 删除与 Windows SDK 冲突的宏定义，直接使用数字字面量
// 旧代码: const uint32_t STD_INPUT_HANDLE = (DWORD)-10;
// 旧代码: const uint32_t STD_OUTPUT_HANDLE = (DWORD)-11;
// GetStdHandle(STD_INPUT_HANDLE) 等价于 GetStdHandle((DWORD)-10)
// GetStdHandle(STD_OUTPUT_HANDLE) 等价于 GetStdHandle((DWORD)-11)

// Windows SDK 已通过 #define 定义了以下宏，不可重新声明为 const:
// KEY_EVENT = 0x0001  (wincon.h)
// VK_RETURN = 0x0D, VK_ESCAPE = 0x1B, VK_BACK = 0x08, VK_TAB = 0x09  (winuser.h)
// 直接使用宏名即可

// ============================================================================
// 全局状态
// ============================================================================

static HANDLE g_hInput = INVALID_HANDLE_VALUE;
static HANDLE g_hOutput = INVALID_HANDLE_VALUE;
static DWORD g_attachedPid = 0;
static bool g_isAttached = false;

// ============================================================================
// N-API 辅助函数
// ============================================================================

#define NAPI_THROW_IF_FAILED(env, status, msg) \
    if ((status) != napi_ok) { \
        napi_throw_error((env), NULL, (msg)); \
        return NULL; \
    }

#define CHECK_ATTACHED(env) \
    if (!g_isAttached) { \
        napi_throw_error((env), NULL, "Not attached to any console. Call attach(pid) first."); \
        return NULL; \
    }

// ============================================================================
// 辅助函数: 获取虚拟扫描码 (P1 #4)
// ============================================================================

// MapVirtualKeyW 函数声明
#pragma comment(lib, "user32.lib")
static UINT GetVirtualScanCode(WORD vk) {
    return MapVirtualKeyW(vk, MAPVK_VK_TO_VSC);
}

// ============================================================================
// 辅助函数: 判断字符是否为 ASCII 字母 (P1 #5)
// ============================================================================

static bool IsAsciiAlpha(wchar_t ch) {
    return (ch >= L'a' && ch <= L'z') || (ch >= L'A' && ch <= L'Z');
}

// ============================================================================
// 实现: findProcesses(name) (P2 #10, #11)
// ============================================================================

napi_value FindProcesses(napi_env env, napi_callback_info info) {
    size_t argc = 1;
    napi_value argv[1];
    napi_get_cb_info(env, info, &argc, argv, NULL, NULL);
    
    if (argc < 1) {
        napi_throw_error(env, NULL, "findProcesses requires 1 argument: processName");
        return NULL;
    }
    
    // P2 #10: 先获取字符串长度
    size_t processNameLen = 0;
    napi_get_value_string_utf8(env, argv[0], NULL, 0, &processNameLen);
    
    if (processNameLen == 0) {
        napi_value result;
        napi_create_array(env, &result);
        return result;
    }
    
    // 动态分配缓冲区
    char* processName = new char[processNameLen + 1];
    napi_get_value_string_utf8(env, argv[0], processName, processNameLen + 1, &processNameLen);
    
    // 创建结果数组
    napi_value result;
    napi_create_array(env, &result);
    uint32_t resultIndex = 0;
    
    // 使用快照方式枚举进程 (Process32First/Next)
    HANDLE hSnapshot = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if (hSnapshot == INVALID_HANDLE_VALUE) {
        delete[] processName;
        return result;  // 返回空数组
    }
    
    PROCESSENTRY32W pe;
    pe.dwSize = sizeof(PROCESSENTRY32W);
    
    // P0 #3: 将 char* (UTF-8) 转换为 wchar_t* 再构造 wstring
    size_t wcharCount = 0;
    mbstowcs_s(&wcharCount, nullptr, 0, processName, processNameLen);
    std::wstring searchLower;
    if (wcharCount > 0) {
        searchLower.resize(wcharCount);
        mbstowcs_s(nullptr, &searchLower[0], wcharCount, processName, processNameLen);
        searchLower.resize(wcharCount - 1);  // mbstowcs_s 包含 null terminator
    }
    for (auto& c : searchLower) {
        if (c >= L'A' && c <= L'Z') c = c - L'A' + L'a';
    }
    
    if (Process32FirstW(hSnapshot, &pe)) {
        do {
            // 将进程名转换为小写
            wchar_t exeName[260];
            wcscpy_s(exeName, pe.szExeFile);
            std::wstring exeLower(exeName);
            for (auto& c : exeLower) {
                if (c >= L'A' && c <= L'Z') c = c - L'A' + L'a';
            }
            
            bool matched = false;
            
            // P2 #11: 先精确匹配 (不含扩展名的情况)
            size_t exeLen = wcslen(pe.szExeFile);
            if (exeLen >= processNameLen) {
                std::wstring exePart(exeName);
                for (size_t i = 0; i < exeLen; i++) {
                    if (exePart[i] >= L'A' && exePart[i] <= L'Z') {
                        exePart[i] = exePart[i] - L'A' + L'a';
                    }
                }
                
                // 检查开头是否匹配
                if (exeLen >= processNameLen && 
                    (exePart[processNameLen] == L'.' || exePart[processNameLen] == L' ' || exePart[processNameLen] == L'\0')) {
                    if (exePart.substr(0, processNameLen) == searchLower) {
                        matched = true;
                    }
                }
            }
            
            // 如果精确匹配失败，使用模糊匹配 (wcsstr)
            if (!matched && wcsstr(exeLower.c_str(), searchLower.c_str()) != NULL) {
                matched = true;
            }
            
            if (matched) {
                // 创建进程信息对象
                napi_value procInfo;
                napi_create_object(env, &procInfo);
                
                // pid
                napi_value pid;
                napi_create_int32(env, pe.th32ProcessID, &pid);
                napi_set_named_property(env, procInfo, "pid", pid);
                
                // name
                napi_value name;
                napi_create_string_utf16(env, (const char16_t*)pe.szExeFile, wcslen(pe.szExeFile), &name);
                napi_set_named_property(env, procInfo, "name", name);
                
                // parentPid
                napi_value parentPid;
                napi_create_int32(env, pe.th32ParentProcessID, &parentPid);
                napi_set_named_property(env, procInfo, "parentPid", parentPid);
                
                napi_set_element(env, result, resultIndex++, procInfo);
            }
        } while (Process32NextW(hSnapshot, &pe));
    }
    
    CloseHandle(hSnapshot);
    delete[] processName;
    
    return result;
}

// ============================================================================
// 实现: attach(pid)
// ============================================================================

napi_value Attach(napi_env env, napi_callback_info info) {
    size_t argc = 1;
    napi_value argv[1];
    napi_get_cb_info(env, info, &argc, argv, NULL, NULL);
    
    if (argc < 1) {
        napi_throw_error(env, NULL, "attach requires 1 argument: pid");
        return NULL;
    }
    
    // 获取 PID
    int32_t pid;
    napi_get_value_int32(env, argv[0], &pid);
    
    // 如果已经附加，先分离
    if (g_isAttached) {
        FreeConsole();
        g_isAttached = false;
    }
    
    // 分离当前控制台
    FreeConsole();
    
    // 附加到目标进程
    if (!AttachConsole((DWORD)pid)) {
        DWORD error = GetLastError();
        char errorMsg[256];
        sprintf_s(errorMsg, "AttachConsole failed with error code: %lu", error);
        napi_throw_error(env, NULL, errorMsg);
        return NULL;
    }
    
    // P0 #1: 使用数字字面量替代被 Windows SDK 宏定义的常量
    g_hInput = GetStdHandle((DWORD)-10);  // STD_INPUT_HANDLE
    g_hOutput = GetStdHandle((DWORD)-11); // STD_OUTPUT_HANDLE
    
    if (g_hInput == INVALID_HANDLE_VALUE || g_hOutput == INVALID_HANDLE_VALUE) {
        FreeConsole();
        napi_throw_error(env, NULL, "Failed to get console handles");
        return NULL;
    }
    
    g_attachedPid = (DWORD)pid;
    g_isAttached = true;
    
    // 返回成功
    napi_value result;
    napi_create_object(env, &result);
    
    napi_value success;
    napi_get_boolean(env, true, &success);
    napi_set_named_property(env, result, "success", success);
    
    napi_value attachedPid;
    napi_create_int32(env, pid, &attachedPid);
    napi_set_named_property(env, result, "pid", attachedPid);
    
    napi_value inputHandle;
    napi_create_int64(env, (int64_t)g_hInput, &inputHandle);
    napi_set_named_property(env, result, "inputHandle", inputHandle);
    
    napi_value outputHandle;
    napi_create_int64(env, (int64_t)g_hOutput, &outputHandle);
    napi_set_named_property(env, result, "outputHandle", outputHandle);
    
    return result;
}

// ============================================================================
// 辅助函数: 发送字符输入事件 (P1 #4, #5)
// ============================================================================

static bool SendCharEvent(HANDLE hInput, wchar_t ch, bool keyDown) {
    INPUT_RECORD ir;
    ir.EventType = KEY_EVENT;
    ir.KeyEvent.bKeyDown = keyDown;
    ir.KeyEvent.wRepeatCount = 1;
    
    // P1 #5: 字母字符的 VK code 统一转为大写
    if (IsAsciiAlpha(ch)) {
        ir.KeyEvent.wVirtualKeyCode = (WORD)towupper(ch);
    } else {
        ir.KeyEvent.wVirtualKeyCode = (WORD)ch;
    }
    
    // P1 #4: 使用 MapVirtualKey 计算 scan code
    ir.KeyEvent.wVirtualScanCode = (WORD)GetVirtualScanCode(ir.KeyEvent.wVirtualKeyCode);
    
    // UnicodeChar 保留原始大小写
    ir.KeyEvent.UnicodeChar = ch;
    ir.KeyEvent.dwControlKeyState = 0;
    
    DWORD written = 0;
    return WriteConsoleInputW(hInput, &ir, 1, &written) && written == 1;
}

// ============================================================================
// 辅助函数: 发送 Enter 键 (P0 #3)
// ============================================================================

static bool SendEnterKey(HANDLE hInput) {
    // KeyDown
    INPUT_RECORD irDown;
    irDown.EventType = KEY_EVENT;
    irDown.KeyEvent.bKeyDown = TRUE;
    irDown.KeyEvent.wRepeatCount = 1;
    irDown.KeyEvent.wVirtualKeyCode = VK_RETURN;
    irDown.KeyEvent.wVirtualScanCode = (WORD)GetVirtualScanCode(VK_RETURN);
    irDown.KeyEvent.UnicodeChar = L'\r';
    irDown.KeyEvent.dwControlKeyState = 0;
    
    DWORD written = 0;
    // P0 #3: 使用参数 hInput 而不是全局 g_hInput
    if (!WriteConsoleInputW(hInput, &irDown, 1, &written) || written != 1) {
        return false;
    }
    
    // KeyUp
    INPUT_RECORD irUp;
    irUp.EventType = KEY_EVENT;
    irUp.KeyEvent.bKeyDown = FALSE;
    irUp.KeyEvent.wRepeatCount = 1;
    irUp.KeyEvent.wVirtualKeyCode = VK_RETURN;
    irUp.KeyEvent.wVirtualScanCode = (WORD)GetVirtualScanCode(VK_RETURN);
    irUp.KeyEvent.UnicodeChar = L'\r';
    irUp.KeyEvent.dwControlKeyState = 0;
    
    // P0 #3: 使用参数 hInput 而不是全局 g_hInput
    return WriteConsoleInputW(hInput, &irUp, 1, &written) && written == 1;
}

// ============================================================================
// 实现: writeInput(text) (P1 #6, #7, P2 #9)
// ============================================================================

napi_value WriteInput(napi_env env, napi_callback_info info) {
    CHECK_ATTACHED(env);
    
    size_t argc = 1;
    napi_value argv[1];
    napi_get_cb_info(env, info, &argc, argv, NULL, NULL);
    
    if (argc < 1) {
        napi_throw_error(env, NULL, "writeInput requires 1 argument: text");
        return NULL;
    }
    
    // P2 #9: 先获取字符串长度
    size_t textLen = 0;
    napi_get_value_string_utf16(env, argv[0], NULL, 0, &textLen);
    
    if (textLen == 0) {
        napi_value result;
        napi_create_object(env, &result);
        napi_value charsWritten;
        napi_create_int32(env, 0, &charsWritten);
        napi_set_named_property(env, result, "charsWritten", charsWritten);
        napi_value success;
        napi_get_boolean(env, true, &success);
        napi_set_named_property(env, result, "success", success);
        return result;
    }
    
    // 动态分配缓冲区
    char16_t* text16 = new char16_t[textLen + 1];
    napi_get_value_string_utf16(env, argv[0], text16, textLen + 1, &textLen);
    
    std::wstring text(text16, textLen);
    delete[] text16;
    
    // P1 #6: 移除写入前的 FlushConsoleInputBuffer 调用，避免吞掉用户按键
    
    int totalWritten = 0;
    
    // 发送每个字符
    for (wchar_t ch : text) {
        // KeyDown 事件
        if (SendCharEvent(g_hInput, ch, true)) {
            totalWritten++;
        }
        
        // KeyUp 事件
        if (SendCharEvent(g_hInput, ch, false)) {
            totalWritten++;
        }
        
        // P1 #7: Sleep(5) 改为 Sleep(1)
        Sleep(1);
    }
    
    // 返回结果
    napi_value result;
    napi_create_object(env, &result);
    
    napi_value charsWritten;
    napi_create_int32(env, (int32_t)textLen, &charsWritten);
    napi_set_named_property(env, result, "charsWritten", charsWritten);
    
    napi_value success;
    napi_get_boolean(env, true, &success);
    napi_set_named_property(env, result, "success", success);
    
    return result;
}

// ============================================================================
// 实现: writeLine(text) (P1 #6, #7)
// ============================================================================

napi_value WriteLine(napi_env env, napi_callback_info info) {
    CHECK_ATTACHED(env);
    
    size_t argc = 1;
    napi_value argv[1];
    napi_get_cb_info(env, info, &argc, argv, NULL, NULL);
    
    if (argc < 1) {
        napi_throw_error(env, NULL, "writeLine requires 1 argument: text");
        return NULL;
    }
    
    // P2 #9: 先获取字符串长度
    size_t textLen = 0;
    napi_get_value_string_utf16(env, argv[0], NULL, 0, &textLen);
    
    if (textLen == 0) {
        // 发送 Enter 键
        bool enterSuccess = SendEnterKey(g_hInput);
        napi_value result;
        napi_create_object(env, &result);
        napi_value charsWritten;
        napi_create_int32(env, 0, &charsWritten);
        napi_set_named_property(env, result, "charsWritten", charsWritten);
        napi_value success;
        napi_get_boolean(env, enterSuccess, &success);
        napi_set_named_property(env, result, "success", success);
        return result;
    }
    
    // 动态分配缓冲区
    char16_t* text16 = new char16_t[textLen + 1];
    napi_get_value_string_utf16(env, argv[0], text16, textLen + 1, &textLen);
    
    std::wstring text(text16, textLen);
    delete[] text16;
    
    // P1 #6: 移除写入前的 FlushConsoleInputBuffer 调用
    
    // 发送每个字符
    for (wchar_t ch : text) {
        SendCharEvent(g_hInput, ch, true);
        SendCharEvent(g_hInput, ch, false);
        // P1 #7: Sleep(5) 改为 Sleep(1)
        Sleep(1);
    }
    
    // 发送 Enter 键
    bool enterSuccess = SendEnterKey(g_hInput);
    
    // 返回结果
    napi_value result;
    napi_create_object(env, &result);
    
    napi_value charsWritten;
    napi_create_int32(env, (int32_t)textLen, &charsWritten);
    napi_set_named_property(env, result, "charsWritten", charsWritten);
    
    napi_value success;
    napi_get_boolean(env, enterSuccess, &success);
    napi_set_named_property(env, result, "success", success);
    
    return result;
}

// ============================================================================
// 实现: flush()
// ============================================================================

napi_value Flush(napi_env env, napi_callback_info info) {
    CHECK_ATTACHED(env);
    
    FlushConsoleInputBuffer(g_hInput);
    
    napi_value result;
    napi_get_boolean(env, true, &result);
    return result;
}

// ============================================================================
// 实现: readScreen() (P2 #8)
// ============================================================================

napi_value ReadScreen(napi_env env, napi_callback_info info) {
    CHECK_ATTACHED(env);
    
    // 获取屏幕缓冲区信息
    CONSOLE_SCREEN_BUFFER_INFO csbi;
    if (!GetConsoleScreenBufferInfo(g_hOutput, &csbi)) {
        napi_throw_error(env, NULL, "GetConsoleScreenBufferInfo failed");
        return NULL;
    }
    
    SHORT width = csbi.srWindow.Right - csbi.srWindow.Left + 1;
    SHORT height = csbi.srWindow.Bottom - csbi.srWindow.Top + 1;
    
    // 分配缓冲区
    DWORD bufferSize = width * height;
    CHAR_INFO* buffer = new CHAR_INFO[bufferSize];
    
    COORD bufferCoord = { 0, 0 };
    COORD bufferSizeCoord = { width, height };
    SMALL_RECT readRegion = csbi.srWindow;
    
    // 读取屏幕内容
    if (!ReadConsoleOutputW(g_hOutput, buffer, bufferSizeCoord, bufferCoord, &readRegion)) {
        delete[] buffer;
        napi_throw_error(env, NULL, "ReadConsoleOutput failed");
        return NULL;
    }
    
    // 转换为字符串数组
    napi_value result;
    napi_create_array(env, &result);
    uint32_t lineIndex = 0;
    
    for (SHORT y = 0; y < height; y++) {
        std::wstring line;
        line.reserve(width);
        
        for (SHORT x = 0; x < width; x++) {
            DWORD idx = y * width + x;
            if (idx < bufferSize) {
                line += buffer[idx].Char.UnicodeChar;
            }
        }
        
        // 移除尾部空白
        while (!line.empty() && iswspace(line.back())) {
            line.pop_back();
        }
        
        // P2 #8: 保留所有行，不跳过空行
        // 旧代码: 检查 hasContent 并跳过空行
        // 新代码: 直接添加所有行
        
        napi_value lineValue;
        napi_create_string_utf16(env, (const char16_t*)line.c_str(), line.length(), &lineValue);
        napi_set_element(env, result, lineIndex++, lineValue);
    }
    
    delete[] buffer;
    
    return result;
}

// ============================================================================
// 实现: detach()
// ============================================================================

napi_value Detach(napi_env env, napi_callback_info info) {
    if (g_isAttached) {
        FreeConsole();
        g_isAttached = false;
        g_hInput = INVALID_HANDLE_VALUE;
        g_hOutput = INVALID_HANDLE_VALUE;
        g_attachedPid = 0;
    }
    
    napi_value result;
    napi_get_boolean(env, true, &result);
    return result;
}

// ============================================================================
// 模块初始化
// ============================================================================

#define DECLARE_NAPI_METHOD(name, func) \
    { name, 0, func, 0, 0, 0, napi_default, 0 }

napi_value Init(napi_env env, napi_value exports) {
    napi_status status;
    
    // 导出函数
    napi_property_descriptor desc[] = {
        DECLARE_NAPI_METHOD("findProcesses", FindProcesses),
        DECLARE_NAPI_METHOD("attach", Attach),
        DECLARE_NAPI_METHOD("writeInput", WriteInput),
        DECLARE_NAPI_METHOD("writeLine", WriteLine),
        DECLARE_NAPI_METHOD("flush", Flush),
        DECLARE_NAPI_METHOD("readScreen", ReadScreen),
        DECLARE_NAPI_METHOD("detach", Detach)
    };
    
    status = napi_define_properties(env, exports, sizeof(desc) / sizeof(desc[0]), desc);
    NAPI_THROW_IF_FAILED(env, status, "Failed to define properties");
    
    // P0 #1: 删除导出的常量定义（避免与 Windows SDK 宏冲突）
    // 旧代码会导出 STD_INPUT_HANDLE 和 STD_OUTPUT_HANDLE，在某些情况下会导致问题
    
    return exports;
}

NAPI_MODULE(NODE_GYP_MODULE_NAME, Init)
