/**
 *     PureEdgeSim:  A Simulation Framework for Performance Evaluation of Cloud, Edge and Mist Computing Environments 
 *
 *     This file is part of PureEdgeSim Project.
 *
 *     PureEdgeSim is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     PureEdgeSim is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with PureEdgeSim. If not, see <http://www.gnu.org/licenses/>.
 *     
 *     @author Charafeddine Mechalikh
 **/
package examples;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Searches a placement of a service graph with a genetic algorithm.
 * <p>
 * A chromosome has one gene per subtask, and each gene is the node that hosts
 * that subtask. Subtasks that receive the same node end up in the same
 * partition, so the algorithm searches partitions and placements at the same
 * time, without the balance constraint of {@link GraphPartitioner}.
 * <p>
 * The fitness of a chromosome is estimated on the graph before the simulation
 * runs:
 * <ul>
 * <li>walk the subtasks in topological order and compute the earliest finish
 * time of each one, adding the transfer time of every dependency that crosses
 * two nodes;</li>
 * <li>add the makespan (the last finish time), a fraction of the total transfer
 * time, and a fraction of the work of the busiest node above the average;</li>
 * </ul>
 * The selection is a tournament of {@value #TOURNAMENT_SIZE}, the crossover is
 * uniform, the mutation replaces one gene by a random node, and the best
 * {@value #ELITISM} chromosomes survive untouched. The seed of the generator is
 * the only source of randomness, so two runs with the same seed return the same
 * placement.
 *
 * @author Charafeddine Mechalikh
 * @since PureEdgeSim 6.0
 *
 * @see ServiceGraph
 * @see Example10GeneticOrchestrator
 */
public class GeneticPartitioner {

	/** Number of chromosomes in each generation. */
	public static final int DEFAULT_POPULATION_SIZE = 40;

	/** Number of generations before the best chromosome is returned. */
	public static final int DEFAULT_GENERATIONS = 60;

	/** Number of chromosomes that compete in each selection. */
	public static final int TOURNAMENT_SIZE = 3;

	/** Probability that two parents exchange their genes. */
	public static final double CROSSOVER_RATE = 0.85;

	/** Probability that a chromosome is mutated, spread over its genes. */
	public static final double MUTATION_RATE = 0.1;

	/** Number of best chromosomes copied unchanged to the next generation. */
	public static final int ELITISM = 2;

	/** Weight of the estimated transfer time in the fitness. */
	public static final double COMMUNICATION_WEIGHT = 0.1;

	/** Weight of the excess work of the busiest node in the fitness. */
	public static final double IMBALANCE_WEIGHT = 0.5;

	/** One node the algorithm can choose for a subtask. */
	public static final class Candidate {

		private final int id;
		private final String label;
		private final double mips;
		private final double bandwidthBitsPerSecond;

		/**
		 * Creates a candidate.
		 *
		 * @param id                     identifier of the computing node
		 * @param label                  readable description, such as its type
		 * @param mips                   computing capacity, in MI per second
		 * @param bandwidthBitsPerSecond representative link rate of the node
		 */
		public Candidate(int id, String label, double mips, double bandwidthBitsPerSecond) {
			this.id = id;
			this.label = label;
			this.mips = Math.max(1, mips);
			this.bandwidthBitsPerSecond = Math.max(1, bandwidthBitsPerSecond);
		}

		/**
		 * Returns the identifier of the computing node.
		 *
		 * @return the node identifier
		 */
		public int getId() {
			return id;
		}

		/**
		 * Returns the readable description of the node.
		 *
		 * @return the label
		 */
		public String getLabel() {
			return label;
		}

		/**
		 * Returns the computing capacity of the node.
		 *
		 * @return the capacity, in MI per second
		 */
		public double getMips() {
			return mips;
		}

		/**
		 * Returns the link rate used for the transfer estimates.
		 *
		 * @return the bandwidth, in bits per second
		 */
		public double getBandwidthBitsPerSecond() {
			return bandwidthBitsPerSecond;
		}

		@Override
		public String toString() {
			return label + "#" + id;
		}
	}

	/** The best placement found by the algorithm. */
	public static final class Plan {

		private final ServiceGraph graph;
		private final List<Candidate> candidates;
		private final int[] assignment;
		private final double fitness;
		private final double makespanSeconds;
		private final double communicationSeconds;
		private final double imbalance;

		Plan(ServiceGraph graph, List<Candidate> candidates, int[] assignment, Evaluation evaluation) {
			this.graph = graph;
			this.candidates = Collections.unmodifiableList(candidates);
			this.assignment = assignment.clone();
			this.fitness = evaluation.fitness;
			this.makespanSeconds = evaluation.makespanSeconds;
			this.communicationSeconds = evaluation.communicationSeconds;
			this.imbalance = evaluation.imbalance;
		}

		/**
		 * Returns the index of the node chosen for a subtask.
		 *
		 * @param step the step index in the graph
		 * @return the index in the candidate list
		 */
		public int candidateIndex(int step) {
			return assignment[step];
		}

		/**
		 * Returns the node chosen for every subtask, by name.
		 *
		 * @return an immutable subtask name to node label map
		 */
		public Map<String, String> placement() {
			Map<String, String> placement = new LinkedHashMap<>();
			for (int step = 0; step < graph.size(); step++)
				placement.put(graph.getSteps().get(step).getName(), candidates.get(assignment[step]).toString());
			return placement;
		}

		/**
		 * Returns the fitness of the best chromosome, lower is better.
		 *
		 * @return the fitness
		 */
		public double getFitness() {
			return fitness;
		}

		/**
		 * Returns the estimated makespan of the placement.
		 *
		 * @return the makespan, in seconds
		 */
		public double getMakespanSeconds() {
			return makespanSeconds;
		}

		/**
		 * Returns the estimated time spent transferring results between nodes.
		 *
		 * @return the transfer time, in seconds
		 */
		public double getCommunicationSeconds() {
			return communicationSeconds;
		}

		/**
		 * Returns the excess work of the busiest node over the average of the nodes
		 * that receive at least one subtask.
		 *
		 * @return the imbalance, zero when the work is spread evenly
		 */
		public double getImbalance() {
			return imbalance;
		}

		@Override
		public String toString() {
			return "fitness " + format(fitness) + " (makespan " + format(makespanSeconds) + " s, transfers "
					+ format(communicationSeconds) + " s, imbalance " + format(imbalance) + ")"
					+ System.lineSeparator() + "  " + placement();
		}

		private static String format(double value) {
			return String.format(java.util.Locale.ROOT, "%.4f", value);
		}
	}

	private final long seed;
	private final int populationSize;
	private final int generations;

	/**
	 * Creates a partitioner with the default population and number of generations.
	 *
	 * @param seed the seed of the random generator
	 */
	public GeneticPartitioner(long seed) {
		this(seed, DEFAULT_POPULATION_SIZE, DEFAULT_GENERATIONS);
	}

	/**
	 * Creates a partitioner.
	 *
	 * @param seed           the seed of the random generator
	 * @param populationSize the number of chromosomes in each generation
	 * @param generations    the number of generations
	 * @throws IllegalArgumentException if the population is lower than two or the
	 *                                  number of generations is negative
	 */
	public GeneticPartitioner(long seed, int populationSize, int generations) {
		if (populationSize < 2)
			throw new IllegalArgumentException("A population needs at least two chromosomes");
		if (generations < 0)
			throw new IllegalArgumentException("The number of generations cannot be negative");
		this.seed = seed;
		this.populationSize = populationSize;
		this.generations = generations;
	}

	/**
	 * Searches the placement with the lowest estimated fitness.
	 *
	 * @param graph      the service graph, which must be acyclic
	 * @param candidates the nodes the algorithm can choose, not empty
	 * @return the best placement found
	 * @throws IllegalArgumentException if there is no candidate
	 */
	public Plan partition(ServiceGraph graph, List<Candidate> candidates) {
		if (candidates.isEmpty())
			throw new IllegalArgumentException("A placement needs at least one computing node");
		List<Integer> order = graph.topologicalOrder();
		Random random = new Random(seed);
		int genes = graph.size();

		List<Evaluation> population = new ArrayList<>(populationSize);
		for (int chromosome = 0; chromosome < populationSize; chromosome++)
			population.add(evaluate(randomGenes(genes, candidates.size(), random), graph, candidates, order));

		Evaluation best = lowest(population);
		for (int generation = 0; generation < generations; generation++) {
			Collections.sort(population);
			List<Evaluation> next = new ArrayList<>(populationSize);
			for (int elite = 0; elite < Math.min(ELITISM, population.size()); elite++)
				next.add(population.get(elite));
			while (next.size() < populationSize) {
				int[] first = tournament(population, random).genes;
				int[] second = tournament(population, random).genes;
				int[] child = crossover(first, second, random);
				mutate(child, candidates.size(), random);
				next.add(evaluate(child, graph, candidates, order));
			}
			population = next;
			Evaluation generationBest = lowest(population);
			if (generationBest.fitness < best.fitness)
				best = generationBest;
		}
		return new Plan(graph, candidates, best.genes, best);
	}

	private int[] randomGenes(int count, int candidates, Random random) {
		int[] genes = new int[count];
		for (int gene = 0; gene < count; gene++)
			genes[gene] = random.nextInt(candidates);
		return genes;
	}

	private static Evaluation lowest(List<Evaluation> population) {
		Evaluation best = population.get(0);
		for (Evaluation evaluation : population)
			if (evaluation.fitness < best.fitness)
				best = evaluation;
		return best;
	}

	private static Evaluation tournament(List<Evaluation> population, Random random) {
		Evaluation winner = population.get(random.nextInt(population.size()));
		for (int opponent = 1; opponent < TOURNAMENT_SIZE; opponent++) {
			Evaluation challenger = population.get(random.nextInt(population.size()));
			if (challenger.fitness < winner.fitness)
				winner = challenger;
		}
		return winner;
	}

	private static int[] crossover(int[] first, int[] second, Random random) {
		if (random.nextDouble() >= CROSSOVER_RATE)
			return first.clone();
		int[] child = new int[first.length];
		for (int gene = 0; gene < first.length; gene++)
			child[gene] = random.nextBoolean() ? first[gene] : second[gene];
		return child;
	}

	private static void mutate(int[] genes, int candidates, Random random) {
		double rate = MUTATION_RATE / genes.length;
		for (int gene = 0; gene < genes.length; gene++)
			if (random.nextDouble() < rate)
				genes[gene] = random.nextInt(candidates);
	}

	/**
	 * Estimates the makespan, transfer time and imbalance of one chromosome.
	 *
	 * @param genes      the node of every subtask
	 * @param graph      the service graph
	 * @param candidates the available nodes
	 * @param order      the topological order of the subtasks
	 * @return the evaluation
	 */
	private static Evaluation evaluate(int[] genes, ServiceGraph graph, List<Candidate> candidates,
			List<Integer> order) {
		double[] finish = new double[graph.size()];
		double[] work = new double[candidates.size()];
		double transfers = 0;

		for (int step : order) {
			ServiceGraph.Step current = graph.getSteps().get(step);
			Candidate node = candidates.get(genes[step]);
			double start = 0;
			for (String dependency : current.getDependencies()) {
				int parent = graph.indexOf(dependency);
				double transfer = 0;
				if (genes[parent] != genes[step]) {
					double bandwidth = Math.min(candidates.get(genes[parent]).bandwidthBitsPerSecond,
							node.bandwidthBitsPerSecond);
					transfer = graph.getSteps().get(parent).getResultSize() / bandwidth;
				}
				transfers += transfer;
				start = Math.max(start, finish[parent] + transfer);
			}
			double duration = current.getLength() / node.mips;
			finish[step] = start + duration;
			work[genes[step]] += duration;
		}

		double makespan = 0;
		double totalWork = 0;
		double busiest = 0;
		int usedNodes = 0;
		for (double value : finish)
			makespan = Math.max(makespan, value);
		for (double value : work) {
			totalWork += value;
			busiest = Math.max(busiest, value);
			if (value > 0)
				usedNodes++;
		}
		double average = usedNodes == 0 ? 0 : totalWork / usedNodes;
		double imbalance = average == 0 ? 0 : (busiest - average) / average;
		double fitness = makespan + COMMUNICATION_WEIGHT * transfers + IMBALANCE_WEIGHT * (busiest - average);
		return new Evaluation(genes, fitness, makespan, transfers, imbalance);
	}

	private static final class Evaluation implements Comparable<Evaluation> {

		private final int[] genes;
		private final double fitness;
		private final double makespanSeconds;
		private final double communicationSeconds;
		private final double imbalance;

		Evaluation(int[] genes, double fitness, double makespanSeconds, double communicationSeconds, double imbalance) {
			this.genes = genes;
			this.fitness = fitness;
			this.makespanSeconds = makespanSeconds;
			this.communicationSeconds = communicationSeconds;
			this.imbalance = imbalance;
		}

		@Override
		public int compareTo(Evaluation other) {
			return Double.compare(fitness, other.fitness);
		}
	}
}
