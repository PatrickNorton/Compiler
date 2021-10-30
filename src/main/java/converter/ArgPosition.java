package main.java.converter;

public sealed interface ArgPosition permits DefaultArgPos, StandardArgPos, VarargPos {
}
