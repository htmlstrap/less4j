package com.github.sommeri.less4j.core.compiler;

import java.util.ArrayList;
import java.util.List;

import com.github.sommeri.less4j.core.ast.ASTCssNode;
import com.github.sommeri.less4j.core.ast.ASTCssNodeType;
import com.github.sommeri.less4j.core.ast.Body;
import com.github.sommeri.less4j.core.ast.Declaration;
import com.github.sommeri.less4j.core.ast.Expression;
import com.github.sommeri.less4j.core.ast.Media;
import com.github.sommeri.less4j.core.ast.MediaExpression;
import com.github.sommeri.less4j.core.ast.MediaExpressionFeature;
import com.github.sommeri.less4j.core.ast.RuleSet;
import com.github.sommeri.less4j.core.ast.StyleSheet;
import com.github.sommeri.less4j.core.compiler.expressions.ExpressionEvaluator;
import com.github.sommeri.less4j.core.compiler.problems.ProblemsHandler;
import com.github.sommeri.less4j.core.compiler.scopes.Scope;
import com.github.sommeri.less4j.core.compiler.stages.ASTManipulator;
import com.github.sommeri.less4j.core.compiler.stages.NestedRulesCollector;
import com.github.sommeri.less4j.core.compiler.stages.ReferencesSolver;
import com.github.sommeri.less4j.core.compiler.stages.ScopeExtractor;

public class LessToCssCompiler {

  private ProblemsHandler problemsHandler;

  public LessToCssCompiler(ProblemsHandler problemsHandler) {
    super();
    this.problemsHandler = problemsHandler;
  }

  public ASTCssNode compileToCss(StyleSheet less) {
    ScopeExtractor scopeBuilder = new ScopeExtractor();
    Scope scope = scopeBuilder.extractScope(less);
    
    ReferencesSolver referencesSolver = new ReferencesSolver(problemsHandler);
    referencesSolver.solveReferences(less, scope);
    
    //just a safety measure
    evaluateExpressions(less);
    freeNestedRuleSets(less);

    return less;
  }

  private void freeNestedRuleSets(Body<ASTCssNode> body) {
    NestedRulesCollector nestedRulesCollector = new NestedRulesCollector();
    
    List<? extends ASTCssNode> childs = new ArrayList<ASTCssNode>(body.getChilds());
    for (ASTCssNode kid : childs) {
      if (kid.getType() == ASTCssNodeType.RULE_SET) {
        List<RuleSet> nestedRulesets = nestedRulesCollector.collectNestedRuleSets((RuleSet) kid);
        body.addMembersAfter(nestedRulesets, kid);
        for (RuleSet ruleSet : nestedRulesets) {
          ruleSet.setParent(body);
        }
      }
      if (kid.getType() == ASTCssNodeType.MEDIA) {
        freeNestedRuleSets((Media) kid);
      }
    }
  }

  private void evaluateExpressions(ASTCssNode node) {
    ASTManipulator manipulator = new ASTManipulator();
    //variables are not supposed to be there now
    ExpressionEvaluator expressionEvaluator = new ExpressionEvaluator(problemsHandler);

    if (node instanceof Expression) {
      Expression value = expressionEvaluator.evaluate((Expression) node);
      manipulator.replace(node, value);
    } else {
      List<? extends ASTCssNode> childs = node.getChilds();
      for (ASTCssNode kid : childs) {
        switch (kid.getType()) {
        case MEDIA_EXPRESSION:
          evaluateInMediaExpressions((MediaExpression) kid);
          break;

        case DECLARATION:
          evaluateInDeclaration((Declaration) kid);
          break;

        default:
          evaluateExpressions(kid);
          break;
        }

      }
    }
  }

  private void evaluateInDeclaration(Declaration node) {
    if (!node.isFontDeclaration()) {
      evaluateExpressions(node);
      return;
    }
  }

  private void evaluateInMediaExpressions(MediaExpression node) {
    MediaExpressionFeature feature = node.getFeature();
    if (!feature.isRatioFeature()) {
      evaluateExpressions(node);
      return;
    }
  }

}
