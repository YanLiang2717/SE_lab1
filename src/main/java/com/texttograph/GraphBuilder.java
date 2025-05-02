package com.texttograph;

import com.texttograph.model.Edge;
import com.texttograph.model.Node;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class GraphBuilder {
    private Set<Node> nodes;
    private List<Edge> edges;
    private Map<String, Edge> edgeMap;
    // 添加邻接表加速查询桥接词查询
    private Map<Node, Set<Node>> adjacencyList = new HashMap<>();//
    //最短路径计算dij
    public Map<Node, Integer> dijkstra(Node start) {
        Map<Node, Integer> distances = new HashMap<>();
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(n -> distances.getOrDefault(n, Integer.MAX_VALUE)));

        // 初始化
        distances.put(start, 0);
        queue.add(start);

        while (!queue.isEmpty()) {
            Node current = queue.poll();
            for (Edge edge : getEdgesFrom(current)) {
                Node neighbor = edge.getTarget();
                int newDist = distances.get(current) + edge.getWeight();
                if (newDist < distances.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    distances.put(neighbor, newDist);
                    queue.add(neighbor);
                }
            }
        }
        return distances;
    }
    //计算pagerank,使用TF-IDF进行初始化
    private Map<Node, Integer> termFrequencyMap;  // 词频统计
    private Map<Node, List<Edge>> inEdgesMap;     // 入边映射
    private Map<Node, Integer> outDegreeMap;      // 出链数统计
    // PageRank结果封装类
    public static class PageRankResult {
        public final Map<Node, Double> values;
        public final int iterations;

        public PageRankResult(Map<Node, Double> values, int iterations) {
            this.values = Collections.unmodifiableMap(values);
            this.iterations = iterations;
        }
    }

    public GraphBuilder() {
        nodes = new HashSet<>();
        edges = new ArrayList<>();
        edgeMap = new HashMap<>();

    }

    public void buildGraph(List<String> words) {
        if (words.size() < 2) return;

        for (int i = 0; i < words.size() - 1; i++) {
            String currentWord = words.get(i);
            String nextWord = words.get(i + 1);

            Node source = new Node(currentWord);
            Node target = new Node(nextWord);

            nodes.add(source);
            nodes.add(target);
            //维护邻接表
            adjacencyList.computeIfAbsent(source, k -> new HashSet<>()).add(target);

            String edgeKey = source + "->" + target;
            Edge edge = edgeMap.get(edgeKey);

            if (edge == null) {
                edge = new Edge(source, target, 1);
                edges.add(edge);
                edgeMap.put(edgeKey, edge);
            } else {
                edge.incrementWeight();
            }

        }
        // 1. 统计词频
        termFrequencyMap = new HashMap<>();
        for (String word : words) {
            Node node = new Node(word);
            termFrequencyMap.put(node, termFrequencyMap.getOrDefault(node, 0) + 1);
        }

        // 2. 构建入边映射
        inEdgesMap = new HashMap<>();
        for (Edge edge : edges) {
            Node target = edge.getTarget();
            inEdgesMap.computeIfAbsent(target, k -> new ArrayList<>()).add(edge);
        }

        // 3. 统计出链数
        outDegreeMap = adjacencyList.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().size()
                ));

    }

    public Set<Node> getNodes() {
        return nodes;
    }

    public List<Edge> getEdges() {
        return edges;
    }

    public void printGraph() {
        System.out.println("Nodes: " + nodes.size());
        nodes.forEach(node -> System.out.println(node));

        System.out.println("\nEdges: " + edges.size());
        edges.forEach(edge -> System.out.println(edge));
    }
    //新增桥接词查询方法
    public List<String> queryBridgeWords(String word1, String word2) {
        Node node1 = new Node(word1.toLowerCase());
        Node node2 = new Node(word2.toLowerCase());

        // 验证节点存在性
        if (!nodes.contains(node1)) return Collections.singletonList("NO_WORD1");
        if (!nodes.contains(node2)) return Collections.singletonList("NO_WORD2");

        List<String> bridges = new ArrayList<>();

        // 查找桥接词逻辑
        Set<Node> word1Neighbors = adjacencyList.getOrDefault(node1, Collections.emptySet());
        for (Node bridge : word1Neighbors) {
            if (adjacencyList.getOrDefault(bridge, Collections.emptySet()).contains(node2)) {
                bridges.add(bridge.getWord());
            }
        }

        return bridges.isEmpty() ? Collections.singletonList("NO_BRIDGE") : bridges;
    }
    public List<String> getValidBridges(String word1, String word2) {
        Node node1 = new Node(word1.toLowerCase());
        Node node2 = new Node(word2.toLowerCase());

        // 检查节点是否存在
        if (!nodes.contains(node1)) return Collections.emptyList();
        if (!nodes.contains(node2)) return Collections.emptyList();

        List<String> bridges = new ArrayList<>();
        Set<Node> neighbors = adjacencyList.getOrDefault(node1, Collections.emptySet());

        for (Node bridge : neighbors) {
            if (adjacencyList.getOrDefault(bridge, Collections.emptySet()).contains(node2)) {
                bridges.add(bridge.getWord());
            }
        }
        return bridges;
    }
    //获取节点所有出边
    private List<Edge> getEdgesFrom(Node node) {
        return edges.stream()
                .filter(e -> e.getSource().equals(node))
                .collect(Collectors.toList());
    }
    // 获取两点间最短路径（重构路径）
    public PathResult getShortestPath(String word1, String word2) {
        Node start = new Node(word1.toLowerCase());
        Node end = new Node(word2.toLowerCase());

        if (!nodes.contains(start)) return new PathResult("START_NOT_FOUND");
        if (!nodes.contains(end)) return new PathResult("END_NOT_FOUND");

        // 执行Dijkstra算法
        Map<Node, Integer> distances = dijkstra(start);
        if (!distances.containsKey(end)) return new PathResult("NO_PATH");

        // 重构路径
        List<Node> path = reconstructPath(start, end, distances);
        return new PathResult(path, distances.get(end));
    }

    // 路径重构辅助方法
    private List<Node> reconstructPath(Node start, Node end, Map<Node, Integer> dist) {
        LinkedList<Node> path = new LinkedList<>();
        Node current = end;
        while (!current.equals(start)) {
            path.addFirst(current);
            current = findPredecessor(current, dist);
            if (current == null) return Collections.emptyList();
        }
        path.addFirst(start);
        return path;
    }

    private Node findPredecessor(Node node, Map<Node, Integer> dist) {
        return edges.stream()
                .filter(e -> e.getTarget().equals(node))
                .filter(e -> dist.getOrDefault(e.getSource(), Integer.MAX_VALUE) + e.getWeight() == dist.get(node))
                .map(Edge::getSource)
                .findFirst()
                .orElse(null);
    }

    // 路径结果封装类
    public static class PathResult {
        String status; // SUCCESS/ERROR_TYPE
        List<Node> path;
        int totalWeight;

        public PathResult(String error) { this.status = error; }
        public PathResult(List<Node> path, int weight) {
            this.status = "SUCCESS";
            this.path = path;
            this.totalWeight = weight;
        }
    }
    //PageRank计算方法
    public PageRankResult calculatePageRank(double dampingFactor, double epsilon, int maxIter) {
        final int N = nodes.size();
        if (N == 0) return new PageRankResult(Collections.emptyMap(), 0);

        Map<Node, Double> pageRank = new HashMap<>();
        Map<Node, Double> newRank = new HashMap<>();

        // 1. TF-IDF初始化（保持原有逻辑）
        final double logN = Math.log(N);
        double sumTFIDF = nodes.stream()
                .mapToDouble(node -> {
                    int tf = termFrequencyMap.getOrDefault(node, 0);
                    int df = outDegreeMap.getOrDefault(node, 0) + 1; // 避免除零
                    double idf = logN - Math.log(df);
                    return tf * idf;
                })
                .sum();
        final double finalSumTFIDF = sumTFIDF;

        Map<Node, Double> finalPageRank = pageRank;
        nodes.forEach(node -> {
            int tf = termFrequencyMap.getOrDefault(node, 0);
            int df = outDegreeMap.getOrDefault(node, 0) + 1;
            double idf = logN - Math.log(df);
            finalPageRank.put(node, (tf * idf) / finalSumTFIDF);
        });

        // 2. 迭代计算（修正部分）
        int iter = 0;
        for (; iter < maxIter; iter++) {
            // 计算悬挂节点贡献（出度为0的节点）
            double danglingSum = nodes.stream()
                    .filter(n -> outDegreeMap.getOrDefault(n, 0) == 0)
                    .mapToDouble(pageRank::get)
                    .sum();

            // 计算常量项：(1-d)/N + d*(danglingSum/N)
            final double constTerm = (1 - dampingFactor)/N + dampingFactor * danglingSum/N;

            // 计算每个节点的新PageRank
            Map<Node, Double> finalPageRank1 = pageRank;
            Map<Node, Double> finalNewRank = newRank;
            nodes.forEach(node -> {
                double incomingSum = inEdgesMap.getOrDefault(node, Collections.emptyList())
                        .stream()
                        .mapToDouble(edge -> {
                            Node src = edge.getSource();
                            int outDegree = outDegreeMap.getOrDefault(src, 0);

                            // 关键修正：出度为0时使用N作为分母
                            double denominator = (outDegree == 0) ? N : outDegree;
                            return finalPageRank1.get(src) / denominator;
                        })
                        .sum();

                finalNewRank.put(node, constTerm + dampingFactor * incomingSum);
            });

            // 检查收敛条件
            Map<Node, Double> finalNewRank1 = newRank;
            Map<Node, Double> finalPageRank2 = pageRank;
            double diff = nodes.stream()
                    .mapToDouble(n -> Math.abs(finalNewRank1.get(n) - finalPageRank2.get(n)))
                    .sum();
            if (diff < epsilon) break;

            // 交换引用准备下一轮迭代
            Map<Node, Double> temp = pageRank;
            pageRank = newRank;
            newRank = temp;
            newRank.clear();
        }

        return new PageRankResult(pageRank, iter + 1);
    }
    //随机游走
    public RandomWalkResult randomWalk() {
        List<Node> path = new ArrayList<>();
        Set<Edge> visitedEdges = new HashSet<>();
        int totalWeight = 0; // 新增权重累计

        // 1. 随机选择起始节点
        List<Node> nodeList = new ArrayList<>(nodes);
        if (nodeList.isEmpty()) return new RandomWalkResult(Collections.emptyList(), "空图",0);

        Node current = nodeList.get(ThreadLocalRandom.current().nextInt(nodeList.size()));
        path.add(current);

        // 2. 开始遍历
        while (true) {
            // 获取当前节点的所有出边
            List<Edge> outEdges = getEdgesFrom(current);
            if (outEdges.isEmpty()) break;

            // 随机选择一条边
            Edge chosenEdge = outEdges.get(ThreadLocalRandom.current().nextInt(outEdges.size()));

            // 3. 终止条件判断
            if (visitedEdges.contains(chosenEdge)) {
                path.add(chosenEdge.getTarget());
                return new RandomWalkResult(
                        path,
                        "发现重复边: " + chosenEdge.getSource() + "→" + chosenEdge.getTarget(),
                        totalWeight); // 不累加重复边权重
            }

            // 4. 记录路径
            totalWeight += chosenEdge.getWeight(); // 累加权重
            visitedEdges.add(chosenEdge);
            current = chosenEdge.getTarget();
            path.add(current);
        }

        return new RandomWalkResult(path, "无出边终止",totalWeight);
    }

    // 结果封装类
    public static class RandomWalkResult {
        public final List<Node> path;
        public final String terminationReason;
        public final int totalWeight; // 新增权重总和


        public RandomWalkResult(List<Node> path, String reason, int weight) {
            this.path = Collections.unmodifiableList(path);
            this.terminationReason = reason;
            this.totalWeight = weight;
        }
    }

}