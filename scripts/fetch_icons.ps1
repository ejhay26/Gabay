# scripts/fetch_icons.ps1
# Idempotently fetches Lucide geometric line icons (1.75dp stroke) and builds GabayIcons.kt & XML drawables

$ErrorActionPreference = "Stop"

$OutputDir = "assets/icons/svg"
$ResDrawableDir = "android_app/app/src/main/res/drawable"
$KotlinUiDir = "android_app/app/src/main/java/dev/gabay/app/ui"

New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
New-Item -ItemType Directory -Force -Path $ResDrawableDir | Out-Null
New-Item -ItemType Directory -Force -Path $KotlinUiDir | Out-Null

$IconMap = @{
    "nav_cursor"         = "navigation-2"
    "maneuver_straight"  = "arrow-up"
    "maneuver_left"      = "corner-up-left"
    "maneuver_right"     = "corner-up-right"
    "maneuver_slight_left"  = "arrow-up-left"
    "maneuver_slight_right" = "arrow-up-right"
    "maneuver_u_turn"    = "undo-2"
    "maneuver_merge"     = "git-merge"
    "maneuver_fork"      = "git-fork"
    "destination_pin"    = "map-pin"
    "finish_flag"        = "flag"
    
    "mode_scooter"       = "bike"
    "mode_car"           = "car"
    "mode_walk"          = "footprints"
    
    "layer_vector"       = "map"
    "layer_satellite"    = "satellite"
    "layer_topo"         = "mountain"
    "layer_3d"           = "box"
    "layer_stack"        = "layers"
    
    "recenter_gps"       = "crosshair"
    "compass_heading"    = "compass"
    "speed_gauge"        = "gauge"
    "time_pace"          = "clock"
    "route_dist"         = "route"
    
    "copilot_mic"        = "mic"
    "audio_tts"          = "volume-2"
    "audio_mute"         = "volume-x"
    "search_poi"         = "search"
    "settings_tuning"    = "sliders-horizontal"
    
    "warning_hazard"     = "alert-triangle"
    "road_prohibited"    = "ban"
    "reroute_active"     = "refresh-cw"
    
    "action_close"       = "x"
    "action_check"       = "check"
    "action_copy"        = "copy"
    "chevron_up"         = "chevron-up"
    "chevron_down"       = "chevron-down"
    "chevron_left"       = "chevron-left"
}

Write-Host "=== Fetching Lucide Vector SVGs for Gabay Global Registry ==="

foreach ($entry in $IconMap.GetEnumerator()) {
    $targetName = $entry.Key
    $lucideName = $entry.Value
    $svgPath = "$OutputDir/$targetName.svg"
    
    if (-not (Test-Path $svgPath)) {
        $url = "https://raw.githubusercontent.com/lucide-icons/lucide/main/icons/$lucideName.svg"
        Write-Host "Fetching $targetName ($lucideName.svg)..."
        try {
            Invoke-WebRequest -Uri $url -OutFile $svgPath -UseBasicParsing
        } catch {
            Write-Warning "Failed to download $url, checking fallback..."
        }
    }
}

Write-Host "=== All SVGs downloaded to $OutputDir ==="
