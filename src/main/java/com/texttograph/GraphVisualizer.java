package com.texttograph;

import com.texttograph.model.Edge;
import com.texttograph.model.Node;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.nio.Attribute;
import org.jgrapht.nio.AttributeType;
import org.jgrapht.nio.DefaultAttribute;
import org.jgrapht.nio.dot.DOTExporter;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class GraphVisualizer {
    public static void visualizeAndSave(Set<Node> nodes, List<Edge> edges, String filename) {
        // 创建临时文件夹
        File outputDir = new File("graph_output");
        if (!outputDir.exists()) {
            outputDir.mkdir();
        }

        // 生成DOT文件
        String dotPath = outputDir + File.separator + filename + ".dot";
        String pngPath = outputDir + File.separator + filename + ".png";

        generateDotFile(nodes, edges, dotPath);
        generatePng(dotPath, pngPath);
    }


    private static void generateDotFile(Set<Node> nodes, List<Edge> edges, String dotPath) {
        Graph<String, DefaultWeightedEdge> graph =
                new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);

        // 添加节点
        for (Node node : nodes) {
            graph.addVertex(node.getWord());
        }

        // 添加边
        for (Edge edge : edges) {
            DefaultWeightedEdge e = graph.addEdge(
                    edge.getSource().getWord(),
                    edge.getTarget().getWord());
            graph.setEdgeWeight(e, edge.getWeight());
        }

        // 导出为DOT文件
        DOTExporter<String, DefaultWeightedEdge> exporter =
                new DOTExporter<>(v -> v);
        exporter.setEdgeAttributeProvider(edge -> {
            Map<String, Attribute> attrs = new HashMap<>();
            double weight = graph.getEdgeWeight(edge);
            attrs.put("label",
                    new DefaultAttribute<>(String.format("%.1f", weight), AttributeType.STRING));
            attrs.put("fontsize",
                    new DefaultAttribute<>("10", AttributeType.STRING));
            attrs.put("fontcolor",
                    new DefaultAttribute<>("blue", AttributeType.STRING));                 // 设置标签颜色
            return attrs;
        });
        try (Writer writer = new FileWriter(dotPath)) {
            exporter.exportGraph(graph, writer);
            System.out.println("DOT文件生成成功：" + new File(dotPath).getAbsolutePath());
        } catch (IOException e) {
            System.err.println("Error saving DOT file: " + e.getMessage());
        }

        // 其他可视化导出格式，如PNG等
    }
    private static void generatePng(String dotPath, String pngPath) {
        try {
            Process process = Runtime.getRuntime().exec(
                    "dot -Tpng " + dotPath + " -o " + pngPath);

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("PNG图片生成成功：" + pngPath);
                // 自动打开图片（仅限桌面环境）
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(new File(pngPath));
                }
            } else {
                System.err.println("图片生成失败，错误码：" + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("请确认Graphviz已正确安装并添加至PATH");
            System.err.println("官方下载地址：https://graphviz.org/download/");
        }
    }
    // 增强的可视化方法（支持路径高亮）
    public static void visualizePath(
            Set<Node> originalNodes,
            List<Edge> originalEdges,
            List<Node> pathNodes,
            int totalWeight,
            String filename
    ) {
        // 1. 创建临时图结构
        Graph<String, DefaultWeightedEdge> graph =
                new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);

        // 2. 添加所有节点
        originalNodes.forEach(node -> graph.addVertex(node.getWord()));

        // 3. 添加带权重的边并记录路径边
        Set<DefaultWeightedEdge> pathEdges = new HashSet<>();
        Map<String, DefaultWeightedEdge> edgeMap = new HashMap<>();

        for (Edge originalEdge : originalEdges) {
            String source = originalEdge.getSource().getWord();
            String target = originalEdge.getTarget().getWord();
            DefaultWeightedEdge e = graph.addEdge(source, target);
            graph.setEdgeWeight(e, originalEdge.getWeight());
            edgeMap.put(source + "->" + target, e);
        }

        // 4. 识别路径上的边
        if (pathNodes != null && !pathNodes.isEmpty()) {
            for (int i = 0; i < pathNodes.size() - 1; i++) {
                String source = pathNodes.get(i).getWord();
                String target = pathNodes.get(i + 1).getWord();
                DefaultWeightedEdge edge = edgeMap.get(source + "->" + target);
                if (edge != null) {
                    pathEdges.add(edge);
                }
            }
        }

        // 5. 创建自定义的DOT导出器
        DOTExporter<String, DefaultWeightedEdge> exporter =
                new DOTExporter<>(v -> v.replaceAll("\\s+", "_"));  // 处理含空格节点名

        // 6. 设置顶点属性（路径节点高亮）
        exporter.setVertexAttributeProvider(v -> {
            Map<String, Attribute> attrs = new HashMap<>();
            if (isPathNode(v, pathNodes)) {
                attrs.put("color", DefaultAttribute.createAttribute("red"));
                attrs.put("penwidth", DefaultAttribute.createAttribute("3"));
                attrs.put("style", DefaultAttribute.createAttribute("filled"));
                attrs.put("fillcolor", DefaultAttribute.createAttribute("#FFF3E0"));
            }
            return attrs;
        });

        // 7. 设置边属性（路径边高亮）
        exporter.setEdgeAttributeProvider(e -> {
            Map<String, Attribute> attrs = new HashMap<>();
            if (pathEdges.contains(e)) {
                attrs.put("color", DefaultAttribute.createAttribute("blue"));
                attrs.put("penwidth", DefaultAttribute.createAttribute("3"));
            } else {
                attrs.put("color", DefaultAttribute.createAttribute("gray"));
            }
            attrs.put("fontsize", DefaultAttribute.createAttribute("10"));
            attrs.put("label", DefaultAttribute.createAttribute(
                    String.format("%.1f", graph.getEdgeWeight(e))
            ));
            return attrs;
        });

        // 8. 设置图整体属性（添加总权重标注）
        exporter.setGraphAttributeProvider(() -> {
            Map<String, Attribute> attrs = new HashMap<>();
            attrs.put("label", DefaultAttribute.createAttribute(
                    "Total_Weight: " + totalWeight + "\n" +
                            "Path: " + formatPath(pathNodes)
            ));
            attrs.put("labelloc", DefaultAttribute.createAttribute("t"));  // 标题在顶部
            attrs.put("labeljust", DefaultAttribute.createAttribute("l")); // 右对齐
            attrs.put("fontsize", DefaultAttribute.createAttribute("16"));
            return attrs;
        });

        // 9. 生成输出目录
        File outputDir = new File("short_path");
        if (!outputDir.exists()) outputDir.mkdir();

        // 10. 生成DOT文件
        String dotPath = outputDir + File.separator + filename + ".dot";
        try (Writer writer = new FileWriter(dotPath)) {
            exporter.exportGraph(graph, writer);
        } catch (IOException e) {
            System.err.println("DOT文件生成失败: " + e.getMessage());
            return;
        }

        // 11. 转换为PNG图片（需要Graphviz环境）
        String pngPath = outputDir + File.separator + filename + ".png";
        try {
            Process process = Runtime.getRuntime().exec(new String[] {
                    "dot",
                    "-Tpng",
                    dotPath,
                    "-o",
                    new File(pngPath).getAbsolutePath()
            });

            // 读取错误流
            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream()));
            StringBuilder errorMsg = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorMsg.append(line).append("\n");
            }

            int exitCode = process.waitFor();
            if (exitCode == 0) {
                // 自动打开生成的图片
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(new File(pngPath));
                }
            } else {
                System.err.println("Graphviz错误:\n" + errorMsg.toString());
            }
        } catch (Exception e) {
            System.err.println("请确认Graphviz已正确安装并添加至PATH");
            System.err.println("官方下载地址：https://graphviz.org/download/");
        }
    }

    // 辅助方法：判断是否为路径节点
    private static boolean isPathNode(String vertex, List<Node> path) {
        if (path == null) return false;
        return path.stream()
                .anyMatch(node -> node.getWord().equals(vertex));
    }

    // 辅助方法：格式化路径显示
    private static String formatPath(List<Node> path) {
        if (path == null || path.isEmpty()) return "无路径";
        return path.stream()
                .map(Node::getWord)
                .collect(Collectors.joining(" → "));
    }


}