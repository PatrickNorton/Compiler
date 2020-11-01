# Compiler

This is a compiler for an as-yet-unnamed language.

## Features

* Const-correctness: Different attributes for different meanings
  * No modifier: Immutable reference, immutable variable
  * `mut`: Mutable reference, mutable variable
  * `final`: Immutable reference, mutable variable
  * `mref`: Mutable reference, immutable variable
* `pubget` access level: Can access from anywhere, but only modify from within
  the class
* Python-style list, set, and dict comprehensions, with additional `while` 
  clause
* Rust-style `enum`s (called `union` here)
* Properties &agrave; la Python
* Generators/coroutines
* Operator overloading
* No semicolons, newline is the statement separator
* 100% null-safe: Options use a `?` and are first-class

## Syntax examples
Hello, World!
```
print("Hello, World!")
```

Fibonacci function
```
func fibonacci(int number) -> int {
    mut int a = 0
    mut int b = 1
    dotimes number {
        a, b = b, a + b
    }
    return a
}
```

Max function
```
func max(list[int] values) -> int {
    mut int currentMax = values[0]
    for int i in values {
        if i > currentMax {
            currentMax = i
        }
    }
    return currentMax
}
```

Summing function
```
func sum(Iterable[int] values) -> int {
    return \+(*values)  # Does not yet compile
}
```

For more examples of code, look anywhere in `./Lib/*`, especially
`./Lib/Sample.newlang`

## To run
This will compile & run the contents of `Sample.newlang`.

This assumes you have Java >= 12 and Cargo installed, 
and has only been tested on MacOS.
1. Clone this repo and `PatrickNorton/rust_runtime`
2. `cd /path/to/this/repo`
3. `cd ./src`
4. `javac ./main/java/parser/Compiler.java`
5. `java main.java.parser.Compiler ../Lib/Sample.newlang`
6. `cd /path/to/runtime/repo`
7. `cargo run -- /path/to/this/repo/Lib/__ncache__/Sample.nbyte`

## What doesn't work yet

This is by no means a complete list.

* Function overloading
* Some compile-time constants
* Some inheritance with function types
* Exceptions
* Non-top-level imports/exports
* Importing whole modules
* Most of the stdlib
* Empty list literals in some contexts
* Generic inference in type constructors
* Unicode, probably
* Tuple expansion in some contexts
* Iterable expansion in all contexts
* Anywhere you see a `TODO` comment, `FIXME` comment, or `CompilerTodoError`
