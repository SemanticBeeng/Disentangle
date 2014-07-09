package net.walend.graph.semiring

import net.walend.heap.HeapOrdering
import net.walend.graph.LabelDigraph

/**
 * Finds one minimal path that use the core semiring.
 *
 * @author dwalend
 * @since v0.1.0
 */

class OnePathFirstStep[Node,CoreLabel,Key](coreSupport:SemiringSupport[CoreLabel,Key]) extends SemiringSupport[Option[FirstStepTrait[Node,CoreLabel]],Key]{

  override type Label = Option[FirstStepTrait[Node, CoreLabel]]

  def semiring: Semiring = OnePathSemiring

  def heapOrdering: HeapOrdering[Key] = coreSupport.heapOrdering

  def heapKeyForLabel:Label=>Key = _.fold(coreSupport.heapOrdering.AlwaysBottom)(x => coreSupport.heapKeyForLabel(x.weight))

  case class FirstStep(weight:CoreLabel,step:Option[Node]) extends FirstStepTrait[Node,CoreLabel] {
    /**
     * Overriding equals to speed up.
     */
    override def equals(any:Any) = {
      if (any.isInstanceOf[FirstStep]) {
        val other: FirstStep = any.asInstanceOf[FirstStep]
        if (this eq other) true //if they share a memory address, no need to compare
        else {
          if (weight == other.weight) {
            step == other.step
          } else false
        }
      } else false
    }

    /**
     * Overriding hashCode because I overrode equals.
     */
    override def hashCode():Int = {
      weight.hashCode() ^ step.hashCode()
    }
  }

  def convertEdgeToLabel[EdgeLabel](coreLabelForEdge:(Node,Node,EdgeLabel)=>CoreLabel)
                              (start: Node, end: Node, coreLabel: EdgeLabel):Label = {
    Some(FirstStep(coreLabelForEdge(start,end,coreLabel),Some(end)))
  }

  def convertEdgeToLabelFunc[EdgeLabel](coreLabelForEdge:(Node,Node,EdgeLabel)=>CoreLabel):((Node,Node,EdgeLabel) => Label) = convertEdgeToLabel(coreLabelForEdge)

  object OnePathSemiring extends Semiring {

    // todo report bug that I can't do this here, but I can if I make coreSemiring a val in the outer OnePathFirstStep
    //val coreSemiring = coreSupport.semiring

    def inDomain(label: Label): Boolean = {
      label match {
        case Some(step:FirstStep) => coreSupport.semiring.inDomain(step.weight)
        case None => true
      }
    }

    //identity and annihilator
    val I = Some(FirstStep(coreSupport.semiring.I,None))
    val O = None

    def summary(fromThroughToLabel:Label,currentLabel:Label):Label = {

      if(currentLabel != O) {
        if(fromThroughToLabel != O){
          val currentStep:FirstStepTrait[Node,CoreLabel] = currentLabel.get
          val fromThroughToStep:FirstStepTrait[Node,CoreLabel] = fromThroughToLabel.get
          val summ = coreSupport.semiring.summary(fromThroughToStep.weight,currentStep.weight)
          if (summ==currentStep.weight) currentLabel
          else if (summ==fromThroughToStep.weight) fromThroughToLabel
          else throw new IllegalStateException("Core semiring's summary "+summ+" did not return either current "+currentStep.weight+" or proposed "+fromThroughToStep.weight+" weight.")
        }
        else currentLabel
      }
      else fromThroughToLabel
    }

    def extend(fromThroughLabel:Label,throughToLabel:Label):Label = {
      //changing the match/case to if/else made this disappear from the sampling profiler
      if((fromThroughLabel != O)&&(throughToLabel != O)) {
        val fromThroughStep:FirstStepTrait[Node,CoreLabel] = fromThroughLabel.get
        val throughToStep:FirstStepTrait[Node,CoreLabel] = throughToLabel.get
        //if fromThroughLabel is identity, use throughToSteps. Otherwise the first step is fine
        val step:Option[Node] = if(fromThroughLabel == I) throughToStep.step
        else fromThroughStep.step

        Some(new FirstStep(coreSupport.semiring.extend(fromThroughStep.weight,throughToStep.weight),step))
      }
      else O
    }
  }

  def leastPath(from:Node,to:Node,edges:Seq[(Node,Node,Label)]):Option[Seq[Node]] = {
    import net.walend.graph.AdjacencyLabelDigraph
    val leastPathDigraph = AdjacencyLabelDigraph(edges = edges,noEdgeExistsValue = semiring.O)
    leastPath(from,to)(leastPathDigraph).map(_.map(node => node.value))
  }

  def leastPath(from:Node,to:Node)(leastPathDigraph:LabelDigraph[Node,Label]):Option[Seq[leastPathDigraph.InnerNodeType]] = {

    def leastPathOfInnerNodes(fromInner:Option[leastPathDigraph.InnerNodeType],
                              toInner:Option[leastPathDigraph.InnerNodeType]):Option[Seq[leastPathDigraph.InnerNodeType]] = {
      (fromInner,toInner) match {
        case (Some(f),Some(t)) => {
          val label:Label = leastPathDigraph.label(f,t)
          label match {
            case Some(firstStep) => {
              firstStep.step match {
                case None => Some(Seq.empty[leastPathDigraph.InnerNodeType]) //No further steps. from should be to and the label should be I
                case Some(step) => {
                  val tailOption:Option[Seq[leastPathDigraph.InnerNodeType]] = leastPathOfInnerNodes(leastPathDigraph.innerNode(step),toInner)
                  tailOption match {
                    case Some(tail) => {
                      val innerStep:leastPathDigraph.InnerNodeType = leastPathDigraph.innerNode(step).get
                      Some(innerStep +: tail)}
                    case None => None //Following a broken path. Should never happen.
                  }
                }
              }
            }
            case None => None //No path from one to the other
          }
        }
        case _ => None //One node or the other isn't in the graph
      }

    }

    val fromInner = leastPathDigraph.innerNode(from)
    val toInner = leastPathDigraph.innerNode(to)
    leastPathOfInnerNodes(fromInner,toInner)
  }
}

trait FirstStepTrait[Node,CoreLabel] {

  def weight:CoreLabel

  def step:Option[Node]

}