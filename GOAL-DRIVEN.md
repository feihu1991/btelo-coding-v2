# Goal-Driven: BTELO Coding 核心功能

## Goal
1. 实现统一思考框，所有子类型消息在一个框内显示，展开后可折叠查看
2. 实现远程更新功能，App 检测 GitHub Release 新版本后下载安装
3. claudex 支持自我修改、重新安装、重启自身

## Criteria for success
1. 用户发送消息后，思考框**立即**显示（不等待响应），灯泡转动
2. 思考过程中，框外**轮动显示**当前收到的子类型消息
3. 所有 5 种子类型消息在**一个框内**累积，展开后每个子类型可折叠，有独立图标
4. Claude 文本回复在思考框**下方**单独显示
5. 思考完成后，灯泡**停止转动**
6. 键盘弹出时消息列表**上移**，发送消息后键盘**不自动收回**
7. App 能检测 GitHub Release 最新版本，提示用户更新，确认后下载 APK 并触发安装
8. claudex 能检测自身代码变更，自动执行 `npm install -g .` 重新安装，并重启进程

## Goal-Driven Prompt Template

```
# Goal-Driven(1 master agent + 1 subagent) System

Goal: [[[[[
1. 实现统一思考框，所有子类型消息在一个框内显示，展开后可折叠查看
2. 实现远程更新功能，App 检测 GitHub Release 新版本后下载安装
3. claudex 支持自我修改、重新安装、重启自身
]]]]]

Criteria for success: [[[[[
1. 用户发送消息后，思考框立即显示（不等待响应），灯泡转动
2. 思考过程中，框外轮动显示当前收到的子类型消息
3. 所有5种子类型消息在一个框内累积，展开后每个子类型可折叠，有独立图标
4. Claude文本回复在思考框下方单独显示
5. 思考完成后，灯泡停止转动
6. 键盘弹出时消息列表上移，发送消息后键盘不自动收回
7. App能检测GitHub Release最新版本，提示用户更新，确认后下载APK并触发安装
8. claudex能检测自身代码变更，自动执行npm install -g .重新安装，并重启进程
]]]]]

## Subagent's description:
The subagent's goal is to complete the task assigned by the master agent.
The subagent should break down the task into smaller sub-tasks and work on them one by one.
The subagent should continue to work on the task until the criteria for success are met.

## Master agent's description:
1. Create subagents to complete the task.
2. If the subagent finishes or fails, evaluate the result against criteria. If not met, restart a new subagent.
3. Check subagent activity every 5 minutes. If inactive, verify status and restart if needed.
4. Continue until criteria are met. DO NOT STOP UNTIL USER STOPS MANUALLY.
```
