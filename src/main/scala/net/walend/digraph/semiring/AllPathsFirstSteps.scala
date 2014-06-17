package net.walend.digraph.semiring

import net.walend.heap.HeapOrdering
import net.walend.digraph.LabelDigraph

/**
 * Finds all minimal paths that use the core semiring.
 *
 * @author dwalend
 * @since v0.1.0
 */

//todo is there some way to make this an inner class and a parameter? (Same for OnePathFirstStep)
//todo drop pathcount after you've got it working in Brandes' algorithm
case class FirstSteps[Node,CoreLabel](weight:CoreLabel,pathCount:Int,choices:Set[Node]) {

  /**
   * Overriding equals to speed up.
   */
  override def equals(any:Any) = {
    if (any.isInstanceOf[FirstSteps[Node, CoreLabel]]) {
      val other: FirstSteps[Node, CoreLabel] = any.asInstanceOf[FirstSteps[Node, CoreLabel]]
      if (this eq other) true //if they share a memory address, no need to compare
      else {
        if (weight == other.weight) {
          choices == other.choices
        } else false
      }
    } else false
  }

  /**
   * Overriding hashCode because I overrode equals.
   */
  override def hashCode():Int = {
    weight.hashCode() ^ choices.hashCode()
  }
}

class AllPathsFirstSteps[Node,CoreLabel,Key](coreSupport:SemiringSupport[CoreLabel,Key]) extends SemiringSupport[Option[FirstSteps[Node,CoreLabel]],Key]{
  
  override type Label = Option[FirstSteps[Node, CoreLabel]]

  def semiring: Semiring = AllPathsSemiring

  def heapOrdering: HeapOrdering[Key] = coreSupport.heapOrdering

  def heapKeyForLabel = {
    case Some(nextStep) => coreSupport.heapKeyForLabel(nextStep.weight)
    case None => coreSupport.heapOrdering.AlwaysBottom
  }

  def convertArcToLabel[ArcLabel](coreLabelForArc:(Node,Node,ArcLabel)=>CoreLabel)
                              (start: Node, end: Node, arcLabel: ArcLabel):Label = {
    Some(FirstSteps[Node,CoreLabel](coreLabelForArc(start,end,arcLabel),1,Set(end)))
  }

  def convertArcToLabelFunc[ArcLabel](coreLabelForArc:(Node,Node,ArcLabel)=>CoreLabel):((Node,Node,ArcLabel) => Label) = convertArcToLabel(coreLabelForArc)

  /*
  //branching to figure this out
  //todo use a Stream
  def allPaths(labelGraph:Digraph[Node,Label],from:Node,to:Node):Seq[(Node,Seq[(Label,Node)])] = {

    val innerFrom = labelGraph.innerNode(from).getOrElse(throw new IllegalArgumentException(s"$from not in labelGraph"))
    val innerTo = labelGraph.innerNode(to).getOrElse(throw new IllegalArgumentException(s"$to not in labelGraph"))

    val label:Label = labelGraph.arc(innerFrom,innerTo)
    label match {
      case Some(firstSteps) => {
        firstSteps.choices.map(step => allPaths(labelGraph,step,to))


      }
      case None => Seq.empty
    }
  }
  */
  /**
   * Create the subgraph defined by AllPathsFirstSteps
   */

  def subgraphEdges(labelGraph:LabelDigraph[Node,Label],from:Node,to:Node):Set[(labelGraph.InnerNodeType,labelGraph.InnerNodeType,Label)] = {

    val innerTo = labelGraph.innerNode(to).getOrElse(throw new IllegalArgumentException(s"$to not in labelGraph"))
    //todo capture visited nodes and don't revisit them, by taking innerFrom as a Set, pulling out bits, passing in Sets of novel nodes to visit, and passing around another set of nodes already visited.
    def recurse(innerFrom:labelGraph.InnerNodeType,innerTo:labelGraph.InnerNodeType):Set[(labelGraph.InnerNodeType,labelGraph.InnerNodeType,Label)] = {
      val label:Label = labelGraph.label(innerFrom,innerTo)
      label match {
        case Some(firstSteps) => {

          val innerChoices = firstSteps.choices.map(choice => labelGraph.innerNode(choice).get)
          val closeArcs:Set[(labelGraph.InnerNodeType,labelGraph.InnerNodeType,Label)] = innerChoices.map((innerFrom,_,label))

          val farArcs:Set[(labelGraph.InnerNodeType,labelGraph.InnerNodeType,Label)] = innerChoices.map(recurse(_,innerTo)).flatten

          closeArcs ++ farArcs
        }
        case None => Set.empty
      }
    }
    val innerFrom = labelGraph.innerNode(from).getOrElse(throw new IllegalArgumentException(s"$from not in labelGraph"))

    recurse(innerFrom,innerTo)
  }

  object AllPathsSemiring extends Semiring {

// todo report bug that I can't do this here
// val coreSemiring = coreSupport.semiring

    def inDomain(label: Label): Boolean = {
      label match {
        case Some(steps:FirstSteps[Node,CoreLabel]) => coreSupport.semiring.inDomain(steps.weight)
        case None => true
      } 
    }
    
    //identity and annihilator
    val I = Some(FirstSteps[Node,CoreLabel](coreSupport.semiring.I,1,Set[Node]()))
    val O = None

    def summary(fromThroughToLabel:Label,currentLabel:Label):Label = {

      if(currentLabel != O) {
        if(fromThroughToLabel != O){
          val currentSteps:FirstSteps[Node,CoreLabel] = currentLabel.get
          val fromThroughToSteps:FirstSteps[Node,CoreLabel] = fromThroughToLabel.get
          val summ = coreSupport.semiring.summary(fromThroughToSteps.weight,currentSteps.weight)
          if((summ==fromThroughToSteps.weight)&&(summ==currentSteps.weight)) {
            Some(new FirstSteps[Node,CoreLabel](currentSteps.weight,
                                                currentSteps.pathCount+fromThroughToSteps.pathCount,
                                                currentSteps.choices ++ fromThroughToSteps.choices))
          }
          else if (summ==fromThroughToSteps.weight) fromThroughToLabel
          else if (summ==currentSteps.weight) currentLabel
          else throw new IllegalStateException("Core semiring's summary "+summ+" did not return either current "+currentSteps.weight+" or proposed "+fromThroughToSteps.weight+" weigt.")
        }
        else currentLabel
      }
      else fromThroughToLabel
    }

    def extend(fromThroughLabel:Label,throughToLabel:Label):Label = {
      //changing the match/case to if/else made this disappear from the sampling profiler
      if((fromThroughLabel != O)&&(throughToLabel != O)) {
        val fromThroughSteps:FirstSteps[Node,CoreLabel] = fromThroughLabel.get
        val throughToSteps:FirstSteps[Node,CoreLabel] = throughToLabel.get
        //if fromThroughLabel is identity, use throughToSteps. Otherwise the first step is fine
        val choices:Set[Node] = if(fromThroughLabel == I) throughToSteps.choices
                                else fromThroughSteps.choices

        Some(new FirstSteps[Node,CoreLabel](coreSupport.semiring.extend(fromThroughSteps.weight,throughToSteps.weight),
                                            fromThroughSteps.pathCount * throughToSteps.pathCount,
                                            choices))
      }
      else O
    }
  } 
}
