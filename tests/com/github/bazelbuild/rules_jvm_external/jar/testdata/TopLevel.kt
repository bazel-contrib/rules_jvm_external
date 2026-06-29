// Copyright 2026 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// A fixture exercising IndexJar's handling of Kotlin top-level declarations. The compiler emits
// these into a "TopLevelKt" file facade; the function, property, and type alias are otherwise
// invisible to a class-name-only index.
package com.example.kotlin

fun topLevelFunction(): String = internalFunction() + privateFunction()

val topLevelProperty: Int = 42

typealias TopLevelAlias = Map<String, Int>

// Non-public top-level declarations: not importable from another artifact, so excluded from the
// index. (Referenced above so the compiler doesn't flag them as unused.)
internal fun internalFunction(): String = "internal"

private fun privateFunction(): String = "private"

class RegularClass
