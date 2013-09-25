/**
 *
 *
 * @author dwalend
 * @since 8/16/13 4:25 PM
 */

val graph = SomeGraph.graph



FloydWarshall.allPairsShortestPaths(graph)(TransitiveClosureSemiring)









FloydWarshall.allPairsShortestPaths(graph)(CountFewestNodesBetweenSemiring)






FloydWarshall.allPairsShortestPaths(graph)(new OneShortestPathSemiring[String])











val allPairs = FloydWarshall.allPairsShortestPaths(graph)(new AllShortestPathsSemiring[String])











println(allPairs.edges.mkString("\n"))


































































































































































































































































