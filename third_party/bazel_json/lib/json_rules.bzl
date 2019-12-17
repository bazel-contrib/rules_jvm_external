# IGNORE THIS FILE... it was probably a bad idea....

# Skylark reference:
# https://docs.bazel.build/versions/master/skylark/language.html
# https://docs.bazel.build/versions/master/skylark/lib/globals.html
# https://docs.bazel.build/versions/master/skylark/lib/attr.html
#
# - None
# - bool
# - dict
# - function
# - int
# - list
# - string
# - depset
# - struct
#
# JSON reference:
# https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/JSON
# JSON Types:
#
# - null
# - boolean
# - string
# - number
# - array
# - object
JsonValue = provider(fields = ["value_internal"])

JsonNullType = provider()
JsonBooleanType = provider(fields = ["boolean_value"])
JsonStringType = provider(fields = ["string_value"])
JsonNumberType = provider(fields = ["number_value", "exponent"])
JsonArrayType = provider(fields = ["json_type_values"])
JsonObjectType = provider(fields = ["json_type_map"])

# A list of lists of JSON type providers, useful as the value to the
# providers attribute of a label attr
_JSON_TYPE_PROVIDERS = [
    [JsonNullType],
    [JsonBooleanType],
    [JsonStringType],
    [JsonNumberType],
    [JsonArrayType],
    [JsonObjectType],
]

def _create_json_type_null(ctx):
    return [
        JsonValue(value_internal = None),
        JsonNullType(),
    ]

def _create_json_type_boolean(ctx):
    v = ctx.attr.boolean_value
    return [
        JsonValue(value_internal = v),
        JsonBooleanType(boolean_value = v),
    ]

def _create_json_type_string(ctx):
    v = ctx.attr.string_value
    return [
        JsonValue(value_internal = v),
        JsonStringType(string_value = v),
    ]

def _create_json_type_number(ctx):
    v = (ctx.attr.number_value, ctx.attr.exponent)
    return [
        JsonValue(value_internal = v),
        JsonNumberType(
            number_value = v[0],
            exponent = v[1],
        ),
    ]

def _create_json_type_array(ctx):
    values = []
    json_types = []
    for label in ctx.attr.array_values:
        values.append(label[JsonValue].value_internal)
        json_types.append(_extract_json_type_from_label(label))
    return [
        JsonValue(value_internal = values),
        JsonArrayType(json_type_values = json_types),
    ]

def _create_json_type_object(ctx):
    value_map = dict()
    json_type_map = dict()
    for label in ctx.attr.inverted_object_map:
        json_key = ctx.attr.inverted_object_map.get(label)
        json_type = _extract_json_type_from_label(label)
        value_map[json_key] = label[JsonValue].value_internal
        json_type_map[json_key] = json_type
    return [
        JsonValue(value_internal = value_map),
        JsonObjectType(json_type_map = json_type_map),
    ]

def _extract_json_type_from_label(label):
    if JsonNullType in label:
        return label[JsonNullType]
    elif JsonBooleanType in label:
        return label[JsonBooleanType]
    elif JsonStringType in label:
        return label[JsonStringType]
    elif JsonNumberType in label:
        return label[JsonNumberType]
    elif JsonArrayType in label:
        return label[JsonArrayType]
    elif JsonObjectType in label:
        return label[JsonObjectType]
    else:
        fail("unknown json type in label %s" % (label.name))

json_type_null = rule(
    implementation = _create_json_type_null,
    attrs = {},
)

json_type_boolean = rule(
    implementation = _create_json_type_boolean,
    attrs = {
        "boolean_value": attr.bool(),
    },
)

json_type_string = rule(
    implementation = _create_json_type_string,
    attrs = {
        "string_value": attr.string(),
    },
)

json_type_number = rule(
    implementation = _create_json_type_number,
    attrs = {
        "number_value": attr.int(),
        "exponent": attr.int(),
    },
)

json_type_array = rule(
    implementation = _create_json_type_array,
    attrs = {
        "array_values": attr.label_list(
            providers = _JSON_TYPE_PROVIDERS,
        ),
    },
)

json_type_object = rule(
    implementation = _create_json_type_object,
    attrs = {
        "inverted_object_map": attr.label_keyed_string_dict(
            providers = _JSON_TYPE_PROVIDERS,
        ),
    },
)

def _json_printer_impl(ctx):
    for json_label in ctx.attr.json_values:
        print("JSON(%s): %s" % (json_label.label, json_label[JsonValue].value_internal))

json_printer = rule(
    implementation = _json_printer_impl,
    attrs = {
        "json_values": attr.label_list(
            providers = [JsonValue],
        ),
    },
)

def json(value):
    if type(value) == "NoneType":
        print("defined null rule")
        json_type_null(name = "json_null_x")
    elif type(value) == "string":
        print("defined string rule")
        json_type_string(name = "json_string_x", string_value = value)
    elif type(value) == "int":
        print("defined number rule")
        json_type_number(name = "json_number_x", number_value = value)
    elif type(value) == "list":
        print("got a list")
    elif type(value) == "dict":
        print("got a dict")
    else:
        fail("unknown type: %s" % type(value))
