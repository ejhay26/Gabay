@echo off
setlocal enabledelayedexpansion
title Gabay Local AI Server (llama.cpp)
color 0B

echo ============================================================
echo           GABAY LOCAL AI SERVER (LLAMA.CPP)
echo ============================================================
echo Hardware Profile: Intel Core i5-6200U (2 Cores, 4 Threads, 16GB RAM)
echo Thermal Safeguard: Limited to 2 threads
echo.

set "LLAMA_DIR=G:\ai\llama.cpp"
set "MODELS_DIR=G:\ai\models"

if not exist "%LLAMA_DIR%\llama-server.exe" (
    echo [ERROR] llama-server.exe not found at %LLAMA_DIR%
    pause
    exit /b 1
)

echo Select Model to Launch:
echo  [1] Qwen2.5-Coder-3B-Instruct   (Fast, Fits easily in RAM, Great for code/scripts)
echo  [2] Qwen2.5-Coder-7B-Instruct   (High-Power Coding, Algorithms, Architecture)
echo  [3] Ornith-1.5-9B-Q4_K_M       (Smart Reasoning, General Chat, Co-pilot)
echo.
set /p CHOICE="Enter choice [1, 2, or 3, default=1]: "
if "%CHOICE%"=="" set CHOICE=1

if "%CHOICE%"=="1" (
    set "MODEL_PATH=%MODELS_DIR%\qwen2.5-coder-3b-instruct-q4_k_m.gguf"
    set "MODEL_NAME=Qwen2.5-Coder-3B-Instruct"
    set "CTX_SIZE=4096"
    set "EXTRA_FLAGS="
) else if "%CHOICE%"=="2" (
    set "MODEL_PATH=%MODELS_DIR%\qwen2.5-coder-7b-instruct-q4_k_m.gguf"
    set "MODEL_NAME=Qwen2.5-Coder-7B-Instruct"
    set "CTX_SIZE=2048"
    set "EXTRA_FLAGS=--no-mmap"
) else (
    set "MODEL_PATH=%MODELS_DIR%\Ornith-1.5-9B-Q4_K_M.gguf"
    set "MODEL_NAME=Ornith-1.5-9B-Q4_K_M"
    set "CTX_SIZE=2048"
    set "EXTRA_FLAGS=--no-mmap"
)

if not exist "%MODEL_PATH%" (
    echo.
    echo [ERROR] Model file not found: %MODEL_PATH%
    echo If this is Qwen2.5-Coder-7B, it may still be downloading in the background.
    echo.
    pause
    exit /b 1
)

:: Get Wi-Fi / LAN IP address
for /f "tokens=2 delims=:" %%a in ('ipconfig ^| findstr /c:"IPv4 Address" ^| findstr "192.168"') do (
    set "RAW_IP=%%a"
    set "LOCAL_IP=!RAW_IP: =!"
)
if "!LOCAL_IP!"=="" set "LOCAL_IP=127.0.0.1"

echo ------------------------------------------------------------
echo Launching: %MODEL_NAME%
echo Endpoint (Local):      http://localhost:8080
echo Endpoint (Phone/LAN):  http://!LOCAL_IP!:8080
echo OpenAI API Base:       http://localhost:8080/v1
echo ------------------------------------------------------------
echo Safe Settings:
echo  - Threads:    2 (Leaves 2 threads free for Windows so CPU won't overheat)
echo  - Context:    %CTX_SIZE% tokens
echo  - Predict:    256 tokens max
echo  - Host:       0.0.0.0 (Accessible to your phone on the same Wi-Fi)
echo  - Memory:     %EXTRA_FLAGS% (Prevents virtual memory allocation failure)
echo ------------------------------------------------------------
echo Press Ctrl+C in this window anytime to stop the server safely.
echo.

"%LLAMA_DIR%\llama-server.exe" ^
    --model "%MODEL_PATH%" ^
    --host 0.0.0.0 ^
    --port 8080 ^
    --threads 2 ^
    --ctx-size %CTX_SIZE% ^
    --n-predict 256 ^
    --batch-size 256 ^
    --temp 0.3 ^
    %EXTRA_FLAGS%

pause
