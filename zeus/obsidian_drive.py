"""Google Drive backed access to an Obsidian vault.

Use this when your Obsidian vault is stored in a Google Drive folder
(e.g. you sync your mobile vault via Google Drive). On the machine
that runs zeus-ai-agent, install the Google API libs and point
OBSIDIAN_DRIVE_FOLDER_ID at the Drive folder that holds the vault.

Authentication uses the installed-app OAuth flow:
  1. Create an OAuth client (Desktop) in Google Cloud Console.
  2. Download credentials.json next to this file (or set
     GOOGLE_OAUTH_CLIENT_SECRETS to its path).
  3. The first call opens a browser for consent; a token is cached
     at GOOGLE_OAUTH_TOKEN_PATH (default: token.json).
"""

from __future__ import annotations

import io
import os
from dataclasses import dataclass, field
from typing import Any

from google.auth.transport.requests import Request
from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import InstalledAppFlow
from googleapiclient.discovery import build
from googleapiclient.errors import HttpError
from googleapiclient.http import MediaIoBaseDownload, MediaIoBaseUpload

SCOPES = ["https://www.googleapis.com/auth/drive"]
MD_MIME = "text/markdown"
FOLDER_MIME = "application/vnd.google-apps.folder"


class ObsidianDriveError(RuntimeError):
    pass


def _load_credentials() -> Credentials:
    token_path = os.environ.get("GOOGLE_OAUTH_TOKEN_PATH", "token.json")
    secrets_path = os.environ.get("GOOGLE_OAUTH_CLIENT_SECRETS", "credentials.json")

    creds: Credentials | None = None
    if os.path.exists(token_path):
        creds = Credentials.from_authorized_user_file(token_path, SCOPES)

    if not creds or not creds.valid:
        if creds and creds.expired and creds.refresh_token:
            creds.refresh(Request())
        else:
            if not os.path.exists(secrets_path):
                raise ObsidianDriveError(
                    f"OAuth client secrets not found at {secrets_path}"
                )
            flow = InstalledAppFlow.from_client_secrets_file(secrets_path, SCOPES)
            creds = flow.run_local_server(port=0)
        with open(token_path, "w", encoding="utf-8") as fh:
            fh.write(creds.to_json())
    return creds


@dataclass
class ObsidianDriveClient:
    folder_id: str
    service: Any = field(default=None)

    @classmethod
    def from_env(cls) -> "ObsidianDriveClient":
        folder_id = os.environ.get("OBSIDIAN_DRIVE_FOLDER_ID")
        if not folder_id:
            raise ObsidianDriveError("OBSIDIAN_DRIVE_FOLDER_ID is not set")
        creds = _load_credentials()
        service = build("drive", "v3", credentials=creds, cache_discovery=False)
        return cls(folder_id=folder_id, service=service)

    def _q(self, parent: str, extra: str = "") -> str:
        clauses = [f"'{parent}' in parents", "trashed = false"]
        if extra:
            clauses.append(extra)
        return " and ".join(clauses)

    def list_notes(self, folder_id: str | None = None) -> list[dict[str, Any]]:
        parent = folder_id or self.folder_id
        files: list[dict[str, Any]] = []
        page_token: str | None = None
        while True:
            resp = (
                self.service.files()
                .list(
                    q=self._q(parent, "mimeType != '" + FOLDER_MIME + "'"),
                    fields="nextPageToken, files(id, name, mimeType, modifiedTime)",
                    pageToken=page_token,
                    pageSize=200,
                )
                .execute()
            )
            files.extend(resp.get("files", []))
            page_token = resp.get("nextPageToken")
            if not page_token:
                break
        return [f for f in files if f["name"].endswith(".md")]

    def list_subfolders(self, folder_id: str | None = None) -> list[dict[str, Any]]:
        parent = folder_id or self.folder_id
        resp = (
            self.service.files()
            .list(
                q=self._q(parent, f"mimeType = '{FOLDER_MIME}'"),
                fields="files(id, name)",
                pageSize=200,
            )
            .execute()
        )
        return resp.get("files", [])

    def find_by_path(self, vault_path: str) -> dict[str, Any] | None:
        parts = [p for p in vault_path.split("/") if p]
        parent = self.folder_id
        for i, part in enumerate(parts):
            is_last = i == len(parts) - 1
            mime_clause = (
                f"mimeType != '{FOLDER_MIME}'" if is_last else f"mimeType = '{FOLDER_MIME}'"
            )
            resp = (
                self.service.files()
                .list(
                    q=self._q(parent, f"{mime_clause} and name = '{part}'"),
                    fields="files(id, name, mimeType)",
                    pageSize=2,
                )
                .execute()
            )
            files = resp.get("files", [])
            if not files:
                return None
            parent = files[0]["id"]
            if is_last:
                return files[0]
        return None

    def read_note(self, vault_path: str) -> str:
        meta = self.find_by_path(vault_path)
        if not meta:
            raise ObsidianDriveError(f"Note not found: {vault_path}")
        request = self.service.files().get_media(fileId=meta["id"])
        buffer = io.BytesIO()
        downloader = MediaIoBaseDownload(buffer, request)
        done = False
        while not done:
            _, done = downloader.next_chunk()
        return buffer.getvalue().decode("utf-8")

    def write_note(self, vault_path: str, content: str) -> str:
        parts = [p for p in vault_path.split("/") if p]
        if not parts:
            raise ObsidianDriveError("vault_path is empty")
        *folder_parts, filename = parts
        parent = self._ensure_folders(folder_parts)
        existing = self._find_child(parent, filename, is_folder=False)

        media = MediaIoBaseUpload(
            io.BytesIO(content.encode("utf-8")), mimetype=MD_MIME, resumable=False
        )
        try:
            if existing:
                file = (
                    self.service.files()
                    .update(fileId=existing["id"], media_body=media, fields="id")
                    .execute()
                )
            else:
                file = (
                    self.service.files()
                    .create(
                        body={
                            "name": filename,
                            "parents": [parent],
                            "mimeType": MD_MIME,
                        },
                        media_body=media,
                        fields="id",
                    )
                    .execute()
                )
        except HttpError as exc:
            raise ObsidianDriveError(str(exc)) from exc
        return file["id"]

    def delete_note(self, vault_path: str) -> None:
        meta = self.find_by_path(vault_path)
        if not meta:
            return
        self.service.files().delete(fileId=meta["id"]).execute()

    def search(self, query: str) -> list[dict[str, Any]]:
        safe = query.replace("'", "\\'")
        resp = (
            self.service.files()
            .list(
                q=f"fullText contains '{safe}' and trashed = false",
                fields="files(id, name, modifiedTime)",
                pageSize=50,
            )
            .execute()
        )
        return resp.get("files", [])

    def _find_child(
        self, parent: str, name: str, *, is_folder: bool
    ) -> dict[str, Any] | None:
        mime_clause = (
            f"mimeType = '{FOLDER_MIME}'" if is_folder else f"mimeType != '{FOLDER_MIME}'"
        )
        safe = name.replace("'", "\\'")
        resp = (
            self.service.files()
            .list(
                q=self._q(parent, f"{mime_clause} and name = '{safe}'"),
                fields="files(id, name)",
                pageSize=2,
            )
            .execute()
        )
        files = resp.get("files", [])
        return files[0] if files else None

    def _ensure_folders(self, parts: list[str]) -> str:
        parent = self.folder_id
        for part in parts:
            existing = self._find_child(parent, part, is_folder=True)
            if existing:
                parent = existing["id"]
                continue
            folder = (
                self.service.files()
                .create(
                    body={
                        "name": part,
                        "parents": [parent],
                        "mimeType": FOLDER_MIME,
                    },
                    fields="id",
                )
                .execute()
            )
            parent = folder["id"]
        return parent
