param([string]$Apk)
$ErrorActionPreference='Stop'
Add-Type -AssemblyName System.IO.Compression
$zip=[IO.Compression.ZipFile]::OpenRead([IO.Path]::GetFullPath($Apk))
try {
foreach($entry in $zip.Entries | Where-Object {$_.FullName -match '^lib/.+\.so$'}) {
 $m=[IO.MemoryStream]::new();$s=$entry.Open();try {$s.CopyTo($m)} finally {$s.Dispose()};$b=$m.ToArray();$m.Dispose()
 if($b[0] -ne 127 -or $b[1] -ne 69 -or $b[2] -ne 76 -or $b[3] -ne 70){throw "Bad ELF $($entry.FullName)"}
 $is64=$b[4] -eq 2
 if($is64){$offset=[BitConverter]::ToUInt64($b,32);$size=[BitConverter]::ToUInt16($b,54);$count=[BitConverter]::ToUInt16($b,56)}
 else {$offset=[BitConverter]::ToUInt32($b,28);$size=[BitConverter]::ToUInt16($b,42);$count=[BitConverter]::ToUInt16($b,44)}
 $aligns=@();for($i=0;$i -lt $count;$i++) {$p=[int]($offset+$i*$size);if([BitConverter]::ToUInt32($b,$p) -eq 1){$a=if($is64){[BitConverter]::ToUInt64($b,$p+48)}else{[BitConverter]::ToUInt32($b,$p+28)};$aligns+=$a}}
 if($is64 -and ($aligns | Where-Object {$_ -lt 16384})){throw "64-bit ELF lacks 16KiB alignment: $($entry.FullName)"}
 "PASS ELF $($entry.FullName): LOAD align $($aligns -join ',')"
}
} finally {$zip.Dispose()}