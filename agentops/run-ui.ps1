# run-ui.ps1 — Launch IncidentIQ JavaFX UI in a dedicated hardware-accelerated JPMS JVM
param(
    [switch]$Rebuild
)

Push-Location -Path $PSScriptRoot
try {
    $repo = if (Test-Path "D:\m2_repo\org\openjfx") {
        "D:\m2_repo"
    } else {
        "$env:USERPROFILE\.m2\repository"
    }

    $mvnCmd = if (Get-Command mvn -ErrorAction SilentlyContinue) { "mvn" } else { ".\mvnw.cmd" }
    $cpFile = Join-Path $PSScriptRoot "target\runtime-cp.txt"
    $stampFile = Join-Path $PSScriptRoot "target\.ui-compiled"

    $needsCompile = $Rebuild -or (-not (Test-Path $stampFile)) -or (-not (Test-Path (Join-Path $PSScriptRoot "target\classes\com\agentops\UI\Launcher.class")))
    if (-not $needsCompile) {
        $latestSrc = (Get-ChildItem (Join-Path $PSScriptRoot "src") -File -Recurse |
            Sort-Object LastWriteTime -Descending | Select-Object -First 1).LastWriteTime
        if ($latestSrc -gt (Get-Item $stampFile).LastWriteTime) {
            $needsCompile = $true
        }
    }

    if ($needsCompile) {
        Write-Host "Compiling updated sources..." -ForegroundColor Cyan
        & $mvnCmd compile -q "-Dmaven.repo.local=$repo"
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Compilation failed."
            exit $LASTEXITCODE
        }
        Set-Content -Path $stampFile -Value (Get-Date -Format o)
    }

    $needCpRebuild = $Rebuild -or (-not (Test-Path $cpFile)) -or ((Get-Item "pom.xml").LastWriteTime -gt (Get-Item $cpFile).LastWriteTime)
    if ($needCpRebuild) {
        Write-Host "Resolving runtime classpath cache..." -ForegroundColor Cyan
        & $mvnCmd -q dependency:build-classpath "-DincludeScope=runtime" "-Dmdep.outputFile=$cpFile" "-Dmaven.repo.local=$repo"
        if ($LASTEXITCODE -ne 0) {
            Write-Error "Failed to resolve runtime classpath."
            exit $LASTEXITCODE
        }
    }

    $rawCp = (Get-Content $cpFile -Raw).Trim()
    # Keep non-JavaFX dependencies on classpath; JavaFX loads cleanly from --module-path as JPMS modules
    $cpFiltered = (($rawCp -split ';') | Where-Object { $_ -and ($_ -notmatch '\\org\\openjfx\\(javafx-|jdk-jsobject)') }) -join ';'

    $m2fx = Join-Path $repo "org\openjfx"
    $v = "21.0.2"
    $mpParts = @(
        "$m2fx\javafx-controls\$v\javafx-controls-$v-win.jar",
        "$m2fx\javafx-graphics\$v\javafx-graphics-$v-win.jar",
        "$m2fx\javafx-base\$v\javafx-base-$v-win.jar",
        "$m2fx\javafx-fxml\$v\javafx-fxml-$v-win.jar",
        "$m2fx\javafx-media\$v\javafx-media-$v-win.jar",
        "$m2fx\javafx-web\$v\javafx-web-$v-win.jar"
    )

    # JDK 24+ removed built-in jdk.jsobject; provide OpenJFX's standalone jdk.jsobject module when absent
    $hasBuiltinJsObject = (& java --list-modules 2>$null | Select-String -Pattern '^jdk\.jsobject(@|$)' -Quiet)
    if (-not $hasBuiltinJsObject) {
        $jsoJar = "$m2fx\jdk-jsobject\26.0.2\jdk-jsobject-26.0.2-win.jar"
        if (-not (Test-Path $jsoJar)) {
            Write-Host "Fetching OpenJFX jdk.jsobject module for JDK 24+..." -ForegroundColor Cyan
            & $mvnCmd -q dependency:get "-Dartifact=org.openjfx:jdk-jsobject:26.0.2:jar:win" "-Dtransitive=false" "-Dmaven.repo.local=$repo"
            if ($LASTEXITCODE -ne 0) {
                Write-Error "Failed to download jdk.jsobject module."
                exit $LASTEXITCODE
            }
        }
        $mpParts += $jsoJar
    }
    $mp = $mpParts -join ';'

    Write-Host "Launching IncidentIQ (hardware-accelerated JavaFX JPMS JVM)..." -ForegroundColor Green

    & java --module-path $mp `
           --add-modules "javafx.controls,javafx.fxml,javafx.graphics,javafx.web,javafx.media,jdk.jsobject" `
           --enable-native-access=javafx.graphics,javafx.web,javafx.media,ALL-UNNAMED `
           -cp "target\classes;$cpFiltered" `
           com.agentops.UI.Launcher
} finally {
    Pop-Location
}
