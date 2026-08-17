param(
    [string]$ExePath = "C:\Program Files\GO捕鱼\fish.exe"
)

function Get-PEHeaders {
    param([string]$FilePath)
    
    if (!(Test-Path $FilePath)) {
        Write-Host "File not found: $FilePath" -ForegroundColor Red
        return $null
    }
    
    $file = [System.IO.File]::ReadAllBytes($FilePath)
    $result = @{}
    
    # DOS Header check
    $dosSignature = [System.Text.Encoding]::ASCII.GetString($file[0..1])
    if ($dosSignature -ne "MZ") {
        Write-Host "Not a valid PE file" -ForegroundColor Red
        return $null
    }
    
    # PE Offset
    $peOffset = [BitConverter]::ToInt32($file, 0x3C)
    
    # COFF Header
    $coffOffset = $peOffset + 4
    $machine = [BitConverter]::ToInt16($file, $coffOffset)
    $numberOfSections = [BitConverter]::ToInt16($file, $coffOffset + 2)
    $timeDateStamp = [BitConverter]::ToInt32($file, $coffOffset + 4)
    $characteristics = [BitConverter]::ToInt16($file, $coffOffset + 18)
    
    # Architecture
    $arch = switch($machine) {
        0x14c { "x86 (32-bit)" }
        0x8664 { "x64 (64-bit)" }
        0xaa64 { "ARM64" }
        0x1c0 { "ARM" }
        default { "Unknown (0x$($machine.ToString('X4')))" }
    }
    
    # Compile time
    $compileTime = [DateTime]::UnixEpoch.AddSeconds($timeDateStamp)
    
    # Subsystem
    $subsystem = [BitConverter]::ToInt16($file, $coffOffset + 20 + 68)
    $subsystemName = switch($subsystem) {
        1 { "Native" }
        2 { "Windows GUI" }
        3 { "Windows CUI" }
        7 { "POSIX CUI" }
        9 { "Windows CE GUI" }
        10 { "EFI Application" }
        default { "Unknown ($subsystem)" }
    }
    
    $result['Architecture'] = $arch
    $result['CompileTime'] = $compileTime.ToString("yyyy-MM-dd HH:mm:ss UTC")
    $result['Subsystem'] = $subsystemName
    $result['Sections'] = $numberOfSections
    $result['FileSize'] = $file.Count
    $result['PE_Offset'] = $peOffset
    
    return $result, $peOffset, $coffOffset, $numberOfSections, $file
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

function Get-Sections {
    param([byte[]]$file, [int]$coffOffset, [int]$numberOfSections)
    
    $sections = @()
    $optHeaderSize = [BitConverter]::ToInt16($file, $coffOffset + 16)
    $sectionOffset = $coffOffset + 20 + $optHeaderSize
    
    for ($i = 0; $i -lt $numberOfSections; $i++) {
        $currentSectionOffset = $sectionOffset + ($i * 40)
        
        $sectionName = [System.Text.Encoding]::ASCII.GetString($file[$currentSectionOffset..($currentSectionOffset + 7)]).TrimEnd("`0")
        $sizeOfRawData = [BitConverter]::ToInt32($file, $currentSectionOffset + 16)
        $pointerToRawData = [BitConverter]::ToInt32($file, $currentSectionOffset + 20)
        
        if ($sizeOfRawData -gt 0 -and $pointerToRawData -lt $file.Count) {
            $endPos = [Math]::Min($pointerToRawData + $sizeOfRawData, $file.Count)
            $sectionData = $file[$pointerToRawData..($endPos - 1)]
            $entropy = Calculate-Entropy $sectionData
        } else {
            $entropy = 0
        }
        
        $sections += @{
            'Name' = $sectionName
            'RawSize' = $sizeOfRawData
            'Entropy' = $entropy
            'Packed' = if ($entropy -gt 7.0) { "YES" } else { "NO" }
        }
    }
    
    return $sections
}

# Main
Write-Host "========================================"
Write-Host "  PE File Static Analysis Tool v1.0"
Write-Host "========================================"
Write-Host ""
Write-Host "Target: $ExePath"

$data = Get-PEHeaders $ExePath
if ($data) {
    $headers = $data[0]
    $peOffset = $data[1]
    $coffOffset = $data[2]
    $numberOfSections = $data[3]
    $file = $data[4]
    
    Write-Host ""
    Write-Host "=== BASIC INFORMATION ==="
    Write-Host "Architecture: $($headers['Architecture'])"
    Write-Host "Compile Time: $($headers['CompileTime'])"
    Write-Host "Subsystem: $($headers['Subsystem'])"
    Write-Host "Number of Sections: $($headers['Sections'])"
    Write-Host "File Size: $($headers['FileSize']) bytes"
    
    Write-Host ""
    Write-Host "=== SECTIONS & ENTROPY ANALYSIS ==="
    $sections = Get-Sections $file $coffOffset $numberOfSections
    $sections | Format-Table -AutoSize
    
    $packed = $sections | Where-Object { $_.Entropy -gt 7.0 }
    if ($packed) {
        Write-Host ""
        Write-Host "WARNING: High entropy sections detected (possible packing)!" -ForegroundColor Red
        foreach ($sec in $packed) {
            Write-Host "  - $($sec.Name): Entropy $($sec.Entropy)"
        }
    }
}

Write-Host ""
Write-Host "========================================"
Write-Host "Analysis Complete"
Write-Host "========================================"
