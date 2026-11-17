param([string]$LogName = "smoke")
$ECS = "D:\Charef\doctorat\Work folder\PureEdgeSim\crosssim\vendor\EdgeCloudSim"
$ECH = "D:\Charef\doctorat\Work folder\PureEdgeSim\crosssim\ecs"
Set-Location $ECS
$cp = "build;lib/cloudsim-7.0.0-alpha.jar;lib/colt.jar;lib/commons-math3-3.6.1.jar;lib/jFuzzyLogic_v3.0.jar;lib/mtj-1.0.4.jar;lib/weka.jar"
$outDir = "$ECH\out_$LogName\"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
& "C:\Program Files\Java\jdk-23\bin\java.exe" -Xmx3g -cp $cp `
  crosssim.CrossSimApp `
  "$ECH\config\cross_config.properties" `
  "$ECH\config\edge_devices.xml" `
  "$ECH\config\applications.xml" `
  $outDir 1 2>&1 | Out-File -Encoding ASCII "$ECH\log_$LogName.txt"
Write-Output "EXIT=$LASTEXITCODE"
