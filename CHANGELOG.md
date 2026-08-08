# Changelog

All notable changes to Bloodline Chat will be tracked in this file.

## [1.4.0+mc1.21.11] - Unreleased

Compared with `1.3.0+mc1.21.11`.

### Added

- Added CoreProtect chat support, including a CoreProtect tab, CoreProtect input mode, command detection, and routing for CoreProtect output.
- Added automatic `rows:8` handling for CoreProtect lookup commands that do not already specify a row count.
- Added ghost timer support with off, queued, and strict modes for delayed chat sending.
- Added a flashing unread private-message `PM` indicator on the HUD.
- Added click-to-open behavior for the unread private-message indicator outside the chat screen.
- Added draggable positioning for the private-message indicator while the chat screen is open.
- Added per-tab options to show or hide private messages and server messages in General, Local, Team, and System tabs.
- Added prefix, whisper-player, and emote picker menus to the chat input.
- Added text selection and clipboard copy support for rendered chat messages.
- Added Discord-style chat formatting as a third font style option.
- Added more emote replacements, including broken heart, fire, eyes, rose, check, x, and wilted rose.
- Added history cleanup controls for pruning old chat log entries.
- Added a mod icon and MIT license file.

### Changed

- Changed the mod version from `1.3.0+mc1.21.11` to `1.4.0+mc1.21.11`.
- Changed the mod metadata to client-only and added an explicit Fabric API dependency.
- Changed the required Fabric Loader documentation to `0.18.4 or newer`.
- Changed archive naming from a spaced `Bloodline chat` base name to `BloodlinesChat`.
- Changed chat message rendering so CoreProtect output keeps plainer formatting without emote or Discord-style transformations.
- Changed chat history access to use cached/revision-aware snapshots for selected message lists.
- Changed Escape handling in chat to call the screen close path.

### Fixed

- Fixed private-message tab reopening so the most recent unread whisper can be opened directly from the HUD indicator.
- Fixed chat input behavior around prefixed messages so blocked ghost-timer sends can stay in the input box.
- Fixed background opacity saving so it is saved when the opacity drag ends instead of on every drag tick.
- Fixed in-bed chat interaction so the leave-bed button can still be pressed.
- Improved raw chat log reload parsing so restored messages preserve parsed sender/content details.

### Notes

- `SkillProgressTracker` exists in the source tree but is currently disabled with `ENABLED = false`.
- `AbstractContainerScreenMixin` exists in the source tree but is not currently listed in `chatplus.mixins.json`, so the skill-progress screen hooks are not active yet.

## [1.3.0+mc1.21.11]

- Previous tracked baseline.
