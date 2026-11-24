# OmniDial: fetch the latest code from GitHub and build the debug APK.
# Run from PowerShell:
#   powershell -ExecutionPolicy Bypass -File "D:\Try\Git-Repos\Kishan-Dialer\scripts\omnidial-fetch-and-build-debug.ps1"
$ErrorActionPreference = "Stop"
Set-Location "D:\Try\Git-Repos\Kishan-Dialer"

# If there are uncommitted local changes, show them and ask before committing + pushing.
$dirty = git status --porcelain
if ($dirty) {
  Write-Host "You have uncommitted changes in this repo:"
  git status --short
  $answer = Read-Host "Commit and push these changes now? [Y/N]"
  if ($answer -ne "Y" -and $answer -ne "y") {
    Write-Host "OK - commit or stash them first, then re-run this script."
    exit 1
  }
  $msg = Read-Host "Commit message (Enter for default)"
  if ([string]::IsNullOrWhiteSpace($msg)) { $msg = "chore: commit local changes before build" }
  git add -A
  git commit -m $msg
  git pull --rebase origin main
  if ($LASTEXITCODE -ne 0) {
    Write-Host "Pull/rebase failed - fix it, then re-run this script."
    exit 1
  }
  git push origin main
  if ($LASTEXITCODE -ne 0) {
    Write-Host "Push failed - fix it, then re-run this script."
    exit 1
  }
}

git fetch origin
git checkout main
git pull --ff-only origin main
git log --oneline -1

# Read the version from build.gradle.kts so the APK filename always matches the build.
$buildGradle = Get-Content app\build.gradle.kts -Raw
if ($buildGradle -match 'versionName = "([^"]+)"') {
  $version = $Matches[1]
} else {
  Write-Host "Could not read versionName from app\build.gradle.kts"
  exit 1
}

.\gradlew.bat :app:assembleDebug
Copy-Item -Force app\build\outputs\apk\debug\app-debug.apk "apks\OmniDial-v$version-debug.apk"
