/**
 * BTELO Console Bridge - Node.js 封装层
 * 
 * 为 Windows 控制台 API 提供跨平台封装
 * 非 Windows 平台会抛出适当的错误
 */

'use strict';

const path = require('path');

// 平台检测
const isWindows = process.platform === 'win32';

// 原生模块缓存
let nativeModule = null;

/**
 * 加载原生模块
 */
function loadNativeModule() {
    if (nativeModule) {
        return nativeModule;
    }
    
    if (!isWindows) {
        throw new Error(
            'Console Bridge is only supported on Windows.\n' +
            'This module uses Win32 Console API (AttachConsole, WriteConsoleInput, ReadConsoleOutput).\n' +
            'For testing on Linux/Mac, use the PowerShell POC script instead.'
        );
    }
    
    try {
        // 尝试加载编译后的原生模块
        nativeModule = require('./build/Release/console_bridge');
    } catch (err) {
        // 模块未编译
        throw new Error(
            'Native module not compiled.\n' +
            'Please run:\n' +
            '  npm install\n' +
            '  npx node-gyp rebuild\n' +
            '\n' +
            'Alternatively, use the PowerShell POC script for testing:\n' +
            '  powershell -ExecutionPolicy Bypass -File test-console-bridge.ps1\n',
            { cause: err }
        );
    }
    
    return nativeModule;
}

/**
 * ConsoleBridge 类
 */
class ConsoleBridge {
    constructor() {
        this._isAttached = false;
        this._attachedPid = null;
    }
    
    /**
     * 查找进程
     * @param {string} name - 进程名称 (如 "claude", "node")
     * @returns {Array<{pid: number, name: string, parentPid: number}>}
     */
    findProcesses(name) {
        const bridge = loadNativeModule();
        return bridge.findProcesses(name);
    }
    
    /**
     * 附加到目标进程的控制台
     * @param {number} pid - 目标进程 ID
     * @returns {{success: boolean, pid: number, inputHandle: number, outputHandle: number}}
     */
    attach(pid) {
        if (this._isAttached) {
            console.warn('[ConsoleBridge] Already attached, detaching first...');
            this.detach();
        }
        
        const bridge = loadNativeModule();
        const result = bridge.attach(pid);
        
        if (result.success) {
            this._isAttached = true;
            this._attachedPid = pid;
        }
        
        return result;
    }
    
    /**
     * 写入文本输入（不自动按 Enter）
     * @param {string} text - 要输入的文本
     * @returns {{success: boolean, charsWritten: number}}
     */
    writeInput(text) {
        const bridge = loadNativeModule();
        return bridge.writeInput(text);
    }
    
    /**
     * P0 #2: 写入文本输入并按 Enter
     * @param {string} text - 要输入的文本
     * @returns {{success: boolean, charsWritten: number}}
     */
    writeLine(text) {
        const bridge = loadNativeModule();
        return bridge.writeLine(text);
    }
    
    /**
     * P0 #2: 清空输入缓冲区
     * @returns {boolean}
     */
    flush() {
        const bridge = loadNativeModule();
        return bridge.flush();
    }
    
    /**
     * 读取屏幕内容
     * @returns {string[]} - 屏幕行数组
     */
    readScreen() {
        const bridge = loadNativeModule();
        return bridge.readScreen();
    }
    
    /**
     * 分离控制台
     * @returns {boolean}
     */
    detach() {
        if (!this._isAttached) {
            return true;
        }
        
        try {
            const bridge = loadNativeModule();
            bridge.detach();
        } catch (err) {
            // 忽略错误
        }
        
        this._isAttached = false;
        this._attachedPid = null;
        return true;
    }
    
    /**
     * 检查是否已附加
     */
    get isAttached() {
        return this._isAttached;
    }
    
    /**
     * 获取已附加的进程 ID
     */
    get attachedPid() {
        return this._attachedPid;
    }
}

// 导出
module.exports = ConsoleBridge;
module.exports.default = ConsoleBridge;

// 如果直接运行，显示帮助信息
if (require.main === module) {
    console.log(`
BTELO Console Bridge - Node.js N-API Addon
==========================================

Platform: ${process.platform}
Status: ${isWindows ? 'Windows detected - native module available' : 'Non-Windows platform - use PowerShell POC'}

Usage:
  const ConsoleBridge = require('./index.js');
  const bridge = new ConsoleBridge();
  
  // Find Claude Code processes
  const processes = bridge.findProcesses('claude');
  
  // Attach to a process
  bridge.attach(pid);
  
  // Write input (without Enter)
  bridge.writeInput('hello');
  
  // Write line (with Enter)
  bridge.writeLine('hello');
  
  // Flush input buffer
  bridge.flush();
  
  // Read screen
  const screen = bridge.readScreen();
  
  // Detach
  bridge.detach();

Note: This module only works on Windows. For testing on other platforms,
use the PowerShell POC script:
  powershell -ExecutionPolicy Bypass -File test-console-bridge.ps1
`);
}
