# Starts the packaged app DETACHED (survives the launching shell) + persists env vars.
$jar = Join-Path $PSScriptRoot "..\..\target\restaurant-reservation-system-0.0.1-SNAPSHOT.jar"
$jar = [System.IO.Path]::GetFullPath($jar)
$work = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot "..\.."))

[System.Environment]::SetEnvironmentVariable("DB_URL", "jdbc:mysql://localhost:3308/restaurant_reservation?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC", "User")
[System.Environment]::SetEnvironmentVariable("DB_USERNAME", "root", "User")
[System.Environment]::SetEnvironmentVariable("DB_PASSWORD", "root", "User")
[System.Environment]::SetEnvironmentVariable("JWT_SECRET", "live-test-secret-key-that-is-long-enough-for-hmac-sha256-min-64-chars-0123456789", "User")

$java = "C:\Users\mahmo\.jdks\ms-21.0.9\bin\javaw.exe"
$r = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{
  CommandLine = "$java -jar ""$jar"""
  CurrentDirectory = $work
}
Write-Output ("started detached PID=" + $r.ProcessId + " : $jar")
