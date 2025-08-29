import re
from pathlib import Path


def strip_non_javadoc_comments(text: str) -> str:
    out = []
    i = 0
    n = len(text)
    in_sl = False
    in_ml = False
    in_jd = False
    in_str = False
    in_chr = False
    # Track escape for strings/chars
    esc = False
    while i < n:
        ch = text[i]
        nxt = text[i+1] if i + 1 < n else ''

        if in_sl:
            # end at newline (preserve newline)
            if ch == '\n':
                out.append(ch)
                in_sl = False
            elif ch == '\r':
                out.append(ch)
                # keep CR; stay in single-line until LF ends too
            # else skip
            i += 1
            continue

        if in_ml:
            # end of block comment
            if ch == '*' and nxt == '/':
                in_ml = False
                i += 2
            else:
                i += 1
            continue

        if in_jd:
            out.append(ch)
            # end of javadoc
            if ch == '*' and nxt == '/':
                out.append('/')
                i += 2
                in_jd = False
                continue
            i += 1
            continue

        # not in any comment
        if in_str:
            out.append(ch)
            if ch == '"' and not esc:
                in_str = False
            esc = (ch == '\\' and not esc)
            if ch != '\\':
                esc = False
            i += 1
            continue

        if in_chr:
            out.append(ch)
            if ch == "'" and not esc:
                in_chr = False
            esc = (ch == '\\' and not esc)
            if ch != '\\':
                esc = False
            i += 1
            continue

        # potential start sequences
        if ch == '"':
            in_str = True
            out.append(ch)
            i += 1
            continue
        if ch == "'":
            in_chr = True
            out.append(ch)
            i += 1
            continue

        if ch == '/' and nxt == '/':
            in_sl = True
            # drop until newline
            i += 2
            continue
        if ch == '/' and nxt == '*':
            third = text[i+2] if i + 2 < n else ''
            if third == '*':
                # javadoc starts
                in_jd = True
                out.append('/')
                out.append('*')
                out.append('*')
                i += 3
                continue
            else:
                in_ml = True
                i += 2
                continue
        # default: copy
        out.append(ch)
        i += 1
    return ''.join(out)


_MOD_OR_ANN = r"(?:public|protected|private|static|final|native|synchronized|abstract|transient|strictfp|default|@\w+(?:\([^)]*\))?)"
_TYPE = r"(?:[\w$\[\].<>?]+)"


def _nearest_class_name(lines, idx):
    # Scan upward for class/record/enum/interface name
    class_re = re.compile(r"\b(class|record|enum|interface)\s+([A-Za-z_][\w$]*)")
    for j in range(idx, -1, -1):
        m = class_re.search(lines[j])
        if m:
            return m.group(2)
    return None


def _is_annotation_line(s: str) -> bool:
    return bool(re.match(r"^\s*@", s))


def _has_javadoc_above(lines, start_idx):
    # Walk up through blank/annotation lines; if doc block exists immediately above, return True
    j = start_idx - 1
    while j >= 0 and (lines[j].strip() == '' or _is_annotation_line(lines[j])):
        j -= 1
    if j >= 0 and lines[j].strip().endswith('*/'):
        # walk back to find opening '/**'
        k = j
        while k >= 0 and '/*' not in lines[k]:
            k -= 1
        if k >= 0 and '/**' in lines[k]:
            return True
    return False


def _split_params(param_str: str):
    params = []
    depth = 0  # for <>
    cur = []
    i = 0
    while i < len(param_str):
        ch = param_str[i]
        if ch == '<':
            depth += 1
        elif ch == '>':
            depth = max(0, depth - 1)
        elif ch == ',' and depth == 0:
            params.append(''.join(cur).strip())
            cur = []
            i += 1
            continue
        cur.append(ch)
        i += 1
    last = ''.join(cur).strip()
    if last:
        params.append(last)
    return [p for p in params if p]


def _param_name_from_decl(decl: str) -> str | None:
    # Remove annotations and modifiers
    decl = re.sub(r"@\w+(?:\([^)]*\))?", " ", decl)
    decl = re.sub(r"\b(final|var)\b", " ", decl)
    decl = decl.replace("...", " ")
    # Name is last identifier
    m = re.search(r"([A-Za-z_][\w$]*)\s*$", decl)
    return m.group(1) if m else None


def _extract_throws(header: str):
    m = re.search(r"\bthrows\s+([^\{;]+)", header)
    if not m:
        return []
    parts = [p.strip() for p in m.group(1).split(',')]
    return [re.sub(r"\s+", " ", p) for p in parts if p]


def add_javadoc_to_methods(text: str) -> str:
    lines = text.splitlines(True)
    out_lines = list(lines)

    i = 0
    while i < len(out_lines):
        # Skip lines that don't look like the start of a declaration
        cur = out_lines[i]
        if '(' not in cur:
            i += 1
            continue
        if cur.lstrip().startswith('@'):
            i += 1
            continue

        # Skip obvious non-declarations
        first_token = re.match(r"^\s*([A-Za-z_][\w$]*)", cur)
        if first_token and first_token.group(1) in {"if", "for", "while", "switch", "catch", "new", "return"}:
            i += 1
            continue

        # accumulate until declaration end
        header = out_lines[i]
        paren = header.count('(') - header.count(')')
        j = i + 1
        while paren > 0 and j < len(out_lines):
            header += out_lines[j]
            paren += out_lines[j].count('(') - out_lines[j].count(')')
            j += 1
        # extend to include up to first '{' or ';' after )
        k = j
        while k < len(out_lines):
            header += out_lines[k]
            if '{' in out_lines[k] or ';' in out_lines[k]:
                k += 1
                break
            k += 1

        flat = re.sub(r"\s+", " ", header)

        # Identify method name as token before first '(' in header
        pre_paren = header.split('(')[0]
        name_match = re.search(r"([A-Za-z_][\w$]*)\s*$", pre_paren)
        if not name_match:
            i = j
            continue
        name = name_match.group(1)

        # Heuristic filters to reduce false positives
        if name in {"if", "for", "while", "switch", "catch", "return"}:
            i = j
            continue
        if '=' in pre_paren:
            i = j
            continue
        # Avoid annotation usages and chained calls like obj.method(
        before_name = pre_paren[: pre_paren.rfind(name)] if name in pre_paren else pre_paren
        if before_name.rstrip().endswith('.'):
            i = j
            continue

        # Must look like a declaration: has a modifier keyword OR a return type before the name
        has_modifier = re.search(r"\b(public|protected|private|static|final|abstract|synchronized|native|strictfp|default)\b", pre_paren) is not None
        # crude return type detection
        type_and_name = re.search(r"([\w$\[\].<>?]+)\s+" + re.escape(name) + r"\s*$", pre_paren)
        has_return_type = False
        if type_and_name:
            tkn = type_and_name.group(1)
            if tkn != name:  # avoid bare calls
                has_return_type = True
        if not has_modifier and not has_return_type:
            # likely a call, not a declaration
            i = j
            continue

        # extract params
        inside = header[header.find('(') + 1: header.find(')')]
        params = _split_params(inside)
        param_names = []
        for p in params:
            n = _param_name_from_decl(p)
            if n:
                param_names.append(n)

        throws_list = _extract_throws(flat)

        # Determine constructor vs method
        cls = _nearest_class_name(out_lines, i)
        is_ctor = (cls == name)

        # Check if Javadoc already present above annotations
        insert_at = i
        while insert_at > 0 and _is_annotation_line(out_lines[insert_at - 1].strip()):
            insert_at -= 1

        if _has_javadoc_above(out_lines, insert_at):
            i = j
            continue

        # Determine return type (void or not)
        returns_void = False
        if not is_ctor:
            # Check 'void' before name in the header
            before_name = pre_paren
            if re.search(r"\bvoid\b", before_name):
                returns_void = True

        indent = re.match(r"^\s*", out_lines[insert_at]).group(0) if insert_at < len(out_lines) else ""

        doc_lines = [indent + "/**\n"]
        if is_ctor:
            doc_lines.append(indent + " * Constructs a new instance.\n")
        else:
            doc_lines.append(indent + " * TODO: describe method.\n")
        if param_names:
            doc_lines.append(indent + " *\n")
            for p in param_names:
                doc_lines.append(indent + f" * @param {p} description\n")
        if not is_ctor and not returns_void:
            doc_lines.append(indent + " * @return description\n")
        for th in throws_list:
            doc_lines.append(indent + f" * @throws {th} description\n")
        doc_lines.append(indent + " */\n")

        out_lines[insert_at:insert_at] = doc_lines
        # Advance i past inserted doc and header we processed
        i = j + len(doc_lines)

    return ''.join(out_lines)


def _is_decl_start(line: str) -> bool:
    s = line.lstrip()
    if not s:
        return False
    # class or interface/enum/record
    if re.match(r"^(class|interface|enum|record)\b", s):
        return True
    # modifiers
    if re.match(r"^(public|protected|private|static|final|abstract|synchronized|native|strictfp|default)\b", s):
        return True
    # method with return type (package-private)
    if re.match(r"^[A-Za-z_][\w$\[\].<>?]*\s+[A-Za-z_][\w$]*\s*\(", s):
        return True
    return False


def prune_orphan_javadocs(text: str) -> str:
    lines = text.splitlines(True)
    i = 0
    out = []
    while i < len(lines):
        line = lines[i]
        stripped = line.lstrip()
        # Drop dangling lines that look like part of a removed Javadoc block
        if stripped.startswith('*') and (not out or not out[-1].rstrip().endswith('/**')):
            # consume until a line containing '*/' (inclusive)
            while i < len(lines):
                if '*/' in lines[i]:
                    i += 1
                    break
                i += 1
            continue
        if stripped.startswith('/**'):
            # capture doc block
            start = i
            end = i
            i += 1
            while i < len(lines):
                end = i
                if '*/' in lines[i]:
                    i += 1
                    break
                i += 1
            # look ahead for next non-blank, skipping annotation lines
            k = i
            while k < len(lines) and lines[k].strip() == '':
                k += 1
            while k < len(lines) and lines[k].lstrip().startswith('@'):
                k += 1
            if k < len(lines) and _is_decl_start(lines[k]):
                # If this looks like placeholder method-doc but precedes a class decl, drop it
                doc_text = ''.join(lines[start:end+1])
                is_placeholder = 'TODO: describe method.' in doc_text
                is_class_decl = bool(re.match(r"^\s*(?:[\w\s]*?)?(class|interface|enum|record)\b", lines[k].lstrip()))
                if is_placeholder and is_class_decl:
                    # drop it
                    pass
                else:
                    out.extend(lines[start:end+1])
            # else: drop it (orphan)
            continue
        else:
            out.append(line)
            i += 1
    return ''.join(out)


def process_file(path: Path):
    src = path.read_text(encoding='utf-8', errors='ignore')
    no_rand = strip_non_javadoc_comments(src)
    no_rand = prune_orphan_javadocs(no_rand)
    with_docs = add_javadoc_to_methods(no_rand)
    if with_docs != src:
        path.write_text(with_docs, encoding='utf-8')


def main():
    root = Path('src/main/java')
    if not root.exists():
        print('No src/main/java found')
        return
    for p in root.rglob('*.java'):
        process_file(p)


if __name__ == '__main__':
    main()
