package com.texttograph;

import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                TextToGraphUI ui = new TextToGraphUI();
                ui.setVisible(true);
                System.out.println("this is a change");
            }
        });
    }
}