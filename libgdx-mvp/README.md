# libGDX desktop (Python parity)

Desktop UI for the same rules as the text prototype under `../src/`:

- **World map**: 3×3 grid **A–I** + **起始点**; columns A–D–G / B–E–H / C–F–I; tap a **green-ring** neighbor to travel. Example layout: **ABC** combat (sword icon), **DEF** shop (**$**), **GHI** random event (**?**). Clear all nine cells to win.
- **Cards**: Attack, Defense, Draw (Readjust), Twin Slash, Atk Up / Def Up / Hit Up — same stats as `src/cards.py` / starter deck.
- **Combat**: d20 vs AC (nat 1 miss, nat 20 crit ×2), monster **intent** pools per `monster_tag` (goblin / beast / hobgoblin), Hobgoblin +1 attack enhancement every 3 enemy turns.
- **Shop**: Twin Slash, Readjust, Attack Potion — prices match `src/shop.py`.
- **Flow**: gold & deck persist; from the map, combat picks a **random** enemy (goblin / beast / hobgoblin); **event** cells roll combat / gold / stub / wandering merchant; tap hand to play, **End Turn** ends your turn; attack potion prompt before map combats if you carry any.

Run with `.\run.bat` or `.\gradlew.bat run` (see below).

## Requirements

- JDK 11 or newer (`java` on `PATH`, or `JAVA_HOME` set)

## Run

**If Cursor’s terminal says Java was not found** (but `java -version` works in Windows Terminal), use the helper — it sets `JAVA_HOME` from `C:\Program Files\Java\jdk-*`:

```powershell
.\run.bat
```

or:

```powershell
.\run.ps1
```

(default task is `run`; pass more Gradle args after: `.\run.ps1 build`)

---

Otherwise, in **PowerShell** use `.\` when calling Gradle:

```powershell
.\gradlew.bat run
```

In **cmd.exe**, you can use either `gradlew.bat run` or `.\gradlew.bat run`.

First run downloads Gradle 8.5 into `%USERPROFILE%\.gradle\wrapper\dists` (needs network once).

Main class: `com.mydndgame.desktop.DesktopLauncher`.

### If Gradle says JAVA_HOME is not set

Point `JAVA_HOME` at the JDK folder (the one that contains `bin\java.exe`), for example:

`C:\Program Files\Java\jdk-21.0.10`

Then open a **new** terminal and run `.\gradlew.bat run` again (PowerShell). Alternatively, use `.\run.bat` (see above). Long-term: add `%JAVA_HOME%\bin` to your user **Path** in Windows environment variables and **restart Cursor** so integrated terminals inherit it.
