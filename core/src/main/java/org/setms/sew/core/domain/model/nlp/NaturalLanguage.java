package org.setms.sew.core.domain.model.nlp;


public interface NaturalLanguage {

  String singular(String noun);

  String plural(String noun);

  String base(String verb);

  String past(String verb);
}
