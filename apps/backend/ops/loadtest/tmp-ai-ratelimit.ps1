Add-Type -AssemblyName System.Net.Http
function B64Url([byte[]]$bytes) { [Convert]::ToBase64String($bytes).TrimEnd('=').Replace('+','-').Replace('/','_') }
$secretValue = $env:JWT_SECRET_BASE64
if ([string]::IsNullOrWhiteSpace($secretValue)) { throw "JWT_SECRET_BASE64 required" }
$secret = [Convert]::FromBase64String($secretValue)
$headerJson = '{"alg":"HS256","typ":"JWT"}'
$now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$payloadObj = [ordered]@{iss='applyflow';aud=@('applyflow-web');sub='11111111-1111-1111-1111-111111111111';email='operational@test.local';role='USER';iat=$now;exp=($now+900);jti=[guid]::NewGuid().ToString()}
$payloadJson = $payloadObj | ConvertTo-Json -Compress
$unsigned = "$(B64Url([Text.Encoding]::UTF8.GetBytes($headerJson))).$(B64Url([Text.Encoding]::UTF8.GetBytes($payloadJson)))"
$token = "$unsigned.$(B64Url(([System.Security.Cryptography.HMACSHA256]::new($secret)).ComputeHash([Text.Encoding]::UTF8.GetBytes($unsigned))))"
$vacancyId = '81d7655e-a1bb-4d8c-a537-bf6b9b2934f8'
$urls = @("http://localhost:8081/api/v1/ai/matches/$vacancyId/enrichment","http://localhost:8082/api/v1/ai/matches/$vacancyId/enrichment")
$client = New-Object System.Net.Http.HttpClient
$client.Timeout = [TimeSpan]::FromSeconds(30)
$calls = @()
for($i=1; $i -le 25; $i++){
  $url = $urls[$i % 2]
  $req = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::Post, $url)
  $req.Headers.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue('Bearer',$token)
  $sw=[System.Diagnostics.Stopwatch]::StartNew()
  $resp = $client.SendAsync($req).GetAwaiter().GetResult()
  $body = $resp.Content.ReadAsStringAsync().GetAwaiter().GetResult()
  $sw.Stop()
  $calls += [pscustomobject]@{idx=$i;instance=$url.Substring(0,21);status=[int]$resp.StatusCode;latencyMs=[int]$sw.ElapsedMilliseconds;bodySnippet=$(if($body.Length -gt 120){$body.Substring(0,120)}else{$body})}
}
$summary = $calls | Group-Object status | Sort-Object Name | ForEach-Object { [pscustomobject]@{status=[int]$_.Name;count=$_.Count} }
[pscustomobject]@{summary=$summary;calls=$calls} | ConvertTo-Json -Depth 6
