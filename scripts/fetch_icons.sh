#!/usr/bin/env bash
# scripts/fetch_icons.sh
# Idempotently fetches Lucide geometric line icons (1.75dp stroke) for Gabay

set -euo pipefail

OUTPUT_DIR="assets/icons/svg"
mkdir -p "$OUTPUT_DIR"

declare -A ICONS=(
    ["nav_cursor"]="navigation-2"
    ["maneuver_straight"]="arrow-up"
    ["maneuver_left"]="corner-up-left"
    ["maneuver_right"]="corner-up-right"
    ["maneuver_slight_left"]="arrow-up-left"
    ["maneuver_slight_right"]="arrow-up-right"
    ["maneuver_u_turn"]="undo-2"
    ["maneuver_merge"]="git-merge"
    ["maneuver_fork"]="git-fork"
    ["destination_pin"]="map-pin"
    ["finish_flag"]="flag"
    
    ["mode_scooter"]="bike"
    ["mode_car"]="car"
    ["mode_walk"]="footprints"
    
    ["layer_vector"]="map"
    ["layer_satellite"]="satellite"
    ["layer_topo"]="mountain"
    ["layer_3d"]="box"
    ["layer_stack"]="layers"
    
    ["recenter_gps"]="crosshair"
    ["compass_heading"]="compass"
    ["speed_gauge"]="gauge"
    ["time_pace"]="clock"
    ["route_dist"]="route"
    
    ["copilot_mic"]="mic"
    ["audio_tts"]="volume-2"
    ["audio_mute"]="volume-x"
    ["search_poi"]="search"
    ["settings_tuning"]="sliders-horizontal"
    
    ["warning_hazard"]="alert-triangle"
    ["road_prohibited"]="ban"
    ["reroute_active"]="refresh-cw"
    
    ["action_close"]="x"
    ["action_check"]="check"
    ["action_copy"]="copy"
    ["chevron_up"]="chevron-up"
    ["chevron_down"]="chevron-down"
    ["chevron_left"]="chevron-left"
)

echo "=== Fetching Lucide Vector SVGs for Gabay Global Registry ==="

for target in "${!ICONS[@]}"; do
    lucide="${ICONS[$target]}"
    filepath="$OUTPUT_DIR/$target.svg"
    if [ ! -f "$filepath" ]; then
        url="https://raw.githubusercontent.com/lucide-icons/lucide/main/icons/$lucide.svg"
        echo "Fetching $target ($lucide.svg)..."
        curl -sSL "$url" -o "$filepath"
    fi
done

echo "=== All SVGs downloaded to $OUTPUT_DIR ==="
