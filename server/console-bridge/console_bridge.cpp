/**
 * BTELO Console Bridge - Node.js N-API Addon
 * 
 * 直接读写 Windows 控制台缓冲区，实现与 Claude Code 终端的双向通信
 * 
 * 编译方式:
 *   npm install
 *   npx node-gyp rebuild
 */

#include <node_api.h>
#include <windows.h>
#include <string>
#include <vector>
#include <algorithm>

// ============================================================================
// 常量定义
// ============================================================================

const uint32_t STD_INPUT_HANDLE = UINT32_MAX - 9;   // (DWORD)-10
const uint32_t STD_OUTPUT_HANDLE = UINT32_MAX - 10; // (DWORD)-11

const uint16_t KEY_EVENT = 0x0001;

const uint16_t VK_RETURN = 0x0D;
const uint16_t VK_ESCAPE = 0x1B;
const uint16_t VK_BACK = 0x08;
const uint16_t VK_TAB = 0x09;

// ============================================================================
// 结构体定义
// ============================================================================

#pragma pack(push, 1)

struct COORD {
    SHORT X;
    SHORT Y;
};

struct SMALL_RECT {
    SHORT Left;
    SHORT Top;
    SHORT Right;
    SHORT Bottom;
    
    SHORT Width() const { return Right - Left + 1; }
    SHORT Height() const { return Bottom - Top + 1; }
};

struct CONSOLE_SCREEN_BUFFER_INFO {
    COORD dwSize;
    COORD dwCursorPosition;
    WORD wAttributes;
    SMALL_RECT srWindow;
    COORD dwMaximumWindowSize;
};

struct KEY_EVENT_RECORD {
    BOOL bKeyDown;
    WORD wRepeatCount;
    WORD wVirtualKeyCode;
    WORD wVirtualScanCode;
    WCHAR UnicodeChar;
    DWORD dwControlKeyState;
};

struct INPUT_RECORD {
    WORD EventType;
    KEY_EVENT_RECORD KeyEvent;
};

struct CHAR_INFO {
    WCHAR UnicodeChar;
    WORD Attributes;
};

#pragma pack(pop)

// ============================================================================
// 全局状态
// ============================================================================

static HANDLE g_hInput = INVALID_HANDLE_VALUE;
static HANDLE g_hOutput = INVALID_HANDLE_VALUE;
static DWORD g_attachedPid = 0;
static bool g_isAttached = false;

// ============================================================================
// Win32 API 函数指针
// ============================================================================

typedef HANDLE(WINAPI* PFN_GetStdHandle)(DWORD);
typedef BOOL(WINAPI* PFN_AttachConsole)(DWORD);
typedef BOOL(WINAPI* PFN_FreeConsole)();
typedef BOOL(WINAPI* PFN_WriteConsoleInputW)(HANDLE, INPUT_RECORD*, DWORD, LPDWORD);
typedef BOOL(WINAPI* PFN_ReadConsoleOutputW)(HANDLE, CHAR_INFO*, COORD, COORD, SMALL_RECT*);
typedef BOOL(WINAPI* PFN_GetConsoleScreenBufferInfo)(HANDLE, CONSOLE_SCREEN_BUFFER_INFO*);
typedef BOOL(WINAPI* PFN_FlushConsoleInputBuffer)(HANDLE);

static HMODULE g_kernel32 = NULL;
static PFN_GetStdHandle pf_GetStdHandle = NULL;
static PFN_AttachConsole pf_AttachConsole = NULL;
static PFN_FreeConsole pf_FreeConsole = NULL;
static PFN_WriteConsoleInputW pf_WriteConsoleInputW = NULL;
static PFN_ReadConsoleOutputW pf_ReadConsoleOutputW = NULL;
static PFN_GetConsoleScreenBufferInfo pf_GetConsoleScreenBufferInfo = NULL;
static PFN_FlushConsoleInputBuffer pf_FlushConsoleInputBuffer = NULL;

// ============================================================================
// 初始化 Win32 API 函数指针
// ============================================================================

bool InitWin32Apis() {
    if (g_kernel32 != NULL) {
        return true;
    }
    
    g_kernel32 = LoadLibraryA("kernel32.dll");
    if (g_kernel32 == NULL) {
        return false;
    }
    
    pf_GetStdHandle = (PFN_GetStdHandle)GetProcAddress(g_kernel32, "GetStdHandle");
    pf_AttachConsole = (PFN_AttachConsole)GetProcAddress(g_kernel32, "AttachConsole");
    pf_FreeConsole = (PFN_FreeConsole)GetProcAddress(g_kernel32, "FreeConsole");
    pf_WriteConsoleInputW = (PFN_WriteConsoleInputW)GetProcAddress(g_kernel32, "WriteConsoleInputW");
    pf_ReadConsoleOutputW = (PFN_ReadConsoleOutputW)GetProcAddress(g_kernel32, "ReadConsoleOutputW");
    pf_GetConsoleScreenBufferInfo = (PFN_GetConsoleScreenBufferInfo)GetProcAddress(g_kernel32, "GetConsoleScreenBufferInfo");
    pf_FlushConsoleInputBuffer = (PFN_FlushConsoleInputBuffer)GetProcAddress(g_kernel32, "FlushConsoleInputBuffer");
    
    return pf_GetStdHandle && pf_AttachConsole && pf_FreeConsole &&
           pf_WriteConsoleInputW && pf_ReadConsoleOutputW &&
           pf_GetConsoleScreenBufferInfo && pf_FlushConsoleInputBuffer;
}

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
// 实现: findProcesses(name)
// 查找包含指定名称的进程
// ============================================================================

napi_value FindProcesses(napi_env env, napi_callback_info info) {
    size_t argc = 1;
    napi_value argv[1];
    napi_get_cb_info(env, info, &argc, argv, NULL, NULL);
    
    if (argc < 1) {
        napi_throw_error(env, NULL, "findProcesses requires 1 argument: processName");
        return NULL;
    }
    
    // 获取进程名称
    char processName[256];
    size_t processNameLen;
    napi_get_value_string_utf8(env, argv[0], processName, sizeof(processName), &processNameLen);
    
    // 创建结果数组
    napi_value result;
    napi_create_array(env, &result);
    uint32_t resultIndex = 0;
    
    // 使用快照方式枚举进程 (Process32First/Next)
    HANDLE hSnapshot = CreateToolhelp32Snapshot(TH32CS_SNAPPROCESS, 0);
    if (hSnapshot == INVALID_HANDLE_VALUE) {
        return result;  // 返回空数组
    }
    
    PROCESSENTRY32W pe;
    pe.dwSize = sizeof(PROCESSENTRY32W);
    
    if (Process32FirstW(hSnapshot, &pe)) {
        do {
            // 将进程名转换为小写比较
            wchar_t exeName[260];
            wcscpy_s(exeName, pe.szExeFile);
            _wcslwr_s(exeName);
            
            // 检查进程名是否匹配
            wchar_t searchName[260];
            mbstowcs_s(NULL, searchName, processName, sizeof(searchName) / sizeof(wchar_t));
            _wcslwr_s(searchName);
            
            if (wcsstr(exeName, searchName) != NULL) {
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
    return result;
}

// ============================================================================
// 实现: attach(pid)
// 附加到目标进程的控制台
// ============================================================================

napi_value Attach(napi_env env, napi_callback_info info) {
    size_t argc = 1;
    napi_value argv[1];
    napi_get_cb_info(env, info, &argc, argv, NULL, NULL);
    
    if (argc < 1) {
        napi_throw_error(env, NULL, "attach requires 1 argument: pid");
        return NULL;
    }
    
    // 初始化 Win32 API
    if (!InitWin32Apis()) {
        napi_throw_error(env, NULL, "Failed to initialize Win32 APIs");
        return NULL;
    }
    
    // 获取 PID
    int32_t pid;
    napi_get_value_int32(env, argv[0], &pid);
    
    // 如果已经附加，先分离
    if (g_isAttached) {
        pf_FreeConsole();
        g_isAttached = false;
    }
    
    // 分离当前控制台
    pf_FreeConsole();
    
    // 附加到目标进程
    if (!pf_AttachConsole((DWORD)pid)) {
        DWORD error = GetLastError();
        char errorMsg[256];
        sprintf_s(errorMsg, "AttachConsole failed with error code: %lu", error);
        napi_throw_error(env, NULL, errorMsg);
        return NULL;
    }
    
    // 获取控制台句柄
    g_hInput = pf_GetStdHandle(STD_INPUT_HANDLE);
    g_hOutput = pf_GetStdHandle(STD_OUTPUT_HANDLE);
    
    if (g_hInput == INVALID_HANDLE_VALUE || g_hOutput == INVALID_HANDLE_VALUE) {
        pf_FreeConsole();
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
// 实现: writeInput(text)
// 写入文本输入
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
    
    // 获取文本
    char16_t text16[4096];
    size_t textLen;
    napi_get_value_string_utf16(env, argv[0], text16, sizeof(text16) / sizeof(char16_t), &textLen);
    
    std::wstring text(text16, textLen);
    
    // 清空输入缓冲区
    pf_FlushConsoleInputBuffer(g_hInput);
    
    DWORD written = 0;
    int totalWritten = 0;
    
    // 发送每个字符
    for (wchar_t ch : text) {
        // KeyDown 事件
        INPUT_RECORD irDown;
        irDown.EventType = KEY_EVENT;
        irDown.KeyEvent.bKeyDown = TRUE;
        irDown.KeyEvent.wRepeatCount = 1;
        irDown.KeyEvent.wVirtualKeyCode = (WORD)ch;
        irDown.KeyEvent.wVirtualScanCode = 0;
        irDown.KeyEvent.UnicodeChar = ch;
        irDown.KeyEvent.dwControlKeyState = 0;
        
        pf_WriteConsoleInputW(g_hInput, &irDown, 1, &written);
        totalWritten += written;
        
        // KeyUp 事件
        INPUT_RECORD irUp;
        irUp.EventType = KEY_EVENT;
        irUp.KeyEvent.bKeyDown = FALSE;
        irUp.KeyEvent.wRepeatCount = 1;
        irUp.KeyEvent.wVirtualKeyCode = (WORD)ch;
        irUp.KeyEvent.wVirtualScanCode = 0;
        irUp.KeyEvent.UnicodeChar = ch;
        irUp.KeyEvent.dwControlKeyState = 0;
        
        pf_WriteConsoleInputW(g_hInput, &irUp, 1, &written);
        totalWritten += written;
        
        Sleep(5);  // 短暂延迟
    }
    
    // 发送 Enter 键
    INPUT_RECORD irEnterDown;
    irEnterDown.EventType = KEY_EVENT;
    irEnterDown.KeyEvent.bKeyDown = TRUE;
    irEnterDown.KeyEvent.wRepeatCount = 1;
    irEnterDown.KeyEvent.wVirtualKeyCode = VK_RETURN;
    irEnterDown.KeyEvent.wVirtualScanCode = 0;
    irEnterDown.KeyEvent.UnicodeChar = L'\r';
    irEnterDown.KeyEvent.dwControlKeyState = 0;
    
    pf_WriteConsoleInputW(g_hInput, &irEnterDown, 1, &written);
    totalWritten += written;
    
    INPUT_RECORD irEnterUp;
    irEnterUp.EventType = KEY_EVENT;
    irEnterUp.KeyEvent.bKeyDown = FALSE;
    irEnterUp.KeyEvent.wRepeatCount = 1;
    irEnterUp.KeyEvent.wVirtualKeyCode = VK_RETURN;
    irEnterUp.KeyEvent.wVirtualScanCode = 0;
    irEnterUp.KeyEvent.UnicodeChar = L'\r';
    irEnterUp.KeyEvent.dwControlKeyState = 0;
    
    pf_WriteConsoleInputW(g_hInput, &irEnterUp, 1, &written);
    totalWritten += written;
    
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
// 实现: readScreen()
// 读取屏幕内容
// ============================================================================

napi_value ReadScreen(napi_env env, napi_callback_info info) {
    CHECK_ATTACHED(env);
    
    // 获取屏幕缓冲区信息
    CONSOLE_SCREEN_BUFFER_INFO csbi;
    if (!pf_GetConsoleScreenBufferInfo(g_hOutput, &csbi)) {
        napi_throw_error(env, NULL, "GetConsoleScreenBufferInfo failed");
        return NULL;
    }
    
    SHORT width = csbi.srWindow.Width();
    SHORT height = csbi.srWindow.Height();
    
    // 分配缓冲区
    DWORD bufferSize = width * height;
    CHAR_INFO* buffer = new CHAR_INFO[bufferSize];
    
    COORD bufferCoord = { 0, 0 };
    COORD bufferSizeCoord = { width, height };
    SMALL_RECT readRegion = csbi.srWindow;
    
    // 读取屏幕内容
    if (!pf_ReadConsoleOutputW(g_hOutput, buffer, bufferSizeCoord, bufferCoord, &readRegion)) {
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
                line += buffer[idx].UnicodeChar;
            }
        }
        
        // 移除尾部空白
        while (!line.empty() && iswspace(line.back())) {
            line.pop_back();
        }
        
        // 只保留有内容的行
        bool hasContent = false;
        for (wchar_t c : line) {
            if (!iswspace(c)) {
                hasContent = true;
                break;
            }
        }
        
        if (hasContent) {
            napi_value lineValue;
            napi_create_string_utf16(env, (const char16_t*)line.c_str(), line.length(), &lineValue);
            napi_set_element(env, result, lineIndex++, lineValue);
        }
    }
    
    delete[] buffer;
    
    return result;
}

// ============================================================================
// 实现: detach()
// 分离控制台
// ============================================================================

napi_value Detach(napi_env env, napi_callback_info info) {
    if (g_isAttached) {
        pf_FreeConsole();
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
        DECLARE_NAPI_METHOD("readScreen", ReadScreen),
        DECLARE_NAPI_METHOD("detach", Detach)
    };
    
    status = napi_define_properties(env, exports, sizeof(desc) / sizeof(desc[0]), desc);
    NAPI_THROW_IF_FAILED(env, status, "Failed to define properties");
    
    // 导出常量
    napi_value inputHandleValue;
    napi_create_int64(env, STD_INPUT_HANDLE, &inputHandleValue);
    napi_set_named_property(env, exports, "STD_INPUT_HANDLE", inputHandleValue);
    
    napi_value outputHandleValue;
    napi_create_int64(env, STD_OUTPUT_HANDLE, &outputHandleValue);
    napi_set_named_property(env, exports, "STD_OUTPUT_HANDLE", outputHandleValue);
    
    return exports;
}

NAPI_MODULE(NODE_GYP_MODULE_NAME, Init)
