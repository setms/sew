package org.setms.sew.core.inbound.tool;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static org.setms.km.domain.model.format.Strings.initUpper;
import static org.setms.km.domain.model.validation.Level.ERROR;
import static org.setms.sew.core.inbound.tool.Inputs.terms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.setms.km.domain.model.artifact.Pointer;
import org.setms.sew.core.domain.model.sdlc.ddd.Term;
import org.setms.km.domain.model.tool.Glob;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.Output;
import org.setms.km.domain.model.tool.OutputSink;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.tool.Tool;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.validation.Location;

@Slf4j
public class GlossaryTool extends Tool {

  @Override
  public List<Input<?>> getInputs() {
    return List.of(terms());
  }

  @Override
  public List<Output> getOutputs() {
    return List.of(new Output(new Glob("build/reports/glossary", "*.html")));
  }

  @Override
  protected void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var terms = inputs.get(Term.class);
    terms.forEach(
        term ->
            Optional.ofNullable(term.getSeeAlso()).stream()
                .flatMap(Collection::stream)
                .forEach(pointer -> validateSeeAlso(term, pointer, terms, diagnostics)));
  }

  private void validateSeeAlso(
      Term term, Pointer pointer, Collection<Term> candidates, Collection<Diagnostic> diagnostics) {
    if (pointer.resolveFrom(candidates).isEmpty()) {
      diagnostics.add(
          new Diagnostic(
              ERROR,
              "Term '%s' refers to unknown term '%s'".formatted(term.getName(), pointer.getId()),
              new Location(term)));
    }
  }

  @Override
  public void build(ResolvedInputs inputs, OutputSink sink, Collection<Diagnostic> diagnostics) {
    var terms = inputs.get(Term.class);
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
    var title = "Glossary: %s".formatted(initUpper(glossary));
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
              .flatMap(Optional::stream)
              .map(a -> "<a href=\"#%s\">%s</a>".formatted(a.getName(), a.getDisplay()))
              .collect(joining(", ")));
    }
    writer.println("</dd>");
  }
}
