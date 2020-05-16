package me.MaximumFX.ParkVisitServer;

import javax.swing.*;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;

class Console {

	private final JFrame frame = new JFrame();
	Console() {
		JTextArea textArea = new JTextArea(24, 80);
		textArea.setBackground(Color.BLACK);
		textArea.setForeground(Color.LIGHT_GRAY);
		textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		System.setOut(new PrintStream(new OutputStream() {
			@Override
			public void write(int b) {
				textArea.append(String.valueOf((char) b));
			}
		}));
		JScrollPane scroll = new JScrollPane(textArea);
		scroll.setBorder(null);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		frame.add(scroll);
	}
	void init() {
		frame.pack();
		frame.setVisible(true);
	}
	JFrame getFrame() {
		return frame;
	}
}
