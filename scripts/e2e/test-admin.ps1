$base = "http://localhost:8080"
$log = "logs\endpoints-admin.log"
"" | Out-File -LiteralPath $log
function Call($method, $path, $body, $token) {
  $args = @("-s", "-w", "`n%{http_code}", "-X", $method, "$base$path")
  if ($body) {
    $tmp = [System.IO.Path]::GetTempFileName()
    [System.IO.File]::WriteAllText($tmp, $body)
    $args += @("-H", "Content-Type: application/json", "-d", "@$tmp")
  }
  if ($token) { $args += @("-H", "Authorization: Bearer $token") }
  $out = & curl.exe @args 2>$null
  if ($tmp -and (Test-Path $tmp)) { Remove-Item $tmp -Force }
  $code = $out[-1]
  $resp = ($out[0..($out.Length - 2)] -join "`n")
  Add-Content -LiteralPath $log -Value "### $method $path -> $code`n$resp`n"
  return @{ code = $code; body = $resp }
}
function Check($name, $cond) {
  $r = if ($cond) { "PASS" } else { "FAIL" }
  Add-Content -LiteralPath $log -Value "[$r] $name"
  Write-Output "[$r] $name"
}

$tmp = [System.IO.Path]::GetTempFileName()
[System.IO.File]::WriteAllText($tmp, '{"email":"e2e@test.com","password":"Pass1234"}')
$login = & curl.exe -s -X POST "$base/api/auth/login" -H "Content-Type: application/json" -d "@$tmp" 2>$null
Remove-Item $tmp -Force
$admin = ([regex]::Match($login, '"accessToken"\s*:\s*"([^"]+)"')).Groups[1].Value
Check "admin login" ($admin.Length -gt 20)

# approve the pending reservation from previous run
$mine = Call "GET" "/api/reservations/my?page=0&size=10" $null $admin
Check "my reservations" ($mine.code -eq "200")
$resId = ([regex]::Match($mine.body, '"id"\s*:\s*(\d+)')).Groups[1].Value
$r = Call "PUT" "/api/admin/reservations/$resId/approve" $null $admin
Check "admin approve" ($r.code -eq "200")
$r = Call "PUT" "/api/admin/reservations/$resId/approve" $null $admin
Check "double approve -> 400" ($r.code -eq "400")

# admin CRUD
$r = Call "POST" "/api/admin/restaurants" '{"name":"E2E Bistro","location":"Test City","openingTime":"10:00","closingTime":"22:00"}' $admin
Check "admin create restaurant" ($r.code -eq "201" -and $r.body -match "E2E Bistro")
$newR = ([regex]::Match($r.body, '"id"\s*:\s*(\d+)')).Groups[1].Value
$r = Call "POST" "/api/admin/restaurants" '{"name":"E2E Bistro","location":"X"}' $admin
Check "duplicate restaurant -> 409" ($r.code -eq "409")
$r = Call "POST" "/api/admin/restaurants" '{"name":"Bad Times","location":"X","openingTime":"25:99"}' $admin
Check "bad time format -> 400" ($r.code -eq "400")
$r = Call "GET" "/api/admin/restaurants?page=0&size=5" $null $admin
Check "admin list restaurants" ($r.code -eq "200")
$r = Call "POST" "/api/admin/tables" "{`"restaurantId`":$newR,`"tableNumber`":1,`"capacity`":4}" $admin
Check "create table" ($r.code -eq "201")
$tbl = ([regex]::Match($r.body, '"id"\s*:\s*(\d+)')).Groups[1].Value
$r = Call "PUT" "/api/admin/tables/$tbl/status" '{"tableStatus":"OCCUPIED"}' $admin
Check "table status OCCUPIED" ($r.code -eq "200" -and $r.body -match "OCCUPIED")
$r = Call "PUT" "/api/admin/tables/$tbl/status" '{"tableStatus":"NOPE"}' $admin
Check "bad table status -> 400" ($r.code -eq "400")
$r = Call "GET" "/api/admin/restaurants/$newR/tables?page=0&size=5" $null $admin
Check "tables by restaurant" ($r.code -eq "200")
$r = Call "POST" "/api/admin/categories" '{"name":"E2E Cat","description":"t"}' $admin
Check "create category" ($r.code -eq "201")
$cat = ([regex]::Match($r.body, '"id"\s*:\s*(\d+)')).Groups[1].Value
$r = Call "PUT" "/api/admin/categories/$cat" '{"name":"E2E Cat","description":"t2"}' $admin
Check "update category same name ok" ($r.code -eq "200")
$r = Call "POST" "/api/admin/menu-items" "{`"categoryId`":$cat,`"name`":`"E2E Dish`",`"price`":99.50}" $admin
Check "create menu item" ($r.code -eq "201" -and $r.body -match "E2E Dish")
$mi = ([regex]::Match($r.body, '"id"\s*:\s*(\d+)')).Groups[1].Value
$r = Call "PUT" "/api/admin/menu-items/$mi" "{`"categoryId`":$cat,`"name`":`"E2E Dish v2`",`"price`":120.00}" $admin
Check "update menu item" ($r.code -eq "200" -and $r.body -match "v2")
$r = Call "DELETE" "/api/admin/menu-items/$mi" $null $admin
Check "soft-delete menu item" ($r.code -eq "200")
$r = Call "DELETE" "/api/admin/categories/$cat" $null $admin
Check "soft-delete category" ($r.code -eq "200")
$r = Call "DELETE" "/api/admin/tables/$tbl" $null $admin
Check "soft-delete table" ($r.code -eq "200")
$r = Call "DELETE" "/api/admin/restaurants/$newR" $null $admin
Check "soft-delete restaurant" ($r.code -eq "200")
$r = Call "GET" "/api/restaurants/$newR"
Check "deleted restaurant hidden -> 404" ($r.code -eq "404")

# assign owner role to self (id lookup via profile)
$me = Call "GET" "/api/users/me" $null $admin
$uid = ([regex]::Match($me.body, '"id"\s*:\s*(\d+)')).Groups[1].Value
$r = Call "PUT" "/api/admin/users/$uid/role" $null $admin
Check "assign owner role" ($r.code -eq "200")

# logout with fresh refresh token
$tmp2 = [System.IO.Path]::GetTempFileName()
[System.IO.File]::WriteAllText($tmp2, '{"email":"e2e@test.com","password":"Pass1234"}')
$login2 = & curl.exe -s -X POST "$base/api/auth/login" -H "Content-Type: application/json" -d "@$tmp2" 2>$null
Remove-Item $tmp2 -Force
$rid = ([regex]::Match($login2, '"refreshTokenId"\s*:\s*"([^"]+)"')).Groups[1].Value
$atk = ([regex]::Match($login2, '"accessToken"\s*:\s*"([^"]+)"')).Groups[1].Value
$r = Call "POST" "/api/auth/logout" "{`"refreshTokenId`":`"$rid`"}" $atk
Check "logout (authenticated)" ($r.code -eq "200")
$r = Call "POST" "/api/auth/logout" "{`"refreshTokenId`":`"$rid`"}"
Check "logout anonymous -> 401/403" ($r.code -eq "401" -or $r.code -eq "403")

Write-Output "DONE - log: $log"
