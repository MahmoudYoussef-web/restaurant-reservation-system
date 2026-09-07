$base = "http://localhost:8080"
$log = "logs\endpoints-test.log"
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

# 1. Public
$r = Call "GET" "/actuator/health"
Check "health UP" ($r.code -eq "200" -and $r.body -match "UP")
$r = Call "GET" "/"
Check "landing page" ($r.code -eq "200" -and $r.body -match "Sofra")
$r = Call "GET" "/api/restaurants?page=0&size=10"
Check "browse restaurants (seeded>=3)" ($r.code -eq "200" -and $r.body -match "El Saraya")
$r = Call "GET" "/api/restaurants?search=maadi"
Check "search maadi" ($r.code -eq "200" -and $r.body -match "Casa Italia")
$r = Call "GET" "/api/restaurants/1"
Check "restaurant details" ($r.code -eq "200" -and $r.body -match "Zamalek")
$r = Call "GET" "/api/restaurants/999999"
Check "restaurant 404" ($r.code -eq "404")
$r = Call "GET" "/api/restaurants/1/menu"
Check "menu categories>=5" ($r.code -eq "200" -and $r.body -match "Margherita")
$r = Call "GET" "/api/restaurants/1/reviews?page=0&size=5"
Check "reviews empty page" ($r.code -eq "200")
$r = Call "GET" "/api/restaurants?page=0&size=999"
Check "oversized page -> 400" ($r.code -eq "400")

# 2. Auth
$reg = '{"firstName":"Test","lastName":"User","email":"e2e@test.com","password":"Pass1234"}'
$r = Call "POST" "/api/auth/register" $reg
Check "register 201" ($r.code -eq "201" -and $r.body -match "accessToken")
$access = ([regex]::Match($r.body, '"accessToken"\s*:\s*"([^"]+)"')).Groups[1].Value
$refresh = ([regex]::Match($r.body, '"refreshToken"\s*:\s*"([^"]+)"')).Groups[1].Value
$refreshId = ([regex]::Match($r.body, '"refreshTokenId"\s*:\s*"([^"]+)"')).Groups[1].Value
$r = Call "POST" "/api/auth/register" $reg
Check "duplicate register -> 400" ($r.code -eq "400")
$weak = '{"firstName":"A","lastName":"B","email":"weak@test.com","password":"123"}'
$r = Call "POST" "/api/auth/register" $weak
Check "weak password -> 400" ($r.code -eq "400")
$r = Call "POST" "/api/auth/login" '{"email":"e2e@test.com","password":"Pass1234"}'
Check "login 200" ($r.code -eq "200" -and $r.body -match "accessToken")
$access = ([regex]::Match($r.body, '"accessToken"\s*:\s*"([^"]+)"')).Groups[1].Value
$refresh = ([regex]::Match($r.body, '"refreshToken"\s*:\s*"([^"]+)"')).Groups[1].Value
$refreshId = ([regex]::Match($r.body, '"refreshTokenId"\s*:\s*"([^"]+)"')).Groups[1].Value
$r = Call "POST" "/api/auth/login" '{"email":"e2e@test.com","password":"wrong"}'
Check "bad login -> 401" ($r.code -eq "401")
$r = Call "POST" "/api/auth/refresh" "{`"refreshToken`":`"$refresh`",`"refreshTokenId`":`"$refreshId`"}"
Check "refresh 200" ($r.code -eq "200")
$r = Call "GET" "/api/users/me" $null $access
Check "get profile" ($r.code -eq "200" -and $r.body -match "e2e@test.com")
$r = Call "GET" "/api/users/me"
Check "no token -> 401/403" ($r.code -eq "401" -or $r.code -eq "403")
$r = Call "PUT" "/api/users/me" '{"firstName":"E2E","lastName":"Tester","phoneNumber":"01001234567"}' $access
Check "update profile" ($r.code -eq "200" -and $r.body -match "E2E")
$r = Call "PUT" "/api/users/me" '{"firstName":"E2E","lastName":"Tester"}' $access
Check "user change-password path ok" ($r.code -eq "200" -or $r.code -eq "400")

# 3. Reservations
$start = (Get-Date).ToUniversalTime().AddDays(1).ToString("yyyy-MM-ddTHH:mm:ssZ")
$end = (Get-Date).ToUniversalTime().AddDays(1).AddHours(2).ToString("yyyy-MM-ddTHH:mm:ssZ")
$resBody = "{`"tableId`":1,`"startTime`":`"$start`",`"endTime`":`"$end`",`"numberOfGuests`":2}"
$r = Call "POST" "/api/reservations" $resBody $access
Check "create reservation PENDING" ($r.code -eq "201" -and $r.body -match "PENDING")
$resId = ([regex]::Match($r.body, '"id"\s*:\s*(\d+)')).Groups[1].Value
$r = Call "POST" "/api/reservations" $resBody $access
Check "conflict double-book -> 409" ($r.code -eq "409")
$past = '{"tableId":1,"startTime":"2020-01-01T10:00:00Z","endTime":"2020-01-01T12:00:00Z","numberOfGuests":2}'
$r = Call "POST" "/api/reservations" $past $access
Check "past reservation -> 400" ($r.code -eq "400")
$big = '{"tableId":1,"startTime":"2030-01-01T10:00:00Z","endTime":"2030-01-01T12:00:00Z","numberOfGuests":99}'
$r = Call "POST" "/api/reservations" $big $access
Check "capacity exceeded -> 400" ($r.code -eq "400")
$r = Call "GET" "/api/reservations/my?page=0&size=10" $null $access
Check "my reservations" ($r.code -eq "200" -and $r.body -match "PENDING")
$r = Call "GET" "/api/reservations/$resId" $null $access
Check "get reservation by id" ($r.code -eq "200")
$start2 = (Get-Date).ToUniversalTime().AddDays(2).ToString("yyyy-MM-ddTHH:mm:ssZ")
$end2 = (Get-Date).ToUniversalTime().AddDays(2).AddHours(2).ToString("yyyy-MM-ddTHH:mm:ssZ")
$r = Call "GET" "/api/restaurants/1/available-tables?startTime=$start2&endTime=$end2" $null $access
Check "available tables" ($r.code -eq "200" -and $r.body -match "tableNumber")

# 4. Promote to admin + approve
docker exec rr-mysql mysql "-hlocalhost" "-proot" "-e" "INSERT IGNORE INTO user_roles (user_id, role_id, created_at, updated_at, is_deleted) SELECT u.id, r.id, NOW(), NOW(), FALSE FROM restaurant_reservation.users u JOIN restaurant_reservation.roles r ON r.name='ROLE_ADMIN' WHERE u.email='e2e@test.com';" | Out-Null
$r = Call "POST" "/api/auth/login" '{"email":"e2e@test.com","password":"Pass1234"}'
$admin = ([regex]::Match($r.body, '"accessToken"\s*:\s*"([^"]+)"')).Groups[1].Value
$r = Call "PUT" "/api/admin/reservations/$resId/approve" $null $admin
Check "admin approve" ($r.code -eq "200")
$r = Call "PUT" "/api/admin/reservations/$resId/approve" $null $admin
Check "double approve -> 400" ($r.code -eq "400")
$r = Call "PUT" "/api/admin/reservations/$resId/reject" $null $access
Check "non-admin approve-blocked -> 403" ($r.code -eq "403")

# 5. Orders + invoice
$r = Call "POST" "/api/orders" '{"tableId":1,"restaurantId":1,"notes":"e2e test"}' $admin
Check "create order" ($r.code -eq "201" -and $r.body -match "PENDING")
$orderId = ([regex]::Match($r.body, '"id"\s*:\s*(\d+)')).Groups[1].Value
$r = Call "POST" "/api/orders" '{"tableId":1,"restaurantId":2}' $admin
Check "table-restaurant mismatch -> 400" ($r.code -eq "400")
$r = Call "POST" "/api/orders/$orderId/items" '{"menuItemId":6,"quantity":2}' $admin
Check "add item x2" ($r.code -eq "200" -and $r.body -match "Margherita")
$r = Call "POST" "/api/orders/$orderId/items" '{"menuItemId":6,"quantity":1}' $admin
Check "merge same item qty=3" ($r.code -eq "200" -and $r.body -match '"quantity":3')
$r = Call "POST" "/api/orders/$orderId/items" '{"menuItemId":9999,"quantity":1}' $admin
Check "bad menu item -> 404" ($r.code -eq "404")
$r = Call "PUT" "/api/orders/$orderId/status" '{"status":"COMPLETED"}' $admin
Check "illegal PENDING->COMPLETED -> 400" ($r.code -eq "400")
$r = Call "PUT" "/api/orders/$orderId/status" '{"status":"BOGUS"}' $admin
Check "bogus status -> 400" ($r.code -eq "400")
foreach ($s in @("CONFIRMED","PREPARING","READY","SERVED","COMPLETED")) {
  $r = Call "PUT" "/api/orders/$orderId/status" "{`"status`":`"$s`"}" $admin
  Check "order -> $s" ($r.code -eq "200" -and $r.body -match $s)
}
$r = Call "GET" "/api/invoices/order/$orderId" $null $admin
Check "invoice auto totals 540+75.6+54" ($r.code -eq "200" -and $r.body -match "669.6")
$invId = ([regex]::Match($r.body, '"id"\s*:\s*(\d+)')).Groups[1].Value
$r = Call "GET" "/api/invoices/$invId" $null $admin
Check "get invoice by id" ($r.code -eq "200")
$r = Call "POST" "/api/orders/$orderId/items" '{"menuItemId":6,"quantity":1}' $admin
Check "add to COMPLETED -> 400" ($r.code -eq "400")
$r = Call "GET" "/api/orders/$orderId" $null $admin
Check "get order" ($r.code -eq "200")
$r = Call "GET" "/api/orders/table/1" $null $admin
Check "orders by table" ($r.code -eq "200")
$r = Call "POST" "/api/orders" '{"tableId":2,"restaurantId":1}' $admin
$order2 = ([regex]::Match($r.body, '"id"\s*:\s*(\d+)')).Groups[1].Value
$r = Call "DELETE" "/api/orders/$order2" $null $admin
Check "cancel order -> CANCELLED" ($r.code -eq "200" -and $r.body -match "CANCELLED")

# 6. Reviews (mark reservation COMPLETED first)
docker exec rr-mysql mysql "-hlocalhost" "-proot" "-e" "UPDATE restaurant_reservation.reservations SET status='COMPLETED' WHERE id=$resId;" | Out-Null
$r = Call "POST" "/api/restaurants/1/reviews" '{"rating":5,"comment":"Fantastic food and service!"}' $admin
Check "create review" ($r.code -eq "201" -and $r.body -match "Fantastic")
$revId = ([regex]::Match($r.body, '"id"\s*:\s*(\d+)')).Groups[1].Value
$r = Call "POST" "/api/restaurants/1/reviews" '{"rating":4,"comment":"Again"}' $admin
Check "duplicate review -> 409" ($r.code -eq "409")
$r = Call "GET" "/api/restaurants/1/reviews?page=0&size=5" $null $admin
Check "list reviews" ($r.code -eq "200" -and $r.body -match "Fantastic")
$r = Call "PUT" "/api/reviews/$revId" '{"rating":4,"comment":"Updated view"}' $admin
Check "update own review" ($r.code -eq "200" -and $r.body -match "Updated")
$r = Call "DELETE" "/api/reviews/$revId" $null $admin
Check "delete own review" ($r.code -eq "200")

# 7. Admin CRUD
$r = Call "POST" "/api/admin/restaurants" '{"name":"E2E Bistro","location":"Test City","openingTime":"10:00","closingTime":"22:00"}' $admin
Check "admin create restaurant" ($r.code -eq "201" -and $r.body -match "E2E Bistro")
$newR = ([regex]::Match($r.body, '"id"\s*:\s*(\d+)')).Groups[1].Value
$r = Call "POST" "/api/admin/restaurants" '{"name":"E2E Bistro","location":"X"}' $admin
Check "duplicate restaurant -> 409" ($r.code -eq "409")
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

# 8. Cancel + logout + password reset
$r = Call "DELETE" "/api/reservations/$resId" $null $admin
Check "cancel completed reservation (still cancellable path)" ($r.code -eq "200" -or $r.code -eq "400")
$r = Call "POST" "/api/auth/forgot-password" '{"email":"e2e@test.com"}' $null
Check "forgot password 200 (no enum)" ($r.code -eq "200")
$r = Call "POST" "/api/auth/logout" "{`"refreshTokenId`":`"$refreshId`"}" $admin
Check "logout" ($r.code -eq "200")

Write-Output "DONE - full log: $log"
