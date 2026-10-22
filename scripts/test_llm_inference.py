import urllib.request
import json
import time

def ask(prompt):
    t0 = time.time()
    system_prompt = (
        "You are Gabay, an offline Philippine motorcycle co-pilot.\n"
        "If the user wants a navigation action, output JSON ONLY:\n"
        "Examples:\n"
        "- 'go to Cabiao' -> {\"action\": \"navigate\", \"target\": \"Cabiao\"}\n"
        "- 'switch to 3D' -> {\"action\": \"3d\"}\n"
        "- 'top down view' -> {\"action\": \"2d\"}\n"
        "- 'cancel route' -> {\"action\": \"cancel\"}\n"
        "- 'recenter map' -> {\"action\": \"follow\"}\n"
        "- 'find gas station' -> {\"action\": \"search\", \"target\": \"Gas\"}\n\n"
        "If the user asks a question, motorcycle advice, or conversation, DO NOT output JSON. "
        "Answer directly, concisely, in under 25 words."
    )
    payload = {
        "messages": [
            {"role": "system", "content": system_prompt},
            {"role": "user", "content": prompt}
        ],
        "temperature": 0.1,
        "max_tokens": 40
    }
    req = urllib.request.Request(
        "http://127.0.0.1:8766/v1/chat/completions",
        data=json.dumps(payload).encode(),
        headers={"Content-Type": "application/json"}
    )
    resp = json.loads(urllib.request.urlopen(req, timeout=10).read().decode())
    elapsed = time.time() - t0
    content = resp["choices"][0]["message"]["content"].strip()
    return content, elapsed

tests = [
    "switch to 3d mode",
    "stop navigation and clear route",
    "take me to gapan city",
    "where is the nearest petron",
    "why does wet asphalt get slippery?",
    "how do i know if my motorcycle drive belt needs replacement?",
    "what is the tire pressure for a 125cc scooter?"
]

for t in tests:
    res, elapsed = ask(t)
    print(f"[{elapsed:.2f}s] Q: {t}\n       A: {res}\n")
