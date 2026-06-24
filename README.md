# Verte

Horror mod for Minecraft **Forge 1.20.1**. Inside your world lives **Verte** — a square creepy AI entity you talk to via `/verte <text>`. It replies live and can do anything in the world: summon mobs, build, change weather and time, teleport, apply effects, give and take items.

## Setup

1. Grab the `.jar` from GitHub Actions (artifact `verte-jar`) and drop it in `mods/`.
2. Launch the game once so `config/verte-common.toml` is generated.
3. Get a free API key (Groq recommended: https://console.groq.com ).
4. Put the key in `config/verte-common.toml` -> `apiKey = "..."` (or set the `VERTE_API_KEY` environment variable).
5. In game: `/verte hello`

## Config

- `apiKey` — key for the OpenAI-compatible endpoint.
- `apiUrl` — endpoint URL (defaults to Groq).
- `model` — model name (defaults to `llama-3.3-70b-versatile`).

> For singleplayer and private worlds with friends only.
