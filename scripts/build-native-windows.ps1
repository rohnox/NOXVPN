param([Parameter(Mandatory=$true)][string]$NdkHome, [string]$BuildDirectory)
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
if (!$BuildDirectory) { $BuildDirectory = Join-Path $root 'native-build' }
$BuildDirectory = [IO.Path]::GetFullPath($BuildDirectory)
$originalSource = Join-Path $root 'hev-socks5-tunnel'
$nativeSource = (Join-Path $BuildDirectory 'source').Replace('\','/')
$output = (Join-Path $BuildDirectory 'libs').Replace('\','/')
$objects = (Join-Path $BuildDirectory 'obj').Replace('\','/')
$abis = @('armeabi-v7a','arm64-v8a','x86','x86_64')
New-Item -ItemType Directory -Force $BuildDirectory | Out-Null
$tempDirectory = Join-Path $BuildDirectory 'tmp'
New-Item -ItemType Directory -Force $tempDirectory | Out-Null
$env:TEMP = $tempDirectory
$env:TMP = $tempDirectory
# Windows Git commonly checks out symlinks as text. Materialize those headers only
# in the build staging tree; the pinned upstream source remains unchanged.
function StageTree($from, $to) {
    New-Item -ItemType Directory -Force $to | Out-Null
    foreach ($entry in Get-ChildItem -LiteralPath $from -Force) {
        if ($entry.Name -eq '.git') { continue }
        $target = Join-Path $to $entry.Name
        if ($entry.PSIsContainer) { StageTree $entry.FullName $target }
        else { Copy-Item -LiteralPath $entry.FullName -Destination $target -Force }
    }
}
StageTree $originalSource $nativeSource
foreach ($relative in @('', 'third-part/hev-task-system', 'third-part/yaml', 'third-part/lwip', 'src/core')) {
    $repository = Join-Path $originalSource $relative
    # Portable source archives already materialize these links and omit Git metadata.
    if (!(Test-Path (Join-Path $repository '.git'))) { continue }
    foreach ($line in (git -C $repository ls-files --stage | Where-Object { $_ -match '^120000 ' })) {
        $path = ($line -split "`t", 2)[1]
        $link = Join-Path $repository $path
        $targetPath = [IO.Path]::GetFullPath((Join-Path (Split-Path $link) ([IO.File]::ReadAllText($link).Trim())))
        if (!$targetPath.StartsWith([IO.Path]::GetFullPath($originalSource) + [IO.Path]::DirectorySeparatorChar)) {
            throw "Native symlink target outside source: $path"
        }
        Copy-Item -LiteralPath $targetPath -Destination (Join-Path (Join-Path $nativeSource $relative) $path) -Force
    }
}
# The pinned HEV Android.mk contains both the JNI library and standalone executable.
# Keep the same native package binding, API, linker flags and four ABIs as upstream.
& (Join-Path $NdkHome 'ndk-build.cmd') `
    'NDK_PROJECT_PATH=null' "APP_BUILD_SCRIPT=$nativeSource/Android.mk" `
    'APP_ABI=armeabi-v7a arm64-v8a x86 x86_64' 'APP_PLATFORM=android-24' `
    "NDK_LIBS_OUT=$output" "NDK_OUT=$objects" `
    'APP_CFLAGS=-O3 -DPKGNAME=com/v2ray/ang/service' `
    'APP_LDFLAGS=-Wl,--build-id=none -Wl,--hash-style=gnu' `
    'ENABLE_MEMALLOC_SLICE=0' '-j4'
if ($LASTEXITCODE -ne 0) { throw 'HEV native build failed' }
foreach ($abi in $abis) {
    $destination = Join-Path $root "V2rayNG/app/libs/$abi"
    New-Item -ItemType Directory -Force $destination | Out-Null
    Copy-Item (Join-Path $output "$abi/libhev-socks5-tunnel.so") $destination -Force
    Copy-Item (Join-Path $output "$abi/hev-socks5-tunnel-bin") (Join-Path $destination 'libhevsockstun.so') -Force
}
