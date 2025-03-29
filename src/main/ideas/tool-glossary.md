# Glossary

## Validations

- Every `seeAlso` must refer to a defined term (error)

## Reporting

- Compile HTML glossary with hyperlinks from terms to related terms

## Suggestions

### Suggest terms

Look at the requirements (business and user requirements, plus use cases) and suggests terms to add to the glossary.

- NLP: named entity recognition, noun phrase extraction
- Statistical methods:
    - Term frequency analysis: Terms with moderate frequency (not too common, not too rare)
    - TF-IDF (Term Frequency-Inverse Document Frequency): Find terms that are distinctive to your documents compared to
      general language
    - Collocation detection: Identify phrases that appear together more often than by chance
- Pattern recognition, e.g. Capitalized Phrases
- Use case: aggregate, read model, command part, event part

Example algorithm:

```text
function identifyGlossaryCandidates(requirementsText):
    // Preprocessing
    sentences = splitIntoSentences(requirementsText)
    tokens = tokenize(requirementsText)
    
    // Apply POS tagging
    taggedTokens = applyPOSTagging(tokens)
    
    // Extract noun phrases and entities
    nounPhrases = extractNounPhrases(taggedTokens)
    namedEntities = extractNamedEntities(taggedTokens)
    
    // Calculate statistics
    termFrequencies = calculateTermFrequency(nounPhrases + namedEntities)
    tfidfScores = calculateTFIDF(nounPhrases + namedEntities, corpusDocuments)
    
    // Apply heuristics
    candidates = []
    for term in (nounPhrases + namedEntities):
        score = 0
        // Scoring heuristics
        if isCapitalized(term): score += 1
        if termFrequencies[term] >= 2 && termFrequencies[term] <= 20: score += 2
        if tfidfScores[term] > threshold: score += 3
        if isCompoundTerm(term): score += 1
        if appearInMultipleDocuments(term): score += 2
        if isSurroundedByDefinitionPatterns(term, sentences): score += 3
        
        // Add high-scoring terms
        if score >= minimumScore:
            candidates.append({'term': term, 'score': score, 'context': extractContext(term, sentences)})
    
    return sortByScore(candidates)
```

### Suggest definitions

Look up terms in dictionaries/wikipedia to suggest definitions.
