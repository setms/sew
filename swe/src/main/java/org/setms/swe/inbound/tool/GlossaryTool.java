package org.setms.swe.inbound.tool;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static org.setms.km.domain.model.format.Strings.initUpper;
import static org.setms.km.domain.model.validation.Level.ERROR;
import static org.setms.swe.inbound.tool.Inputs.terms;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Optional;
import java.util.TreeSet;
import lombok.extern.slf4j.Slf4j;
import org.setms.km.domain.model.artifact.Link;
import org.setms.km.domain.model.tool.BaseTool;
import org.setms.km.domain.model.tool.Input;
import org.setms.km.domain.model.tool.ResolvedInputs;
import org.setms.km.domain.model.validation.Diagnostic;
import org.setms.km.domain.model.workspace.Resource;
import org.setms.swe.domain.model.sdlc.ddd.Term;

@Slf4j
public class GlossaryTool extends BaseTool<Term> {

  @Override
  public Input<Term> getMainInput() {
    return terms();
  }

  @Override
  public void validate(ResolvedInputs inputs, Collection<Diagnostic> diagnostics) {
    var terms = inputs.get(Term.class);
    terms.forEach(
        term ->
            Optional.ofNullable(term.getSeeAlso()).stream()
                .flatMap(Collection::stream)
                .forEach(link -> validateSeeAlso(term, link, terms, diagnostics)));
  }

  private void validateSeeAlso(
      Term term, Link link, Collection<Term> candidates, Collection<Diagnostic> diagnostics) {
    if (link.resolveFrom(candidates).isEmpty()) {
      diagnostics.add(
          new Diagnostic(
              ERROR,
              "Term '%s' refers to unknown term '%s'".formatted(term.getName(), link.getId()),
              term.toLocation()));
    }
  }

  @Override
  public void build(
      ResolvedInputs inputs, Resource<?> resource, Collection<Diagnostic> diagnostics) {
    var terms = inputs.get(Term.class);
    var termsByPackage = terms.stream().collect(groupingBy(Term::getPackage));
    termsByPackage.forEach(
        (glossary, glossaryTerms) ->
            buildGlossaryFile(resource, glossary, new TreeSet<>(glossaryTerms), diagnostics));
  }

  private void buildGlossaryFile(
      Resource<?> resource,
      String glossary,
      Collection<Term> terms,
      Collection<Diagnostic> diagnostics) {
    var report = resource.select("%s.html".formatted(glossary));
    try (var writer = new PrintWriter(report.writeTo())) {
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
              .map(link -> link.resolveFrom(terms))
              .flatMap(Optional::stream)
              .map(a -> "<a href=\"#%s\">%s</a>".formatted(a.getName(), a.getDisplay()))
              .collect(joining(", ")));
    }
    writer.println("</dd>");
  }
}
