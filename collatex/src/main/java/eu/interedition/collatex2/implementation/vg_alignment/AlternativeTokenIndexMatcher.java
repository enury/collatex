package eu.interedition.collatex2.implementation.vg_alignment;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import eu.interedition.collatex2.implementation.input.Phrase;
import eu.interedition.collatex2.interfaces.IAlignmentTable;
import eu.interedition.collatex2.interfaces.IColumn;
import eu.interedition.collatex2.interfaces.IColumns;
import eu.interedition.collatex2.interfaces.IInternalColumn;
import eu.interedition.collatex2.interfaces.IMatch;
import eu.interedition.collatex2.interfaces.INormalizedToken;
import eu.interedition.collatex2.interfaces.IPhrase;
import eu.interedition.collatex2.interfaces.ITokenContainer;
import eu.interedition.collatex2.interfaces.ITokenIndex;
import eu.interedition.collatex2.interfaces.ITokenMatch;
import eu.interedition.collatex2.interfaces.ITokenMatcher;
import eu.interedition.collatex2.interfaces.IWitness;
import eu.interedition.collatex2.legacy.alignmenttable.Columns;
import eu.interedition.collatex2.legacy.indexing.NullToken;
import eu.interedition.collatex2.legacy.tokenmatching.ColumnPhraseMatch;

//TODO: remove explicit dependency on NullToken
public class AlternativeTokenIndexMatcher implements ITokenMatcher {
  private static final Logger LOG = LoggerFactory.getLogger(TokenIndexMatcher.class);
  private final ITokenContainer base;

  public AlternativeTokenIndexMatcher(ITokenContainer base) {
    this.base = base;
  }

  @Override
  public List<ITokenMatch> getMatches(IWitness witness) {
    final List<String> repeatedTokens = combineRepeatedTokens(witness);
    ITokenIndex baseIndex = base.getTokenIndex(repeatedTokens);
    return findMatches(baseIndex, witness.getTokenIndex(repeatedTokens), witness);
  }

  //TODO: change return type from List into Set?
  private List<String> combineRepeatedTokens(final IWitness witness) {
    final Set<String> repeatedTokens = Sets.newHashSet();
    repeatedTokens.addAll(base.getRepeatedTokens());
    repeatedTokens.addAll(witness.getRepeatedTokens());
    return Lists.newArrayList(repeatedTokens);
  }

  private List<ITokenMatch> findMatches(final ITokenIndex tableIndex, final ITokenIndex tokenIndex, IWitness witness) {
    final List<Sequence> matches = Lists.newArrayList();
    final Set<String> keys = tokenIndex.keys();
    for (final String key : keys) {
      // IndexMatcher.LOG.debug("Looking for phrase: " + key);
      if (tableIndex.contains(key)) {
        // IndexMatcher.LOG.debug("FOUND!");
        final IPhrase phrase = tokenIndex.getPhrase(key);
        final IPhrase tablePhrase = tableIndex.getPhrase(key);
        matches.add(new Sequence(tablePhrase, phrase));
      }
    }
    TokenIndexMatcher.LOG.debug("unfiltered matches: " + matches);
    return joinOverlappingMatches(matches, witness);
  }

  private List<ITokenMatch> joinOverlappingMatches(final List<Sequence> matches, IWitness witness) {
    final List<ITokenMatch> newMatches = filterMatchesBasedOnPositionMatches(matches, witness);
    TokenIndexMatcher.LOG.debug("filtered matches: " + newMatches);
    return newMatches;
  }

  // TODO: make IColumns Iterable!
  // NOTE: There is a potential situation here where 1 column matches with
  // multiple phrases
  // NOTE: The other phrases are seen as additions, which causes too many empty
  // columns
  // NOTE: --> not the optimal alignment
  @SuppressWarnings("boxing")
  private List<ITokenMatch> filterMatchesBasedOnPositionMatches(final List<Sequence> matches, IWitness witness) {
    Map<INormalizedToken, INormalizedToken> witnessToTable;
    witnessToTable = Maps.newLinkedHashMap();
    //BB niet hier al de SecondChoices uitfilteren, maar aangeven, zodat in getMatchesUsingWitnessIndex beslist kan worden welke alternatieven weg kunnen
    //    List<PhraseMatch> filteredMatches = filterAwaySecondChoicesMultipleTokensOneColumn(filterAwaySecondChoicesMultipleColumnsOneToken(matches));
    //    for (final PhraseMatch match : filteredMatches) {
    for (final Sequence match : matches) {
      // step 1. Gather data
      List<TokenPair> pairs = Lists.newArrayList();
      final IPhrase tablePhrase = match.getTablePhrase();
      final IPhrase witnessPhrase = match.getPhrase();
      final Iterator<INormalizedToken> tokens = witnessPhrase.getTokens().iterator();
      for (final INormalizedToken tableToken : tablePhrase.getTokens()) {
        final INormalizedToken token = tokens.next();
        // skip NullColumn and NullToken
        if (!(tableToken instanceof NullToken)) {
          pairs.add(new TokenPair(tableToken, token));
        }
      }
      
      // step 2. Split phrase matches
      for (TokenPair pair : pairs) {
        final INormalizedToken column = pair.tableToken;
        final INormalizedToken token = pair.witnessToken;
        witnessToTable.put(token, column);
      }
      
    }

    final List<ITokenMatch> newMatches = Lists.newArrayList();
    for (final INormalizedToken token : witness.getTokens()) {
      final INormalizedToken tableToken = witnessToTable.get(token);
      if (tableToken!=null) {
        final ITokenMatch newMatch = new TokenMatch(tableToken, token);
        newMatches.add(newMatch);
      }
    }
    return newMatches;
  }

  // check whether this match has an alternative that is equal in weight
  // if so, then skip the alternative!
  // NOTE: multiple columns match with the same token!
  private static List<ITokenMatch> filterAwaySecondChoicesMultipleColumnsOneToken(List<ITokenMatch> matches) {
    List<ITokenMatch> filteredMatches = Lists.newArrayList();
    Multimap<INormalizedToken, INormalizedToken> baseToken2witnessToken = ArrayListMultimap.create();

    for (final ITokenMatch match : matches) {
      baseToken2witnessToken.put(match.getBaseToken(), match.getWitnessToken());
    }

    for (Entry<INormalizedToken, Collection<INormalizedToken>> entry : baseToken2witnessToken.asMap().entrySet()) {
      INormalizedToken baseToken = entry.getKey();
      Collection<INormalizedToken> witnessTokens = entry.getValue();
      INormalizedToken witnessToken = witnessTokens.iterator().next();
      filteredMatches.add(new TokenMatch(baseToken, witnessToken));
    }

    return filteredMatches;
  }

  // check whether this match has an alternative that is equal in weight
  // if so, then skip the alternative!
  // NOTE: multiple witness tokens match with the same table column!
  private static List<ITokenMatch> filterAwaySecondChoicesMultipleTokensOneColumn(List<ITokenMatch> matches, Map<INormalizedToken, IInternalColumn> baseTokenToColumn) {
    List<ITokenMatch> filteredMatches = Lists.newArrayList();
    Multimap<INormalizedToken, INormalizedToken> witnessToken2baseToken = ArrayListMultimap.create();

    for (final ITokenMatch match : matches) {
      witnessToken2baseToken.put(match.getWitnessToken(), match.getBaseToken());
    }

    Multimap<INormalizedToken, IInternalColumn> witnessToken2column = ArrayListMultimap.create();
    for (Entry<INormalizedToken, Collection<INormalizedToken>> entry : witnessToken2baseToken.asMap().entrySet()) {
      INormalizedToken witnessToken = entry.getKey();
      for (INormalizedToken baseToken : entry.getValue()) {
        witnessToken2column.put(witnessToken, baseTokenToColumn.get(baseToken));
      }
    }

    Map<INormalizedToken, IInternalColumn> columnForWitnessToken = Maps.newHashMap();
    for (Entry<INormalizedToken, Collection<IInternalColumn>> entry : witnessToken2column.asMap().entrySet()) {
      columnForWitnessToken.put(entry.getKey(), entry.getValue().iterator().next());
    }

    Map<IInternalColumn, INormalizedToken> columnToBaseToken = Maps.newHashMap();
    for (Entry<INormalizedToken, IInternalColumn> entry : baseTokenToColumn.entrySet()) {
      columnToBaseToken.put(entry.getValue(), entry.getKey());
    }

    for (Entry<INormalizedToken, IInternalColumn> entry : columnForWitnessToken.entrySet()) {
      INormalizedToken witnessToken = entry.getKey();
      INormalizedToken baseToken = columnToBaseToken.get(entry.getValue());
      filteredMatches.add(new TokenMatch(baseToken, witnessToken));
    }

    return filteredMatches;
  }

  //NOTE: This method becomes legacy when the VariantGraph code is integrated!
  public static List<IMatch> getMatchesUsingWitnessIndex(IAlignmentTable table, IWitness witness) {
    // Map base tokens to IColumn
    Map<INormalizedToken, IInternalColumn> baseTokenToColumn = Maps.newLinkedHashMap();
    for (IColumn col : table.getColumns()) {
      for (String sigil : col.getInternalColumn().getSigla()) {
        INormalizedToken baseToken = col.getInternalColumn().getToken(sigil);
        baseTokenToColumn.put(baseToken, col.getInternalColumn());
      }
    }
    //TODO: this should work on a VariantGraph, not on a table!
    // Do the token matching
    TokenIndexMatcher matcher = new TokenIndexMatcher((ITokenContainer) table);
    // Convert matches to legacy
    List<IMatch> result = Lists.newArrayList();
    //BB hier is+ de alignmenttable aanwezig, en moet de informatie over multiple matches verwerkt worden, i.e. 
    List<ITokenMatch> matches = matcher.getMatches(witness);
    List<ITokenMatch> filteredMatches = filterAwaySecondChoicesMultipleColumnsOneToken(filterAwaySecondChoicesMultipleTokensOneColumn(matches, baseTokenToColumn));
    for (ITokenMatch match : filteredMatches) {
      INormalizedToken base = match.getBaseToken();
      INormalizedToken witnessT = match.getWitnessToken();
      IInternalColumn column = baseTokenToColumn.get(base);
      IColumns columns = new Columns(Lists.newArrayList(column));
      IPhrase phrase = new Phrase(Lists.newArrayList(witnessT));
      IMatch columnMatch = new ColumnPhraseMatch(columns, phrase);
      result.add(columnMatch);
    }
    // System.out.println("!!"+result);
    // Order results based on position in table
    List<IMatch> ordered = Lists.newArrayList(result);
    Comparator<? super IMatch> c = new Comparator<IMatch>() {

      @Override
      public int compare(IMatch o1, IMatch o2) {
        return o1.getColumns().getBeginPosition() - o2.getColumns().getBeginPosition();
      }

    };
    Collections.sort(ordered, c);
    // System.out.println("!!"+ordered);
    return ordered;
  }
}