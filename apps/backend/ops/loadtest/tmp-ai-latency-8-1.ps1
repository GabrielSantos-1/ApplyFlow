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
$client = New-Object System.Net.Http.HttpClient
$client.Timeout = [TimeSpan]::FromSeconds(30)
$targets = @(
 @{flow='match-enrichment';url="http://localhost:8081/api/v1/ai/matches/$vacancyId/enrichment"},
 @{flow='cv-improvement';url="http://localhost:8081/api/v1/ai/matches/$vacancyId/cv-improvement"},
 @{flow='application-draft';url="http://localhost:8081/api/v1/ai/matches/$vacancyId/application-draft"},
 @{flow='match-enrichment';url="http://localhost:8082/api/v1/ai/matches/$vacancyId/enrichment"},
 @{flow='cv-improvement';url="http://localhost:8082/api/v1/ai/matches/$vacancyId/cv-improvement"},
 @{flow='application-draft';url="http://localhost:8082/api/v1/ai/matches/$vacancyId/application-draft"}
)
$rows=@()
foreach($r in 1..2){
 foreach($t in $targets){
  $req = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::Post, $t.url)
  $req.Headers.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue('Bearer',$token)
  $sw=[System.Diagnostics.Stopwatch]::StartNew()
  $resp=$client.SendAsync($req).GetAwaiter().GetResult(); $body=$resp.Content.ReadAsStringAsync().GetAwaiter().GetResult(); $sw.Stop();
  $obj=$null; try{$obj=$body|ConvertFrom-Json}catch{}
  $rows += [pscustomobject]@{flow=$t.flow;instance=($t.url.Substring(0,21));status=[int]$resp.StatusCode;latencyMs=[int]$sw.ElapsedMilliseconds;fallbackUsed=($(if($obj){$obj.fallbackUsed}else{$null}));outChars=$body.Length}
 }
}
$summary = $rows | Group-Object flow | ForEach-Object {
 $g = $_.Group | Sort-Object latencyMs
 $count = $g.Count
 $avg = [Math]::Round((($g | Measure-Object latencyMs -Average).Average),2)
 $p50 = $g[[Math]::Floor(($count-1)*0.50)].latencyMs
 $p95 = $g[[Math]::Floor(($count-1)*0.95)].latencyMs
 $max = ($g | Measure-Object latencyMs -Maximum).Maximum
 [pscustomobject]@{flow=$_.Name;calls=$count;avgMs=$avg;p50Ms=$p50;p95Ms=$p95;maxMs=$max;status200=(($_.Group|Where-Object status -eq 200).Count);status429=(($_.Group|Where-Object status -eq 429).Count);fallbackTrue=(($_.Group|Where-Object fallbackUsed -eq $true).Count)}
}
[pscustomobject]@{summary=$summary;rows=$rows} | ConvertTo-Json -Depth 6
