# This is a Skylark/Java adaptation of http://www.json.org/JSON_checker/ a
# push-down automaton originally design to check JSON syntax, attempting to be
# extended to a full JSON parser.
#
# Unfortunately due to several reasons there are severe limitations for this
# parser, and I do not recommend this for anything serious.
#
# Due to the (IMO) original awkward grammar of JSON_checker (namely the root
# object is of a different mode than nested objects, no mode pushed on the stack
# for object key/value entries (instead a pop/push cycle happens around
# colons)... this grammar has been hacked to make reduction rules possible, but
# it's all still quite hacky.
#
# Also due to Java/Skylark limitations on unicode character support, any parse
# attempt is a best effort: https://github.com/bazelbuild/bazel/issues/4862
#
# I suggest reading the implementation at
# http://www.json.org/JSON_checker/JSON_checker.c to get an idea of what's
# happening here, the formatting there is much easier to read.
#
# See `json_parse` to get started.

# TODO: (lots, this is non-exhaustive)
# - Give names to these actions instead of negative numbers
# - More descriptive errors, currently the "invalid action -1" error
#   is extremely unhelpful.

def _enumify_iterable(iterable, enum_dict):
    """A hacky function to turn an iterable into a dict with whose keys are the
    members of the iterable, and value is the index."""
    for i, t in enumerate(iterable):
        enum_dict[t] = i
    return enum_dict

__ = -1  # Alias for the invalid class
_TOKEN_CLASSES = _enumify_iterable(iterable = [
    "C_SPACE",  # space
    "C_WHITE",  # other whitespace
    "C_LCURB",  # {
    "C_RCURB",  # }
    "C_LSQRB",  # [
    "C_RSQRB",  # ]
    "C_COLON",  # :
    "C_COMMA",  # ,
    "C_QUOTE",  # "
    "C_BACKS",  # \
    "C_SLASH",  # /
    "C_PLUS",  # +
    "C_MINUS",  # -
    "C_POINT",  # .
    "C_ZERO",  # 0
    "C_DIGIT",  # 123456789
    "C_LOW_A",  # a
    "C_LOW_B",  # b
    "C_LOW_C",  # c
    "C_LOW_D",  # d
    "C_LOW_E",  # e
    "C_LOW_F",  # f
    "C_LOW_L",  # l
    "C_LOW_N",  # n
    "C_LOW_R",  # r
    "C_LOW_S",  # s
    "C_LOW_T",  # t
    "C_LOW_U",  # u
    "C_ABCDF",  # ABCDF
    "C_E",  # E
    "C_ETC",  # everything else
], enum_dict = {"__": __})

_STATE_MAP = {
    "GO": "start",
    "OK": "ok",
    "OB": "object",
    "KE": "key",
    "CO": "colon",
    "VA": "value",
    "AR": "array",
    "ST": "string",
    "ES": "escape",
    "U1": "u1",
    "U2": "u2",
    "U3": "u3",
    "U4": "u4",
    "MI": "minus",
    "ZE": "zero",
    "IN": "integer",
    "FR": "fraction",
    "E1": "e",
    "E2": "ex",
    "E3": "exp",
    "T1": "tr",
    "T2": "tru",
    "T3": "true",
    "F1": "fa",
    "F2": "fal",
    "F3": "fals",
    "F4": "false",
    "N1": "nu",
    "N2": "nul",
    "N3": "null",
}

_STATES = _enumify_iterable(iterable = _STATE_MAP.keys(), enum_dict = {})
_S = _STATES  # A short alias

# Tread some states as another for tokenizing. This is a hack to avoid
# introducing new modes for parsing things like escaped string,
# negative numbers, exponents, floats, etc...
_TOKENIZER_STATE = {
    _S["ES"]: _S["ST"],
    _S["MI"]: _S["IN"],
    _S["FR"]: _S["IN"],
    _S["E1"]: _S["IN"],
    _S["E2"]: _S["IN"],
    _S["E3"]: _S["IN"],
}

_STATE_NAMES = _STATE_MAP.values()

# Used for debugging and reduction hook names

_MODE_NAMES = [
    "EMPTY",
    "ARRAY",
    "OBJECT",
    "ENTRY_KEY",
    "ENTRY_VALUE",
]

_MODES = _enumify_iterable(iterable = _MODE_NAMES, enum_dict = {})

_ASCII_CODEPOINT_MAP = {
    # Currenlty non-printable ascii characters are simply not referencable in
    # Java skylark
    # https://github.com/bazelbuild/bazel/issues/4862
    #
    # For now these characters just do not exist in the map, this means unicode
    # cannot be supported.

    # "\x00" : 0, "\x01" : 1, "\x02" : 2, "\x03" : 3, "\x04" : 4, "\x05" : 5, "\x06" : 6, "\a" : 7,
    # "\b" : 8, "\t" : 9, "\n" : 10, "\v" : 11, "\f" : 12, "\r" : 13, "\x0E" : 14, "\x0F" : 15,
    # "\x10" : 16, "\x11" : 17, "\x12" : 18, "\x13" : 19, "\x14" : 20, "\x15" : 21, "\x16" : 22, "\x17" : 23,
    # "\x18" : 24, "\x19" : 25, "\x1A" : 26, "\e" : 27, "\x1C" : 28, "\x1D" : 29, "\x1E" : 30, "\x1F" : 31,
    "\t": 9,
    "\n": 10,
    "\r": 13,
    " ": 32,
    "!": 33,
    "\"": 34,
    "#": 35,
    "$": 36,
    "%": 37,
    "&": 38,
    "'": 39,
    "(": 40,
    ")": 41,
    "*": 42,
    "+": 43,
    ",": 44,
    "-": 45,
    ".": 46,
    "/": 47,
    "0": 48,
    "1": 49,
    "2": 50,
    "3": 51,
    "4": 52,
    "5": 53,
    "6": 54,
    "7": 55,
    "8": 56,
    "9": 57,
    ":": 58,
    ";": 59,
    "<": 60,
    "=": 61,
    ">": 62,
    "?": 63,
    "@": 64,
    "A": 65,
    "B": 66,
    "C": 67,
    "D": 68,
    "E": 69,
    "F": 70,
    "G": 71,
    "H": 72,
    "I": 73,
    "J": 74,
    "K": 75,
    "L": 76,
    "M": 77,
    "N": 78,
    "O": 79,
    "P": 80,
    "Q": 81,
    "R": 82,
    "S": 83,
    "T": 84,
    "U": 85,
    "V": 86,
    "W": 87,
    "X": 88,
    "Y": 89,
    "Z": 90,
    "[": 91,
    "\\": 92,
    "]": 93,
    "^": 94,
    "_": 95,
    "`": 96,
    "a": 97,
    "b": 98,
    "c": 99,
    "d": 100,
    "e": 101,
    "f": 102,
    "g": 103,
    "h": 104,
    "i": 105,
    "j": 106,
    "k": 107,
    "l": 108,
    "m": 109,
    "n": 110,
    "o": 111,
    "p": 112,
    "q": 113,
    "r": 114,
    "s": 115,
    "t": 116,
    "u": 117,
    "v": 118,
    "w": 119,
    # Commented out for the same reason as above, given the backspace code, \x7F
    #    "x" : 120, "y" : 121, "z" : 122, "{" : 123, "|" : 124, "}" : 125, "~" : 126, "\x7F" : 127,
    "x": 120,
    "y": 121,
    "z": 122,
    "{": 123,
    "|": 124,
    "}": 125,
    "~": 126,
}

# This array maps the 128 ASCII characters into character classes.
# The remaining Unicode characters should be mapped to C_ETC.
# Non-whitespace control characters are errors.
def _create_ascii_mappings(positioned_token_list):
    ascii_mappings = []
    for token in positioned_token_list:
        ascii_mappings.append(_TOKEN_CLASSES[token])
    return ascii_mappings

_ASCII_CLASS_LIST = _create_ascii_mappings(positioned_token_list = [
    "__",
    "__",
    "__",
    "__",
    "__",
    "__",
    "__",
    "__",
    "__",
    "C_WHITE",
    "C_WHITE",
    "__",
    "__",
    "C_WHITE",
    "__",
    "__",
    "__",
    "__",
    "__",
    "__",
    "__",
    "__",
    "__",
    "__",
    "__",
    "__",
    "__",
    "__",
    "__",
    "__",
    "__",
    "__",
    "C_SPACE",
    "C_ETC",
    "C_QUOTE",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_PLUS",
    "C_COMMA",
    "C_MINUS",
    "C_POINT",
    "C_SLASH",
    "C_ZERO",
    "C_DIGIT",
    "C_DIGIT",
    "C_DIGIT",
    "C_DIGIT",
    "C_DIGIT",
    "C_DIGIT",
    "C_DIGIT",
    "C_DIGIT",
    "C_DIGIT",
    "C_COLON",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ABCDF",
    "C_ABCDF",
    "C_ABCDF",
    "C_ABCDF",
    "C_E",
    "C_ABCDF",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_LSQRB",
    "C_BACKS",
    "C_RSQRB",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_LOW_A",
    "C_LOW_B",
    "C_LOW_C",
    "C_LOW_D",
    "C_LOW_E",
    "C_LOW_F",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_LOW_L",
    "C_ETC",
    "C_LOW_N",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_LOW_R",
    "C_LOW_S",
    "C_LOW_T",
    "C_LOW_U",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_ETC",
    "C_LCURB",
    "C_ETC",
    "C_RCURB",
    "C_ETC",
    "C_ETC",
])

_STATE_TRANSITION_TABLE = [
    #   The state transition table takes the current state and the current symbol,
    #   and returns either a new state or an action. An action is represented as a
    #   negative number. A JSON text is accepted if at the end of the text the
    #   state is OK and if the mode is MODE_EMPTY.
    #
    #   See the table at http://www.json.org/JSON_checker/JSON_checker.c for better
    #   readability.
    #
    #   This one has been modified to simplify reductions.
    #
    #                  white                                      1-9                                   ABCDF  etc
    #       space        |  {  }  [  ]  :  ,  "  \  /  +  -  .  0  |  a  b  c  d  e  f  l  n  r  s  t  u  |  E  |
    [_S["GO"], _S["GO"], -6, __, -5, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __],  # start  GO
    [_S["OK"], _S["OK"], __, -8, __, -7, __, -3, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __],  # ok     OK
    [_S["OB"], _S["OB"], __, -9, __, __, __, __, _S["ST"], __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __],  # object OB
    [_S["KE"], _S["KE"], __, -9, __, __, __, __, -4, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __],  # key    KE
    [_S["CO"], _S["CO"], __, __, __, __, -2, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __],  # colon  CO
    [_S["VA"], _S["VA"], -6, __, -5, __, __, __, _S["ST"], __, __, __, _S["MI"], __, _S["ZE"], _S["IN"], __, __, __, __, __, _S["F1"], __, _S["N1"], __, __, _S["T1"], __, __, __, __],  # value  VA
    [_S["AR"], _S["AR"], -6, __, -5, -7, __, __, _S["ST"], __, __, __, _S["MI"], __, _S["ZE"], _S["IN"], __, __, __, __, __, _S["F1"], __, _S["N1"], __, __, _S["T1"], __, __, __, __],  # array  AR
    [_S["ST"], __, _S["ST"], _S["ST"], _S["ST"], _S["ST"], _S["ST"], _S["ST"], -4, _S["ES"], _S["ST"], _S["ST"], _S["ST"], _S["ST"], _S["ST"], _S["ST"], _S["ST"], _S["ST"], _S["ST"], _S["ST"], _S["ST"], _S["ST"], _S["ST"], _S["ST"], _S["ST"], _S["ST"], _S["ST"], _S["ST"], _S["ST"], _S["ST"], _S["ST"]],  # string ST
    [__, __, __, __, __, __, __, __, _S["ST"], _S["ST"], _S["ST"], __, __, __, __, __, __, _S["ST"], __, __, __, _S["ST"], __, _S["ST"], _S["ST"], __, _S["ST"], _S["U1"], __, __, __],  # escape ES
    [__, __, __, __, __, __, __, __, __, __, __, __, __, __, _S["U2"], _S["U2"], _S["U2"], _S["U2"], _S["U2"], _S["U2"], _S["U2"], _S["U2"], __, __, __, __, __, __, _S["U2"], _S["U2"], __],  # u1     U1
    [__, __, __, __, __, __, __, __, __, __, __, __, __, __, _S["U3"], _S["U3"], _S["U3"], _S["U3"], _S["U3"], _S["U3"], _S["U3"], _S["U3"], __, __, __, __, __, __, _S["U3"], _S["U3"], __],  # u2     U2
    [__, __, __, __, __, __, __, __, __, __, __, __, __, __, _S["U4"], _S["U4"], _S["U4"], _S["U4"], _S["U4"], _S["U4"], _S["U4"], _S["U4"], __, __, __, __, __, __, _S["U4"], _S["U4"], __],  # u3     U3
    [__, __, __, __, __, __, __, __, __, __, __, __, __, __, _S["ST"], _S["ST"], _S["ST"], _S["ST"], _S["ST"], _S["ST"], _S["ST"], _S["ST"], __, __, __, __, __, __, _S["ST"], _S["ST"], __],  # u4     U4
    [__, __, __, __, __, __, __, __, __, __, __, __, __, __, _S["ZE"], _S["IN"], __, __, __, __, __, __, __, __, __, __, __, __, __, __, __],  # minus  MI
    [_S["OK"], _S["OK"], __, -8, __, -7, __, -3, __, __, __, __, __, _S["FR"], __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __],  # zero   ZE
    [_S["OK"], _S["OK"], __, -8, __, -7, __, -3, __, __, __, __, __, _S["FR"], _S["IN"], _S["IN"], __, __, __, __, _S["E1"], __, __, __, __, __, __, __, __, _S["E1"], __],  # int    IN
    [_S["OK"], _S["OK"], __, -8, __, -7, __, -3, __, __, __, __, __, __, _S["FR"], _S["FR"], __, __, __, __, _S["E1"], __, __, __, __, __, __, __, __, _S["E1"], __],  # frac   FR
    [__, __, __, __, __, __, __, __, __, __, __, _S["E2"], _S["E2"], __, _S["E3"], _S["E3"], __, __, __, __, __, __, __, __, __, __, __, __, __, __, __],  # e      E1
    [__, __, __, __, __, __, __, __, __, __, __, __, __, __, _S["E3"], _S["E3"], __, __, __, __, __, __, __, __, __, __, __, __, __, __, __],  # ex     E2
    [_S["OK"], _S["OK"], __, -8, __, -7, __, -3, __, __, __, __, __, __, _S["E3"], _S["E3"], __, __, __, __, __, __, __, __, __, __, __, __, __, __, __],  # exp    E3
    [__, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, _S["T2"], __, __, __, __, __, __],  # tr     T1
    [__, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, _S["T3"], __, __, __],  # tru    T2
    [__, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, _S["OK"], __, __, __, __, __, __, __, __, __, __],  # true   T3
    [__, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, _S["F2"], __, __, __, __, __, __, __, __, __, __, __, __, __, __],  # fa     F1
    [__, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, _S["F3"], __, __, __, __, __, __, __, __],  # fal    F2
    [__, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, _S["F4"], __, __, __, __, __],  # fals   F3
    [__, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, _S["OK"], __, __, __, __, __, __, __, __, __, __],  # false  F4
    [__, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, _S["N2"], __, __, __],  # nu     N1
    [__, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, _S["N3"], __, __, __, __, __, __, __, __],  # nul    N2
    [__, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, __, _S["OK"], __, __, __, __, __, __, __, __],  # null   N3
]

_MAX_DEPTH = 20
_DEBUG = False

def _reject(checker, reason = "unknown reason"):
    if (checker["rejected"]):
        return False

    checker["rejected"] = True
    checker["rejected_reason"] = reason
    if (checker["_DEBUG"]):
        fail("failed to parse JSON: %s" % reason)
    return False

def _push(checker, mode):
    checker["top"] += 1
    if (checker["top"] > checker["max_depth"]):
        return _reject(checker, "max depth exceeded")

    checker["mode_stack"].insert(checker["top"], mode)
    checker["reduction_stack"].insert(checker["top"], [])

    if (checker["_DEBUG"]):
        print("push mode: %s" % [_MODE_NAMES[m] for m in checker["mode_stack"]])

def _pop(checker, mode):
    top = checker["top"]
    if (top < 0):
        return _reject(checker, "invalid top index")
    elif (not _peek_mode(checker) == mode):
        return _reject(
            checker,
            "cannot pop unexpected mode %s expected %s" % (mode, checker["mode_stack"][top]),
        )

    if (checker["_DEBUG"]):
        print("reducing " + _MODE_NAMES[mode])
    reduction = _reduce(checker)

    checker["reduction_stack"] = checker["reduction_stack"][0:top]
    checker["reduction_stack"][top - 1].append(reduction)

    checker["mode_stack"] = checker["mode_stack"][0:top]
    checker["top"] -= 1

    if (checker["_DEBUG"]):
        print("after pop token stack: %s" % checker["reduction_stack"])
        print("pop mode: %s" % [_MODE_NAMES[m] for m in checker["mode_stack"]])

def _set_state(checker, state):
    if (_get_tokenizer_state(state) != _get_tokenizer_state_from_checker(checker)):
        _tokenize(checker)

    if (checker["_DEBUG"]):
        print("set_state: %s" % _STATE_NAMES[state])
    checker["state"] = state

def _get_state(checker):
    return checker["state"]

def _get_tokenizer_state_from_checker(checker):
    return _get_tokenizer_state(checker["state"])

def _get_tokenizer_state(state):
    if state in _TOKENIZER_STATE:
        state = _TOKENIZER_STATE[state]
    return state

def _add_next_char_to_state(checker, next_char):
    if (checker["state_chars"] == None):
        checker["state_chars"] = ""
    checker["state_chars"] += next_char

def _get_reduction_list(checker):
    top = checker["top"]
    return checker["reduction_stack"][top]

def _tokenize(checker):
    if (checker["state_chars"] == None):
        return

    mode_name = _MODE_NAMES[_peek_mode(checker)]
    state = _get_tokenizer_state_from_checker(checker)
    state_name = _STATE_NAMES[state]
    chars = checker["state_chars"]

    if (state_name in checker["tokenizer_hooks"]):
        token = checker["tokenizer_hooks"][state_name](chars)
        if (checker["_DEBUG"]):
            print("tokenizing state '%s' with chars '%s'" % (state_name, chars))
        _get_reduction_list(checker).append({
            "mode": mode_name,
            "state": state_name,
            "reduction": token,
        })
    elif (checker["_DEBUG"]):
        print("no tokenizer for state '%s' with chars '%s'" % (state_name, chars))

    checker["state_chars"] = None

def _reduce(checker):
    _tokenize(checker)

    state = _get_state(checker)
    state_name = _STATE_NAMES[state]
    mode_name = _MODE_NAMES[_peek_mode(checker)]

    top = checker["top"]
    upstream_reductions = _get_reduction_list(checker)

    reducer_name = mode_name.lower()
    if (not reducer_name in checker["reduction_hooks"]):
        if (checker["_DEBUG"]):
            print("no reducer found for: %s" % (reducer_name))
        return upstream_reductions

    if (checker["_DEBUG"]):
        print("reduce_%s: %s" % (reducer_name, upstream_reductions))
    reduction = checker["reduction_hooks"][reducer_name](upstream_reductions)

    if (checker["_DEBUG"]):
        print("_reduce to: %s" % reduction)
    return {
        "mode": mode_name,
        "state": state_name,
        "reduction": reduction,
    }

def _peek_mode(checker):
    top = checker["top"]
    return checker["mode_stack"][top]

def _handle_next_char(checker, json_string, char_index):
    if (checker["rejected"]):
        return False

    next_json_char = json_string[char_index]
    if (checker["_DEBUG"]):
        print("handling char: %s (%d/%d): " % (next_json_char, char_index, len(json_string) - 1))

    next_class = None
    next_state = None
    next_char = None

    if (not next_json_char in _ASCII_CODEPOINT_MAP):
        if (not checker["hacky_treatment_unknown_chars_as_etc"]):
            return _reject(
                checker,
                "unable to map character: %s (%d/%d)" %
                (next_json_char, char_index, len(json_string) - 1),
            )
        else:
            print(
                "trying to treat unknown char as C_ETC...",
                "hoping for the best: char (%d/%d)" % (char_index, len(json_string) - 1),
            )
            next_char = 127  # my hardcoded hack for C_ETC resolution
    else:
        next_char = _ASCII_CODEPOINT_MAP[next_json_char]

    if (next_char == None):
        return _reject(checker, "unprintable Java/skylark char: %s" % next_json_char)

    next_class = _ASCII_CLASS_LIST[next_char]
    if (next_class <= __):
        return _reject(checker, "unknown character class for char: %s" % next_json_char)

    next_state = _STATE_TRANSITION_TABLE[checker["state"]][next_class]

    OK = _STATES["OK"]
    OB = _STATES["OB"]
    AR = _STATES["AR"]
    CO = _STATES["CO"]
    KE = _STATES["KE"]
    VA = _STATES["VA"]
    ST = _STATES["ST"]

    orig_state = checker["state"]
    orig_mode = _peek_mode(checker)

    if (next_state >= 0):
        _set_state(checker, next_state)
    else:
        if (checker["_DEBUG"]):
            print("action: %s" % next_state)

        if next_state == -9:  # empty }
            _pop(checker, _MODES["OBJECT"])
            _set_state(checker, OK)

        elif next_state == -8:  # }
            current_mode = _peek_mode(checker)
            if current_mode == _MODES["ENTRY_VALUE"]:
                _pop(checker, _MODES["ENTRY_VALUE"])

            _pop(checker, _MODES["OBJECT"])
            _set_state(checker, OK)

        elif next_state == -7:  # ]
            _pop(checker, _MODES["ARRAY"])
            _set_state(checker, OK)

        elif next_state == -6:  # {
            _push(checker, _MODES["OBJECT"])
            _set_state(checker, KE)

        elif next_state == -5:  # [
            _push(checker, _MODES["ARRAY"])
            _set_state(checker, AR)

        elif next_state == -4:  # "
            current_mode = _peek_mode(checker)
            if current_mode == _MODES["OBJECT"]:
                _push(checker, _MODES["ENTRY_KEY"])
                _set_state(checker, ST)
            elif current_mode == _MODES["ENTRY_KEY"]:
                _set_state(checker, CO)
            elif current_mode == _MODES["ENTRY_VALUE"]:
                _pop(checker, _MODES["ENTRY_VALUE"])
                _set_state(checker, OK)
            elif current_mode == _MODES["ARRAY"]:  # or current_mode == _MODES["OBJECT"]:
                _set_state(checker, OK)
            else:
                return _reject(checker, "invalid state transition from mode: %s" % current_mode)

        elif next_state == -3:  # ,
            current_mode = _peek_mode(checker)

            if current_mode == _MODES["ENTRY_VALUE"]:
                _pop(checker, _MODES["ENTRY_VALUE"])
                _set_state(checker, KE)

            elif current_mode == _MODES["OBJECT"]:
                _set_state(checker, KE)

            elif current_mode == _MODES["ARRAY"]:
                _set_state(checker, VA)

            else:
                return _reject(checker, "invalid state transition from mode: %s" % current_mode)

        elif next_state == -2:  # :
            current_mode = _peek_mode(checker)
            _pop(checker, _MODES["ENTRY_KEY"])
            _push(checker, _MODES["ENTRY_VALUE"])
            _set_state(checker, VA)

        else:
            first_char_index = char_index - 200 if char_index - 200 > 0 else 0
            last_char_index = char_index + 200
            return _reject(
                checker,
                "Could not parse the input:\n\n%s...\n\n at:\n\n%s...\n" %
                (json_string[char_index:char_index + 15], json_string[first_char_index:last_char_index].strip()),
            )

    _add_next_char_to_state(checker, next_json_char)

    return checker["rejected"]

def _verify_valid(checker):
    return (checker["rejected"] == False and
            checker["state"] == _STATES["OK"] and
            _peek_mode(checker) == _MODES["EMPTY"])

def _create_checker(
        max_depth = _MAX_DEPTH,
        debug = _DEBUG,
        tokenizer_hooks = {},
        reduction_hooks = {}):
    """Creates a new JSON checker, given a proper set of tokenizer_hooks and
    reduction_hooks, the checker can be extended into a parser.

    tokenizer_hooks - called on state changes
    reduction_hooks - called on mode exit

    """
    checker = {
        "rejected": False,
        "rejected_reason": None,
        "max_depth": max_depth,
        "mode_stack": [],
        "top": -1,
        "state": _STATES["GO"],
        "state_chars": None,  # Characters collected in the current state
        "reduction_stack": [],
        "reduction_hooks": reduction_hooks,
        "tokenizer_hooks": tokenizer_hooks,

        # This is a big frowny face. This hack will treat double byte
        # unicode characters at 2 separate chars. I /think/ this is
        # safe for the parse itself, but it probably just means that
        # the content will be f'd.
        "hacky_treatment_unknown_chars_as_etc": True,
        "_DEBUG": debug,
    }
    _push(checker, _MODES["EMPTY"])
    return checker

def _reduce_array(reductions):
    arr = []
    for i in range(0, len(reductions)):
        arr.append(reductions[i]["reduction"])
    return arr

def _reduce_object(reductions):
    obj = dict()
    for i in range(0, len(reductions) // 2):
        idx = i * 2
        key = reductions[idx]["reduction"]
        val = reductions[idx + 1]["reduction"]
        obj[key] = val
    return obj

def _reduce_literal(reductions):
    return reductions[0]["reduction"]

# https://docs.bazel.build/versions/master/skylark/lib/int.html
_MAX_INT = 2147483647
_MIN_INT = -2147483647

def _tokenize_int(collected_chars):
    # Drops precision due to no decimals in Skylark.
    #
    # https://tools.ietf.org/html/rfc8259#section-6
    # "This specification allows implementations to set limits on the range
    # and precision of numbers accepted."
    if collected_chars.lower().find("e") >= 0:
        print("crappily handling JSON exponents: %s" % collected_chars)
        sig, exp = collected_chars.split("e", 2)
        sign = "+"
        if exp[0] == "-":
            sign = "-"
            exp = exp[1:len(exp)]
        elif exp[0] == "+":
            exp = exp[1:len(exp)]

        sig = int(sig)
        for i in range(0, int(exp)):
            if sign == "+":
                if _MAX_INT // 10 <= sig:
                    return _MAX_INT
                elif _MIN_INT // 10 >= sig:
                    return _MIN_INT
                sig *= 10
            elif sign == "-":
                if sig < 0:
                    return 0
                sig //= 10

        return sig
    elif collected_chars.find(".") >= 0:
        print("crappily handling JSON decimal: %s" % collected_chars)
        integer, fraction = collected_chars.split(".", 2)
        if integer == "":
            integer = 0
        return int(integer)

    return int(collected_chars)

def _tokenize_null(collected_chars):
    return None

def _tokenize_true(collected_chars):
    return True

def _tokenize_false(collected_chars):
    return False

def _tokenize_string(collected_chars):
    # Trim the leading "
    return collected_chars[1:len(collected_chars)]

def _print_reduction_stack(checker):
    print("final reduction stack: %s" % checker["reduction_stack"])

def _json_parser(**kwargs):
    args = {
        "max_depth": kwargs.get("max_depth", _MAX_DEPTH),
        "debug": kwargs.get("debug", _DEBUG),
    }
    return _create_checker(
        tokenizer_hooks = {
            "string": _tokenize_string,
            "integer": _tokenize_int,
            "null": _tokenize_null,
            "true": _tokenize_true,
            "false": _tokenize_false,
        },
        reduction_hooks = {
            "entry_key": _reduce_literal,
            "entry_value": _reduce_literal,
            "object": _reduce_object,
            "array": _reduce_array,
        },
        **args
    )

def json_parse(json_string, fail_on_invalid = True, **kwargs):
    parser = _json_parser(**kwargs)

    for i in range(0, len(json_string)):
        _handle_next_char(parser, json_string = json_string, char_index = i)

    is_valid = _verify_valid(parser)
    if (not is_valid):
        invalid_msgs = [parser["rejected_reason"]]
        if (fail_on_invalid):
            fail("JSON parsing failed.\n%s\n\nparser:\n%s" % ("\n".join(invalid_msgs), parser))
        else:
            print("JSON parsing failed. %s" % "\n".join([str(msg) for msg in invalid_msgs]))
            return None

    return parser["reduction_stack"][0][0]["reduction"]
