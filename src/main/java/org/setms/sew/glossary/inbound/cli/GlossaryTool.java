package org.setms.sew.glossary.inbound.cli;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static org.setms.sew.util.Strings.initCap;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.setms.sew.format.sew.SewFormat;
import org.setms.sew.schema.Pointer;
import org.setms.sew.tool.Glob;
import org.setms.sew.tool.Input;
import org.setms.sew.tool.Tool;
import org.setms.sew.tool.ToolException;

@Slf4j
public class GlossaryTool implements Tool {

  @Override
  public Collection<Input<?>> getInputs() {
    return List.of(
        new Input<>(
            "terms", new Glob("src/main/glossary", "**/*.term"), new SewFormat(), Term.class));
  }

  @Override
  public void run(File dir, ResolvedInputs inputs) {
    var terms = inputs.get("terms", Term.class);
    validate(terms);
    buildGlossary(dir, terms);
  }

  private void validate(Collection<Term> terms) {
    terms.forEach(
        term ->
            Optional.ofNullable(term.getSeeAlso()).stream()
                .flatMap(Collection::stream)
                .forEach(pointer -> validateSeeAlso(term, pointer, terms)));
  }

  private void validateSeeAlso(Term term, Pointer pointer, Collection<Term> candidates) {
    try {
      pointer.resolveFrom(candidates);
    } catch (Exception e) {
      throw new ToolException(
          "Term '%s' refers to unknown term '%s'".formatted(term.getName(), pointer.getId()), e);
    }
  }

  private void buildGlossary(File dir, Collection<Term> terms) {
    var termsByPackage = terms.stream().collect(groupingBy(Term::getPackage));
    termsByPackage.forEach(
        (glossary, glossaryTerms) ->
            buildGlossaryFile(dir, glossary, new TreeSet<>(glossaryTerms)));
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  private void buildGlossaryFile(File dir, String glossary, Collection<Term> terms) {
    var file = new File(dir, "build/reports/glossary/%s.html".formatted(glossary));
    file.getParentFile().mkdirs();
    try (var writer = new PrintWriter(file)) {
      buildGlossary(glossary, writer, terms);
    } catch (IOException e) {
      throw new ToolException("Failed to write " + file, e);
    }
  }

  private void buildGlossary(String glossary, PrintWriter writer, Collection<Term> terms) {
    var title = "Glossary: %s".formatted(initCap(glossary));
    writer.println("<html>");
    writer.println("  <head>");
    writer.printf("    <title>%s</title>%n", title);
    writer.println("  </head>");
    writer.println("  <body>");
    writer.printf("    <h1>%s</h1>%n", title);
    writer.println("    <dl>");
    terms.forEach(term -> writeTerm(term, terms, writer));
    writer.println("    </dl>");
    writer.println("  </body>");
    writer.println("</html>");
  }

  private void writeTerm(Term term, Collection<Term> terms, PrintWriter writer) {
    writer.printf("      <dt id=\"%s\">%s</dt>%n", term.getName(), term.getDisplay());
    writer.printf("      <dd>%s", term.getDescription());
    if (term.getSeeAlso() != null && !term.getSeeAlso().isEmpty()) {
      writer.println("<br/>");
      writer.print("        See also: ");
      writer.printf(
          "%s.%n      ",
          term.getSeeAlso().stream()
              .map(pointer -> pointer.resolveFrom(terms))
              .map(a -> "<a href=\"#%s\">%s</a>".formatted(a.getName(), a.getDisplay()))
              .collect(joining(", ")));
    }
    writer.println("</dd>");
  }
}
