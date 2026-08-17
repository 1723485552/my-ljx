# 🔍 fish.exe 静态指纹识别与特征分析报告

## 执行日期
2026-08-17

---

## 📋 第一阶段：静态指纹与特征识别

### 1. **基础信息概览**

| 属性 | 值 |
|------|-----|
| **文件名** | fish.exe |
| **完整路径** | C:\Program Files\GO捕鱼\fish.exe |
| **文件大小** | 5,803,008 字节 (~5.5 MB) |
| **PE 架构** | **x86 (32-bit)** |
| **子系统** | Windows GUI Application |
| **编译时间** | [待确认 - 时间戳损坏或特殊格式] |

---

### 2. **加壳与压缩检测**

#### 区段熵值分析：

| 区段 | 大小 | 熵值 | 状态 |
|------|------|------|------|
| `.text` | 3507200 字节 | **6.56** | ✅ Normal |
| `.rdata` | 1830912 字节 | **6.3** | ✅ Normal |
| `.data` | 166400 字节 | **5.3** | ✅ Normal |
| `.rsrc` | ? | **5.36** | ✅ Normal |
| `.reloc` | ? | **6.67** | ✅ Normal |

**最高熵值**: 6.67（远低于 7.0）

#### 加壳判断：
```
❌ 无加壳检测到
✅ 文件为未压缩状态
✅ 文件为未加密状态
```

**结论**: **这是一个完全未压缩的可执行文件**，可以直接进行静态反编译。

---

### 3. **技术栈/运行时识别**

#### 导入库分析（28 个 DLL 检测到）：

**核心库:**
- `kernel32.dll` - Windows 核心 API
- `user32.dll` - UI 相关 API
- `gdi32.dll` - 图形绘制
- `advapi32.dll` - 系统管理

**C++ 运行时:**
- `MSVCP120.dll` - MSVC C++ Standard Library (Visual Studio 2013)
- `OLEAUT32.dll` - COM/OLE 自动化

**游戏引擎:**
- **`libcocos2d_2013.dll`** ⭐ - Cocos2D 游戏引擎 (C++)
- `glew32.dll` - OpenGL 扩展库
- `zlib1.dll` - 压缩库

**网络通信:**
- `Winsock.dll` - Winsock API
- `WS2_32.dll` - Winsock 2 (TCP/IP)

**其他:**
- `iconv.dll` - 字符集转换
- `loadall.dll` - [未知功能]
- `COMDLG32.dll` - 文件对话框
- `winmm.dll` - Windows 多媒体

#### 技术栈识别：

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
🎮 TECHNOLOGY STACK DETECTED:
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

✅ Language: C/C++ (Native MSVC Compilation)
✅ Framework: Cocos2D Game Engine (2013 版本)
✅ Graphics: OpenGL (via GLEW)
✅ Platform: Win32 API
✅ Compiler: Visual Studio 2013 (MSVC v120)
✅ Runtime: MSVC Runtime v120

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📊 NOT: .NET | Python | Electron | Golang | Rust
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

### 4. **关键导入 API 分析**

#### 网络相关 API：
```
✅ Winsock.dll (WSA* functions)
✅ WS2_32.dll (socket, connect, send, recv)
```
→ **存在网络通信能力** | 可用于连接服务器、网络游戏

#### 加解密相关：
```
✅ 检测到: MD5、SHA 字符串
❌ 未检测到: CryptEncrypt、BCrypt 等高级加密 API
```
→ **可能使用开源加密库或自实现哈希**

#### 注册表操作：
```
❌ 未检测到 RegOpenKey、RegSetValue 等
```
→ **不修改系统注册表**

#### 文件 I/O：
```
❌ 未明确检测到 CreateFile/WriteFile
✅ 标准 C runtime 文件操作可能存在
```

---

### 5. **敏感字符串初筛**

#### 检测到的 URL：
```
1. http://www.google-analytics.com/collect
   → Google Analytics 数据收集

2. http://lua-users.org/lists/lua-l/2011-02/msg01012.html
   → Lua 编程论坛文档链接

3. http://www.w3.org/2001/XMLSchema
   → XML Schema 定义

4. http://www.w3.org/2001/XMLSchema-instance
   → XML Schema 实例

5. http://bugs.activestate.com/show_bug.cgi?id=81178
   → ActiveState 虫报告

6. http://stackoverflow.com/questions/7134590/...
   → Stack Overflow 帖子

7. http://www.softwareverify.com/blog/?p=319
   → 博客链接
```

**分析**：
- 这些 URL 似乎是**嵌入的文档链接和帮助参考**
- 不是实际的恶意 C2 服务器地址
- Google Analytics 表示程序可能有**远程使用分析**

#### 检测到的 IP 地址：
```
1. 127.0.0.1  (localhost - 本机回环)
2. 0.0.0.0    (全零地址 - 默认路由)
6.0.0.0       (保留地址)
```

**分析**：
- 这些是**硬编码的通用配置**，不是真实服务器
- 可能用于本地测试或默认配置

#### 加密相关字符串：
```
✅ MD5
✅ SHA
```

---

## 🎯 结论与建议

### **这是什么语言打包的？**
```
🔹 Native C/C++ (MSVC Visual Studio 2013 编译)
🔹 使用 Cocos2D 游戏引擎（2013 版本）
🔹 完全原生 Win32/OpenGL 应用程序
🔹 NOT Python, NOT .NET, NOT Electron
```

### **是否有壳？**
```
❌ NO PACKING DETECTED
✅ All sections have normal entropy (< 7.0)
✅ 文件为明文可读状态
✅ 可以直接用反编译器分析
```

### **下一步解包或反编译应该怎么做？**

#### 推荐工具链：

| 工具 | 用途 | 说明 |
|------|------|------|
| **Ghidra** (免费) | 反汇编 + 反编译 | NSA 开源，支持 x86，生成伪代码 |
| **IDA Pro** (商业) | 专业反编译 | 业界标准，功能最强 |
| **x32dbg** | 动态调试 | 开源免费调试器 |
| **dnSpy** (如有.NET) | .NET 分析 | 仅在误判情况下使用 |
| **strings.exe** | 字符串提取 | Sysinternals，快速找敏感信息 |

#### 建议的分析步骤：

**第 1 步：快速字符串提取**
```bash
strings.exe fish.exe > strings_output.txt
# 或
strings fish.exe | grep -i "http\|config\|server\|key"
```

**第 2 步：用 Ghidra 打开二进制**
```
1. 启动 Ghidra
2. File → New Project
3. Import File → 选择 fish.exe
4. 自动分析会开始
5. 导航到 main() 函数查看程序流
```

**第 3 步：关键函数定位**
- 在 Ghidra 中搜索 `WinMain` 或 `main`（程序入口）
- 追踪网络调用（Winsock 函数）
- 追踪资源加载（`.rsrc` 段）
- 追踪 Cocos2D 初始化代码

**第 4 步：动态分析（可选）**
```
1. 用 x32dbg 加载 fish.exe
2. 设置断点在 WinMain 上
3. Step Through 代码查看运行时行为
4. 用 Wireshark 监听网络流量
5. 用 Process Monitor 监听文件/注册表访问
```

---

## 📊 技术细节总结

### PE 文件信息：
```
Magic: MZ (0x4D5A) - Valid PE
Architecture: Intel 80386 (0x014C) - 32-bit
Machine Type: i386
Subsystem: 2 (Windows GUI)
Sections: 5
Debug Symbols: None (stripped)
```

### 编译环境：
```
Compiler: MSVC v120 (Visual Studio 2013)
C++ Runtime: MSVCP120.dll
Target Platform: Windows (XP SP3 及以上)
```

### 游戏引擎版本：
```
Cocos2D: 2013 vintage build
OpenGL: Via GLEW (跨平台图形)
Audio: 可能使用 libfmod 或 OpenAL
```

---

## ⚠️ 安全建议

1. **虚拟机执行** - 即使无壳，也应在隔离环境中运行
2. **网络隔离** - 运行时断网以防止回连
3. **持续监控** - 使用 Process Monitor 和 Wireshark
4. **保留副本** - 用于多轮分析的不同工具

---

## 📝 分析依据

- PE Header 直接提取
- Section Entropy 计算（Shannon Entropy）
- DLL Import Table 解析
- ASCII 字符串搜索和正则提取
- 已知签名库对比（Cocos2D 特征）

---

**报告生成者**: Reverse Engineering Assistant  
**分析深度**: Phase 1 Complete (Static Fingerprinting)  
**下一阶段**: 动态分析、函数逆向、配置文件解析

