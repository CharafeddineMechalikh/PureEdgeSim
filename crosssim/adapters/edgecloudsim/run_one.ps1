# Runs the EdgeCloudSim common-scenario adapter for one architecture and seed.
# Usage: powershell -File run_one.ps1 -Arch EDGE_ONLY -Seed 1
param([string]$Arch = "EDGE_ONLY", [int]$Seed = 1)

$ErrorActionPreference = "Continue"
$root = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot))   # repository root
Set-Location $root
$ecs = "crosssim\vendor\EdgeCloudSim"
$adapter = "crosssim\adapters\edgecloudsim"
$cp = "$ecs\build;" + ((Get-ChildItem "$ecs\lib" -Filter *.jar | ForEach-Object { "$ecs\lib\$($_.Name)" }) -join ";") + ";$adapter\classes"
$out = "$root\crosssim\results\common_edgecloudsim\$Arch-seed$Seed"
New-Item -ItemType Directory -Force -Path $out | Out-Null
$trace = "$root\crosssim\traces\trace_s$Seed.csv"
$decisions = "$out\decisions.csv"

java -cp $cp `
  "-Dcbs.trace=$trace" "-Dcbs.arch=$Arch" "-Dcbs.decisions=$decisions" `
  "-Dcbs.seed=$Seed" `
  crosssim.CommonMainApp `
  "$adapter\config\default_config.properties" `
  "$adapter\config\edge_devices.xml" `
  "$adapter\config\applications.xml" `
  $out $Seed 2>&1 | Out-File -Encoding ASCII "$out\log.txt"
Write-Output "done $Arch seed=$Seed exit=$LASTEXITCODE out=$out"
