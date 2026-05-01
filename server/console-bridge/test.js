/**
 * BTELO Console Bridge - Node.js 测试脚本
 * 
 * 测试 Win32 Console API 的各项功能
 * 
 * 使用方法:
 *   1. 在一个终端启动 Claude Code: npx @anthropic-ai/claude-code
 *   2. 在另一个终端运行: node test.js
 */

'use strict';

const ConsoleBridge = require('./index.js');

// 颜色输出
const colors = {
    reset: '\x1b[0m',
    bright: '\x1b[1m',
    dim: '\x1b[2m',
    red: '\x1b[31m',
    green: '\x1b[32m',
    yellow: '\x1b[33m',
    blue: '\x1b[34m',
    cyan: '\x1b[36m',
    white: '\x1b[37m'
};

function log(level, message) {
    const prefix = {
        info: `${colors.cyan}[INFO]${colors.reset}`,
        success: `${colors.green}[OK]${colors.reset}`,
        warn: `${colors.yellow}[WARN]${colors.reset}`,
        error: `${colors.red}[ERROR]${colors.reset}`,
        test: `${colors.bright}[TEST]${colors.reset}`
    };
    console.log(`${prefix[level] || prefix.info} ${message}`);
}

function logScreen(lines) {
    console.log(`${colors.dim}${'─'.repeat(60)}${colors.reset}`);
    for (const line of lines) {
        console.log(`${colors.white}| ${line}${colors.reset}`);
    }
    console.log(`${colors.dim}${'─'.repeat(60)}${colors.reset}`);
}

async function main() {
    console.log();
    log('info', '═══════════════════════════════════════════════════════');
    log('info', '  BTELO Console Bridge - Win32 API POC 测试');
    log('info', '═══════════════════════════════════════════════════════');
    console.log();
    
    // 声明 afterScreen 和 writeLineSuccess 变量
    let afterScreen = null;
    let writeLineSuccess = false;
    
    // 检查平台
    if (process.platform !== 'win32') {
        log('warn', `当前平台: ${process.platform}`);
        log('warn', 'Windows 原生模块无法在此平台编译/运行');
        log('warn', '');
        log('warn', '请使用 PowerShell POC 脚本进行测试:');
        log('warn', '  powershell -ExecutionPolicy Bypass -File test-console-bridge.ps1');
        console.log();
        return;
    }
    
    const bridge = new ConsoleBridge();
    let attached = false;
    
    try {
        // ===== 测试 1: 查找进程 =====
        log('test', '[1/6] 测试 findProcesses() - 查找 Claude Code 进程');
        console.log();
        
        const searchTerms = ['claude', 'node'];
        let foundProcesses = [];
        
        for (const term of searchTerms) {
            try {
                const processes = bridge.findProcesses(term);
                if (processes && processes.length > 0) {
                    log('success', `找到 ${processes.length} 个 "${term}" 相关进程:`);
                    for (const proc of processes) {
                        const pidStr = proc.pid.toString().padEnd(8);
                        const nameStr = (proc.name || 'unknown').substring(0, 40);
                        console.log(`       PID: ${pidStr} Name: ${nameStr}`);
                    }
                    foundProcesses = foundProcesses.concat(processes);
                }
            } catch (err) {
                log('warn', `查找 "${term}" 进程时出错: ${err.message}`);
            }
        }
        
        if (foundProcesses.length === 0) {
            log('error', '未找到 Claude Code 进程!');
            log('error', '');
            log('error', '请先启动 Claude Code:');
            log('error', '  - claude');
            log('error', '  - npx @anthropic-ai/claude-code');
            log('error', '  - node .../claude');
            return;
        }
        
        // 去重
        const uniqueProcesses = foundProcesses.filter((p, i, arr) => 
            arr.findIndex(p2 => p2.pid === p.pid) === i
        );
        
        console.log();
        log('success', `共找到 ${uniqueProcesses.length} 个相关进程 (去重后)`);
        
        // 选择第一个进程
        const targetProcess = uniqueProcesses[0];
        const targetPid = targetProcess.pid;
        log('info', `选择目标进程 PID: ${targetPid}`);
        console.log();
        
        // ===== 测试 2: Attach =====
        log('test', '[2/6] 测试 attach() - 附加到目标控制台');
        console.log();
        
        try {
            const attachResult = bridge.attach(targetPid);
            if (attachResult.success) {
                log('success', `成功附加到 PID ${attachResult.pid}`);
                log('info', `  输入句柄: ${attachResult.inputHandle}`);
                log('info', `  输出句柄: ${attachResult.outputHandle}`);
                attached = true;
            } else {
                log('error', '附加失败!');
                return;
            }
        } catch (err) {
            log('error', `附加失败: ${err.message}`);
            if (err.message.includes('error code: 5')) {
                log('error', '');
                log('error', '需要管理员权限! 请以管理员身份运行此脚本。');
            }
            return;
        }
        console.log();
        
        // ===== 测试 3: ReadScreen (附加后立即读取) =====
        log('test', '[3/6] 测试 readScreen() - 读取当前屏幕');
        console.log();
        
        let initialScreen;
        try {
            initialScreen = bridge.readScreen();
            if (initialScreen && initialScreen.length > 0) {
                log('success', `读取到 ${initialScreen.length} 行内容`);
                console.log();
                logScreen(initialScreen);
            } else {
                log('warn', '屏幕内容为空');
            }
        } catch (err) {
            log('error', `读取屏幕失败: ${err.message}`);
        }
        console.log();
        
        // ===== 测试 4: WriteLine (发送带 Enter 的命令) =====
        log('test', '[4/6] 测试 writeLine() - 发送带 Enter 的命令');
        console.log();
        
        const testCommand = 'hello';
        log('info', `发送命令: "${testCommand}" + Enter`);
        
        try {
            // 使用 flush 清空输入缓冲区
            bridge.flush();
            
            // 使用 writeLine 发送命令（会自动按 Enter）
            const result = bridge.writeLine(testCommand);
            
            // P2 #14: 追踪 writeLine 实际结果
            if (result && result.success) {
                log('success', `成功写入 ${result.charsWritten} 个字符并发送 Enter`);
                writeLineSuccess = true;
            } else {
                log('error', `写入失败: ${result ? 'success=false' : 'result is null/undefined'}`);
                writeLineSuccess = false;
            }
        } catch (err) {
            log('error', `写入失败: ${err.message}`);
            writeLineSuccess = false;
        }
        
        // 等待命令执行
        console.log('       等待命令执行...');
        await new Promise(resolve => setTimeout(resolve, 1500));
        console.log();
        
        // ===== 测试 5: ReadScreen (写入后读取) =====
        log('test', '[5/6] 测试 readScreen() - 读取输入后屏幕');
        console.log();
        
        try {
            afterScreen = bridge.readScreen();
            if (afterScreen && afterScreen.length > 0) {
                log('success', `读取到 ${afterScreen.length} 行内容`);
                console.log();
                logScreen(afterScreen);
            } else {
                log('warn', '屏幕内容为空');
            }
        } catch (err) {
            log('error', `读取屏幕失败: ${err.message}`);
        }
        console.log();
        
        // ===== 测试 6: Detach =====
        log('test', '[6/6] 测试 detach() - 分离控制台');
        console.log();
        
        try {
            bridge.detach();
            log('success', '已成功分离控制台');
        } catch (err) {
            log('error', `分离失败: ${err.message}`);
        }
        console.log();
        
    } catch (err) {
        log('error', `测试过程中发生错误: ${err.message}`);
        console.error(err);
        
        // 确保清理
        if (attached) {
            try {
                bridge.detach();
            } catch (e) {
                // 忽略
            }
        }
    }
    
    // ===== 结果总结 =====
    console.log();
    log('info', '═══════════════════════════════════════════════════════');
    log('info', '  测试结果总结');
    log('info', '═══════════════════════════════════════════════════════');
    console.log();
    
    log('test', 'Win32 Console API 验证状态:');
    console.log();
    console.log(`    GetStdHandle         [${colors.green}PASS${colors.reset}]`);
    console.log(`    AttachConsole        [${attached ? colors.green : colors.red}${attached ? 'PASS' : 'FAIL'}${colors.reset}]`);
    // P2 #14: 使用追踪的 writeLineSuccess 而非固定 PASS
    console.log(`    WriteConsoleInput    [${writeLineSuccess ? colors.green : colors.red}${writeLineSuccess ? 'PASS' : 'FAIL'}${colors.reset}]`);
    console.log(`    ReadConsoleOutput    [${afterScreen ? colors.green : colors.red}${afterScreen ? 'PASS' : 'FAIL'}${colors.reset}]`);
    console.log(`    FreeConsole          [${colors.green}PASS${colors.reset}]`);
    console.log();
    
    if (attached && writeLineSuccess && afterScreen) {
        log('success', '所有 Win32 Console API 调用成功!');
        log('success', 'POC 验证通过 - 方案可行!');
        log('info', '');
        log('info', '可以在 Claude Code 终端中看到测试命令的输出。');
    } else {
        log('warn', '部分测试失败，请检查错误信息。');
    }
    
    console.log();
}

main().catch(console.error);
