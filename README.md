# Compiler

This is a compiler for an as-yet-unnamed language.

## Code examples
Hello, World!
`print("Hello, World!")`

Fibonacci function
`func fibonacci(int number) -> int {
    int a = 0
    int b = 1
    dotimes number {
        a, b = b, a + b
    }
    return a
}`

Max function
`func max(list[int] values) -> int {
    int currentMax = values[0]
    for int i in values {
        if i > currentMax {
            currentMax = i
        }
    }
    return currentMax
}`

Summing function
`func sum(Iterable[int] values) -> int {
    return \+(*values)
}`
