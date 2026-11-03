param(
    [ValidateSet('all', 'forge-1.20.1', 'neoforge-1.21.1', 'neoforge-26.1.2')]
    [string]$Target = 'all',
    [switch]$Offline,
    [switch]$Install
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot

function Find-Jdk([int]$Major) {
    $candidates = @()
    $configuredJdk = [Environment]::GetEnvironmentVariable("JAVA${Major}_HOME")
    if ($configuredJdk) { $candidates += $configuredJdk }
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    $roots = @(
        "$env:ProgramFiles\Java",
        "$env:ProgramFiles\Microsoft",
        "$env:ProgramFiles\Eclipse Adoptium",
        "$env:USERPROFILE\.jdks",
        "$env:USERPROFILE\.gradle\jdks",
        "$env:USERPROFILE\curseforge\minecraft\Install\runtime"
    )
    foreach ($root in $roots) {
        if (Test-Path -LiteralPath $root) {
            $candidates += Get-ChildItem -LiteralPath $root -Filter release -File -Recurse -ErrorAction SilentlyContinue |
                ForEach-Object { $_.DirectoryName }
        }
    }
    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        $release = Join-Path $candidate 'release'
        $compiler = Join-Path $candidate 'bin\javac.exe'
        if ((Test-Path -LiteralPath $release) -and (Test-Path -LiteralPath $compiler)) {
            if ((Get-Content -Raw -LiteralPath $release) -match ('JAVA_VERSION="' + $Major + '(\.|"|-)')) {
                return $candidate
            }
        }
    }
    throw "JDK $Major not found. Install it or configure JAVA${Major}_HOME."
}

$matrix = @(
    @{ Module = 'forge-1.20.1'; Java = 17; Instance = '20.1' },
    @{ Module = 'neoforge-1.21.1'; Java = 21; Instance = '21' },
    @{ Module = 'neoforge-26.1.2'; Java = 25; Instance = '26.1.2' }
)
if ($Target -ne 'all') { $matrix = @($matrix | Where-Object { $_.Module -eq $Target }) }

$originalJavaHome = $env:JAVA_HOME
$artifacts = Join-Path $projectRoot 'artifacts'
$built = @()
try {
    New-Item -ItemType Directory -Path $artifacts -Force | Out-Null
    foreach ($entry in $matrix) {
        $env:JAVA_HOME = Find-Jdk $entry.Java
        $moduleDirectory = Join-Path $projectRoot $entry.Module
        $properties = ConvertFrom-StringData (Get-Content -Raw -LiteralPath (Join-Path $moduleDirectory 'gradle.properties'))
        $jarName = "$($properties.mod_id)-$($entry.Module)-$($properties.mod_version).jar"
        Push-Location $moduleDirectory
        try {
            $buildArguments = @('build', '--no-daemon', '--console=plain')
            if ($Offline) { $buildArguments += '--offline' }
            & '.\gradlew.bat' @buildArguments
            if ($LASTEXITCODE -ne 0) { throw "Build failed: $($entry.Module)" }
        } finally { Pop-Location }
        $jar = Join-Path $moduleDirectory "build\libs\$jarName"
        if (-not (Test-Path -LiteralPath $jar)) { throw "JAR not found: $jar" }
        $artifact = Join-Path $artifacts $jarName
        Copy-Item -LiteralPath $jar -Destination $artifact -Force
        $built += @{ Artifact = $artifact; Name = $jarName; Instance = $entry.Instance; Id = $properties.mod_id }
    }

    $hashes = @($built | ForEach-Object {
        $hash = (Get-FileHash -LiteralPath $_.Artifact -Algorithm SHA256).Hash.ToLowerInvariant()
        "$hash  $($_.Name)"
    })
    $hashFile = if ($Target -eq 'all') { 'SHA256SUMS.txt' } else { "SHA256SUMS-$Target.txt" }
    [System.IO.File]::WriteAllLines((Join-Path $artifacts $hashFile), $hashes, [System.Text.UTF8Encoding]::new($false))

    if ($Install) {
        foreach ($entry in $built) {
            $modsDirectory = Join-Path $env:USERPROFILE "curseforge\minecraft\Instances\$($entry.Instance)\mods"
            if (-not (Test-Path -LiteralPath $modsDirectory)) { throw "Instance not found: $modsDirectory" }
            foreach ($old in (Get-ChildItem -LiteralPath $modsDirectory -File -Filter "$($entry.Id)-*.jar")) {
                if ($old.Name -ne $entry.Name) {
                    $backup = $old.FullName + '.disabled'
                    if (Test-Path -LiteralPath $backup) { $backup += '.' + [Guid]::NewGuid().ToString('N') }
                    Move-Item -LiteralPath $old.FullName -Destination $backup
                }
            }
            Copy-Item -LiteralPath $entry.Artifact -Destination (Join-Path $modsDirectory $entry.Name) -Force
            Write-Output "Installed: $($entry.Name) -> $modsDirectory"
        }
    }
    Write-Output "Build complete. Artifacts: $artifacts"
} finally {
    $env:JAVA_HOME = $originalJavaHome
}
