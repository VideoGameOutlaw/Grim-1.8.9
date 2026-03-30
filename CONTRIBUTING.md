# Contributing to GrimLegacy (Grim-1.8.9 Fork)

Thanks for contributing.

This fork has different priorities from modern upstream Grim. Please align changes with the fork goals before opening a PR.

## Fork Development Priorities

1. **Server stability first**
2. **Exploit/crash resistance**
3. **Practical detection for hostile public servers**
4. **Low false positives (especially under latency and chaotic fights)**
5. **Maintainable, documented code**

## Scope Expectations

This repository is focused on:

- Minecraft **1.8.9-oriented** behavior,
- **Java 8 compatibility goal** (ongoing migration),
- Vanilla-anarchy/public-survival operational realities.

Avoid PRs that optimize only for tightly controlled minigame environments while increasing false positives for public survival/anarchy traffic.

## Pull Request Guidelines

### Compatibility

- Keep changes safe for the fork's 1.8.9-focused deployment model.
- Prefer legacy-safe assumptions when packet behavior differs by version.
- Do not add new runtime requirements that conflict with Java 8 migration goals.

### Hardening policy

Security-oriented changes should:

- be defensive and efficient,
- fail safely,
- avoid expensive work in packet hot paths,
- include operator-facing config controls where reasonable,
- avoid publishing exploit-reproduction details in public docs/comments.

### Non-acceptable PR patterns

- Checks that are extremely sensitive and likely to false in high-latency public fights.
- Heavy changes that add significant per-packet overhead without clear abuse-defense value.
- Broad architectural rewrites with no migration plan or profiling/validation notes.
- Version-expansion work that conflicts with the fork's legacy-focused purpose.

### PR quality requirements

- Use clear commit messages.
- Include concise rationale in PR description.
- Call out behavior changes and operator impact.
- Document new config options and defaults.
- Add comments for non-obvious legacy-specific logic.

## Build & Test

Typical compile checks:

```bash
./gradlew :common:compileJava :bukkit:compileJava
```

Legacy migration progress check:

```bash
./gradlew :common:compileJava -PlegacyJava8=true
```

If Java 8 profile fails, include the failure class/category in your PR notes and explain what was or was not migrated.

## Developer Notes

- Keep packet hot paths low-allocation where possible.
- Prefer fast sanity checks before deep analysis.
- Keep mitigation behavior configurable (cancel, alert, log, kick policy).
- Preserve readability: this fork should not become a pile of undocumented edge-case hacks.

## Questions

Open an issue using the templates in `.github/ISSUE_TEMPLATE/` for bug reports, false positives, bypasses, and hardening requests.
