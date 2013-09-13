package com.github.sommeri.less4j.core.compiler.scopes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.sommeri.less4j.core.ast.Expression;
import com.github.sommeri.less4j.core.ast.Variable;
import com.github.sommeri.less4j.core.compiler.scopes.refactoring.BasicScope;
import com.github.sommeri.less4j.core.compiler.scopes.refactoring.ILocalScope;
import com.github.sommeri.less4j.core.compiler.scopes.refactoring.ISurroundingScopes;

public class ScopeView extends BasicScope {

  private ScopeView publicParent = null;
  private List<IScope> publicChilds = null;

  private Map<IScope, ScopeView> fakeChildsMap = new HashMap<IScope, ScopeView>();
  private IScope decoree;
  private IScope joinToParentTree;
  private LocalScopeData fakeLocalData;

  public ScopeView(IScope decoree, IScope joinToParentTree) {
    super(decoree.getLocalScope(), decoree.getSurroundingScopes());
    this.decoree = decoree;
    this.joinToParentTree = joinToParentTree;
  }

  //FIXME: (!!!) functional but hack
  public void saveLocalDataForTheWholeWayUp() {
    this.fakeLocalData = decoree.getLocalData().clone();
    if (hasParent())
      getParent().saveLocalDataForTheWholeWayUp();
  }

  @Override
  public ScopeView getParent() {
    if (publicParent != null)
      return publicParent;

    publicParent = createPublicParent();
    return publicParent;
  }

  public boolean hasParent() {
    return getParent()!=null;
  }

  protected ScopeView createPublicParent() {
    IScope realParent = super.getParent();
    if (realParent != null)
      return createParentScopeView(realParent, decoree, this);

    if (joinToParentTree==null)
      return null;
    
    return new ScopeJointParent(joinToParentTree, this);
  }

  protected ScopeView createParentScopeView(IScope realParent, IScope realChild, ScopeView fakeChild) {
    ScopeView result = new ScopeView(realParent, joinToParentTree);
    result.fakeChildsMap.put(realChild, fakeChild);

    return result;
  }

  @Override
  public List<IScope> getChilds() {
    if (publicChilds != null)
      return publicChilds;

    publicChilds = createPublicChilds();
    return publicChilds;
  }

  protected List<IScope> createPublicChilds() {
    List<IScope> realChilds = super.getChilds();
    if (realChilds == null)
      return null;

    List<IScope> result = new ArrayList<IScope>();
    for (IScope childScope : realChilds) {
      if (fakeChildsMap.containsKey(childScope)) {
        result.add(fakeChildsMap.get(childScope));
      } else {
        result.add(createChildScopeView(childScope));
      }
    }
    
    return result;

  }

  protected IScope createChildScopeView(IScope realChild) {
    ScopeView result = new ScopeView(realChild, joinToParentTree);
    result.publicParent = this;

    return result;
  }

  public IScope getRootScope() {
    if (!hasParent())
      return this;

    return getParent().getRootScope();
  }
  
  public Expression getLocalValue(Variable variable) {
    if (fakeLocalData!=null) {
      //FIXME: (!!!) wrong and weird, probably incorrect (unsnapshoted version will be searched)
      Expression value = fakeLocalData.getVariables().getValue(variable.getName());
      if (value!=null)
        return value;
    }
    return decoree.getLocalValue(variable);
  }

  public Expression getLocalValue(String name) {
    if (fakeLocalData!=null) {
      //FIXME: (!!!) wrong and weird, probably incorrect (unsnapshoted version will be searched)
      Expression value = fakeLocalData.getVariables().getValue(name);
      if (value!=null)
        return value;
    }
    return decoree.getLocalValue(name);
  }

  public ISurroundingScopes getSurroundingScopes() {
    return new DummySurroundingScopes();
  }

  public ILocalScope getLocalScope() {
    return this;
  }
  
  class DummySurroundingScopes implements ISurroundingScopes {

    @Override
    public void addChild(IScope child) {
      ScopeView.this.addChild(child);
    }

    @Override
    public IScope getParent() {
      return ScopeView.this.getParent();
    }

    @Override
    public List<IScope> getChilds() {
      return ScopeView.this.getChilds();
    }

    @Override
    public boolean hasParent() {
      return ScopeView.this.hasParent();
    }

    @Override
    public void setParent(IScope parent) {
      ScopeView.this.setParent(parent);
    }

    @Override
    public int getTreeSize() {
      return ScopeView.this.getTreeSize();
    }
    
  }

}