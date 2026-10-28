# Duo Control (Fabric, Minecraft 26.2)

Two players, one body.

- **Keyboard player (body):** WASD, jump, sneak, sprint, inventory, number keys. Their own mouse is disabled in-game (it still works inside menus).
- **Mouse player:** sees through the body's eyes. Controls look direction, left click (attack/break), right click (use/place) and scroll wheel (hotbar).

## Commands
- `/duo invite <player>`  you = KEYBOARD, they = MOUSE
- `/duo accept`           accept an invite
- `/duo swap`             swap roles
- `/duo stop`             end the duo

## Build
Requires JDK 25. Everyone (and the server/host) needs this mod + Fabric API 0.158.0+26.2.

Copy `gradlew`, `gradlew.bat` and the `gradle/` folder from a fresh template
(https://fabricmc.net/develop/template/) into this folder, then:

    ./gradlew build

Jar: build/libs/duo-control-1.0.0.jar
