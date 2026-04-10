"""ANSI styling for terminal output (Windows 10+ VT, modern terminals)."""

from __future__ import annotations

import os
import sys

RESET = "\033[0m"
GREEN = "\033[32m"
YELLOW = "\033[33m"


def try_enable_windows_ansi() -> None:
    """Enable ANSI escape sequences in legacy Windows consoles when possible."""
    if os.name != "nt" or not sys.stdout.isatty():
        return
    try:
        import ctypes

        kernel32 = ctypes.windll.kernel32
        handle = kernel32.GetStdHandle(-11)
        mode = ctypes.c_ulong()
        if kernel32.GetConsoleMode(handle, ctypes.byref(mode)):
            enable_vt = 0x0004  # ENABLE_VIRTUAL_TERMINAL_PROCESSING
            kernel32.SetConsoleMode(handle, mode.value | enable_vt)
    except Exception:
        pass


def style_hit(text: str) -> str:
    """Green — successful hit / crit."""
    return f"{GREEN}{text}{RESET}"


def style_miss(text: str) -> str:
    """Yellow — miss / failed attack."""
    return f"{YELLOW}{text}{RESET}"


def style_hp_ratio(current: int, maximum: int) -> str:
    """Green current/max HP fragment."""
    return f"{GREEN}{current}/{maximum}{RESET}"
