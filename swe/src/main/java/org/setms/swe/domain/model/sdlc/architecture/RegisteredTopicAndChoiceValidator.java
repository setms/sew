package org.setms.swe.domain.model.sdlc.architecture;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RegisteredTopicAndChoiceValidator
    implements ConstraintValidator<RegisteredTopicAndChoice, Decision> {

  @Override
  public boolean isValid(Decision decision, ConstraintValidatorContext context) {
    var topic = decision.getTopic();
    if (topic == null || !Topics.names().contains(topic)) {
      return false;
    }
    var choice = decision.getChoice();
    return choice != null && Topics.choicesFor(topic).contains(choice);
  }
}
