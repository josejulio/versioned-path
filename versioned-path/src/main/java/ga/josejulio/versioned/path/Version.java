package ga.josejulio.versioned.path;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version implements Comparable<Version> {

    private final int major;
    private final int minor;
    private final int patch;
    private static final Pattern pattern = Pattern.compile("^(\\d+)(?:\\.(\\d+)(?:\\.(\\d+))?)?$");

    public Version(int major, int minor, int patch) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
    }

    public Version(String versionString) {
        Matcher matcher = pattern.matcher(versionString);
        if (!matcher.matches()) {
            throw new RuntimeException("Invalid version string:" + versionString);
        }

        this.major = Integer.parseInt(matcher.group(1));
        this.minor = matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0;
        this.patch = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public String toShortVersionString() {
        if (patch == 0) {
            if (minor == 0) {
                return major + "";
            } else {
                return "%d.%d".formatted(major, minor);
            }
        }

        return toString();
    }

    public String toMinorVersionString() {
        return "%d.%d".formatted(major, minor);
    }

    @Override
    public String toString() {
        return "%d.%d.%d".formatted(major, minor, patch);
    }

    @Override
    public int compareTo(Version that) {

        if (major < that.major) {
            return -1;
        } else if (major > that.major) {
            return 1;
        }

        if (minor < that.minor) {
            return -1;
        } else if (minor > that.minor) {
            return 1;
        }

        if (patch < that.patch) {
            return -1;
        } else if (patch > that.patch) {
            return 1;
        }

        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Version that) {
            return major == that.major && minor == that.minor && patch == that.patch;
        }

        return false;
    }

    @Override
    public int hashCode() {
        return major ^ (minor << 8) ^ (patch << 8);
    }
}
