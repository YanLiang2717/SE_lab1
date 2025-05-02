package com.texttograph;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TextProcessor {
    public static List<String> processText(String text) {
        // 1. 将换行符/回车符替换为空格
        text = text.replaceAll("[\\r\\n]+", " ");

        // 2. 将所有标点符号和非字母字符替换为空格
        text = text.replaceAll("[^a-zA-Z\\s]", " ");

        // 3. 将多个连续空格合并为一个
        text = text.replaceAll("\\s+", " ").trim();

        // 分割为单词列表并转为小写
        return Arrays.stream(text.split(" "))
                .filter(word -> !word.isEmpty())
                .map(String::toLowerCase)
                .collect(Collectors.toList());
    }
}