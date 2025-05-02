package org.setms.sew.core.inbound.tool;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static org.setms.sew.core.domain.model.format.Strings.initCap;
import static org.setms.sew.core.domain.model.tool.Level.ERROR;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.setms.sew.core.domain.model.sdlc.Pointer;
import org.setms.sew.core.domain.model.sdlc.Term;
import org.setms.sew.core.domain.model.tool.Diagnostic;
import org.setms.sew.core.domain.model.tool.Glob;
import org.setms.sew.core.domain.model.tool.Input;
import org.setms.sew.core.domain.model.tool.InputSource;
import org.setms.sew.core.domain.model.tool.Output;
import org.setms.sew.core.domain.model.tool.OutputSink;
import org.setms.sew.core.domain.model.tool.ResolvedInputs;
import org.setms.sew.core.domain.model.tool.Tool;
import org.setms.sew.core.inbound.format.sew.SewFormat;

@Slf4j
public class GlossaryTool extends Tool {

  @Override
  public List<Input<?>> getInputs() {
    return List.of(
        new Input<>(
            "terms", new Glob("src/main/glossary", "**/*.term"), new SewFormat(), Term.class));
  }

  @Override
  public List<Output> getOutputs() {
    return List.of(new Output(new Glob("build/reports/glossary", "*.html")));
  }

  @Override
  protected void validate(
      InputSource source, ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var terms = inputs.get("terms", Term.class);
    terms.forEach(
        term ->
            Optional.ofNullable(term.getSeeAlso()).stream()
                .flatMap(Collection::stream)
                .forEach(pointer -> validateSeeAlso(term, pointer, terms, diagnostics)));
  }

  private void validateSeeAlso(
      Term term, Pointer pointer, Collection<Term> candidates, Collection<Diagnostic> diagnostics) {
    try {
      pointer.resolveFrom(candidates);
    } catch (Exception e) {
      diagnostics.add(
          new Diagnostic(
              ERROR,
              "Term '%s' refers to unknown term '%s'".formatted(term.getName(), pointer.getId())));
    }
  }

  @Override
  public void build(ResolvedInputs inputs, OutputSink sink, Collection<Diagnostic> diagnostics) {
    var terms = inputs.get("terms", Term.class);
    var termsByPackage = terms.stream().collect(groupingBy(Term::getPackage));
    termsByPackage.forEach(
        (glossary, glossaryTerms) ->
            buildGlossaryFile(sink, glossary, new TreeSet<>(glossaryTerms), diagnostics));
  }

  private void buildGlossaryFile(
      OutputSink sink,
      String glossary,
      Collection<Term> terms,
      Collection<Diagnostic> diagnostics) {
    var report = sink.select("reports/glossary/%s.html".formatted(glossary));
    try (var writer = new PrintWriter(report.open())) {
      buildGlossary(glossary, writer, terms);
    } catch (IOException e) {
      diagnostics.add(new Diagnostic(ERROR, e.getMessage()));
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
