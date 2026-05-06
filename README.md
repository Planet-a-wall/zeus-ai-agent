# Zeus LINE Agent

An Android app that runs your **personal task-followup robot** on LINE.

The bot lives as a LINE Official Account ("Zeus" — your stand-in). You add it to
each project group on LINE; the app remembers which LINE group belongs to which
Asana project; and from your phone you can send a follow-up message that
represents you (with a list of open tasks and owners) into the right group.

```
┌─────────────────────────┐         ┌────────────────────────────┐
│  Android app (this)     │ Asana → │  Asana REST API            │
│  - Settings / tokens    │         └────────────────────────────┘
│  - Group ↔ project map  │
│  - "Compose follow-up"  │ LINE  → ┌────────────────────────────┐
│  - Send button          │         │  LINE Messaging API        │
│                         │         │  /v2/bot/message/push      │
│                         │ Zapier→ │  + Zapier Catch Hook       │
└─────────────────────────┘         │  (Calendar, Sheets, etc.)  │
                                    └────────────────────────────┘
```

## What the bot does

- Pushes messages **as the bot account** (which is named after you) into any
  LINE chat the bot has been invited to — group, room, or 1:1.
- Pulls open tasks from the Asana project mapped to that group and assembles a
  follow-up message ("Could we get a status update on … ?").
- Optionally fans the same event out to Zapier, so you can wire Calendar
  events, Google Sheets logs, Slack mirrors, etc.

> ⚠️ The LINE Messaging API does **not** let a bot impersonate your personal
> LINE account — that is a platform restriction. The bot uses a separate
> Official Account that you name and brand to represent you.

---

## 1. Set up the LINE Messaging API channel

1. Go to <https://developers.line.biz/console/> and sign in with your LINE
   account.
2. Create a **Provider** (e.g. "Zeus").
3. Inside the provider, create a **Messaging API channel**.
   - Channel name: e.g. "Zeus — \<your name>"
   - Icon: a photo of you, or a robot avatar.
4. Open the new channel → **Messaging API** tab:
   - **Use webhook**: ON (you can leave the URL blank for now or point it at
     a Zapier "Catch Hook" — see step 4 below).
   - **Auto-reply messages**: OFF.
   - **Greeting messages**: optional.
5. **Issue a long-lived channel access token** and copy it.
6. Copy the **Channel secret** from the *Basic settings* tab.
7. From the **Messaging API** tab, find the bot's QR code or the
   **Add friend** URL. Use it to:
   - Add the bot as a friend on your phone, **and**
   - Invite the bot into each project group/room.

When the bot joins a group it cannot send messages until you have the
**group ID**. The simplest way to capture it:

- Point the channel's webhook URL at a Zapier "Catch Hook" (see step 4).
- Add the bot to a group, have anyone send a message, and inspect the captured
  webhook payload — `events[0].source.groupId` is what you want.
- Copy that ID into the app's *Group mappings* screen.

---

## 2. Get your Asana credentials

1. Go to <https://app.asana.com/0/my-apps> → **Personal access tokens** →
   *Create new token*. Copy it.
2. (Optional) Open any project in your browser. The URL contains the project
   gid, e.g. `https://app.asana.com/0/1209876543210987/list` →
   `1209876543210987` is the **project gid**.
3. (Optional) Find your **workspace gid** at
   <https://app.asana.com/api/1.0/workspaces> (use the token).

---

## 3. Run the Android app

Requirements: Android Studio Hedgehog (2023.1) or newer, JDK 17, an Android
device on Android 8.0 (API 26) or above.

```
git clone <this repo>
cd zeus-ai-agent
# Open the folder in Android Studio. It will run "Sync Now" and generate the
# Gradle wrapper automatically.
```

Then in the app:

1. **Settings** → paste your LINE channel access token, Asana PAT, optional
   workspace gid, optional Zapier webhook URL, and your display name.
2. **Group mappings → +** → add an entry per LINE group:
   - *Label*: human name ("Project Athena")
   - *LINE target ID*: the group/room/user ID
   - *Asana project gid*: the project the bot is following up on
3. From **Home** tap the send icon next to a group → tap
   *Generate follow-up template* → review/edit → **Send to LINE**.

---

## 4. (Optional) Zapier integration

Create a Zap with **Webhooks by Zapier → Catch Hook** as the trigger and copy
its URL into Settings. Every successful follow-up the app sends will also POST
to Zapier with this payload:

```json
{
  "event": "line_followup_sent",
  "project": "Project Athena",
  "asana_project": "1209876543210987",
  "line_target": "Cxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
  "message": "...",
  "sender": "Your name"
}
```

From Zapier you can fan this out to:

- **Google Calendar** — create a "follow-up sent" event.
- **Google Sheets** — append a row to your project log.
- **Asana** — add a comment on the relevant tasks.
- **Slack / email** — mirror the message somewhere else.

You can also point the LINE channel webhook at a *separate* Zapier Catch Hook
to capture inbound messages and group IDs.

---

## Project structure

```
app/
├── build.gradle.kts
└── src/main/
    ├── AndroidManifest.xml
    ├── kotlin/com/zeus/lineagent/
    │   ├── ZeusApp.kt              Application + AppContainer wiring
    │   ├── MainActivity.kt
    │   ├── AppContainer.kt         Manual DI (settings + repos)
    │   ├── data/
    │   │   ├── SettingsRepository.kt   DataStore-backed prefs + mappings
    │   │   └── GroupMapping.kt
    │   ├── network/
    │   │   ├── NetworkModule.kt        Retrofit/OkHttp/JSON setup
    │   │   ├── LineApi.kt              /v2/bot/message/push
    │   │   ├── LineRepository.kt
    │   │   ├── AsanaApi.kt             projects + tasks endpoints
    │   │   ├── AsanaRepository.kt
    │   │   └── ZapierApi.kt            Catch Hook fan-out
    │   └── ui/
    │       ├── AppNavigation.kt
    │       ├── theme/Theme.kt
    │       └── screens/
    │           ├── HomeScreen.kt
    │           ├── SettingsScreen.kt
    │           ├── GroupMappingsScreen.kt
    │           └── ComposeFollowupScreen.kt
    └── res/
        ├── values/{strings,colors,themes}.xml
        ├── xml/{backup_rules,data_extraction_rules}.xml
        ├── drawable/ic_launcher_foreground.xml
        └── mipmap-anydpi-v26/ic_launcher{,_round}.xml
```

## Security notes

- Tokens are stored in the app's private DataStore and excluded from cloud
  backup / device-transfer (`backup_rules.xml`, `data_extraction_rules.xml`).
- For a release build, add `keystore.properties` (already gitignored) and a
  signing config to `app/build.gradle.kts`.

## Roadmap ideas

- Schedule recurring follow-ups (WorkManager).
- Inbound webhook → Cloud Functions → push to the app via FCM, so you can
  capture group IDs without the Zapier dance.
- Google Calendar OAuth directly in the app (skip Zapier).
- Voice/photo follow-ups using LINE's image and audio message types.
