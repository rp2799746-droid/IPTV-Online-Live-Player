<#
    Cleans an M3U playlist before it is uploaded for the app.

    1. Drops channels the player can never play (YouTube/Twitch/Dailymotion pages,
       rtsp/rtmp/udp links, broken lines, expired tokens) and unauthorized restreams.
    2. Opens every remaining link and keeps it only if a real stream comes back.

    Output:  clean.m3u    -> working channels only (upload this file)
             removed.csv  -> every removed channel with the reason

    Usage:
      powershell -ExecutionPolicy Bypass -File tools\clean-playlist.ps1 -InputFile playlist.m3u

    Options:
      -OutputFile clean.m3u   -ReportFile removed.csv
      -Threads 30             -TimeoutSec 10
      -KeepGeoBlocked         keep channels marked (G)  (removed by default)
      -KeepNot247             keep channels marked (S)  (removed by default)
      -SkipNetworkCheck       apply only the rules, do not open the links
#>
param(
    [Parameter(Mandatory = $true)][string]$InputFile,
    [string]$OutputFile = "clean.m3u",
    [string]$ReportFile = "removed.csv",
    [int]$Threads = 30,
    [int]$TimeoutSec = 10,
    [switch]$KeepGeoBlocked,
    [switch]$KeepNot247,
    [switch]$SkipNetworkCheck
)

$ErrorActionPreference = "Stop"

# Circled letters used by iptv-org style lists (kept as char codes so this file stays ASCII).
$MarkYouTube     = [char]0x24CE   # Y
$MarkTwitch      = [char]0x24C9   # T
$MarkDailymotion = [char]0x24B9   # D
$MarkVk          = [char]0x24CB   # V
$MarkOk          = [char]0x24C4   # O
$MarkGeo         = [char]0x24BC   # G
$MarkNot247      = [char]0x24C8   # S

# Pages / platforms ExoPlayer cannot play directly.
$PageHosts = @(
    "youtube.com", "youtu.be", "twitch.tv", "ttvnw.net", "dailymotion.com",
    "vk.com", "ok.ru", "lcp.fr", "publicsenat.fr", "player.smotrim.ru", "telefe.com",
    "ythls.armelin.one", "tiscali.it"
)

# Relays / resellers that rebroadcast paid channels without rights (Play policy risk).
$RestreamHosts = @(
    "tvkaista.net", "sktv-forwarders.7m.pl", "sktv.plainrock127.xyz", "strims.top",
    "stream01.willfonk.com", "ye23.vip", "seb.sason.top", "sky4k.top", "hotiptv.site",
    "ott-cdn.ucom.am", "televizor-24-tochka.ru", "interskytech.com", "streamhost.cc",
    "m3u8.ott.blue", "mytv.dothome.co.kr", "allanf181.github.io", "raw.githubusercontent.com",
    "onlinestream.live", "cdnua05.hls.tv"
)
$RestreamIpPrefixes = @(
    "152.89.62.", "51.91.73.", "176.65.146.", "185.189.225.", "181.78.109.",
    "37.122.156.", "212.79.122.", "212.0.211.", "190.2.155.", "149.5.17.", "78.109.104."
)

function Test-HostMatch([string]$hostName, [string[]]$list) {
    foreach ($item in $list) {
        if ($hostName -eq $item -or $hostName.EndsWith("." + $item)) { return $item }
    }
    return $null
}

function Get-RuleReason([string]$name, [string]$url) {
    if ($url -match '[\s\[\]]') { return "Broken URL" }
    if ($url -notmatch '^https?://') { return "Unsupported protocol" }

    $uri = $null
    if (-not [Uri]::TryCreate($url, [UriKind]::Absolute, [ref]$uri)) { return "Invalid URL" }
    $hostName = $uri.Host.ToLowerInvariant()

    $hit = Test-HostMatch $hostName $PageHosts
    if ($hit) { return "Web page, not a stream ($hit)" }
    $hit = Test-HostMatch $hostName $RestreamHosts
    if ($hit) { return "Unauthorized restream ($hit)" }
    foreach ($prefix in $RestreamIpPrefixes) {
        if ($hostName.StartsWith($prefix)) { return "Unauthorized restream ($hostName)" }
    }
    # Xtream-codes style: host:port/user/password/id
    if ($url -match '://[^/]+:\d+/[A-Za-z0-9]+/[A-Za-z0-9]+/\d+$') { return "Unauthorized restream (user/password URL)" }
    if ($uri.AbsolutePath -match '/udp/') { return "Unsupported protocol (udp relay)" }
    if ($uri.AbsolutePath -match '\.wmv$') { return "Unsupported format (wmv)" }

    if ($url -match '(?:exp|expires)=(\d{10})') {
        if ([long]$Matches[1] -lt [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()) { return "Expired token" }
    }

    if ($name.Contains($MarkYouTube))     { return "YouTube channel" }
    if ($name.Contains($MarkTwitch))      { return "Twitch channel" }
    if ($name.Contains($MarkDailymotion)) { return "Dailymotion channel" }
    if ($name.Contains($MarkVk) -or $name.Contains($MarkOk)) { return "Social video page" }
    if (-not $KeepGeoBlocked -and $name.Contains($MarkGeo)) { return "Geo-blocked" }
    if (-not $KeepNot247 -and $name.Contains($MarkNot247))  { return "Not 24/7" }
    return $null
}

# ---------- Parse ----------
$inputPath = (Resolve-Path $InputFile).Path
$lines = [IO.File]::ReadAllLines($inputPath, [Text.Encoding]::UTF8)

$kept = New-Object System.Collections.Generic.List[object]
$removed = New-Object System.Collections.Generic.List[object]
$seenUrls = New-Object 'System.Collections.Generic.HashSet[string]'
$info = $null
$extra = New-Object System.Collections.Generic.List[string]

foreach ($raw in $lines) {
    $line = $raw.Trim()
    if ($line -eq "" -or $line.StartsWith("#EXTM3U")) { continue }

    if ($line.StartsWith("#EXTINF")) {
        if ($info) { $removed.Add([pscustomobject]@{ Name = $info; Url = ""; Reason = "No URL after #EXTINF" }) }
        $info = $line
        $extra = New-Object System.Collections.Generic.List[string]
        continue
    }
    if ($line.StartsWith("#")) {
        if ($info) { $extra.Add($line) }
        continue
    }

    if (-not $info) {
        $removed.Add([pscustomobject]@{ Name = "(missing #EXTINF)"; Url = $line; Reason = "URL without #EXTINF" })
        continue
    }

    $name = $info
    if ($info -match '^#EXTINF:-?\d+(?:\s+[\w-]+="[^"]*")*\s*,(.*)$') { $name = $Matches[1].Trim() }

    $reason = Get-RuleReason $name $line
    if (-not $reason -and -not $seenUrls.Add($line)) { $reason = "Duplicate URL" }

    $entry = [pscustomobject]@{ Name = $name; Info = $info; Extra = $extra; Url = $line }
    if ($reason) {
        $removed.Add([pscustomobject]@{ Name = $name; Url = $line; Reason = $reason })
    } else {
        $kept.Add($entry)
    }
    $info = $null
}
if ($info) { $removed.Add([pscustomobject]@{ Name = $info; Url = ""; Reason = "No URL after #EXTINF" }) }

$ruleRemoved = $removed.Count
Write-Host ("Rules: {0} removed, {1} left to check" -f $ruleRemoved, $kept.Count)

# ---------- Network check ----------
$check = {
    param([string]$url, [int]$timeoutMs)

    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12 -bor [Net.SecurityProtocolType]::Tls11 -bor [Net.SecurityProtocolType]::Tls
    [Net.ServicePointManager]::DefaultConnectionLimit = 100

    function Fetch([string]$u) {
        $req = [Net.HttpWebRequest]::Create($u)
        $req.Method = "GET"
        $req.Timeout = $timeoutMs
        $req.ReadWriteTimeout = $timeoutMs
        $req.AllowAutoRedirect = $true
        $req.MaximumAutomaticRedirections = 5
        $req.UserAgent = "ExoPlayerLib/2.18.1"
        $req.AutomaticDecompression = [Net.DecompressionMethods]::GZip -bor [Net.DecompressionMethods]::Deflate
        $resp = $req.GetResponse()
        try {
            $stream = $resp.GetResponseStream()
            $buf = New-Object byte[] 65536
            $total = 0
            while ($total -lt $buf.Length) {
                $n = $stream.Read($buf, $total, $buf.Length - $total)
                if ($n -le 0) { break }
                $total += $n
            }
            return @{
                Text  = [Text.Encoding]::UTF8.GetString($buf, 0, $total)
                Type  = [string]$resp.ContentType
                Final = $resp.ResponseUri
                Bytes = $total
            }
        } finally {
            $resp.Close()
        }
    }

    try {
        $original = New-Object Uri($url)
        $r = Fetch $url
        $text = $r.Text.TrimStart([char]0xFEFF, ' ', "`r", "`n", "`t")

        # ExoPlayer's default data source refuses http <-> https redirects.
        if ($r.Final.Scheme -ne $original.Scheme) { return "Redirect between http and https" }
        # The app only has the HLS module and picks the type from the URL path.
        if ($text -match '<MPD') { return "DASH (.mpd) not supported by app" }
        if ($text.StartsWith("#EXTM3U") -and $original.AbsolutePath -notmatch '\.m3u8$') {
            return "HLS link does not end with .m3u8"
        }

        if ($text.StartsWith("#EXTM3U")) {
            # Follow master playlists down to a media playlist (max 3 levels).
            $level = 0
            while ($text -match '#EXT-X-STREAM-INF[^\n]*\n\s*([^\s#][^\r\n]*)' -and $level -lt 3) {
                $variant = New-Object Uri($r.Final, $Matches[1].Trim())
                $r = Fetch $variant.AbsoluteUri
                $text = $r.Text.TrimStart([char]0xFEFF, ' ', "`r", "`n", "`t")
                if (-not $text.StartsWith("#EXTM3U")) { return "Quality playlist is not valid" }
                $level++
            }
            # The player needs real video data: download the first segment.
            $segment = $null
            foreach ($seg in ($text -split "`n")) {
                $s = $seg.Trim()
                if ($s -and -not $s.StartsWith("#")) { $segment = $s; break }
            }
            if (-not $segment) { return "Playlist has no video segments" }
            $segUri = New-Object Uri($r.Final, $segment)
            $sr = Fetch $segUri.AbsoluteUri
            if ($sr.Bytes -lt 1000) { return "Video segment is empty" }
            if ($sr.Type -match 'html') { return "Video segment is a web page" }
            return "OK"
        }
        if ($r.Type -match 'html') { return "Web page, not a stream" }
        if ($r.Type -match 'video|audio|mpegurl|mp2t|octet-stream' -and $r.Bytes -gt 1000) { return "OK" }
        return "Not a stream ($($r.Type))"
    } catch {
        $ex = $_.Exception
        while ($ex.InnerException -and -not ($ex -is [Net.WebException])) { $ex = $ex.InnerException }
        if ($ex -is [Net.WebException]) {
            if ($ex.Response) { return "HTTP " + [int]$ex.Response.StatusCode }
            return "No response (" + $ex.Status + ")"
        }
        return "Error: " + $ex.Message
    }
}

$working = New-Object System.Collections.Generic.List[object]
if ($SkipNetworkCheck) {
    $working = $kept
} elseif ($kept.Count -gt 0) {
    [Net.ServicePointManager]::DefaultConnectionLimit = 100
    $pool = [RunspaceFactory]::CreateRunspacePool(1, $Threads)
    $pool.Open()

    $jobs = foreach ($entry in $kept) {
        $ps = [PowerShell]::Create()
        $ps.RunspacePool = $pool
        [void]$ps.AddScript($check).AddArgument($entry.Url).AddArgument($TimeoutSec * 1000)
        [pscustomobject]@{ Entry = $entry; PS = $ps; Handle = $ps.BeginInvoke() }
    }

    $done = 0
    foreach ($job in $jobs) {
        $output = $job.PS.EndInvoke($job.Handle)
        $job.PS.Dispose()
        $done++
        Write-Progress -Activity "Checking channels" -Status ("{0} / {1}" -f $done, $kept.Count) -PercentComplete ($done * 100 / $kept.Count)

        $result = [string]($output | Select-Object -Last 1)
        if ($result -eq "OK") {
            $working.Add($job.Entry)
        } else {
            $removed.Add([pscustomobject]@{ Name = $job.Entry.Name; Url = $job.Entry.Url; Reason = $result })
        }
    }
    Write-Progress -Activity "Checking channels" -Completed
    $pool.Close()
    $pool.Dispose()
}

# ---------- Write ----------
$out = New-Object System.Collections.Generic.List[string]
$out.Add("#EXTM3U")
foreach ($entry in $working) {
    $out.Add($entry.Info)
    foreach ($e in $entry.Extra) { $out.Add($e) }
    $out.Add($entry.Url)
}
$outputPath = [IO.Path]::GetFullPath((Join-Path (Get-Location) $OutputFile))
[IO.File]::WriteAllLines($outputPath, $out, (New-Object Text.UTF8Encoding($false)))

$reportPath = [IO.Path]::GetFullPath((Join-Path (Get-Location) $ReportFile))
$removed | Export-Csv -Path $reportPath -NoTypeInformation -Encoding UTF8

Write-Host ""
Write-Host ("Working channels : {0}" -f $working.Count)
Write-Host ("Removed by rules : {0}" -f $ruleRemoved)
Write-Host ("Removed (offline): {0}" -f ($removed.Count - $ruleRemoved))
Write-Host ("Clean playlist   : {0}" -f $outputPath)
Write-Host ("Removed report   : {0}" -f $reportPath)
