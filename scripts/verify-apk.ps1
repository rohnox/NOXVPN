param([Parameter(Mandatory=$true)][string]$Apk, [Parameter(Mandatory=$true)][string]$AndroidSdk)
$ErrorActionPreference = 'Stop'
$tools = Join-Path $AndroidSdk 'build-tools/37.0.0'
$archive = [IO.Compression.ZipFile]::OpenRead([IO.Path]::GetFullPath($Apk))
try {
    if ($archive.GetEntry('assets/xposed_init')) { throw 'Unexpected Xposed initialization asset' }
    foreach ($dex in $archive.Entries | Where-Object FullName -match '^classes\d*\.dex$') {
        $memory = [IO.MemoryStream]::new()
        $inputStream = $dex.Open()
        try { $inputStream.CopyTo($memory) } finally { $inputStream.Dispose() }
        $types = [Text.Encoding]::ASCII.GetString($memory.ToArray())
        $memory.Dispose()
        if ($types.Contains('Lorg/acra/') -or $types.Contains('Lde/robv/android/xposed/')) {
            throw 'Unexpected ACRA or Xposed runtime classes'
        }
    }
    'PASS: ACRA/Xposed runtime and initialization asset absent.'
    foreach ($abi in @('arm64-v8a','armeabi-v7a','x86','x86_64')) {
        foreach ($library in @('libgojni.so','libhev-socks5-tunnel.so','libhevsockstun.so','libopenvpn.so','libovpnexec.so','libovpnutil.so','libosslutil.so','libopenconnect.so','libstoken.so','libcurl-bin.so','libcsd-wrapper.so')) {
            $name = "lib/$abi/$library"
            $item = $archive.GetEntry($name)
            if (!$item -or $item.Length -eq 0) { throw "Missing native input: $name" }
            "PASS: $name ($($item.Length) bytes)"
        }
    }
    if (!$archive.GetEntry('assets/ISPEED-GPL-3.0.txt')) { throw 'Missing embedded GPL license' }
    foreach ($license in @('assets/ispeed-engines/OpenVPN-LICENSE.txt','assets/ispeed-engines/OpenConnect-COPYING.txt')) { if (!$archive.GetEntry($license)) { throw "Missing engine license: $license" } }; 'PASS: host and engine licenses.'
} finally { $archive.Dispose() }
$badging = & (Join-Path $tools 'aapt2.exe') dump badging $Apk
if ($LASTEXITCODE -ne 0) { throw 'APK manifest analysis failed' }
if (!($badging -match "package: name='com.ispeedtopvpn.app'")) { throw 'Unexpected application ID' }
if (!($badging -match "sdkVersion:'24'")) { throw 'Unexpected minimum SDK' }
$labels = $badging | Where-Object { $_ -match '^application-label' }
if ($labels | Where-Object { $_ -notmatch 'iSpeedTopVPN APP' }) { throw 'Brand label differs by locale' }
'PASS: package com.ispeedtopvpn.app, min SDK 24, branded labels in every packaged locale.'
& (Join-Path $tools 'apksigner.bat') verify --verbose --print-certs $Apk
if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed' }
& (Join-Path $tools 'zipalign.exe') -c -P 16 4 $Apk
if ($LASTEXITCODE -ne 0) { throw 'APK ZIP alignment verification failed' }
'PASS: APK signature and ZIP alignment verified.'
Get-FileHash -LiteralPath $Apk -Algorithm SHA256 | Select-Object Hash
