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
$routes = @(
 @{flow='match-enrichment';method='POST';path="/api/v1/ai/matches/$vacancyId/enrichment"},
 @{flow='cv-improvement';method='POST';path="/api/v1/ai/matches/$vacancyId/cv-improvement"},
 @{flow='application-draft';method='POST';path="/api/v1/ai/matches/$vacancyId/application-draft"}
)
$client = New-Object System.Net.Http.HttpClient
$client.Timeout = [TimeSpan]::FromSeconds(30)
$base='http://localhost:8081'
$results=@()
foreach($r in $routes){
  $req = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::Post, "$base$($r.path)")
  $req.Headers.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue('Bearer',$token)
  $sw=[System.Diagnostics.Stopwatch]::StartNew()
  try {
    $resp = $client.SendAsync($req).GetAwaiter().GetResult()
    $body = $resp.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    $sw.Stop()
    $obj=$null; try{$obj = $body|ConvertFrom-Json}catch{}
    $results += [pscustomobject]@{
      flow=$r.flow; method='POST'; path=$r.path; status=[int]$resp.StatusCode; latencyMs=[int]$sw.ElapsedMilliseconds;
      fallbackUsed=($(if($obj -and $obj.PSObject.Properties.Name -contains 'fallbackUsed'){[bool]$obj.fallbackUsed}else{$null}));
      deterministicScore=($(if($obj -and $obj.PSObject.Properties.Name -contains 'deterministicScore'){[int]$obj.deterministicScore}else{$null}));
      recommendation=($(if($obj -and $obj.PSObject.Properties.Name -contains 'deterministicRecommendation'){[string]$obj.deterministicRecommendation}else{$null}));
      bodySnippet=($(if($body.Length -gt 220){$body.Substring(0,220)}else{$body}))
    }
  } catch {
    $sw.Stop()
    $results += [pscustomobject]@{flow=$r.flow;method='POST';path=$r.path;status=0;latencyMs=[int]$sw.ElapsedMilliseconds;fallbackUsed=$null;deterministicScore=$null;recommendation=$null;bodySnippet=$_.Exception.Message}
  }
}
$results | ConvertTo-Json -Depth 6
