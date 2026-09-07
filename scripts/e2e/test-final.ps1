$base = "http://localhost:8080"
function Call($method, $path, $file, $token) {
  $args = @("-s", "-w", "`n%{http_code}", "-X", $method, "$base$path")
  if ($file) { $args += @("-H", "Content-Type: application/json", "-d", "@$file") }
  if ($token) { $args += @("-H", "Authorization: Bearer $token") }
  $out = & curl.exe @args 2>$null
  $code = $out[-1]
  $resp = ($out[0..($out.Length - 2)] -join "`n")
  Write-Output "### $method $path -> $code"
  Write-Output $resp
  return @{ code = $code; body = $resp }
}
function Check($name, $cond) { if ($cond) { Write-Output "[PASS] $name" } else { Write-Output "[FAIL] $name" } }

$login = Call "POST" "/api/auth/login" "reg2.json" $null
$admin = ([regex]::Match($login.body, '"accessToken"\s*:\s*"([^"]+)"')).Groups[1].Value
$rt = ([regex]::Match($login.body, '"refreshToken"\s*:\s*"([^"]+)"')).Groups[1].Value
$rid = ([regex]::Match($login.body, '"refreshTokenId"\s*:\s*"([^"]+)"')).Groups[1].Value
Check "admin has ADMIN role in JWT" ($login.body -match "ROLE_ADMIN")

Set-Content -LiteralPath resA.json -Value '{"tableId":2,"startTime":"2026-09-10T12:00:00Z","endTime":"2026-09-10T14:00:00Z","numberOfGuests":2}'
Set-Content -LiteralPath resB.json -Value '{"tableId":3,"startTime":"2026-09-11T12:00:00Z","endTime":"2026-09-11T14:00:00Z","numberOfGuests":3}'
$a = Call "POST" "/api/reservations" "resA.json" $admin
$b = Call "POST" "/api/reservations" "resB.json" $admin
$idA = ([regex]::Match($a.body, '"id"\s*:\s*(\d+)')).Groups[1].Value
$idB = ([regex]::Match($b.body, '"id"\s*:\s*(\d+)')).Groups[1].Value
$r = Call "PUT" "/api/admin/reservations/$idA/approve" $null $admin
Check "approve PENDING -> 200 APPROVED" ($r.code -eq "200" -and $r.body -match "approved")
$g = Call "GET" "/api/reservations/$idA" $null $admin
Check "reservation status APPROVED" ($g.body -match "APPROVED")
$r = Call "PUT" "/api/admin/reservations/$idB/reject" $null $admin
Check "reject PENDING -> 200" ($r.code -eq "200" -and $r.body -match "rejected")
$r = Call "DELETE" "/api/admin/reservations/$idB" $null $admin
Check "admin cancel rejected -> 200" ($r.code -eq "200")
Set-Content -LiteralPath logout.json -Value "{`"refreshToken`":`"$rt`",`"refreshTokenId`":`"$rid`"}"
$r = Call "POST" "/api/auth/logout" "logout.json" $admin
Check "logout full body -> 200" ($r.code -eq "200")
$r = Call "POST" "/api/auth/refresh" "logout.json" $null
Check "refresh with revoked token -> 400" ($r.code -eq "400")
Write-Output DONE
