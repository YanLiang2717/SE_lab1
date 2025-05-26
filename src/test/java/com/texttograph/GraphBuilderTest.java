package com.texttograph;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;

import static org.junit.jupiter.api.Assertions.*;

public class GraphBuilderTest {
    private GraphBuilder builder;

    @BeforeEach
    public void setUp() throws IOException {
        builder = new GraphBuilder();
        String text = new String(Files.readAllBytes(
                Paths.get("src/test/resources/text.txt")));
        List<String> words = TextProcessor.processText(text);
        builder.buildGraph(words);

    }
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

    // 测试用例1：存在桥接词
    @Test
    public void testBridgeWordExists() {
        String word1 = "but";
        String word2 = "team";
        List<String> response = builder.queryBridgeWords(word1, word2);

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


        assertEquals("The bridge words from but to team are: the.", message);
    }

    // 测试用例2：无桥接词但单词存在
    @Test
    public void testNoBridgeWords() {

        String word1 = "but";
        String word2 = "requested";
        List<String> response = builder.queryBridgeWords(word1, word2);

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
        assertEquals("No bridge words from but to requested!", message);
    }

    // 测试用例3：空单词输入
    @Test
    public void testEmptyWordInput() {
        String word1 = "but";
        String word2 = "";
        List<String> response = builder.queryBridgeWords(word1, word2);

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
        assertEquals("No  in the graph!", message);
    }
}