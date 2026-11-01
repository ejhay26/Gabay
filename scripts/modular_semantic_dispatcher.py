#!/usr/bin/env python3
"""
ModularSemanticDispatcher (Python)
----------------------------------
A standalone, modular AI Intent & Context Dispatcher for Python, Agents, and Tools.

Key Capabilities:
1. Register tool actions dynamically (name, description, parameter schema, examples).
2. Connects to any OpenAI-compatible API endpoint (local llama-server, Ollama, LM Studio, or Cloud).
3. Evaluates runtime context (active file, git branch, system status, etc.).
4. Returns either a structured tool call or conversational response.
5. Zero external framework dependencies (standard library urllib/json).
"""

import json
import urllib.request
import urllib.error
from typing import Dict, Any, List, Optional

class ModularSemanticDispatcher:
    def __init__(
        self,
        endpoint_url: str = "http://127.0.0.1:8080/v1/chat/completions",
        system_role: str = "You are an autonomous AI coding agent with tool execution capabilities.",
        temperature: float = 0.2,
        max_tokens: int = 256
    ):
        self.endpoint_url = endpoint_url
        self.system_role = system_role
        self.temperature = temperature
        self.max_tokens = max_tokens
        self.actions: Dict[str, Dict[str, Any]] = {}

    def register_action(
        self,
        name: str,
        description: str,
        target_description: Optional[str] = None,
        examples: Optional[List[str]] = None,
        keywords: Optional[List[str]] = None
    ) -> "ModularSemanticDispatcher":
        self.actions[name.lower()] = {
            "name": name,
            "description": description,
            "target_description": target_description,
            "examples": examples or [],
            "keywords": keywords or []
        }
        return self

    def build_system_prompt(self, context_state: Optional[Dict[str, Any]] = None) -> str:
        prompt = [self.system_role, "\n\n"]

        if context_state:
            prompt.append("CURRENT RUNTIME CONTEXT:\n")
            for k, v in context_state.items():
                prompt.append(f"- {k}: {v}\n")
            prompt.append("\n")

        prompt.append("ACTION INSTRUCTIONS:\n")
        prompt.append("1. If the user request requires an action, output a JSON object ONLY: {\"action\": \"...\", \"target\": \"...\"}\n")
        prompt.append("   Registered Actions:\n")

        for act in self.actions.values():
            target_info = f" (Target: {act['target_description']})" if act["target_description"] else ""
            prompt.append(f"   - \"{act['name']}\": {act['description']}{target_info}\n")

        prompt.append("\n   Examples:\n")
        for act in self.actions.values():
            for ex in act["examples"][:2]:
                prompt.append(f"   - \"{ex}\" -> {{\"action\": \"{act['name']}\", \"target\": \"...\"}}\n")

        prompt.append("\n2. If the user asks a general question or conversational request, reply naturally without JSON.\n")
        prompt.append("3. NEVER wrap commands in markdown code fences.\n")
        return "".join(prompt)

    def dispatch(
        self,
        query: str,
        context_state: Optional[Dict[str, Any]] = None,
        history: Optional[List[Dict[str, str]]] = None
    ) -> Dict[str, Any]:
        system_prompt = self.build_system_prompt(context_state)
        messages = [{"role": "system", "content": system_prompt}]

        if history:
            messages.extend(history)

        messages.append({"role": "user", "content": query})

        payload = {
            "messages": messages,
            "temperature": self.temperature,
            "max_tokens": self.max_tokens
        }

        try:
            req = urllib.request.Request(
                self.endpoint_url,
                data=json.dumps(payload).encode("utf-8"),
                headers={"Content-Type": "application/json"},
                method="POST"
            )
            with urllib.request.urlopen(req, timeout=12) as response:
                res_data = json.loads(response.read().decode("utf-8"))
                content = res_data["choices"][0]["message"]["content"].strip()
                return self._parse_response(content)
        except Exception as e:
            # Fuzzy fallback
            return self._fuzzy_fallback(query, str(e))

    def _parse_response(self, content: str) -> Dict[str, Any]:
        clean = content.replace("```json", "").replace("```", "").strip()
        start = clean.find("{")
        end = clean.rfind("}")

        if start != -1 and end != -1 and end > start:
            try:
                candidate = json.loads(clean[start:end+1])
                action = candidate.get("action", "").lower()
                if action in self.actions:
                    return {
                        "type": "action",
                        "action": action,
                        "target": candidate.get("target", ""),
                        "raw": clean
                    }
            except Exception:
                pass

        return {"type": "conversation", "text": clean}

    def _fuzzy_fallback(self, query: str, err: str) -> Dict[str, Any]:
        q_lower = query.lower()
        for name, act in self.actions.items():
            for kw in act["keywords"]:
                if kw.lower() in q_lower:
                    return {"type": "action", "action": name, "target": query, "fallback": True}
        return {"type": "conversation", "text": f"Offline ({err}): Received '{query}'."}


if __name__ == "__main__":
    dispatcher = ModularSemanticDispatcher()
    dispatcher.register_action(
        name="run_command",
        description="Execute a shell command in project directory",
        target_description="Command line string",
        examples=["run gradle build", "check git status", "start docker"],
        keywords=["run", "execute", "build", "git"]
    )
    dispatcher.register_action(
        name="search_web",
        description="Search documentation or Google for APIs and bugs",
        target_description="Search query",
        examples=["search rust uniffi async", "how to fix compose crash"],
        keywords=["search", "find", "lookup", "google"]
    )

    print("ModularSemanticDispatcher initialized with actions:", list(dispatcher.actions.keys()))
