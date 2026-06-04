# Monopoly Deal

Java Swing implementation of Monopoly Deal.

## Run

Default GUI entry point (home screen, then Local Game):

- `com.monopolydeal.Main`

Console entry point:

- `com.monopolydeal.MonopolyDealConsole`

## Project layout

- `src/main/java/com/monopolydeal/gui/` - Swing UI (home menu + table)
- `src/main/resources/Background_graph/` - main menu background image
- `src/main/java/com/monopolydeal/logic/` - game rules and actions
- `src/main/java/com/monopolydeal/model/` - deck, players, hand, and assets
- `src/main/resources/Card_Library/` - all card images

## Notes

- Card image file names must match the files in `Card_Library`.
- The GUI deals opening hands first, then lets each player review their own starting cards.
- The project does not use an extra build tool. Open it directly in the IDE or compile it with `javac`.

## Command-line compile

```powershell
$files = Get-ChildItem -Recurse src\main\java -Filter *.java | ForEach-Object { $_.FullName }
javac -encoding UTF-8 -d out\check $files
```
