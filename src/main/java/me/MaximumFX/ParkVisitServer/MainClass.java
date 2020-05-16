package me.MaximumFX.ParkVisitServer;

import me.MaximumFX.ParkVisitServer.api.ConsoleColor;

import javax.swing.*;

public class MainClass extends JFrame {

	public static void main(String[] args) {
		System.out.println("Oo--------------oO " + ConsoleColor.CYAN + ConsoleColor.BOLD + "ParkVisitServer" + ConsoleColor.RESET + " Oo--------------oO");
		System.out.println(ConsoleColor.GREEN + ConsoleColor.BOLD + "				Version	1.0" + ConsoleColor.RESET);
		System.out.println(ConsoleColor.GREEN + ConsoleColor.BOLD + "				 Author	MaximumFX" + ConsoleColor.RESET);
		System.out.println(ConsoleColor.GREEN + ConsoleColor.BOLD + "				 Status	Enabled" + ConsoleColor.RESET);
		System.out.println("Oo-----------------------oOo-----------------------oO");

		WebsocketServer ws = new WebsocketServer();

		try {
			ws.enable();
		} catch (Exception e) {
			e.printStackTrace();
		}

		Console console = new Console();
		console.init();
		MainClass launcher = new MainClass();
		launcher.setVisible(true);
		console.getFrame().setLocation(launcher.getX() + launcher.getWidth() + launcher.getInsets().right, launcher.getY());
	}

	private MainClass() {
		super();
		setSize(64, 64);
		setResizable(false);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
}