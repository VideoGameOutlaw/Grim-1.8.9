# GrimLegacy (Grim-1.8.9 Fork)

A legacy-focused fork of [Grim AntiCheat](https://github.com/GrimAnticheat/Grim), tailored for **Minecraft 1.8.9** public servers that prioritize **stability**, **exploit resistance**, and **practical anticheat behavior** over hypersensitive competitive detection.

> This repository is not a drop-in mirror of modern upstream Grim goals. It is a specialized fork for hostile public server environments (vanilla anarchy/survival style).

---

## Project Purpose

GrimLegacy is designed for server operators who need a hardened, maintainable anticheat + packet-defense layer in environments where players may intentionally:

- abuse malformed packets,
- spam interactions to create lag,
- attempt old-client crash vectors,
- use blatant movement/combat cheats,
- and exploit legacy protocol edge cases.

### Priority order in this fork

1. **Server stability**
2. **Exploit/crash resistance**
3. **Practical cheat detection**
4. **Low false positives under real lag**
5. **Maintainability**

---

## Supported Environment (Fork Policy)

- **Primary game target:** Minecraft **1.8.9 behavior and packet flow**
- **Primary deployment model:** Bukkit/Spigot/Paper-family backend for 1.8.9-style servers
- **Java runtime target:** **Java 8** (fork objective)

### Important current status

This fork includes Java 8 build-profile scaffolding (`-PlegacyJava8=true`), but parts of the codebase still contain modern Java language features inherited from upstream. That means full Java 8 source compatibility is **in progress**, not fully completed yet.

If you are deploying immediately, validate your exact branch build and runtime environment before production rollout.

---

## What Changed vs Upstream Grim

Compared to upstream (which targets modern multi-version support goals), this fork emphasizes:

- legacy 1.8.9-oriented tuning and operational behavior,
- public-server hardening against malformed/abusive packet patterns,
- lower-noise defaults for chaotic/high-latency fights,
- an operator-focused configuration approach for mitigation and incident response.

This repository still contains upstream architecture and many upstream checks, but policy and documentation are now oriented around **legacy hostile-environment operation**.

---

## Hardening & Anti-Exploit Focus

This fork includes and/or prioritizes defensive protections such as:

- malformed packet rejection,
- packet-rate limiting on abusive packet families,
- custom payload sanity checks and size limits,
- invalid movement/rotation/coordinate filtering,
- inventory/window interaction sanity filtering,
- movement and interaction spam mitigation,
- crash-attempt handling designed to fail safely.

### About defensive disclosure

Documentation here intentionally stays high-level and defensive. We avoid publishing exploit-reproduction details that would help abuse operators.

---

## Practical Detection Philosophy

For vanilla-anarchy/public-survival usage, defaults and review policy favor:

- catching **blatant** movement/combat/timer abuse,
- preserving playability during latency spikes,
- reducing false bans from edge-case micro detections,
- allowing operators to tune strictness by config and punishment policy.

---

## Installation (Server Owners)

1. Build or obtain the Bukkit artifact for this fork.
2. Put the jar in your server `plugins/` directory.
3. Start server once to generate config files.
4. Review `config.yml` and `punishments.yml` before public launch.
5. Restart server after configuration updates.

### Recommended rollout

- Start in alert-heavy / conservative punish mode.
- Observe for a few days of real traffic.
- Tighten thresholds gradually on clear abuse channels.

---

## Build Instructions (Developers)

```bash
git clone <your-fork-url>
cd Grim-1.8.9
./gradlew :common:build :bukkit:build
```

### Legacy Java 8 profile (work in progress)

```bash
./gradlew :common:compileJava -PlegacyJava8=true
```

This profile is used to track Java 8 migration progress, but compile success is not yet guaranteed until remaining modern-language usages are refactored.

---

## Configuration Overview

Key files:

- `config.yml` – check behavior, hardening limits, packet handling, debug options
- `punishments.yml` – escalation/action policy (alerts, setbacks, kicks, commands)
- `messages.yml` – user/staff-facing message templates

### Legacy hardening block

The `legacy-hardening` section provides first-pass packet safety controls (rate limits, payload/chat size limits, sanity guards, mitigation actions).

Tune this section first for hostile public traffic.

---

## Recommended Baseline for Public 1.8.9 Anarchy

- Keep hardening enabled.
- Prefer cancel + alert + temporary ignore before immediate kick.
- Keep log rate limiting on to avoid console spam.
- Use punitive actions for repeated or severe malformed streams.
- Keep punishments focused on blatant abuse classes.

---

## Known Limitations

- Full Java 8 compatibility is not fully completed yet across all source paths.
- Some upstream components still reflect broader modern-version architecture and require gradual legacy-focused cleanup/refinement.
- Not every upstream feature is equally valuable in a 1.8.9 anarchy context; behavior may be restricted/tuned accordingly over time.

---

## Contributing

Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a PR.

For this fork, contributions should preserve:

- legacy 1.8.9 intent,
- low false positives under lag,
- efficient packet-level hardening,
- clear and maintainable implementation.

---

## Upstream Acknowledgement

This project is based on upstream Grim AntiCheat and remains GPL-licensed. Upstream authors and contributors did the foundational architecture and core anticheat work that this fork builds on.

---

## License

Licensed under **GPL-3.0**. See [LICENSE](LICENSE).
