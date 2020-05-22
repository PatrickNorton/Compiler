package main.java.converter.classbody;

import main.java.converter.TypeObject;

import java.util.ArrayDeque;
import java.util.Deque;

public final class FunctionReturnInfo {
    private final Deque<TypeObject[]> returns = new ArrayDeque<>();
    private final Deque<Boolean> generators = new ArrayDeque<>();

     /**
     * Adds the given function returns to the file.
     *
     * @param values The values to be added
     * @see #currentFnReturns()
     */
    public void addFunctionReturns(TypeObject[] values) {
        returns.push(values);
        generators.push(false);
    }

    public void addFunctionReturns(boolean isGen, TypeObject[] values) {
        returns.push(values);
        generators.push(isGen);
    }

    /**
     * Gets the return values of the currently-being-compiled function, as
     * given in {@link #addFunctionReturns}.
     *
     * @return The returns of the current function
     * @see #addFunctionReturns(TypeObject[])
     */
    public TypeObject[] currentFnReturns() {
        assert returns.peekFirst() != null;
        return returns.peekFirst();
    }

    public boolean isGenerator() {
        assert generators.peekFirst() != null;
        return generators.peekFirst();
    }

    /**
     * Pops the function returns, for when the function is done being compiled.
     *
     * @see #addFunctionReturns(TypeObject[])
     * @see #currentFnReturns()
     */
    public void popFnReturns() {
        returns.pop();
        generators.pop();
    }

    /**
     * If the compiler is currently not compiling a function.
     *
     * @return If no function returns exist
     */
    public boolean notInFunction() {
        return returns.isEmpty();
    }

}
