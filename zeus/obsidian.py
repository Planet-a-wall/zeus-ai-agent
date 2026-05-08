"""Client for the Obsidian Local REST API community plugin.

Setup on the machine that hosts the vault:
  1. Install the "Local REST API" community plugin in Obsidian.
  2. Enable it, copy the API key, and note the base URL
     (default: https://127.0.0.1:27124 with a self-signed cert,
      or http://127.0.0.1:27123 for plain HTTP).
  3. Export OBSIDIAN_API_KEY and OBSIDIAN_BASE_URL in your environment
     (see .env.example).
"""

from __future__ import annotations

import os
from dataclasses import dataclass
from typing import Any, Iterable
from urllib.parse import quote

import requests


class ObsidianError(RuntimeError):
    pass


@dataclass
class ObsidianClient:
    base_url: str
    api_key: str
    verify_tls: bool = False
    timeout: float = 30.0

    @classmethod
    def from_env(cls) -> "ObsidianClient":
        base_url = os.environ.get("OBSIDIAN_BASE_URL", "https://127.0.0.1:27124")
        api_key = os.environ.get("OBSIDIAN_API_KEY")
        if not api_key:
            raise ObsidianError("OBSIDIAN_API_KEY is not set")
        verify = os.environ.get("OBSIDIAN_VERIFY_TLS", "false").lower() == "true"
        return cls(base_url=base_url.rstrip("/"), api_key=api_key, verify_tls=verify)

    def _headers(self, content_type: str | None = None) -> dict[str, str]:
        headers = {"Authorization": f"Bearer {self.api_key}"}
        if content_type:
            headers["Content-Type"] = content_type
        return headers

    def _request(self, method: str, path: str, **kwargs: Any) -> requests.Response:
        url = f"{self.base_url}{path}"
        response = requests.request(
            method,
            url,
            verify=self.verify_tls,
            timeout=self.timeout,
            **kwargs,
        )
        if response.status_code >= 400:
            raise ObsidianError(
                f"{method} {path} -> {response.status_code}: {response.text}"
            )
        return response

    def ping(self) -> dict[str, Any]:
        return self._request("GET", "/", headers=self._headers()).json()

    def list_vault(self, folder: str = "") -> list[str]:
        path = f"/vault/{quote(folder)}" if folder else "/vault/"
        data = self._request("GET", path, headers=self._headers()).json()
        return data.get("files", [])

    def read_note(self, vault_path: str) -> str:
        path = f"/vault/{quote(vault_path)}"
        return self._request("GET", path, headers=self._headers()).text

    def write_note(self, vault_path: str, content: str) -> None:
        path = f"/vault/{quote(vault_path)}"
        self._request(
            "PUT",
            path,
            headers=self._headers("text/markdown"),
            data=content.encode("utf-8"),
        )

    def append_note(self, vault_path: str, content: str) -> None:
        path = f"/vault/{quote(vault_path)}"
        self._request(
            "POST",
            path,
            headers=self._headers("text/markdown"),
            data=content.encode("utf-8"),
        )

    def delete_note(self, vault_path: str) -> None:
        path = f"/vault/{quote(vault_path)}"
        self._request("DELETE", path, headers=self._headers())

    def search(self, query: str, context_length: int = 100) -> list[dict[str, Any]]:
        params = {"query": query, "contextLength": context_length}
        return self._request(
            "GET", "/search/simple/", headers=self._headers(), params=params
        ).json()

    def open_active(self) -> dict[str, Any]:
        return self._request("GET", "/active/", headers=self._headers()).json()

    def iter_notes(self, folder: str = "") -> Iterable[str]:
        for entry in self.list_vault(folder):
            full = f"{folder}/{entry}" if folder else entry
            if entry.endswith("/"):
                yield from self.iter_notes(full.rstrip("/"))
            else:
                yield full
