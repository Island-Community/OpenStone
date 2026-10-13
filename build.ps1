param(
    [ValidateSet("Release", "Debug", "RelWithDebInfo", "MinSizeRel")]
    [string]$Config = "Release",
    [string]$BuildDir = "build/native",
    [string]$JavaHome = "",
    [string]$CmakeVersion = "3.30.5",
    [string]$JdkVersion = "21",
    [switch]$NoAutoInstall
)

$ErrorActionPreference = "Stop"

Write-Host ""
Write-Host "================================================================" -ForegroundColor Yellow
Write-Host "  DEV BUILD ONLY - NO PROTECTION" -ForegroundColor Yellow
Write-Host "================================================================" -ForegroundColor Yellow
Write-Host "  This is a development build. It has NO protection:"
Write-Host "    - No obfuscation"
Write-Host "    - No license wall / auth server"
Write-Host "    - No HWID binding"
Write-Host "    - No debugger or tamper checks"
Write-Host "  Local use only. Do not distribute binaries built with this script."
Write-Host "================================================================" -ForegroundColor Yellow
Write-Host ""

$RepoRoot = $PSScriptRoot
Set-Location -LiteralPath $RepoRoot

function Find-CommandPath {
    param([string]$Name, [string[]]$Fallbacks = @())
    $found = Get-Command $Name -ErrorAction SilentlyContinue
    if ($found) { return $found.Source }
    foreach ($fallback in $Fallbacks) {
        if (Test-Path -LiteralPath $fallback) { return $fallback }
    }
    return $null
}

function Find-CMake {
    $portable = @()
    foreach ($root in @((Join-Path $RepoRoot "build\tools"), (Join-Path $env:LOCALAPPDATA "StoneBuild"))) {
        if (Test-Path -LiteralPath $root) {
            $hits = Get-ChildItem -Path $root -Filter "cmake.exe" -Recurse -ErrorAction SilentlyContinue |
                Where-Object { $_.FullName -like "*\bin\cmake.exe" } |
                Select-Object -First 1
            if ($hits) { $portable += $hits.FullName }
        }
    }
    $fallbacks = @(
        "C:\Program Files\CMake\bin\cmake.exe",
        "C:\Program Files (x86)\CMake\bin\cmake.exe"
    ) + $portable
    return (Find-CommandPath "cmake" $fallbacks)
}

function Invoke-Native {
    param([string]$Exe, [string[]]$StepArgs = @())
    & $Exe @StepArgs
    return $LASTEXITCODE
}

function Install-CMakePortable {
    param([string]$Version)
    $zipName = "cmake-$Version-windows-x86_64.zip"
    $url = "https://github.com/Kitware/CMake/releases/download/v$Version/$zipName"
    $destRoot = Join-Path $RepoRoot "build\tools\cmake-$Version"
    $zipPath = Join-Path $env:TEMP $zipName
    Write-Host "Downloading portable CMake $Version ..." -ForegroundColor Cyan
    Write-Host "    $url"
    try {
        if (Test-Path -LiteralPath $destRoot) {
            Remove-Item -LiteralPath $destRoot -Recurse -Force -ErrorAction SilentlyContinue
        }
        New-Item -ItemType Directory -Force -Path $destRoot | Out-Null
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -Uri $url -OutFile $zipPath -UseBasicParsing
        Expand-Archive -LiteralPath $zipPath -DestinationPath $destRoot -Force
        $exe = Get-ChildItem -Path $destRoot -Filter "cmake.exe" -Recurse -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -like "*\bin\cmake.exe" } |
            Select-Object -First 1
        if (-not $exe) {
            Write-Host "Portable CMake download did not contain bin/cmake.exe." -ForegroundColor Yellow
            return $null
        }
        Write-Host "Portable CMake ready: $($exe.FullName)" -ForegroundColor Green
        return $exe.FullName
    } catch {
        Write-Host ("Portable CMake install failed: " + $_.Exception.Message) -ForegroundColor Yellow
        return $null
    } finally {
        if (Test-Path -LiteralPath $zipPath) {
            Remove-Item -LiteralPath $zipPath -Force -ErrorAction SilentlyContinue
        }
    }
}

function Ensure-CMake {
    param([string]$Version, [switch]$NoInstall)
    $exe = Find-CMake
    if ($exe) { return $exe }
    if ($NoInstall) {
        Write-Error "cmake was not found. Install CMake 3.21+ (with PATH entry) or rerun without -NoAutoInstall."
        exit 1
    }
    Write-Host "cmake was not found. Installing automatically ..." -ForegroundColor Yellow

    $winget = Get-Command "winget" -ErrorAction SilentlyContinue
    if ($winget) {
        Write-Host "Trying winget (Kitware.CMake) ..." -ForegroundColor Cyan
        $code = Invoke-Native "winget" @("install", "-e", "--id", "Kitware.CMake", "--silent",
            "--accept-package-agreements", "--accept-source-agreements")
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" +
            [System.Environment]::GetEnvironmentVariable("Path", "User")
        $exe = Find-CMake
        if ($code -eq 0 -and $exe) {
            Write-Host "CMake installed via winget: $exe" -ForegroundColor Green
            return $exe
        }
        Write-Host "winget install did not yield cmake, trying next method." -ForegroundColor Yellow
    } else {
        Write-Host "winget not available, trying next method." -ForegroundColor DarkYellow
    }

    $choco = Get-Command "choco" -ErrorAction SilentlyContinue
    if ($choco) {
        Write-Host "Trying Chocolatey (cmake) ..." -ForegroundColor Cyan
        $code = Invoke-Native "choco" @("install", "cmake", "-y", "--no-progress")
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" +
            [System.Environment]::GetEnvironmentVariable("Path", "User")
        $exe = Find-CMake
        if ($code -eq 0 -and $exe) {
            Write-Host "CMake installed via Chocolatey: $exe" -ForegroundColor Green
            return $exe
        }
        Write-Host "Chocolatey install did not yield cmake, trying portable ZIP." -ForegroundColor Yellow
    } else {
        Write-Host "Chocolatey not available, falling back to portable ZIP." -ForegroundColor DarkYellow
    }

    $exe = Install-CMakePortable -Version $Version
    if ($exe) { return $exe }
    Write-Error ("Automatic CMake install failed. Install CMake $Version+ manually " +
        "from https://cmake.org/download/ and rerun.")
    exit 1
}

function Invoke-Step {
    param([string]$Label, [string]$Exe, [string[]]$StepArgs = @())
    Write-Host ""
    Write-Host "==> $Label" -ForegroundColor Cyan
    Write-Host ("    " + $Exe + " " + ($StepArgs -join " "))
    & $Exe @StepArgs
    if ($LASTEXITCODE -ne 0) {
        Write-Error "$Label failed with exit code $LASTEXITCODE"
        exit $LASTEXITCODE
    }
}

$MinJdkMajor = 17

function Get-JdkMajor {
    param([string]$JdkHome)
    $release = Join-Path $JdkHome "release"
    if (-not (Test-Path -LiteralPath $release)) { return 0 }
    $m = Select-String -LiteralPath $release -Pattern '^JAVA_VERSION="(?:1\.)?(\d+)' | Select-Object -First 1
    if ($m) { return [int]$m.Matches[0].Groups[1].Value }
    return 0
}

function Test-JdkHome {
    param([string]$Candidate)
    if (-not $Candidate) { return $false }
    if (-not (Test-Path -LiteralPath (Join-Path $Candidate "include\jni.h"))) { return $false }
    return ((Get-JdkMajor $Candidate) -ge $MinJdkMajor)
}

function Find-JdkFromCommand {
    param([string]$Command)
    $cmd = Get-Command $Command -ErrorAction SilentlyContinue
    if (-not $cmd -or -not $cmd.Source) { return $null }
    $jdkHome = Split-Path -Parent (Split-Path -Parent $cmd.Source)
    if (Test-JdkHome $jdkHome) { return $jdkHome }
    return $null
}

function Select-BestJdk {
    param([string[]]$Candidates)
    $seen = @()
    $unique = @()
    foreach ($c in $Candidates) {
        if ($c -and ($seen -notcontains $c)) { $seen += $c; $unique += $c }
    }
    if ($unique.Count -eq 0) { return $null }
    $best = $unique[0]
    $bestVer = Get-JdkMajor $best
    foreach ($c in $unique) {
        $v = Get-JdkMajor $c
        if ($v -gt $bestVer) { $best = $c; $bestVer = $v }
    }
    return $best
}
function Find-Jdk {
    $candidates = @()
    $roots = @((Join-Path $RepoRoot "build\tools"))
    if ($env:LOCALAPPDATA) { $roots += (Join-Path $env:LOCALAPPDATA "StoneBuild") }
    foreach ($root in $roots) {
        if (Test-Path -LiteralPath $root) {
            $hit = Get-ChildItem -Path $root -Filter "jni.h" -Recurse -ErrorAction SilentlyContinue |
                Where-Object { $_.FullName -like "*\include\jni.h" } |
                Select-Object -First 1
            if ($hit) {
                $jdkHome = Split-Path -Parent (Split-Path -Parent $hit.FullName)
                if (Test-JdkHome $jdkHome) { $candidates += $jdkHome }
            }
        }
    }
    foreach ($c in @((Find-JdkFromCommand "javac"), (Find-JdkFromCommand "java"))) {
        if ($c) { $candidates += $c }
    }
    $patterns = @(
        "C:\Program Files\Eclipse Adoptium\*\include\jni.h",
        "C:\Program Files\Java\*\include\jni.h",
        "C:\Program Files\Microsoft\*\include\jni.h",
        "C:\Program Files\Zulu\*\include\jni.h",
        "C:\Program Files\Amazon Corretto\*\include\jni.h",
        "C:\Program Files\BellSoft\*\include\jni.h",
        "C:\jdk*\include\jni.h"
    )
    if ($env:USERPROFILE) { $patterns += (Join-Path $env:USERPROFILE ".jdks\*\include\jni.h") }
    if ($env:LOCALAPPDATA) {
        $patterns += (Join-Path $env:LOCALAPPDATA "Programs\Eclipse Adoptium\*\include\jni.h")
    }
    $found = @()
    foreach ($p in $patterns) {
        $hits = Get-Item -Path $p -ErrorAction SilentlyContinue
        foreach ($h in $hits) { $found += $h.FullName }
    }
    foreach ($f in $found) {
        $jdkHome = Split-Path -Parent (Split-Path -Parent $f)
        if (Test-JdkHome $jdkHome) { $candidates += $jdkHome }
    }
    return (Select-BestJdk $candidates)
}

function Install-JdkPortable {
    param([string]$Version)
    $destRoot = Join-Path $RepoRoot "build\tools\jdk-$Version"
    $zipPath = Join-Path $env:TEMP "temurin-$Version-jdk-windows-x64.zip"
    $url = "https://api.adoptium.net/v3/binary/latest/$Version/ga/windows/x64/jdk/hotspot/normal/eclipse"
    Write-Host "Downloading Temurin JDK $Version (portable, user-local) ..." -ForegroundColor Cyan
    Write-Host "    $url"
    try {
        if (Test-Path -LiteralPath $destRoot) {
            Remove-Item -LiteralPath $destRoot -Recurse -Force -ErrorAction SilentlyContinue
        }
        New-Item -ItemType Directory -Force -Path $destRoot | Out-Null
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -Uri $url -OutFile $zipPath -UseBasicParsing
        Expand-Archive -LiteralPath $zipPath -DestinationPath $destRoot -Force
        $hit = Get-ChildItem -Path $destRoot -Filter "jni.h" -Recurse -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -like "*\include\jni.h" } |
            Select-Object -First 1
        if (-not $hit) {
            Write-Host "JDK download did not contain include/jni.h." -ForegroundColor Yellow
            return $null
        }
        $jdkHome = Split-Path -Parent (Split-Path -Parent $hit.FullName)
        Write-Host "Portable JDK ready: $jdkHome" -ForegroundColor Green
        return $jdkHome
    } catch {
        Write-Host ("Portable JDK install failed: " + $_.Exception.Message) -ForegroundColor Yellow
        return $null
    } finally {
        if (Test-Path -LiteralPath $zipPath) {
            Remove-Item -LiteralPath $zipPath -Force -ErrorAction SilentlyContinue
        }
    }
}

function Ensure-Jdk {
    param([string]$Explicit, [string]$Version, [switch]$NoInstall)
    if ($Explicit) {
        if (Test-JdkHome $Explicit) { return $Explicit }
        Write-Error "-JavaHome is not a JDK $MinJdkMajor+ (include/jni.h and release file): $Explicit"
        exit 1
    }
    foreach ($e in @($env:COLD_JAVA_HOME, $env:JAVA_HOME, $env:JDK_HOME)) {
        if ($e -and (Test-JdkHome $e)) { return $e }
    }
    $jdkHome = Find-Jdk
    if ($jdkHome) { return $jdkHome }
    if ($NoInstall) {
        Write-Error "No JDK $MinJdkMajor+ found. Install Temurin $JdkVersion from https://adoptium.net, or pass -JavaHome <path> / set JAVA_HOME. Rerun without -NoAutoInstall to download one automatically."
        exit 1
    }
    Write-Host "No JDK found. Installing Temurin JDK $Version automatically ..." -ForegroundColor Yellow
    $winget = Get-Command "winget" -ErrorAction SilentlyContinue
    if ($winget) {
        Write-Host "Trying winget (Temurin $Version JDK) ..." -ForegroundColor Cyan
        $code = Invoke-Native "winget" @("install", "-e", "--id", "EclipseAdoptium.Temurin.$Version.JDK",
            "--silent", "--accept-package-agreements", "--accept-source-agreements")
        $env:Path = [System.Environment]::GetEnvironmentVariable("Path", "Machine") + ";" +
            [System.Environment]::GetEnvironmentVariable("Path", "User")
        $jdkHome = Find-Jdk
        if ($code -eq 0 -and $jdkHome) {
            Write-Host "JDK installed via winget: $jdkHome" -ForegroundColor Green
            return $jdkHome
        }
        Write-Host "winget install did not yield a JDK, falling back to portable ZIP." -ForegroundColor Yellow
    } else {
        Write-Host "winget not available, falling back to portable ZIP." -ForegroundColor DarkYellow
    }
    $jdkHome = Install-JdkPortable -Version $Version
    if ($jdkHome) { return $jdkHome }
    Write-Error "Automatic JDK install failed. Install Temurin $Version+ from https://adoptium.net and rerun, or pass -JavaHome <path>."
    exit 1
}

$CmakeExe = Ensure-CMake -Version $CmakeVersion -NoInstall:$NoAutoInstall

$JavaHome = (Ensure-Jdk -Explicit $JavaHome -Version $JdkVersion -NoInstall:$NoAutoInstall).TrimEnd('\')

Write-Host "Repo:      $RepoRoot"
Write-Host "Config:    $Config"
Write-Host "BuildDir:  $BuildDir"
Write-Host "JDK:       $JavaHome"
Write-Host "CMake:     $CmakeExe"
& $CmakeExe --version | Select-Object -First 1

Invoke-Step "[1/2] Configuring ($Config|x64)" $CmakeExe @(
    "-S", ".",
    "-B", $BuildDir,
    "-A", "x64",
    "-DCOLD_JAVA_HOME=$JavaHome"
)

foreach ($name in @("stone.dll", "stone-injector.exe")) {
    $path = Join-Path $RepoRoot "dist\$name"
    if (-not (Test-Path -LiteralPath $path)) { continue }
    try {
        [System.IO.File]::Open($path, "Open", "ReadWrite", "None").Dispose()
    } catch {
        $old = "$path.old"
        Remove-Item -LiteralPath $old -Force -ErrorAction SilentlyContinue
        if (Test-Path -LiteralPath $old) { $old = "$path.$(Get-Date -Format yyyyMMddHHmmss).old" }
        Move-Item -LiteralPath $path -Destination $old
        Write-Host "$name is in use; moved it to $(Split-Path -Leaf $old)" -ForegroundColor DarkYellow
    }
}

Invoke-Step "[2/2] Building ($Config)" $CmakeExe @(
    "--build", $BuildDir,
    "--config", $Config
)

if (Test-Path "$env:APPDATA\stone\scripts") {
    Copy-Item -Path "$RepoRoot\scripts\*" -Destination "$env:APPDATA\stone\scripts\" -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host ""
Write-Host "Build OK ($Config|x64)." -ForegroundColor Green
$Dll = Join-Path $RepoRoot "dist\stone.dll"
$Loader = Join-Path $RepoRoot "dist\stone-injector.exe"
foreach ($artifact in @($Dll, $Loader)) {
    if (Test-Path -LiteralPath $artifact) {
        $item = Get-Item -LiteralPath $artifact
        Write-Host ("  {0}  ({1:N1} KiB)" -f $item.Name, ($item.Length / 1KB))
    } else {
        Write-Host ("  MISSING: {0}" -f $artifact) -ForegroundColor Yellow
    }
}
Write-Host ""
Write-Host "REMINDER: dev build, no protection. Local use only." -ForegroundColor Yellow
