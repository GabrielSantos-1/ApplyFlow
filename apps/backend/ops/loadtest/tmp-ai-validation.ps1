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
$hmac = [System.Security.Cryptography.HMACSHA256]::new($secret)
$token = "$unsigned.$(B64Url($hmac.ComputeHash([Text.Encoding]::UTF8.GetBytes($unsigned))))"

$vacancyId = '81d7655e-a1bb-4d8c-a537-bf6b9b2934f8'
$targets = @(
 @{instance='backend-1';url="http://localhost:8081/api/v1/ai/matches/$vacancyId/enrichment";flow='match-enrichment'},
 @{instance='backend-1';url="http://localhost:8081/api/v1/ai/matches/$vacancyId/cv-improvement";flow='cv-improvement'},
 @{instance='backend-1';url="http://localhost:8081/api/v1/ai/matches/$vacancyId/application-draft";flow='application-draft'},
 @{instance='backend-2';url="http://localhost:8082/api/v1/ai/matches/$vacancyId/enrichment";flow='match-enrichment'},
 @{instance='backend-2';url="http://localhost:8082/api/v1/ai/matches/$vacancyId/cv-improvement";flow='cv-improvement'},
 @{instance='backend-2';url="http://localhost:8082/api/v1/ai/matches/$vacancyId/application-draft";flow='application-draft'}
)
$client = New-Object System.Net.Http.HttpClient
$client.Timeout = [TimeSpan]::FromSeconds(30)
$results = @()
foreach($t in $targets){
  $req = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::Post, $t.url)
  $req.Headers.Authorization = New-Object System.Net.Http.Headers.AuthenticationHeaderValue('Bearer',$token)
  $sw=[System.Diagnostics.Stopwatch]::StartNew()
  try {
    $resp = $client.SendAsync($req).GetAwaiter().GetResult()
    $body = $resp.Content.ReadAsStringAsync().GetAwaiter().GetResult()
    $sw.Stop()
    $obj = $null
    try { $obj = $body | ConvertFrom-Json } catch {}
    $results += [pscustomobject]@{
      instance=$t.instance;flow=$t.flow;status=[int]$resp.StatusCode;latencyMs=[int]$sw.ElapsedMilliseconds;
      outputChars=$body.Length;fallbackUsed=($(if($obj -and $obj.PSObject.Properties.Name -contains 'fallbackUsed'){[bool]$obj.fallbackUsed}else{$null}));
      deterministicScore=($(if($obj -and $obj.PSObject.Properties.Name -contains 'deterministicScore'){[int]$obj.deterministicScore}else{$null}));
      deterministicRecommendation=($(if($obj -and $obj.PSObject.Properties.Name -contains 'deterministicRecommendation'){[string]$obj.deterministicRecommendation}else{$null}));
      bodySnippet=($(if($body.Length -gt 180){$body.Substring(0,180)}else{$body}))
    }
  } catch {
    $sw.Stop()
    $results += [pscustomobject]@{instance=$t.instance;flow=$t.flow;status=0;latencyMs=[int]$sw.ElapsedMilliseconds;outputChars=0;fallbackUsed=$null;deterministicScore=$null;deterministicRecommendation=$null;bodySnippet=$_.Exception.Message}
  }
}
$results | ConvertTo-Json -Depth 6
