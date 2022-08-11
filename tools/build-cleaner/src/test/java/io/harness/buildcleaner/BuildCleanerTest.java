package io.harness.buildcleaner;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.buildcleaner.bazel.BuildFile;
import io.harness.buildcleaner.bazel.JavaLibrary;
import io.harness.buildcleaner.bazel.LoadStatement;

import com.google.common.collect.ImmutableSet;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class BuildCleanerTest {
  private static final String PACKAGE_ROOT = "io.harness";

  @Rule public TemporaryFolder workspace = new TemporaryFolder();
  private static final String CLASS_PATTERN =
      new StringBuilder().append("public class %s {").append(System.lineSeparator()).append("}").toString();

  @Test
  public void generateIndex_singleDirectory_generatesHarnessSymbolMap() throws IOException, ClassNotFoundException {
    // Arrange
    writeClassToFile(workspace.getRoot(), "A1.java");
    writeClassToFile(workspace.getRoot(), "A2.java");

    BuildCleaner buildCleaner =
        new BuildCleaner(new String[] {"--workspace", workspace.getRoot().toString(), "--indexSourceGlob", "*.java"});

    // Act & Assert
    assertThat(buildCleaner.buildHarnessSymbolMap().getSymbolToTargetMap())
        .isEqualTo(Map.of("io.harness.A1", "", "io.harness.A2", ""));
  }

  @Test
  public void generateIndex_nestedDirectories_generatesHarnessSymbolMap() throws IOException, ClassNotFoundException {
    // Arrange
    writeClassToFile(workspace.getRoot(), "Root.java");

    File nestedFolder = workspace.newFolder("nested");
    writeClassToFile(nestedFolder, "Nested.java");

    BuildCleaner buildCleaner =
        new BuildCleaner(new String[] {"--workspace", workspace.getRoot().toString(), "--indexSourceGlob", "**"});

    // Act & Assert
    assertThat(buildCleaner.buildHarnessSymbolMap().getSymbolToTargetMap())
        .isEqualTo(Map.of("io.harness.Root", "", "io.harness.nested.Nested", "nested"));
  }

  @Test
  public void generateBuildForModule_oneSourceFile_allImportsInSymbolMap_returnBuildWithDeps() throws IOException {
    // Arrange
    List<String> imports = List.of("io.harness.RootClass", "io.harness.nested.NestedClass");
    writeClassToFile(workspace.getRoot(), "File.java", imports);

    BuildCleaner buildCleaner = new BuildCleaner(new String[] {"--workspace", workspace.getRoot().toString()});

    SymbolDependencyMap indexedDependencyMap = new SymbolDependencyMap();
    indexedDependencyMap.addSymbolTarget("io.harness.RootClass", "root");
    indexedDependencyMap.addSymbolTarget("io.harness.nested.NestedClass", "root/nested");

    // Act
    BuildFile outputBuildFile = buildCleaner.generateBuildForModule(Paths.get(""), indexedDependencyMap).get();

    // Assert
    assertTrue(outputBuildFile.getJavaBinaryList().isEmpty());

    Set<LoadStatement> loadStatementSet = outputBuildFile.getLoadStatements();
    assertEquals(1, loadStatementSet.size());
    assertEquals("load(\"@rules_java//java:defs.bzl\", \"java_library\")",
        loadStatementSet.stream().findFirst().get().toString());

    List<JavaLibrary> javaLibraryTargets = outputBuildFile.getJavaLibraryList();
    assertEquals(1, javaLibraryTargets.size());
    assertEquals(ImmutableSet.of("//root", "//root/nested"), javaLibraryTargets.get(0).getDeps());
  }

  private void writeClassToFile(File folder, String fileName) throws IOException, FileNotFoundException {
    writeClassToFile(folder, fileName, Collections.emptyList());
  }

  private void writeClassToFile(File folder, String fileName, List<String> imports)
      throws IOException, FileNotFoundException {
    File file = new File(folder + "/" + fileName);
    file.createNewFile();

    String packageName = Paths.get(workspace.getRoot().getPath()).relativize(Paths.get(folder.getPath())).toString();
    if (packageName.isEmpty()) {
      packageName = PACKAGE_ROOT;
    } else {
      packageName = PACKAGE_ROOT + "." + packageName;
    }

    String content = String.format(CLASS_PATTERN, fileName.substring(0, fileName.indexOf(".")));

    try (PrintWriter out = new PrintWriter(file);) {
      out.println(String.format("package %s;", packageName));
      for (String packageToImport : imports) {
        out.println(String.format("import %s;", packageToImport));
      }
      out.println(content);
      out.flush();
    }
  }
}