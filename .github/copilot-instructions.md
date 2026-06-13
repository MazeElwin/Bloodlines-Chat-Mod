# Chat Plus Mod - Development Guide

## Project Overview

A Minecraft 1.21.1 Fabric mod that organizes chat messages into tabs (General, Local Chat, Whisper, System).

## Project Setup Checklist

- [x] Verify copilot-instructions.md in .github directory
- [x] Clarify Project Requirements (Minecraft 1.21.1 + Fabric 0.19.2)
- [x] Scaffold the Project (Gradle + Fabric structure)
- [x] Customize the Project (Chat handling implementation)
- [ ] Compile the Project
- [ ] Create and Run Task

## Build Instructions

```bash
# Build the mod
./gradlew build

# Build and run in dev environment
./gradlew runClient
```

## Chat Message Handling

The mod captures incoming chat using Fabric API's `ClientReceiveMessageEvents.CHAT` event and parses messages based on:

- **Whisper**: `/w`, `/whisper`, `/msg`, `/tell`, `/dm`
- **Local Chat**: `/lc`, `/local`
- **System Messages**: Server announcements and alerts
- **General**: Default player chat

## Key Classes

- `ChatTabManager`: Singleton managing tab storage and routing
- `ChatMessageParser`: Regex-based message classification
- `ChatMessage`: Data model with metadata (sender, timestamp, type)
- `ChatTabType`: Enum for the four tab categories

## Next Steps

1. Run `./gradlew build` to compile the mod
2. Extend `ChatMessageParser` with server-specific patterns
3. Implement client UI for tab switching (future)
4. Add configuration for custom patterns
