# Debugging Controls

Use the in-game command to control debug mode:

- `sspdebug on` — enable debug mode
- `sspdebug off` — disable debug mode
- `sspdebug toggle` — toggle debug mode
- `sspdebug status` — show current debug state

Requires permission level 2 (ops by default).

In code, gate extra logs/behavior using:

```java
import com.Gabou.sereneseasonsplus.util.DebugMode;

if (DebugMode.isEnabled()) {
    // debug-only logic/logs here
}
```

