

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;


import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;


/**
 * This class builds a networked TicTacToe game with multi-client interactive sessions. It handles client and server interactions.
 * 
 * @author Fiona
 *
 */

public class TicTacToe implements Runnable {

	private String ip = "localhost";
	private int port = 22222;
	private JFrame frame;
	private final int WIDTH = 506;
	private final int HEIGHT = 527;
	private Thread thread;

	private Painter painter;
	private Socket socket;
	private DataOutputStream dos;
	private DataInputStream dis;

	private ServerSocket serverSocket;
	JButton submit_button;

	private BufferedImage board;
	private BufferedImage redX;
	JTextField inputname;
	
	JFrame Dialog = new JFrame("Messagee");


	private BufferedImage blueCircle;

	private String[] spaces = new String[9];

	private boolean yourTurn = false;
	private boolean circle = true;
	private boolean accepted = false;
	private boolean unableToCommunicateWithOpponent = false;
	private boolean won = false;
	private boolean enemyWon = false;
	private boolean tie = false;
	JLabel text_label;

	private int lengthOfSpace = 160;
	private int errors = 0;
	private int firstSpot = -1;
	private int secondSpot = -1;




	private int[][] wins = new int[][] { { 0, 1, 2 }, { 3, 4, 5 }, { 6, 7, 8 }, { 0, 3, 6 }, { 1, 4, 7 }, { 2, 5, 8 }, { 0, 4, 8 }, { 2, 4, 6 } };


	/**
	 * This constructor builds the TicTacToe GUI interface, builds the frame and in the frame
	 * 
	 */
	public TicTacToe() {

		loadImages();

		painter = new Painter();
		painter.setPreferredSize(new Dimension(WIDTH, HEIGHT));

		if (!connect()) initializeServer();

		frame = new JFrame();
		frame.setTitle("Tic-Tac-Toe");
		
		JPanel gameboard=new JPanel();
		JPanel info_panel=new JPanel();
		JPanel name_panel=new JPanel();	
		gameboard.add(painter);
		
		
		frame.setSize(WIDTH, 700);
		frame.setLocationRelativeTo(null);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		

		
		
		JMenuBar menuBar= new JMenuBar();
		JMenu control_menu = new JMenu("Control");
		JMenu help_menu = new JMenu("Help");
		JMenuItem exit_menuItem = new JMenuItem("Exit");
		JMenuItem instruction_menuItem = new JMenuItem("Instruction");
		exit_menuItem.addActionListener(new exit_menuItem_Listener());
		instruction_menuItem.addActionListener(new instruction_menuItem_Listener());
		
		inputname=new JTextField(20);
		
		text_label=new JLabel();
		text_label.setText("Enter your player name...");
		submit_button=new JButton("Submit");
		
		name_panel.add(inputname);

		name_panel.add(submit_button);
		info_panel.add(text_label);
		control_menu.add(exit_menuItem);
		help_menu.add(instruction_menuItem);
		menuBar.add(control_menu);
		menuBar.add(help_menu);
		frame.add(info_panel,BorderLayout.NORTH);
		frame.add(gameboard,BorderLayout.CENTER);
		frame.add(name_panel,BorderLayout.SOUTH);
		submit_button.addActionListener(new butn_start_Listener());
		
		frame.setJMenuBar(menuBar);
		thread = new Thread(this, "TicTacToe");
		thread.start();
		frame.setVisible(true);
	}

	/**
	 * This overwrites the run() method, calling the repaint() method.
	 * 
	 */
	public void run() {
		while (true) {
			tick();
			painter.repaint();

			if (!circle && !accepted) {
				listenForServerRequest();
			}

		}
	}

	private void render(Graphics g) {
		g.drawImage(board, 0, 0, null);
		if (unableToCommunicateWithOpponent) {
			
			text_label.setText("Game Ends. One of the players left.");
			
			return;
		}

		if (accepted) {
			for (int i = 0; i < spaces.length; i++) {
				if (spaces[i] != null) {
					if (spaces[i].equals("X")) {
						if (circle) {
							g.drawImage(redX, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
						} else {
							g.drawImage(redX, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
						}
					} else if (spaces[i].equals("O")) {
						if (circle) {
							g.drawImage(blueCircle, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
						} else {
							g.drawImage(blueCircle, (i % 3) * lengthOfSpace + 10 * (i % 3), (int) (i / 3) * lengthOfSpace + 10 * (int) (i / 3), null);
						}
					}
				}
			}
			if (won || enemyWon) {
				
				if (won) {
					
					text_label.setText( "Congradulations, you won!");
						
					return;
					
				} else if (enemyWon) {
					
					text_label.setText( "You lose.");
					
					return;
				}
			}
			if (tie) {
				
				text_label.setText( "Tie.");
				return;
				
			}
		} else {
			
			
			return;
		}

	}

	private void tick() {
		if (errors >= 10) unableToCommunicateWithOpponent = true;

		if (!yourTurn && !unableToCommunicateWithOpponent) {
			try {
				int space = dis.readInt();
				if (circle) spaces[space] = "X";
				else spaces[space] = "O";
				checkForEnemyWin();
				checkForTie();
				yourTurn = true;
				text_label.setText("Your opponent has moved, now is your turn");
			} catch (IOException e) {
				e.printStackTrace();
				errors++;
			}
		}
	}

	private void checkForWin() {
		for (int i = 0; i < wins.length; i++) {
			if (circle) {
				if (spaces[wins[i][0]] == "O" && spaces[wins[i][1]] == "O" && spaces[wins[i][2]] == "O") {
					firstSpot = wins[i][0];
					secondSpot = wins[i][2];
					won = true;
				}
			} else {
				if (spaces[wins[i][0]] == "X" && spaces[wins[i][1]] == "X" && spaces[wins[i][2]] == "X") {
					firstSpot = wins[i][0];
					secondSpot = wins[i][2];
					won = true;
				}
			}
		}
	}

	private void checkForEnemyWin() {
		for (int i = 0; i < wins.length; i++) {
			if (circle) {
				if (spaces[wins[i][0]] == "X" && spaces[wins[i][1]] == "X" && spaces[wins[i][2]] == "X") {
					firstSpot = wins[i][0];
					secondSpot = wins[i][2];
					enemyWon = true;
				}
			} else {
				if (spaces[wins[i][0]] == "O" && spaces[wins[i][1]] == "O" && spaces[wins[i][2]] == "O") {
					firstSpot = wins[i][0];
					secondSpot = wins[i][2];
					enemyWon = true;
				}
			}
		}
	}

	private void checkForTie() {
		for (int i = 0; i < spaces.length; i++) {
			if (spaces[i] == null) {
				return;
			}
		}
		tie = true;
	}

	private void listenForServerRequest() {
		Socket socket = null;
		try {
			socket = serverSocket.accept();
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
			accepted = true;
			System.out.println("CLIENT HAS REQUESTED TO JOIN, AND WE HAVE ACCEPTED");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean connect() {
		try {
			socket = new Socket(ip, port);
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
			accepted = true;
		} catch (IOException e) {
			
			return false;
		}
		
		return true;
	}

	private void initializeServer() {
		try {
			serverSocket = new ServerSocket(port, 8, InetAddress.getByName(ip));
		} catch (Exception e) {
			e.printStackTrace();
		}
		yourTurn = true;
		circle = false;
	}

	private void loadImages() {
		try {
			board = ImageIO.read(getClass().getResourceAsStream("/board.png"));
			redX = ImageIO.read(getClass().getResourceAsStream("/redX.png"));
			blueCircle = ImageIO.read(getClass().getResourceAsStream("/blueCircle.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * This main method creates a TicTacToe object.
	 * 
	 * @param args Unused
	 */
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		TicTacToe ticTacToe = new TicTacToe();
	}
	
	/**
	 * This is an ActionListener inner class, once actionPerformed on the instruction button,
	 * the instruction inner dialoug box would be shown
	 * 
	 * @author Leung Hiu Ching
	 *
	 */
	class instruction_menuItem_Listener implements ActionListener{
		public void actionPerformed(ActionEvent event) {
			JOptionPane.showMessageDialog(Dialog, "Some information about the game:\n"+"Criteria for a valid move:\r\n" + 
					"- The move is not occupied by any mark.\r\n" + 
					"- The move is made in the player’s turn.\r\n" + 
					"- The move is made within the 3 x 3 board.\r\n" + 
					"The game would continue and switch among the opposite player until it reaches either one of the following conditions:\r\n" + 
					"- Player 1 wins.\r\n" + 
					"- Player 2 wins.\r\n" + 
					"- Draw.");
		}
	}
	/**
	 * This is an ActionListener inner class, once actionPerformed on the exit button,
	 * the game windows would be closed
	 * 
	 * @author Leung Hiu Ching
	 *
	 */
	class exit_menuItem_Listener implements ActionListener{
		public void actionPerformed(ActionEvent event) {
			System.exit(0);
		}
	}
	/**
	 * This is an ActionListener inner class, once actionPerformed on the start button,
	 * the game will start if bet is correctly placed
	 * 
	 * @author Leung Hiu Ching
	 */
	class butn_start_Listener implements ActionListener{
		public void actionPerformed(ActionEvent event ) {
			String name = inputname.getText();
			text_label.setText("WELCOME "+name);
			inputname.setEnabled(false);
			submit_button.setEnabled(false);
			frame.setTitle("Tic Tac Toe-Player: "+name);
			
			
		}
	}
	private class Painter extends JPanel implements MouseListener {
		private static final long serialVersionUID = 1L;

		public Painter() {
			setFocusable(true);
			requestFocus();
			setBackground(Color.WHITE);
			addMouseListener(this);
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			render(g);
		}

		@Override
		public void mouseClicked(MouseEvent e) {
			if (accepted) {
				if (yourTurn && !unableToCommunicateWithOpponent && !won && !enemyWon) {
					int x = e.getX() / lengthOfSpace;
					int y = e.getY() / lengthOfSpace;
					y *= 3;
					int position = x + y;

					if (spaces[position] == null) {
						if (!circle) spaces[position] = "X";
						else spaces[position] = "O";
						yourTurn = false;
						text_label.setText("Valid move, wait for your opponent");
						repaint();
						Toolkit.getDefaultToolkit().sync();

						try {
							dos.writeInt(position);
							dos.flush();
						} catch (IOException e1) {
							errors++;
							e1.printStackTrace();
						}

						System.out.println("DATA WAS SENT");
						checkForWin();
						checkForTie();

					}
				}
			}
		}
		/**
		 * Override
		 * @param e Unused
		 */
		@Override
		public void mousePressed(MouseEvent e) {

		}
		/**
		 * Override
		 * @param e Unused
		 */

		@Override
		public void mouseReleased(MouseEvent e) {

		}
		/**
		 * Override
		 * @param e Unused
		 */

		@Override
		public void mouseEntered(MouseEvent e) {

		}
		/**
		 * Override
		 * @param e Unused
		 */
		@Override
		public void mouseExited(MouseEvent e) {

		}

	}

}
