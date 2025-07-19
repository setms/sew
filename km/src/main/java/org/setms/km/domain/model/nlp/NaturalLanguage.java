package org.setms.km.domain.model.nlp;

public interface NaturalLanguage {

  String singular(String noun);

  default String plural(String noun) {
    return plural(noun, 2);
  }

  String plural(String noun, int count);

  String base(String verb);

  String past(String verb);
}
