package com.texttograph;
import com.texttograph.model.Node;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
public class GraphBuilderTest2 {
    private GraphBuilder builder;
    private TextToGraphUI text;

    @BeforeEach
    public void setUp() throws IOException {
        builder = new GraphBuilder();
        String text = new String(Files.readAllBytes(
                Paths.get("src/test/resources/text2.txt")));
        List<String> words = TextProcessor.processText(text);
        builder.buildGraph(words);
    }

    // 测试用例1：输入两个有效单词（存在路径）
    @Test
    public void testTwoValidWords() {
        GraphBuilder.PathResult result = builder.getShortestPath("A", "C");
        assertEquals("a → b → c", formatPath(result.path));

    }

    // 测试用例2：输入单个单词（查询到所有可达节点）
    @Test
    public void testSingleWord() {
        Map<Node, GraphBuilder.PathResult> allPaths = new HashMap<>();
        for (Node node : builder.getNodes()) {
            if (!node.getWord().equals("A")) {
                allPaths.put(node, builder.getShortestPath("A", node.getWord()));
            }
        }

        // 验证关键路径
        assertEquals("a → b", formatPath(allPaths.get(new Node("B")).path));
        assertEquals("a → b → x", formatPath(allPaths.get(new Node("X")).path));
        assertEquals("a → b → c", formatPath(allPaths.get(new Node("C")).path));
        assertEquals("a → b → c → d", formatPath(allPaths.get(new Node("D")).path));

    }

    // 测试用例3：输入不存在于图中的单词
    @Test
    public void testInvalidWords() {
        // Case 3.1: 起始单词不存在
        GraphBuilder.PathResult result1 = builder.getShortestPath("UNKNOWN", "A");
        assertEquals("START_NOT_FOUND", result1.status);

        // Case 3.2: 目标单词不存在
        GraphBuilder.PathResult result2 = builder.getShortestPath("A", "UNKNOWN");
        assertEquals("END_NOT_FOUND", result2.status);

        // Case 3.3: 单词不存在（输入一个单词）



    }
    @Test
    public void testNoPathBetweenNodes() {
        GraphBuilder.PathResult result = builder.getShortestPath("x", "b");
        //System.out.println(formatPath(result.path));
        assertEquals("NO_PATH", result.status);

    }
    @Test
    public void testSingleNonExistentWord() {
        String word = "unknow";
        Node start = new Node(word.toLowerCase());

        assertFalse(builder.getNodes().contains(start));
    }

    @Test
    public void testMultiWordInput() {
        String input = "known a a"; // 多个空格分隔
        String[] words = input.trim().split("\\s+");
        assertTrue(words.length>2); // 应能正确分割
    }
    // 辅助方法：格式化路径输出
    private String formatPath(List<Node> path) {
        return path.stream()
                .map(Node::getWord)
                .collect(Collectors.joining(" → "));
    }
}
