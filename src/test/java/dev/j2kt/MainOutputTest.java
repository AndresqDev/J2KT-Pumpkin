package dev.j2kt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainOutputTest {
    @TempDir
    Path temp;

    @Test
    void writesSameSimpleNameIntoDistinctPackageDirectories() throws IOException {
        Path source = temp.resolve("src");
        Path out = temp.resolve("out");
        Files.createDirectories(source.resolve("com/a"));
        Files.createDirectories(source.resolve("com/b"));
        Files.writeString(source.resolve("com/a/Thing.java"),
                "package com.a;\npublic class Thing {}\n");
        Files.writeString(source.resolve("com/b/Thing.java"),
                "package com.b;\npublic class Thing {}\n");

        Main.main(new String[]{"--target", "jvm", "--no-header", "--out", out.toString(), source.toString()});

        assertTrue(Files.isRegularFile(out.resolve("com/a/Thing.kt")));
        assertTrue(Files.isRegularFile(out.resolve("com/b/Thing.kt")));
        assertEquals("package com.a", Files.readString(out.resolve("com/a/Thing.kt")).lines().findFirst().orElse(""));
        assertEquals("package com.b", Files.readString(out.resolve("com/b/Thing.kt")).lines().findFirst().orElse(""));
    }
}
