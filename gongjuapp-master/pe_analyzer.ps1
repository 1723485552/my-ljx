# PE 文件静态分析脚本（PowerShell 版本）
# 用于初步识别二进制文件的特征

param(
    [string]$ExePath = "C:\Program Files\GO捕鱼\fish.exe"
)

function Get-PEHeaders {
    param([string]$FilePath)
    
    if (!(Test-Path $FilePath)) {
        Write-Host "错误：文件不存在 - $FilePath" -ForegroundColor Red
        return $null
    }
    
    $file = [System.IO.File]::ReadAllBytes($FilePath)
    $result = @{}
    
    # DOS Header 检查
    $dosSignature = [System.Text.Encoding]::ASCII.GetString($file[0..1])
    if ($dosSignature -ne "MZ") {
        Write-Host "错误：不是有效的 PE 文件" -ForegroundColor Red
        return $null
    }
    
    # PE Offset
    $peOffset = [BitConverter]::ToInt32($file, 0x3C)
    
    # PE 签名检查
    $peSignature = [System.Text.Encoding]::ASCII.GetString($file[$peOffset..($peOffset+3)])
    if ($peSignature -ne "PE`0`0") {
        Write-Host "警告：非标准 PE 文件格式" -ForegroundColor Yellow
    }
    
    # COFF Header (位置：PE offset + 4)
    $coffOffset = $peOffset + 4
    $machine = [BitConverter]::ToInt16($file, $coffOffset)
    $numberOfSections = [BitConverter]::ToInt16($file, $coffOffset + 2)
    $timeDateStamp = [BitConverter]::ToInt32($file, $coffOffset + 4)
    $characteristics = [BitConverter]::ToInt16($file, $coffOffset + 18)
    
    # 确定架构
    $arch = switch($machine) {
        0x14c { "x86 (32-bit)" }
        0x8664 { "x64 (64-bit)" }
        0xaa64 { "ARM64" }
        0x1c0 { "ARM" }
        default { "Unknown (0x$($machine.ToString('X4')))" }
    }
    
    # 编译时间戳
    $compileTime = [DateTime]::UnixEpoch.AddSeconds($timeDateStamp)
    
    # 获取子系统
    $optHeaderSize = [BitConverter]::ToInt16($file, $coffOffset + 16)
    $subsystem = [BitConverter]::ToInt16($file, $coffOffset + 20 + 68)
    
    $subsystemName = switch($subsystem) {
        1 { "Native" }
        2 { "Windows GUI" }
        3 { "Windows CUI" }
        7 { "POSIX CUI" }
        9 { "Windows CE GUI" }
        10 { "EFI Application" }
        11 { "EFI Boot Service Driver" }
        12 { "EFI Runtime Driver" }
        13 { "EFI ROM" }
        14 { "Xbox" }
        16 { "Windows Boot Application" }
        default { "Unknown ($subsystem)" }
    }
    
    # 特性标志
    $characteristics_str = @()
    if ($characteristics -band 0x0002) { $characteristics_str += "EXECUTABLE_IMAGE" }
    if ($characteristics -band 0x0004) { $characteristics_str += "LINKER_OUTPUT" }
    if ($characteristics -band 0x0008) { $characteristics_str += "LARGE_ADDRESS_AWARE" }
    if ($characteristics -band 0x0010) { $characteristics_str += "BYTES_REVERSED_LO" }
    if ($characteristics -band 0x0100) { $characteristics_str += "32BIT_MACHINE" }
    if ($characteristics -band 0x0200) { $characteristics_str += "DEBUG_STRIPPED" }
    if ($characteristics_str.Count -eq 0) { $characteristics_str = @("None") }
    
    $result['Architecture'] = $arch
    $result['CompileTime'] = $compileTime.ToString("yyyy-MM-dd HH:mm:ss UTC")
    $result['Subsystem'] = $subsystemName
    $result['Sections'] = $numberOfSections
    $result['Characteristics'] = $characteristics_str -join ", "
    $result['FileSize'] = $file.Count
    $result['PE_Offset'] = $peOffset
    
    return $result
}

function Get-Sections {
    param([string]$FilePath)
    
    $file = [System.IO.File]::ReadAllBytes($FilePath)
    $peOffset = [BitConverter]::ToInt32($file, 0x3C)
    $coffOffset = $peOffset + 4
    $numberOfSections = [BitConverter]::ToInt16($file, $coffOffset + 2)
    
    $sections = @()
    $sectionOffset = $coffOffset + 20 + [BitConverter]::ToInt16($file, $coffOffset + 16)
    
    for ($i = 0; $i -lt $numberOfSections; $i++) {
        $currentSectionOffset = $sectionOffset + ($i * 40)
        
        $sectionName = [System.Text.Encoding]::ASCII.GetString($file[$currentSectionOffset..($currentSectionOffset + 7)]).TrimEnd("`0")
        $virtualSize = [BitConverter]::ToInt32($file, $currentSectionOffset + 8)
        $virtualAddress = [BitConverter]::ToInt32($file, $currentSectionOffset + 12)
        $sizeOfRawData = [BitConverter]::ToInt32($file, $currentSectionOffset + 16)
        $pointerToRawData = [BitConverter]::ToInt32($file, $currentSectionOffset + 20)
        $characteristics = [BitConverter]::ToInt32($file, $currentSectionOffset + 36)
        
        # 计算熵值
        $sectionData = $file[$pointerToRawData..($pointerToRawData + $sizeOfRawData - 1)]
        $entropy = Calculate-Entropy $sectionData
        
        $sections += @{
            'Name' = $sectionName
            'VirtualSize' = $virtualSize
            'VirtualAddress' = "0x$($virtualAddress.ToString('X8'))"
            'RawSize' = $sizeOfRawData
            'RawPointer' = "0x$($pointerToRawData.ToString('X8'))"
            'Entropy' = $entropy
            'Packed' = if ($entropy -gt 7.0) { "YES (High Entropy)" } else { "NO" }
        }
    }
    
    return $sections
}

function Calculate-Entropy {
    param([byte[]]$Data)
    
    if ($Data.Count -eq 0) { return 0 }
    
    $freq = @{}
    foreach ($byte in $Data) {
        if ($freq.ContainsKey($byte)) {
            $freq[$byte]++
        } else {
            $freq[$byte] = 1
        }
    }
    
    $entropy = 0
    foreach ($count in $freq.Values) {
        $p = [double]$count / $Data.Count
        if ($p -gt 0) {
            $entropy -= $p * [Math]::Log($p, 2)
        }
    }
    
    return [Math]::Round($entropy, 2)
}

function Get-ImportedDLLs {
    param([string]$FilePath)
    
    $file = [System.IO.File]::ReadAllBytes($FilePath)
    $peOffset = [BitConverter]::ToInt32($file, 0x3C)
    $coffOffset = $peOffset + 4
    
    # 这是一个简化版本，提取 ASCII 字符串中可能的 DLL 名称
    $text = [System.Text.Encoding]::ASCII.GetString($file) -split "`0"
    
    $dlls = $text | Where-Object { 
        $_ -match '\.dll$' -and $_.Length -lt 100 
    } | Select-Object -Unique | Sort-Object
    
    return $dlls
}

function Get-SuspiciousStrings {
    param([string]$FilePath)
    
    $file = [System.IO.File]::ReadAllBytes($FilePath)
    $text = [System.Text.Encoding]::ASCII.GetString($file)
    
    $suspicious = @()
    
    # 搜索 URLs
    $urls = $text | Select-String -Pattern 'https?://[^\s<>"{}|\\^`\[\]]*' -AllMatches
    if ($urls) {
        $suspicious += @{ Type = "URLs"; Content = $urls.Matches.Value }
    }
    
    # 搜索 IP 地址
    $ips = $text | Select-String -Pattern '\b(?:\d{1,3}\.){3}\d{1,3}\b' -AllMatches
    if ($ips) {
        $suspicious += @{ Type = "IP Addresses"; Content = $ips.Matches.Value }
    }
    
    return $suspicious
}

# 主程序
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  PE 文件静态分析工具 v1.0" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

Write-Host "目标文件: $ExePath" -ForegroundColor Green

# 获取基础信息
$headers = Get-PEHeaders $ExePath
if ($headers) {
    Write-Host ""
    Write-Host "【基础信息】" -ForegroundColor Yellow
    Write-Host "架构: $($headers['Architecture'])"
    Write-Host "编译时间: $($headers['CompileTime'])"
    Write-Host "子系统: $($headers['Subsystem'])"
    Write-Host "区段数: $($headers['Sections'])"
    Write-Host "文件大小: $($headers['FileSize']) 字节"
    Write-Host "特性: $($headers['Characteristics'])"
}

# 获取区段信息
Write-Host ""
Write-Host "【区段信息 & 熵值分析】" -ForegroundColor Yellow
$sections = Get-Sections $ExePath
$sections | Format-Table -AutoSize -Property Name, VirtualSize, RawSize, Entropy, Packed

# 检测加壳
$packed = $sections | Where-Object { $_.Entropy -gt 7.0 }
if ($packed) {
    Write-Host ""
    Write-Host "⚠ 检测到高熵区段 - 可能已加壳！" -ForegroundColor Red
    foreach ($sec in $packed) {
        Write-Host "  - $($sec.Name): 熵值 $($sec.Entropy)"
    }
}

# 获取导入的 DLL
Write-Host ""
Write-Host "【导入的 DLL】" -ForegroundColor Yellow
$dlls = Get-ImportedDLLs $ExePath
$dlls | ForEach-Object { Write-Host "  $_" }

# 敏感字符串
Write-Host ""
Write-Host "【可疑字符串初筛】" -ForegroundColor Yellow
$suspicious = Get-SuspiciousStrings $ExePath
if ($suspicious.Count -gt 0) {
    foreach ($item in $suspicious) {
        Write-Host "  $($item.Type):"
        $item.Content | ForEach-Object { Write-Host "    $_" }
    }
} else {
    Write-Host "  (未找到明显的 URL/IP)"
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "分析完成！" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
