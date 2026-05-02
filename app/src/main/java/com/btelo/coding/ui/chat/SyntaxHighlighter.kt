package com.btelo.coding.ui.chat

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString

// Syntax colors (GitHub Dark theme)
val SyntaxKeyword = Color(0xFFFF7B72)
val SyntaxString = Color(0xFFA5D6FF)
val SyntaxComment = Color(0xFF8B949E)
val SyntaxNumber = Color(0xFF79C0FF)
val SyntaxType = Color(0xFFFFA657)
val SyntaxFunction = Color(0xFFD2A8FF)
val SyntaxProperty = Color(0xFF7EE787)

object SyntaxHighlighter {

    fun highlight(code: String, language: String): AnnotatedString {
        if (code.isBlank()) return AnnotatedString(code)

        val tokens = tokenize(code, language.lowercase().trim())
        return buildAnnotatedString {
            append(code)
            tokens.forEach { token ->
                if (token.type != TokenType.PLAIN) {
                    addStyle(
                        SpanStyle(color = token.type.color),
                        token.start,
                        token.end
                    )
                }
            }
        }
    }

    private data class Token(val start: Int, val end: Int, val type: TokenType)

    private enum class TokenType(val color: Color) {
        COMMENT(SyntaxComment),
        STRING(SyntaxString),
        KEYWORD(SyntaxKeyword),
        NUMBER(SyntaxNumber),
        TYPE(SyntaxType),
        FUNCTION(SyntaxFunction),
        PLAIN(Color.Unspecified)
    }

    private fun tokenize(code: String, language: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0

        while (i < code.length) {
            val matched = tryMatchToken(code, i, language)
            if (matched != null) {
                tokens.add(matched)
                i = matched.end
            } else {
                i++
            }
        }
        return tokens
    }

    private fun tryMatchToken(code: String, pos: Int, language: String): Token? {
        // Comments (highest priority)
        matchLineComment(code, pos, language)?.let { return it }
        matchBlockComment(code, pos, language)?.let { return it }

        // Strings
        matchString(code, pos)?.let { return it }
        matchTemplateString(code, pos)?.let { return it }

        // Numbers
        matchNumber(code, pos)?.let { return it }

        // Keywords and identifiers
        matchKeywordOrIdentifier(code, pos, language)?.let { return it }

        return null
    }

    private fun matchLineComment(code: String, pos: Int, language: String): Token? {
        val prefix = when (language) {
            "kotlin", "java", "javascript", "js", "typescript", "ts", "go", "rust", "c", "cpp", "c++", "swift", "dart" -> "//"
            "python", "py", "shell", "bash", "sh", "ruby", "rb", "yaml", "yml", "toml", "r" -> "#"
            "sql" -> "--"
            "html", "xml", "css" -> return null // no single-line comment
            else -> "//"
        }
        if (code.startsWith(prefix, pos)) {
            val end = code.indexOf('\n', pos).let { if (it == -1) code.length else it }
            return Token(pos, end, TokenType.COMMENT)
        }
        return null
    }

    private fun matchBlockComment(code: String, pos: Int, language: String): Token? {
        if (language in listOf("html", "xml") && code.startsWith("<!--", pos)) {
            val end = code.indexOf("-->", pos + 4).let { if (it == -1) code.length else it + 3 }
            return Token(pos, end, TokenType.COMMENT)
        }
        if (code.startsWith("/*", pos)) {
            val end = code.indexOf("*/", pos + 2).let { if (it == -1) code.length else it + 2 }
            return Token(pos, end, TokenType.COMMENT)
        }
        return null
    }

    private fun matchString(code: String, pos: Int): Token? {
        val quote = code[pos]
        if (quote != '\'' && quote != '"') return null
        var i = pos + 1
        while (i < code.length) {
            if (code[i] == '\\') {
                i += 2
                continue
            }
            if (code[i] == quote) {
                return Token(pos, i + 1, TokenType.STRING)
            }
            if (code[i] == '\n') break
            i++
        }
        return null
    }

    private fun matchTemplateString(code: String, pos: Int): Token? {
        if (code[pos] != '`') return null
        var i = pos + 1
        while (i < code.length) {
            if (code[i] == '\\') {
                i += 2
                continue
            }
            if (code[i] == '`') {
                return Token(pos, i + 1, TokenType.STRING)
            }
            i++
        }
        return null
    }

    private fun matchNumber(code: String, pos: Int): Token? {
        if (!code[pos].isDigit()) return null
        // Check it's not part of an identifier
        if (pos > 0 && (code[pos - 1].isLetterOrDigit() || code[pos - 1] == '_')) return null

        var i = pos
        while (i < code.length && (code[i].isDigit() || code[i] in "._xXabcdefABCDEF")) {
            i++
        }
        // Include suffix like f, L, u, ul
        while (i < code.length && code[i].lowercaseChar() in "ful") {
            i++
        }
        return Token(pos, i, TokenType.NUMBER)
    }

    private fun matchKeywordOrIdentifier(code: String, pos: Int, language: String): Token? {
        if (!code[pos].isLetter() && code[pos] != '_') return null
        // Check it's not part of a larger identifier
        if (pos > 0 && (code[pos - 1].isLetterOrDigit() || code[pos - 1] == '_')) return null

        var i = pos
        while (i < code.length && (code[i].isLetterOrDigit() || code[i] == '_')) {
            i++
        }
        val word = code.substring(pos, i)

        val type = when {
            word in getKeywords(language) -> TokenType.KEYWORD
            word in getTypes(language) -> TokenType.TYPE
            i < code.length && code[i] == '(' -> TokenType.FUNCTION
            else -> TokenType.PLAIN
        }

        return if (type != TokenType.PLAIN) Token(pos, i, type) else null
    }

    private fun getKeywords(language: String): Set<String> = when (language) {
        "kotlin" -> setOf(
            "as", "break", "class", "continue", "do", "else", "false", "for", "fun",
            "if", "in", "interface", "is", "null", "object", "package", "return", "super",
            "this", "throw", "true", "try", "typealias", "typeof", "val", "var", "when", "while"
        )
        "java" -> setOf(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
            "class", "continue", "default", "do", "double", "else", "enum", "extends",
            "final", "finally", "float", "for", "if", "implements", "import", "instanceof",
            "int", "interface", "long", "native", "new", "null", "package", "private",
            "protected", "public", "return", "short", "static", "strictfp", "super",
            "switch", "synchronized", "this", "throw", "throws", "transient", "true",
            "try", "void", "volatile", "while"
        )
        "javascript", "js", "typescript", "ts" -> setOf(
            "async", "await", "break", "case", "catch", "class", "const", "continue",
            "debugger", "default", "delete", "do", "else", "enum", "export", "extends",
            "false", "finally", "for", "from", "function", "if", "import", "in",
            "instanceof", "let", "new", "null", "of", "return", "static", "super",
            "switch", "this", "throw", "true", "try", "typeof", "undefined", "var",
            "void", "while", "with", "yield"
        )
        "python", "py" -> setOf(
            "and", "as", "assert", "async", "await", "break", "class", "continue",
            "def", "del", "elif", "else", "except", "False", "finally", "for", "from",
            "global", "if", "import", "in", "is", "lambda", "None", "nonlocal", "not",
            "or", "pass", "raise", "return", "True", "try", "while", "with", "yield"
        )
        "go" -> setOf(
            "break", "case", "chan", "const", "continue", "default", "defer", "do",
            "else", "fallthrough", "for", "func", "go", "goto", "if", "import",
            "interface", "map", "package", "range", "return", "select", "struct",
            "switch", "type", "var"
        )
        "rust" -> setOf(
            "as", "async", "await", "break", "const", "continue", "crate", "dyn",
            "else", "enum", "extern", "false", "fn", "for", "if", "impl", "in",
            "let", "loop", "match", "mod", "move", "mut", "pub", "ref", "return",
            "self", "static", "struct", "super", "trait", "true", "type", "unsafe",
            "use", "where", "while", "yield"
        )
        "c", "cpp", "c++" -> setOf(
            "auto", "break", "case", "char", "const", "continue", "default", "do",
            "double", "else", "enum", "extern", "float", "for", "goto", "if",
            "inline", "int", "long", "register", "restrict", "return", "short",
            "signed", "sizeof", "static", "struct", "switch", "typedef", "union",
            "unsigned", "void", "volatile", "while"
        )
        "swift" -> setOf(
            "as", "associatedtype", "break", "case", "catch", "class", "continue",
            "convenience", "default", "defer", "deinit", "do", "else", "enum",
            "extension", "fallthrough", "false", "fileprivate", "final", "for",
            "func", "guard", "if", "import", "in", "indirect", "init", "inout",
            "internal", "is", "lazy", "let", "mutating", "nil", "none", "open",
            "operator", "optional", "override", "private", "protocol", "public",
            "repeat", "required", "rethrows", "return", "self", "static", "struct",
            "subscript", "super", "switch", "throw", "throws", "true", "try",
            "typealias", "unowned", "var", "weak", "where", "while", "willSet"
        )
        "shell", "bash", "sh" -> setOf(
            "if", "then", "else", "elif", "fi", "case", "esac", "for", "while",
            "until", "do", "done", "in", "function", "select", "time", "coproc",
            "return", "exit", "export", "readonly", "declare", "local", "typeset",
            "unset", "shift", "source", "alias", "bg", "fg", "jobs", "kill",
            "wait", "suspend", "continue", "break", "trap", "set", "unset",
            "shopt", "true", "false"
        )
        "sql" -> setOf(
            "SELECT", "FROM", "WHERE", "AND", "OR", "NOT", "IN", "BETWEEN",
            "LIKE", "IS", "NULL", "INSERT", "INTO", "VALUES", "UPDATE", "SET",
            "DELETE", "CREATE", "TABLE", "ALTER", "DROP", "INDEX", "VIEW",
            "JOIN", "LEFT", "RIGHT", "INNER", "OUTER", "ON", "AS", "GROUP",
            "BY", "ORDER", "ASC", "DESC", "HAVING", "LIMIT", "OFFSET",
            "UNION", "ALL", "DISTINCT", "EXISTS", "CASE", "WHEN", "THEN",
            "ELSE", "END", "BEGIN", "COMMIT", "ROLLBACK", "GRANT", "REVOKE"
        )
        else -> setOf(
            "if", "else", "for", "while", "do", "return", "function", "class",
            "import", "export", "from", "const", "let", "var", "true", "false",
            "null", "undefined", "new", "this", "super", "try", "catch",
            "finally", "throw", "switch", "case", "break", "continue", "default"
        )
    }

    private fun getTypes(language: String): Set<String> = when (language) {
        "kotlin" -> setOf(
            "Any", "Boolean", "Byte", "Char", "Double", "Float", "Int", "Long",
            "Nothing", "Short", "String", "Unit", "Array", "List", "Map", "Set",
            "MutableList", "MutableMap", "MutableSet", "Pair", "Triple",
            "Sequence", "Iterable", "Iterator", "Collection"
        )
        "java" -> setOf(
            "Boolean", "Byte", "Character", "Class", "Double", "Enum", "Float",
            "Integer", "Long", "Number", "Object", "Short", "String", "Void",
            "ArrayList", "HashMap", "HashSet", "LinkedList", "List", "Map", "Set"
        )
        "javascript", "js", "typescript", "ts" -> setOf(
            "Array", "Boolean", "Date", "Error", "Function", "Map", "Number",
            "Object", "Promise", "RegExp", "Set", "String", "Symbol", "WeakMap",
            "WeakSet", "Promise", "ArrayBuffer", "DataView", "Float32Array",
            "Float64Array", "Int8Array", "Int16Array", "Int32Array", "Uint8Array",
            "Uint16Array", "Uint32Array"
        )
        "python", "py" -> setOf(
            "bool", "int", "float", "str", "list", "dict", "set", "tuple",
            "bytes", "bytearray", "complex", "frozenset", "type", "object",
            "Exception", "ValueError", "TypeError", "KeyError", "IndexError",
            "AttributeError", "ImportError", "RuntimeError", "StopIteration"
        )
        "go" -> setOf(
            "bool", "byte", "complex64", "complex128", "error", "float32",
            "float64", "int", "int8", "int16", "int32", "int64", "rune",
            "string", "uint", "uint8", "uint16", "uint32", "uint64", "uintptr"
        )
        "rust" -> setOf(
            "bool", "char", "f32", "f64", "i8", "i16", "i32", "i64", "i128",
            "isize", "str", "u8", "u16", "u32", "u64", "u128", "usize",
            "String", "Vec", "Box", "Rc", "Arc", "Option", "Result", "HashMap",
            "HashSet", "BTreeMap", "BTreeSet"
        )
        "c", "cpp", "c++" -> setOf(
            "bool", "char", "double", "float", "int", "long", "short", "signed",
            "unsigned", "void", "wchar_t", "size_t", "ptrdiff_t", "nullptr_t",
            "string", "vector", "map", "set", "list", "queue", "stack",
            "deque", "array", "pair", "tuple", "shared_ptr", "unique_ptr"
        )
        "swift" -> setOf(
            "Bool", "Int", "Int8", "Int16", "Int32", "Int64", "UInt", "UInt8",
            "UInt16", "UInt32", "UInt64", "Float", "Float32", "Float64",
            "Double", "Character", "String", "Array", "Dictionary", "Set",
            "Optional", "Error", "Result", "Any", "AnyObject", "Self"
        )
        else -> emptySet()
    }
}
