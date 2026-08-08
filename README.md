# Bloodline Chat - Minecraft Fabric Mod

A Minecraft Fabric mod that organizes Bloodlines server chat into tabs.

## Features

- **Chat Tabs**: Automatically separates chat into different tabs:
  - **General**: All regular chat messages
  - **Local Chat**: Messages from `/lc` command
  - **Whisper**: Direct messages using `/w` command
  - **System**: Server system messages and alerts

- **Message Parsing**: Automatically detects and categorizes incoming chat based on message format

- **Chat Management**: View, clear, and track messages by tab type

## Requirements

- Minecraft 1.21.11
- Fabric Loader 0.18.4 or newer
- Fabric API
- Java 21 or higher

## Installation

1. Download the mod JAR file from releases
2. Place it in your `mods` folder
3. Launch Minecraft with Fabric

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for version-by-version changes.

## Commands

Currently supports:
- `/w <player> <message>` - Send a whisper/DM
- `/lc <message>` - Send a local chat message

## Project Structure

```
src/main/java/com/example/chatplus/
├── ChatPlusMod.java              # Main mod entry point
├── ChatPlusClient.java            # Client-side initialization
└── chat/
    ├── ChatTabManager.java        # Core tab management
    ├── ChatTabType.java           # Tab type enum
    ├── ChatMessage.java           # Message model
    ├── ChatMessageParser.java     # Message parsing logic
    └── ChatTabListener.java       # Observer interface
```

## Building

```bash
./gradlew build
```

The compiled mod will be in `build/libs/`

## Future Features

- [ ] Client-side chat UI with tab switching
- [ ] Configurable chat filters
- [ ] Message search and history
- [ ] Custom chat colors and formatting
- [ ] Server integration for chat relay

## License

MIT

## Contributing

Feel free to open issues and pull requests for improvements!
