package org.setms.km.domain.model.nlp;

import java.util.Map;

public class English implements NaturalLanguage {

  private static final Map<String, String> IRREGULAR_NOUNS =
      Map.of(
          "children", "child",
          "men", "man",
          "women", "woman",
          "mice", "mouse",
          "feet", "foot",
          "teeth", "tooth",
          "geese", "goose",
          "people", "person",
          "oxen", "ox");
  private static final Map<String, String> VES_EXCEPTIONS =
      Map.of(
          "knives", "knife", "wives", "wife", "wolves", "wolf", "leaves", "leaf", "lives", "life",
          "selves", "self");
  private static final Map<String, String> IRREGULAR_VERBS =
      Map.ofEntries(
          Map.entry("go", "went"),
          Map.entry("be", "was"), // or "were" depending on number
          Map.entry("have", "had"),
          Map.entry("do", "did"),
          Map.entry("run", "ran"),
          Map.entry("come", "came"),
          Map.entry("eat", "ate"),
          Map.entry("get", "got"),
          Map.entry("see", "saw"),
          Map.entry("make", "made"),
          Map.entry("buy", "bought"),
          Map.entry("think", "thought"),
          Map.entry("teach", "taught"),
          Map.entry("bring", "brought"),
          Map.entry("build", "built"),
          Map.entry("leave", "left"),
          Map.entry("send", "sent"),
          Map.entry("sell", "sold"));

  @Override
  public String singular(String noun) {
    if (IRREGULAR_NOUNS.containsKey(noun)) {
      return IRREGULAR_NOUNS.get(noun);
    }
    if (VES_EXCEPTIONS.containsKey(noun)) {
      return VES_EXCEPTIONS.get(noun);
    }
    if (noun.matches(".*[^aeiou]ies$")) {
      return noun.replaceAll("ies$", "y");
    }
    if (noun.endsWith("ves")) {
      return noun.substring(0, noun.length() - 3) + "f";
    }
    if (noun.matches(".*(s|sh|ch|x|z)es$")) {
      return noun.replaceAll("es$", "");
    }
    if (noun.endsWith("es")) {
      return noun.substring(0, noun.length() - 2);
    }
    if (noun.endsWith("s")) {
      return noun.substring(0, noun.length() - 1);
    }
    return noun;
  }

  @Override
  public String plural(String noun, int count) {
    return org.atteo.evo.inflector.English.plural(noun, count);
  }

  @Override
  public String base(String verb) {
    if (verb.equals("has")) {
      return "have";
    }
    if (verb.matches(".*[^aeiou]ies$")) {
      return verb.replaceAll("ies$", "y");
    }
    if (verb.endsWith("oes")) {
      return verb.substring(0, verb.length() - 2); // remove "es"
    }
    if (verb.matches(".*(s|sh|ch|x|z)es$")) {
      return verb.replaceAll("es$", "");
    }
    if (verb.endsWith("s")) {
      return verb.substring(0, verb.length() - 1);
    }
    return verb;
  }

  @Override
  public String past(String verb) {
    if (IRREGULAR_VERBS.containsKey(verb)) {
      return IRREGULAR_VERBS.get(verb);
    }
    if (verb.matches(".*[^aeiou]y$")) {
      return verb.substring(0, verb.length() - 1) + "ied";
    }
    if (verb.matches(".*[aeiou]y$")) {
      return verb + "ed";
    }
    if (verb.endsWith("e")) {
      return verb + "d";
    }
    if (isCVC(verb)) {
      char lastChar = verb.charAt(verb.length() - 1);
      return verb + lastChar + "ed";
    }
    return verb + "ed";
  }

  private static boolean isCVC(String verb) {
    if (verb.length() < 3) {
      return false;
    }
    char c1 = verb.charAt(verb.length() - 3);
    char v = verb.charAt(verb.length() - 2);
    char c2 = verb.charAt(verb.length() - 1);
    return isConsonant(c1)
        && isVowel(v)
        && isConsonant(c2)
        && !endsInWXY(verb); // avoid doubling final w, x, y
  }

  private static boolean isVowel(char c) {
    return "aeiou".indexOf(c) != -1;
  }

  private static boolean isConsonant(char c) {
    return Character.isLetter(c) && !isVowel(c);
  }

  private static boolean endsInWXY(String verb) {
    char last = verb.charAt(verb.length() - 1);
    return last == 'w' || last == 'x' || last == 'y';
  }
}
