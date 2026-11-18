#!/usr/bin/env python3
"""Generate a Java-friendly pumpkin.Event stub from Pumpkin's event.wit."""
from __future__ import annotations

import re
import sys
from pathlib import Path

WIT = Path(sys.argv[1]) if len(sys.argv) > 1 else Path("pumpkin-api-kt/wit/v0.1/event.wit")
OUT = Path(sys.argv[2]) if len(sys.argv) > 2 else Path("pumpkin-stub/src/main/java/pumpkin/Event.java")

text = WIT.read_text()


def pascal(value: str) -> str:
    value = value.replace("%", "")
    return "".join(part[:1].upper() + part[1:] for part in value.split("-"))


def camel(value: str) -> str:
    value = value.replace("%", "")
    p = pascal(value)
    return p[:1].lower() + p[1:]


def snake(value: str) -> str:
    return value.replace("-", "_").upper()


def block(name: str) -> str:
    match = re.search(rf"{name}\s*\{{(.*?)\n    \}}", text, re.S)
    return match.group(1) if match else ""


def split_top(value: str) -> list[str]:
    parts, depth, current = [], 0, ""
    for ch in value:
        if ch == "<":
            depth += 1
        elif ch == ">":
            depth -= 1
        if ch == "," and depth == 0:
            parts.append(current.strip())
            current = ""
        else:
            current += ch
    if current.strip():
        parts.append(current.strip())
    return parts


def java_type(wit: str) -> str:
    wit = wit.strip()
    if wit.startswith("list<"):
        return "java.util.List<" + boxed(java_type(wit[5:-1])) + ">"
    if wit.startswith("option<"):
        return boxed(java_type(wit[7:-1]))
    if wit.startswith("tuple<"):
        inner = split_top(wit[6:-1])
        if len(inner) == 3 and all(x.strip() in ("f64", "f32") for x in inner):
            return "Position"
        return "Object"
    base = {
        "string": "String", "bool": "boolean",
        "u8": "int", "u16": "int", "u32": "int", "u64": "long",
        "s8": "int", "s16": "int", "s32": "int", "s64": "long",
        "f32": "float", "f64": "double",
        "player": "Player", "%world": "World", "world": "World",
        "text-component": "TextComponent", "position": "Position",
        "block-pos": "BlockPos", "item-stack": "ItemStack",
        "uuid": "Uuid", "game-mode": "Common.GameMode",
        "hand": "Common.Hand", "click-type": "Object",
        "enchantment": "Object", "damage-type": "Object",
        "screen": "Object", "dialog": "Object",
        "java-serverbound-packet": "Object", "java-clientbound-packet": "Object",
        "bedrock-serverbound-packet": "Object", "bedrock-clientbound-packet": "Object",
    }.get(wit)
    if base:
        return base
    if wit.endswith("-data") or wit.endswith("-event") or wit.endswith("-offer"):
        return "Object"
    return "Object"


def boxed(value: str) -> str:
    return {"boolean": "Boolean", "int": "Integer", "long": "Long", "float": "Float", "double": "Double"}.get(value, value)


records_block = block("record player-join-event-data")  # noqa: F841  (keeps regex anchored)
record_matches = list(re.finditer(r"record ([\w-]+) \{(.*?)\n    \}", text, re.S))
records = {}
for match in record_matches:
    fields = []
    for line in match.group(2).splitlines():
        line = line.strip()
        if not line or line.startswith("///"):
            continue
        if ":" not in line:
            continue
        name, type_ = line.split(":", 1)
        type_ = type_.strip().rstrip(",")
        fields.append((camel(name.strip()), java_type(type_), type_))
    records[match.group(1)] = fields

variant_match = re.search(r"variant event \{(.*?)\n    \}", text, re.S)
variant_body = variant_match.group(1) if variant_match else ""
variants = []
for line in variant_body.splitlines():
    line = line.strip().rstrip(",")
    if not line:
        continue
    m = re.match(r"([\w-]+)(?:\(([\w-]+)\))?", line)
    if m:
        variants.append((pascal(m.group(1)), m.group(2)))

type_body = block("enum event-type")
event_types = [snake(x.strip().rstrip(",")) for x in type_body.splitlines() if x.strip() and not x.strip().startswith("///")]
priority_body = block("enum event-priority")
priorities = [snake(x.strip().rstrip(",")) for x in priority_body.splitlines() if x.strip() and not x.strip().startswith("///")]

lines = [
    "package pumpkin;",
    "",
    "import java.util.List;",
    "",
    "/**",
    " * Java-friendly mirror of Pumpkin's generated event API.",
    " * Generated from event.wit; do not edit by hand.",
    " */",
    "public class Event {",
    "    public enum EventPriority { " + ", ".join(priorities) + " }",
    "    public enum EventType { " + ", ".join(event_types) + " }",
    "",
    "    public boolean isCancelled() { return false; }",
    "    public void setCancelled(boolean cancelled) {}",
    "",
]

for name, fields in records.items():
    cls = pascal(name)
    lines.append(f"    public static class {cls} {{")
    for field, jtype, _ in fields:
        lines.append(f"        private {jtype} {field};")
    lines.append(f"        public {cls}() {{")
    lines.append("        }")
    lines.append(f"        public {cls}(" + ", ".join(f"{jtype} {field}" for field, jtype, _ in fields) + ") {")
    for field, _, _ in fields:
        lines.append(f"            this.{field} = {field};")
    lines.append("        }")
    for field, jtype, _ in fields:
        cap = field[:1].upper() + field[1:]
        prefix = "is" if jtype == "boolean" else "get"
        lines.append(f"        public {jtype} {prefix}{cap}() {{ return {field}; }}")
        lines.append(f"        public void set{cap}({jtype} value) {{ this.{field} = value; }}")
    lines.append("    }")
    lines.append("")

for variant, data_name in variants:
    data_cls = pascal(data_name) if data_name else None
    lines.append(f"    public static final class {variant} extends Event {{")
    if data_cls:
        lines.append(f"        private final {data_cls} value;")
        lines.append(f"        public {variant}({data_cls} value) {{ this.value = value; }}")
        lines.append(f"        public {data_cls} getValue() {{ return value; }}")
        fields = records.get(data_name, [])
        for field, jtype, _ in fields:
            cap = field[:1].upper() + field[1:]
            prefix = "is" if jtype == "boolean" else "get"
            lines.append(f"        public {jtype} {prefix}{cap}() {{ return value.{prefix}{cap}(); }}")
            if field == "cancelled":
                lines.append(f"        public void set{cap}({jtype} value) {{ this.value.set{cap}(value); }}")
    else:
        lines.append("        public " + variant + "() { }")
    lines.append("    }")
    lines.append("")

lines.append("}")
OUT.parent.mkdir(parents=True, exist_ok=True)
OUT.write_text("\n".join(lines) + "\n")
print(f"generated {OUT} with {len(records)} records and {len(variants)} variants")
