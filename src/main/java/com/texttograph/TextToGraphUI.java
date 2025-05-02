package com.texttograph;
import com.texttograph.model.Node;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class TextToGraphUI extends JFrame {
    private JTextArea textArea;
    private JButton openFileBtn;
    private JButton processBtn;
    private JButton visualizeBtn;
    private JFileChooser fileChooser;
    // 添加桥接词查询按钮
    private JButton bridgeBtn ;
    //添加生成新文本按钮
    private JButton generateBtn;
    //添加最短路径生成按钮
    private JButton pathBtn;
    //计算pagerank
    private JButton pageRankBtn;
    //随机游走
    private JButton randomWalkBtn;
    private static final String OUTPUT_FILE = "random_walk.txt";

    private List<String> processedWords;
    private GraphBuilder graphBuilder;

    public TextToGraphUI() {
        setTitle("Text to Graph Converter");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // 初始化组件
        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);

        openFileBtn = new JButton("Open Text File");
        processBtn = new JButton("Process Text");
        visualizeBtn = new JButton("Visualize Graph");
        bridgeBtn = new JButton("Query Bridge Words");
        generateBtn = new JButton("Generate New Text");
        pathBtn = new JButton("Shortest Path");
        pageRankBtn = new JButton("Show PageRank");
        randomWalkBtn = new JButton("Random Walk");

        processBtn.setEnabled(false);
        visualizeBtn.setEnabled(false);
        bridgeBtn.setEnabled(false);
        generateBtn.setEnabled(false);
        pathBtn.setEnabled(false);
        pageRankBtn.setEnabled(false);
        randomWalkBtn.setEnabled(false);

        fileChooser = new JFileChooser();

        // 按钮面板
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(openFileBtn);
        buttonPanel.add(processBtn);
        buttonPanel.add(visualizeBtn);
        // 添加bridgeBtn到按钮面板
        buttonPanel.add(bridgeBtn);
        //添加generateBtn到按钮面板
        buttonPanel.add(generateBtn);
        //添加pathBtn到按钮面板
        buttonPanel.add(pathBtn);
        //计算pagerank
        buttonPanel.add(pageRankBtn);
        buttonPanel.add(randomWalkBtn);


        // 添加组件到窗口
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // 事件监听
        openFileBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int returnVal = fileChooser.showOpenDialog(TextToGraphUI.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    try {
                        String content = new String(Files.readAllBytes(
                                Paths.get(fileChooser.getSelectedFile().getPath())));
                        textArea.setText(content);
                        processBtn.setEnabled(true);
                    } catch (IOException ex) {
                        JOptionPane.showMessageDialog(TextToGraphUI.this,
                                "Error reading file: " + ex.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        processBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String text = textArea.getText();
                processedWords = TextProcessor.processText(text);
                graphBuilder = new GraphBuilder();
                graphBuilder.buildGraph(processedWords);
                graphBuilder.printGraph();
                visualizeBtn.setEnabled(true);
                JOptionPane.showMessageDialog(TextToGraphUI.this,
                        "Text processed successfully!\n" +
                                "Nodes: " + graphBuilder.getNodes().size() + "\n" +
                                "Edges: " + graphBuilder.getEdges().size(),
                        "Processing Complete", JOptionPane.INFORMATION_MESSAGE);
                bridgeBtn.setEnabled(true);
                generateBtn.setEnabled(true);
                pathBtn.setEnabled(true);
                pageRankBtn.setEnabled(true);
                randomWalkBtn.setEnabled(true);

            }
        });

        visualizeBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String filename = JOptionPane.showInputDialog(TextToGraphUI.this,
                        "Enter filename for graph (without extension):",
                        "graph_output");
                if (filename != null && !filename.trim().isEmpty()) {
                    String fullPath = new File("graph_output/" + filename + ".png").getAbsolutePath();
                    GraphVisualizer.visualizeAndSave(
                            graphBuilder.getNodes(),
                            graphBuilder.getEdges(),
                            filename);
                    JOptionPane.showMessageDialog(TextToGraphUI.this,
                            "Graph visualization saved to " + fullPath ,
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });
        bridgeBtn.addActionListener(e -> queryBridgeWords());
        generateBtn.addActionListener(e -> generateNewText());
        pathBtn.addActionListener(e -> calculateShortestPath());
        pageRankBtn.addActionListener(e -> showPageRank());
        randomWalkBtn.addActionListener(e -> performRandomWalk());
    }
    // 桥接词查询处理方法
    private void queryBridgeWords() {
        JPanel inputPanel = new JPanel(new GridLayout(2, 2));
        JTextField word1Field = new JTextField();
        JTextField word2Field = new JTextField();

        inputPanel.add(new JLabel("Word 1:"));
        inputPanel.add(word1Field);
        inputPanel.add(new JLabel("Word 2:"));
        inputPanel.add(word2Field);

        int result = JOptionPane.showConfirmDialog(
                this, inputPanel,
                "Enter Two Words",
                JOptionPane.OK_CANCEL_OPTION
        );

        if (result == JOptionPane.OK_OPTION) {
            String word1 = word1Field.getText().trim();
            String word2 = word2Field.getText().trim();

            List<String> response = graphBuilder.queryBridgeWords(word1, word2);

            // 结果处理逻辑
            String message;
            if (response.get(0).startsWith("NO_")) {
                switch(response.get(0)) {
                    case "NO_WORD1":
                        message = "No " + word1 + " in the graph!";
                        break;
                    case "NO_WORD2":
                        message = "No " + word2 + " in the graph!";
                        break;
                    default:
                        message = "No bridge words from " + word1 + " to " + word2 + "!";
                }
            } else {
                message = formatBridgeWords(response, word1, word2);
            }

            JOptionPane.showMessageDialog(this, message);
        }
    }

    // 格式化输出结果
    private String formatBridgeWords(List<String> bridges, String word1, String word2) {
        if (bridges.isEmpty()) return "";

        StringJoiner joiner = new StringJoiner(", ");
        bridges.forEach(joiner::add);
        String raw = joiner.toString();

        // 替换最后一个逗号为" and"
        String formatted = raw.replaceAll(", ([^,]+)$", " and $1");

        return "The bridge words from " + word1 + " to " + word2
                + " are: " + formatted + ".";
    }
    // 生成新文本核心逻辑
    private void generateNewText() {
        String input = JOptionPane.showInputDialog(this, "请输入需要处理的新文本(英文):");
        if (input == null || input.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "输入不能为空！");
            return;
        }


        // 预处理新文本（保留原始大小写）
        List<String> rawWords = Arrays.asList(input.split(" +"));
        List<String> processedWords = TextProcessor.processText(input);
        if (processedWords.size() < 2) {
            JOptionPane.showMessageDialog(this, "至少需要两个单词才能生成新文本");
            return;
        }
        // 构建结果文本
        StringBuilder newText = new StringBuilder();
        boolean modified = false;
        Random rand = new Random();

        // 遍历原始单词对（保留大小写）
        for (int i = 0; i < processedWords.size() - 1; i++) {
            String current = processedWords.get(i);
            String next = processedWords.get(i + 1);

            // 保留原始单词大小写
            newText.append(rawWords.get(i)).append(" ");

            // 查询桥接词
            List<String> bridges = graphBuilder.getValidBridges(current, next);
            if (!bridges.isEmpty()) {
                String bridge = bridges.get(rand.nextInt(bridges.size()));
                newText.append(bridge).append(" ");
                modified = true;
            }
        }

        // 添加最后一个单词
        newText.append(rawWords.get(rawWords.size() - 1));

        // 显示结果
        if (modified) {
            JOptionPane.showMessageDialog(this,
                    "生成的新文本:\n" + newText.toString().replaceAll(" +", " "));
        } else {
            JOptionPane.showMessageDialog(this, "文本未改变");
        }
    }
    private void calculateShortestPath() {
        String input = JOptionPane.showInputDialog(this,
                "输入一个或两个单词（用空格分隔）:");

        if (input == null) return;
        String[] words = input.trim().split("\\s+");

        // 处理不同输入情况
        if (words.length == 1) {
            handleSingleWord(words[0]);
        } else if (words.length == 2) {
            handleTwoWords(words[0], words[1]);
        } else {
            JOptionPane.showMessageDialog(this, "输入格式错误！");
        }
    }

    private void handleSingleWord(String word) {
        Node start = new Node(word.toLowerCase());
        if (!graphBuilder.getNodes().contains(start)) {
            JOptionPane.showMessageDialog(this, "单词不存在！");
            return;
        }
        // 1. 创建可滚动的文本区域
        JTextArea textArea = new JTextArea(20, 60); // 初始20行，每行约60字符
        textArea.setEditable(false);
        textArea.setLineWrap(true);    // 自动换行
        textArea.setWrapStyleWord(true); // 按单词换行
        textArea.setFont(new Font("等宽字体", Font.PLAIN, 14));

        StringBuilder result = new StringBuilder();
        for (Node end : graphBuilder.getNodes()) {
            if (!end.equals(start)) {
                GraphBuilder.PathResult pr = graphBuilder.getShortestPath(word, end.getWord());
                if (pr.status.equals("SUCCESS")) {
                    result.append(String.format("%s → %s (总权重: %d)\n路径: %s\n\n",
                            word, end.getWord(), pr.totalWeight, formatPath(pr.path)));
                }
            }
        }
        textArea.setText(result.toString());
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(800, 600)); // 设置首选大小
        JOptionPane.showMessageDialog(
                this,
                scrollPane,  // 使用滚动面板作为消息内容
                "单源最短路径查询结果 - " + word,
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void handleTwoWords(String word1, String word2) {
        GraphBuilder.PathResult pr = graphBuilder.getShortestPath(word1, word2);

        switch (pr.status) {
            case "START_NOT_FOUND":
                JOptionPane.showMessageDialog(this, word1 + "不存在！");
                break;
            case "END_NOT_FOUND":
                JOptionPane.showMessageDialog(this, word2 + "不存在！");
                break;
            case "NO_PATH":
                JOptionPane.showMessageDialog(this, "没有路径");
                break;
            case "SUCCESS":
                String pathInfo = String.format("最短路径权重: %d\n路径: %s",
                        pr.totalWeight, formatPath(pr.path));
                // 可视化展示
                String filename = "shortest_path_" + word1 + "_" + word2;
                GraphVisualizer.visualizePath(
                        graphBuilder.getNodes(),
                        graphBuilder.getEdges(),
                        pr.path,
                        pr.totalWeight,
                        filename
                );
                JOptionPane.showMessageDialog(this, pathInfo);
                break;
        }
    }

    private String formatPath(List<Node> path) {
        return path.stream()
                .map(Node::getWord)
                .collect(Collectors.joining(" → "));
    }
    private void showPageRank() {
        GraphBuilder.PageRankResult result = graphBuilder.calculatePageRank(0.85, 1e-6, 100);

        // 创建带滚动条的展示面板
        JTextArea textArea = new JTextArea(25, 60);
        textArea.setEditable(false);
        textArea.setFont(new Font("等宽字体", Font.PLAIN, 14));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);

        // 按PageRank值排序
        List<Map.Entry<Node, Double>> sorted = result.values.entrySet().stream()
                .sorted(Map.Entry.<Node, Double>comparingByValue().reversed())
                .collect(Collectors.toList());

        // 构建显示内容
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("迭代次数: %d\n\n", result.iterations));
        for (Map.Entry<Node, Double> entry : sorted) {
            sb.append(String.format("%-20s %.6f\n",
                    entry.getKey().getWord(),
                    entry.getValue()));
        }

        textArea.setText(sb.toString());

        // 创建滚动面板
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(800, 600));

        // 显示对话框
        JOptionPane.showMessageDialog(
                this,
                scrollPane,
                "PageRank 计算结果",
                JOptionPane.INFORMATION_MESSAGE
        );
    }
    private void performRandomWalk() {
        // 执行随机游走
        GraphBuilder.RandomWalkResult result = graphBuilder.randomWalk();

        // 构建输出内容
        StringBuilder sb = new StringBuilder();
        sb.append("=== 随机游走结果 ===\n")
                .append("终止原因: ").append(result.terminationReason).append("\n")
                .append("路径总权重: ").append(result.totalWeight).append("\n")
                .append("路径序列: \n");
        GraphVisualizer.visualizePath(
                graphBuilder.getNodes(),
                graphBuilder.getEdges(),
                result.path,
                result.totalWeight, // 权重参数不适用
                "random_walk"
        );



        for (int i = 0; i < result.path.size(); i++) {
            sb.append(i + 1).append(". ").append(result.path.get(i).getWord());
            if (i != result.path.size() - 1) sb.append(" → ");
            if ((i + 1) % 5 == 0) sb.append("\n"); // 每5个节点换行
        }
        sb.append("\n\n");

        // 写入文件（追加模式）
        try (FileWriter writer = new FileWriter(OUTPUT_FILE, true)) {
            writer.write(sb.toString());
            // 显示结果
            JOptionPane.showMessageDialog(this,
                    sb.toString(),
                    "随机游走结果",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this,
                    "文件写入失败: " + ex.getMessage(),
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
        }
    }



}