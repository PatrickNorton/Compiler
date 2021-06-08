package main.java.converter;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class Version implements Comparable<Version> {
    private final int major;
    private final int minor;
    private final int bugfix;

    public Version(int major, int minor, int bugfix) {
        this.major = major;
        this.minor = minor;
        this.bugfix = bugfix;
    }

    @Override
    public int compareTo(@NotNull Version o) {
        if (this.major != o.major) {
            return Integer.compare(this.major, o.major);
        } else if (this.minor != o.minor) {
            return Integer.compare(this.minor, o.minor);
        } else {
            return Integer.compare(this.bugfix, o.bugfix);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Version version = (Version) o;
        return major == version.major && minor == version.minor && bugfix == version.bugfix;
    }

    @Override
    public int hashCode() {
        return Objects.hash(major, minor, bugfix);
    }

    public String strValue() {
        return String.format("%d.%d.%d", major, major, bugfix);
    }

    public static Version parse(String value) {
        try {
            var dot = value.indexOf('.');
            if (dot < 0) {
                return null;
            }
            var major = Integer.parseUnsignedInt(value.substring(0, dot));
            var dot2 = value.indexOf('.', dot + 1);
            if (dot2 < 0) {
                return null;
            }
            var minor = Integer.parseUnsignedInt(value.substring(dot + 1, dot2));
            var bugfix = Integer.parseUnsignedInt(value.substring(dot2 + 1));
            return new Version(major, minor, bugfix);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
