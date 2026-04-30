#!/usr/bin/env python3
"""
One-time Discord server setup for Logistics mod.

Creates roles, channel categories, channels, and permission overwrites to match
the recommended structure for the mod's community server.

=============================================================================
SETUP (do this once before running the script)
=============================================================================

1. Create a Discord bot:
   a. Go to https://discord.com/developers/applications
   b. Click "New Application" → name it "Logistics Setup Bot" (or anything)
   c. Go to the "Bot" tab → click "Reset Token" → copy the token
   d. Under "Privileged Gateway Intents" nothing needs to be enabled

2. Invite the bot to your server WITH Administrator permission:
   a. Go to "OAuth2" → "URL Generator"
   b. Scopes: check "bot"
   c. Bot Permissions: check "Administrator"
   d. Copy the generated URL, open it, select your server, click Authorize

3. Get your server (guild) ID:
   a. In Discord, enable Developer Mode: Settings → Advanced → Developer Mode
   b. Right-click your server name in the sidebar → "Copy Server ID"

4. Run the script:
   DISCORD_BOT_TOKEN=your_token_here DISCORD_GUILD_ID=your_server_id python3 tools/setup_discord.py

   Or set them as environment variables first:
   export DISCORD_BOT_TOKEN=...
   export DISCORD_GUILD_ID=...
   python3 tools/setup_discord.py

=============================================================================
WHAT IT CREATES
=============================================================================

Roles:
  @Developer   — can post in #announcements, #dev-log; sees all channels; red
  @Contributor — access to #contributors and #dev-log (read); blue
  @everyone    — standard member; green (no color change)

Categories and channels:
  📋 INFO         — read-only for everyone, only Developer posts
    #rules
    #about
    #announcements
  💬 GENERAL      — open to everyone
    #general
    #showcase
  🔧 SUPPORT      — open to everyone
    #help
    #bug-reports
  🧪 TESTING      — open to everyone
    #beta-testing
  🤝 CONTRIBUTORS — Contributor + Developer only (hidden from everyone else)
    #contributors
  👨‍💻 DEV          — Developer posts, Contributor reads; hidden from everyone
    #dev-log

=============================================================================
AFTER RUNNING
=============================================================================

  • Assign @Developer to yourself and any co-maintainers
  • Assign @Contributor to trusted testers / regular contributors
  • Add the Discord webhook URL for #announcements to GitHub:
      repo Settings → Webhooks → Add webhook
      Payload URL: your-webhook-url (Discord provides this under Server Settings
                   → Integrations → Webhooks)
      Content type: application/json
      Events: Let me select individual events → check "Releases"
  • You can delete the bot from the server after setup if you wish
"""

import json
import os
import sys
import time
import urllib.error
import urllib.request
from typing import Optional

# ---------------------------------------------------------------------------
# Discord API helpers
# ---------------------------------------------------------------------------

BASE = "https://discord.com/api/v10"
REQUEST_TIMEOUT = 10  # seconds


def _headers(token: str) -> dict:
    return {
        "Authorization": f"Bot {token}",
        "Content-Type": "application/json",
        "User-Agent": "LogisticsSetupScript/1.0",
    }


def api(method: str, path: str, token: str, body: Optional[dict] = None) -> dict:
    """Make a Discord API call, respecting rate limits automatically."""
    url = f"{BASE}{path}"
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, headers=_headers(token), method=method)

    for attempt in range(5):
        try:
            with urllib.request.urlopen(req, timeout=REQUEST_TIMEOUT) as resp:
                raw = resp.read()
                # 204 No Content
                return json.loads(raw) if raw else {}
        except urllib.error.HTTPError as e:
            if e.code == 429:
                retry_after = float(json.loads(e.read()).get("retry_after", 1))
                print(f"  rate-limited, waiting {retry_after:.1f}s…")
                time.sleep(retry_after + 0.1)
                continue
            body_text = e.read().decode(errors="replace")
            print(f"  ERROR {e.code} {method} {path}: {body_text}", file=sys.stderr)
            raise

    raise RuntimeError(f"Failed after retries: {method} {path}")


def get(path: str, token: str) -> dict:
    return api("GET", path, token)


def post(path: str, token: str, body: dict) -> dict:
    time.sleep(0.3)  # gentle pacing to avoid rate limits
    return api("POST", path, token, body)


def patch(path: str, token: str, body: dict) -> dict:
    time.sleep(0.3)
    return api("PATCH", path, token, body)


def put(path: str, token: str, body: dict) -> dict:
    time.sleep(0.3)
    return api("PUT", path, token, body)


# ---------------------------------------------------------------------------
# Permission bit flags
# ---------------------------------------------------------------------------

VIEW             = 1 << 10   # VIEW_CHANNEL
SEND             = 1 << 11   # SEND_MESSAGES
HISTORY          = 1 << 16   # READ_MESSAGE_HISTORY
REACTIONS        = 1 << 6    # ADD_REACTIONS
EMBEDS           = 1 << 14   # EMBED_LINKS
ATTACHMENTS      = 1 << 15   # ATTACH_FILES
EXTERNAL_EMOJI   = 1 << 18   # USE_EXTERNAL_EMOJIS
MANAGE_MESSAGES  = 1 << 13   # MANAGE_MESSAGES (for pinning etc)

# Convenient bundles
MEMBER_BASE   = VIEW | SEND | HISTORY | REACTIONS | EMBEDS | ATTACHMENTS | EXTERNAL_EMOJI
READ_ONLY     = VIEW | HISTORY | REACTIONS
DEV_EXTRA     = MANAGE_MESSAGES  # can pin, delete messages in their channels


# ---------------------------------------------------------------------------
# Helpers: roles and permission overwrites
# ---------------------------------------------------------------------------

def find_existing(items: list[dict], name: str) -> Optional[dict]:
    return next((x for x in items if x.get("name") == name), None)


def ensure_role(guild_id: str, token: str, existing_roles: list[dict],
                name: str, color: int, hoist: bool = True, mentionable: bool = True) -> str:
    """Create role if it doesn't exist; return its ID."""
    existing = find_existing(existing_roles, name)
    if existing:
        print(f"  Role already exists: @{name} ({existing['id']})")
        return existing["id"]

    role = post(f"/guilds/{guild_id}/roles", token, {
        "name": name,
        "color": color,
        "hoist": hoist,        # show separately in member list
        "mentionable": mentionable,
        "permissions": "0",    # channel-level overwrites handle actual access
    })
    print(f"  Created role: @{name} ({role['id']})")
    return role["id"]


def ensure_category(guild_id: str, token: str, existing_channels: list[dict],
                    name: str, overwrites: list[dict]) -> str:
    """Create channel category if it doesn't exist; return its ID."""
    existing = find_existing(existing_channels, name)
    if existing and existing.get("type") == 4:
        print(f"  Category already exists: {name} ({existing['id']})")
        return existing["id"]

    ch = post(f"/guilds/{guild_id}/channels", token, {
        "name": name,
        "type": 4,
        "permission_overwrites": overwrites,
    })
    print(f"  Created category: {name} ({ch['id']})")
    return ch["id"]


def ensure_channel(guild_id: str, token: str, existing_channels: list[dict],
                   name: str, category_id: str, topic: str,
                   overwrites: list[dict]) -> str:
    """Create text channel if it doesn't exist; return its ID."""
    existing = find_existing(existing_channels, name)
    if existing and existing.get("type") == 0:
        print(f"  Channel already exists: #{name} ({existing['id']})")
        return existing["id"]

    ch = post(f"/guilds/{guild_id}/channels", token, {
        "name": name,
        "type": 0,
        "parent_id": category_id,
        "topic": topic,
        "permission_overwrites": overwrites,
    })
    print(f"  Created channel: #{name} ({ch['id']})")
    return ch["id"]


def overwrite(id_: str, type_: int, allow: int = 0, deny: int = 0) -> dict:
    """Build a permission overwrite object. type_=0 for role, type_=1 for member."""
    return {"id": id_, "type": type_, "allow": str(allow), "deny": str(deny)}


# ---------------------------------------------------------------------------
# Server structure definition
# ---------------------------------------------------------------------------

def setup(guild_id: str, token: str) -> None:
    print(f"\nSetting up Discord server (guild {guild_id})…\n")

    # Fetch current state
    guild        = get(f"/guilds/{guild_id}", token)
    everyone_id  = guild_id   # @everyone role ID always equals the guild ID
    all_roles    = get(f"/guilds/{guild_id}/roles", token)
    all_channels = get(f"/guilds/{guild_id}/channels", token)

    print("Creating roles…")
    dev_id  = ensure_role(guild_id, token, all_roles, "Developer",   color=0xE74C3C)
    cont_id = ensure_role(guild_id, token, all_roles, "Contributor", color=0x3498DB)
    # @everyone color is managed via guild settings; no change needed here

    # Re-fetch channels in case we need fresh list (roles were just created)
    all_channels = get(f"/guilds/{guild_id}/channels", token)

    # -----------------------------------------------------------------------
    # Reusable overwrite sets
    # -----------------------------------------------------------------------

    # INFO channels: everyone reads, Developer posts
    ow_info = [
        overwrite(everyone_id, 0, allow=READ_ONLY, deny=SEND),
        overwrite(dev_id,      0, allow=MEMBER_BASE | DEV_EXTRA),
    ]

    # GENERAL / SUPPORT / TESTING: everyone full access, Developer has manage
    ow_open = [
        overwrite(everyone_id, 0, allow=MEMBER_BASE),
        overwrite(dev_id,      0, allow=MEMBER_BASE | DEV_EXTRA),
        overwrite(cont_id,     0, allow=MEMBER_BASE),
    ]

    # CONTRIBUTORS: hidden from everyone, Contributor + Developer can chat
    ow_contributors = [
        overwrite(everyone_id, 0, deny=VIEW),
        overwrite(cont_id,     0, allow=MEMBER_BASE),
        overwrite(dev_id,      0, allow=MEMBER_BASE | DEV_EXTRA),
    ]

    # DEV-LOG: hidden from everyone, Contributor reads, Developer posts
    ow_devlog = [
        overwrite(everyone_id, 0, deny=VIEW),
        overwrite(cont_id,     0, allow=READ_ONLY),
        overwrite(dev_id,      0, allow=MEMBER_BASE | DEV_EXTRA),
    ]

    # -----------------------------------------------------------------------
    # 📋 INFO
    # -----------------------------------------------------------------------
    print("\nCreating INFO category…")
    cat_info = ensure_category(guild_id, token, all_channels, "📋 INFO", ow_info)

    ensure_channel(guild_id, token, all_channels, "rules", cat_info,
                   "Server rules. Read before participating.",
                   ow_info)
    ensure_channel(guild_id, token, all_channels, "about", cat_info,
                   "What Logistics: Automation is and where to find it.",
                   ow_info)
    ensure_channel(guild_id, token, all_channels, "announcements", cat_info,
                   "Release announcements. GitHub posts here automatically.",
                   ow_info)

    # -----------------------------------------------------------------------
    # 💬 GENERAL
    # -----------------------------------------------------------------------
    print("\nCreating GENERAL category…")
    cat_general = ensure_category(guild_id, token, all_channels, "💬 GENERAL", ow_open)

    ensure_channel(guild_id, token, all_channels, "general", cat_general,
                   "General chat about the mod, Minecraft, and anything on-topic.",
                   ow_open)
    ensure_channel(guild_id, token, all_channels, "showcase", cat_general,
                   "Share screenshots and builds using Logistics pipes.",
                   ow_open)

    # -----------------------------------------------------------------------
    # 🔧 SUPPORT
    # -----------------------------------------------------------------------
    print("\nCreating SUPPORT category…")
    cat_support = ensure_category(guild_id, token, all_channels, "🔧 SUPPORT", ow_open)

    ensure_channel(guild_id, token, all_channels, "help", cat_support,
                   "Install issues, config questions, and how-to questions.",
                   ow_open)
    ensure_channel(guild_id, token, all_channels, "bug-reports", cat_support,
                   "Report bugs here or at https://github.com/indemnity83/logistics/issues",
                   ow_open)

    # -----------------------------------------------------------------------
    # 🧪 TESTING
    # -----------------------------------------------------------------------
    print("\nCreating TESTING category…")
    cat_testing = ensure_category(guild_id, token, all_channels, "🧪 TESTING", ow_open)

    ensure_channel(guild_id, token, all_channels, "beta-testing", cat_testing,
                   "Pre-release builds and feedback. See pinned post for test instructions.",
                   ow_open)

    # -----------------------------------------------------------------------
    # 🤝 CONTRIBUTORS (Contributor + Developer only)
    # -----------------------------------------------------------------------
    print("\nCreating CONTRIBUTORS category…")
    cat_contrib = ensure_category(guild_id, token, all_channels, "🤝 CONTRIBUTORS",
                                  ow_contributors)

    ensure_channel(guild_id, token, all_channels, "contributors", cat_contrib,
                   "Lower-noise technical discussion for contributors and maintainers.",
                   ow_contributors)

    # -----------------------------------------------------------------------
    # 👨‍💻 DEV (Developer posts, Contributor reads)
    # -----------------------------------------------------------------------
    print("\nCreating DEV category…")
    cat_dev = ensure_category(guild_id, token, all_channels, "👨‍💻 DEV", ow_devlog)

    ensure_channel(guild_id, token, all_channels, "dev-log", cat_dev,
                   "Brief notes from the maintainer when notable fixes land.",
                   ow_devlog)

    # -----------------------------------------------------------------------
    # Done
    # -----------------------------------------------------------------------
    print("\n" + "=" * 60)
    print("Setup complete!")
    print()
    print("Next steps:")
    print(f"  1. Assign @Developer to yourself:")
    print(f"       Server Settings → Members → find yourself → + → Developer")
    print(f"  2. Set up the GitHub release webhook:")
    print(f"       Server Settings → Integrations → Webhooks → New Webhook")
    print(f"       Channel: #announcements → copy the URL")
    print(f"       GitHub repo Settings → Webhooks → Add webhook")
    print(f"       Payload URL: (paste webhook URL)")
    print(f"       Content type: application/json")
    print(f"       Events: Releases only")
    print(f"  3. Pin test instructions in #beta-testing before your next pre-release.")
    print(f"  4. You can kick the setup bot from the server now if you wish.")
    print()


# ---------------------------------------------------------------------------
# Entry point
# ---------------------------------------------------------------------------

def main() -> int:
    token    = os.environ.get("DISCORD_BOT_TOKEN", "").strip()
    guild_id = os.environ.get("DISCORD_GUILD_ID", "").strip()

    if not token or not guild_id:
        print("Usage: DISCORD_BOT_TOKEN=... DISCORD_GUILD_ID=... python3 tools/setup_discord.py")
        print()
        print("See the top of this file for full setup instructions.")
        return 1

    # Quick sanity check: verify the token works and bot is in the guild
    try:
        guild = get(f"/guilds/{guild_id}", token)
        print(f"Connected to server: {guild['name']!r}")
    except urllib.error.HTTPError as e:
        if e.code == 401:
            print("ERROR: Invalid bot token. Double-check DISCORD_BOT_TOKEN.")
        elif e.code == 403:
            print("ERROR: Bot lacks permission. Make sure you invited it with Administrator.")
        elif e.code == 404:
            print("ERROR: Guild not found. Check DISCORD_GUILD_ID and that the bot is in the server.")
        else:
            print(f"ERROR: HTTP {e.code} — {e.read().decode()}")
        return 1

    setup(guild_id, token)
    return 0


if __name__ == "__main__":
    sys.exit(main())
