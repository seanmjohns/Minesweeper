package com.pigcoder.minesweeper;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class Minesweeper extends JFrame {

	public static int columns = 25;
	public static int rows = 25;

	public static int nextColumns = columns;
	public static int nextRows = rows;

	public static final int MAXSIZE = 500;

	public static Tile tiles[][] = new Tile[MAXSIZE][MAXSIZE];

	public static int mineFieldBorderWidth = 5;

	public static Dimension mineFieldSize = new Dimension(
			(int)((columns)*Tile.SIZE.getWidth() + (columns)*Tile.margin) + mineFieldBorderWidth*2,
			(int)((rows)*Tile.SIZE.getHeight() + (rows)*Tile.margin) + mineFieldBorderWidth*2
	);

	public static int mineCountDivider = 5;
	public static int mineCount = (rows*columns/mineCountDivider);

	public static boolean gameLost = false;
	public static boolean gameWon = false;
	public static boolean waitingToStart = true;
	public static boolean focuslost = false;
	public static boolean paused = false;
	public static boolean inProgress = false;
	public static boolean gettingHint = false;

	public static long timePlayed = 0;
	public static long timeStarted = 0;
	public static long timePaused = 0;
	public static long totalTimePaused = 0;

	public static boolean settingsVisible = false;

	public static int numberOfRevealedTiles = 0;
	public static int numberOfFlaggedTiles = 0;
	public static int numberOfMinesFlagged = 0;

	public static int maxHints = 3;
	public static int hintsLeft = maxHints;

	//Pictures
	public static Image flagPicture;
	public static Dimension flagPictureDimensions;

	public static Image minePicture;
	public static Dimension minePictureDimensions;

	public static Image possibleMinePicture;
	public static Dimension possibleMinePictureDimensions;

	//Components
	public static JFrame window;
	public static JPanel contentPane;
	public static MineField mineField;
	public static MenuPanel menuPanel;
	public static JButton settingsButton;
	public static JButton stopwatch;
	public static JButton hintsLeftButton; //NOTE: This is not used as a button, but just uses the jbutton appearance to match the other buttons
	public static FootPanel footPanel;
	public static JButton restartButton;
	public static JButton pauseButton;
	public static JButton hintButton;

	public static JFrame settingsWindow;
	public static JPanel settingsPanel;
	public static JSpinner rowsSpinner;
	public static JSpinner columnsSpinner;
	public static JLabel xLabel = new JLabel(" X ");
	public static JButton newGameButton;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(() -> window = new Minesweeper());
	}

	public static void loadPictures() {

		try {
			flagPicture = ImageIO.read(Minesweeper.class.getResource("/flag.png"));
			flagPictureDimensions = new Dimension(flagPicture.getWidth(null), flagPicture.getHeight(null));
		} catch(IllegalArgumentException | IOException e) { e.printStackTrace(); }
		try {
			minePicture = ImageIO.read(Minesweeper.class.getResource("/mine.png"));
			minePictureDimensions = new Dimension(minePicture.getWidth(null), minePicture.getHeight(null));
		} catch(IllegalArgumentException | IOException e) { e.printStackTrace(); }
		try {
			possibleMinePicture = ImageIO.read(Minesweeper.class.getResource("/possibleMine.png"));
			possibleMinePictureDimensions = new Dimension(possibleMinePicture.getWidth(null), possibleMinePicture.getHeight(null));
		} catch(IllegalArgumentException | IOException e) { e.printStackTrace(); }

	}

	public Minesweeper() {

		//Load images

		loadPictures();

		//Create the gui here

		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.setTitle("Pigcoder's Minesweeper Clone");
		this.setResizable(false);

		this.addWindowFocusListener(new WindowFocusListener() {
			@Override
			public void windowGainedFocus(WindowEvent e) { }

			@Override
			public void windowLostFocus(WindowEvent e) {
				focuslost = true;
				mineField.repaint();
			}
 		});

		contentPane = new JPanel(new BorderLayout());
		this.setContentPane(contentPane);

		mineField = new MineField();
		mineField.setPreferredSize(mineFieldSize);

		contentPane.add(mineField, BorderLayout.CENTER);

		menuPanel = new MenuPanel();

		contentPane.add(menuPanel, BorderLayout.NORTH);

		footPanel = new FootPanel();

		contentPane.add(footPanel, BorderLayout.SOUTH);

		for(int i=0; i<MAXSIZE; i++) {
			for(int p=0; p<MAXSIZE; p++) {
				tiles[i][p] = new Tile(i,p);
			}
		}

		//repainter
		ScheduledThreadPoolExecutorThatCatchesErrors executor = new ScheduledThreadPoolExecutorThatCatchesErrors(1);

		executor.scheduleAtFixedRate(() -> {
			if(!paused && !waitingToStart && !gameLost && !gameWon) {
				timePlayed = System.currentTimeMillis() - totalTimePaused - timeStarted;
				int hours = (int)Math.floor(timePlayed/1000/60/60);
				long leftOver = timePlayed - hours*1000*60*60;
				int minutes = (int)Math.floor(leftOver/1000/60);
				leftOver = timePlayed - minutes*1000*60;
				int seconds = (int)Math.floor(leftOver/1000);
				stopwatch.setText(hours + ":" + String.format("%02d", minutes) + ":" + String.format("%02d", seconds));
			}
		}, 0, 10, TimeUnit.MILLISECONDS);

		//Mouse clicking
		this.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				super.mousePressed(e);
				if((inProgress || waitingToStart) && !paused && !focuslost) {
					int x = (int) Math.floor((e.getX() - mineFieldBorderWidth) / (Tile.SIZE.getWidth() + Tile.margin));
					int y = (int) Math.floor((e.getY() - mineFieldBorderWidth - window.getInsets().top - MenuPanel.HEIGHT - 2) / (Tile.SIZE.getHeight() + Tile.margin));
					if (x < 0 || y < 0) {
						return;
					}

					if(gettingHint && !tiles[x][y].getRevealed()) {
						if(tiles[x][y].getIsMine()) {
							tiles[x][y].setState(2);
						} else {
							tiles[x][y].setRevealed(true);
						}
						gettingHint = false;
						hintsLeft--;
						hintButton.setText("Hint");
						hintsLeftButton.setText("Hints: " + hintsLeft);
						if(hintsLeft < 1) {
							hintButton.setEnabled(false);
						}
					} if (waitingToStart) {
						startGame(new Point(x, y));
						waitingToStart = false;
					} if (!gameLost) {
						Tile t = tiles[x][y];
						if (t == null) {
							return;
						}
						if (SwingUtilities.isRightMouseButton(e)) {
							t.setState(t.getState()+1);
						} else {
							if (!t.getFlagged() && !t.getPossibleMine()) {
								t.setRevealed(true);
								if (t.getIsMine()) {
									mineField.revealAll(true);
									gameLost();
									gameWon = false;
								} else if (t.getNumber() == 0) {
									findAllNearbyZeroes(t);
								}
							}
						}
					}

					if (numberOfFlaggedTiles + numberOfRevealedTiles >= rows*columns) {
						checkIfWon();
					}
				}
				if(focuslost) { focuslost = false; }
				mineField.repaint();
			}
		});

		contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('r'), "restart");
		contentPane.getActionMap().put("restart", new AbstractAction("restart") {
			@Override
			public void actionPerformed(ActionEvent e) {
				nextRows = (int)rowsSpinner.getValue();
				nextColumns = (int)columnsSpinner.getValue();
				mineField.repaint();
				restart();
			}
		});
		contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke('p'), "solve");
		contentPane.getActionMap().put("solve", new AbstractAction("start") {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(waitingToStart || gameLost || gameWon) {
					startGame(new Point(ThreadLocalRandom.current().nextInt(0, columns),ThreadLocalRandom.current().nextInt(0,rows)));
				}
				mineField.repaint();
				solve();
			}
		});

		//settings window

		settingsWindow = new JFrame("Settings - 500 rows/cols max");
		settingsWindow.setResizable(false);

		settingsPanel = new JPanel();

		rowsSpinner = new JSpinner(new SpinnerNumberModel(rows, 1, MAXSIZE, 1));
		rowsSpinner.setBorder(new CompoundBorder(new LineBorder(Color.BLACK), new EmptyBorder(0,5,0,5)));
		columnsSpinner = new JSpinner(new SpinnerNumberModel(columns, 5, MAXSIZE, 1));
		columnsSpinner.setBorder(new CompoundBorder(new LineBorder(Color.BLACK), new EmptyBorder(0,5,0,5)));

		JComponent editorRows = rowsSpinner.getEditor();
		JFormattedTextField tfRows = ((JSpinner.DefaultEditor) editorRows).getTextField();
		tfRows.setColumns(4);

		JComponent editorColumns = columnsSpinner.getEditor();
		JFormattedTextField tfColumns = ((JSpinner.DefaultEditor) editorColumns).getTextField();
		tfColumns.setColumns(4);

		settingsPanel.add(rowsSpinner);
		settingsPanel.add(xLabel);
		settingsPanel.add(columnsSpinner);

		newGameButton = new JButton("New Game");
		newGameButton.setBorder(new CompoundBorder(new LineBorder(Color.BLACK), new EmptyBorder(4,5,4,5)));
		newGameButton.addActionListener(e -> {
			nextRows = (int)rowsSpinner.getValue();
			nextColumns = (int)columnsSpinner.getValue();
			mineField.repaint();
			restart();
		});

		settingsPanel.add(newGameButton);

		settingsWindow.add(settingsPanel);

		settingsWindow.pack();

		this.pack();
		this.setVisible(true);
	}

	public static void startGame(Point p) {

		//Generate minefield

		gameLost = false;
		gameWon = false;
		mineField.hideAll();
		resetAllTiles();
		numberOfRevealedTiles = 0;
		numberOfFlaggedTiles = 0;

		//Put mines down
		int minesPlaced = 0;
		while(minesPlaced < mineCount) {
			int x = ThreadLocalRandom.current().nextInt(0, columns);
			int y = ThreadLocalRandom.current().nextInt(0, rows);
			if(x < 0 || y < 0) { return; }
			Tile tile = tiles[x][y];

			if(tile != null && !tile.getIsMine()) {

				//If it is touching the start then pick another tile
				if(x == p.getX() && y == p.getY()) { continue; }
				if(x-1 == p.getX() && y == p.getY()) { continue; }
				if(x-1 == p.getX() && y-1 == p.getY()) { continue; }
				if(x == p.getX() && y-1 == p.getY()) { continue; }
				if(x+1 == p.getX() && y == p.getY()) { continue; }
				if(x+1 == p.getX() && y+1 == p.getY()) { continue; }
				if(x == p.getX() && y+1 == p.getY()) { continue; }
				if(x+1 == p.getX() && y-1 == p.getY()) { continue; }
				if(x-1 == p.getX() && y+1 == p.getY()) { continue; }

				tile.setIsMine(true);
				minesPlaced++;
				//Add 1 to all tiles around this tile (This puts numbers in for tiles)
				try { if(!tiles[x-1][y].getIsMine()) { tiles[x-1][y].setNumber(tiles[x-1][y].getNumber()+1); } } catch(NullPointerException | IndexOutOfBoundsException e) { }
				try { if(!tiles[x-1][y-1].getIsMine()) {tiles[x-1][y-1].setNumber(tiles[x-1][y-1].getNumber()+1); } } catch(NullPointerException | IndexOutOfBoundsException e) { }
				try { if(!tiles[x][y-1].getIsMine()) { tiles[x][y-1].setNumber(tiles[x][y-1].getNumber()+1); } } catch(NullPointerException | IndexOutOfBoundsException e) { }
				try { if(!tiles[x+1][y].getIsMine()) { tiles[x+1][y].setNumber(tiles[x+1][y].getNumber()+1); } } catch(NullPointerException | IndexOutOfBoundsException e) { }
				try { if(!tiles[x+1][y+1].getIsMine()) { tiles[x+1][y+1].setNumber(tiles[x+1][y+1].getNumber()+1); } } catch(NullPointerException | IndexOutOfBoundsException e) { }
				try { if(!tiles[x][y+1].getIsMine()) { tiles[x][y+1].setNumber(tiles[x][y+1].getNumber()+1); } } catch(NullPointerException | IndexOutOfBoundsException e) { }
				try { if(!tiles[x+1][y-1].getIsMine()) { tiles[x+1][y-1].setNumber(tiles[x+1][y-1].getNumber()+1); } } catch(NullPointerException | IndexOutOfBoundsException e) { }
				try { if(!tiles[x-1][y+1].getIsMine()) { tiles[x-1][y+1].setNumber(tiles[x-1][y+1].getNumber()+1); } } catch(NullPointerException | IndexOutOfBoundsException e) { }
			}
		}

		inProgress = true;
		gameWon = false;
		gameLost = false;
		timeStarted = System.currentTimeMillis();
		totalTimePaused = 0;
		timePlayed = 0;

		pauseButton.setEnabled(true);
		hintButton.setEnabled(true);

		Minesweeper.mineField.repaint();

	}

	public static void restart() {
		columns = nextColumns;
		rows = nextRows;
		mineCount = rows*columns/mineCountDivider;
		mineField.recalculateSize();
		mineField.setPreferredSize(mineFieldSize);
		menuPanel.setPreferredSize(new Dimension((int)mineFieldSize.getWidth(), MenuPanel.HEIGHT));
		footPanel.setPreferredSize(new Dimension((int)mineFieldSize.getWidth(), FootPanel.HEIGHT));
		//if(window.getSize().getWidth() < (int)mineFieldSize.getWidth() || window.getSize().getHeight() < (int)(mineFieldSize.getHeight() + MenuPanel.HEIGHT + window.getInsets().top + window.getInsets().bottom)) {
		window.pack();
		window.setSize(new Dimension((int) mineFieldSize.getWidth(), (int) (mineFieldSize.getHeight() + MenuPanel.HEIGHT + FootPanel.HEIGHT + window.getInsets().top + window.getInsets().bottom)));
		resetAllTiles();
		gameWon = false;
		gameLost = false;
		waitingToStart = true;
		inProgress = false;
		timePlayed = 0;
		stopwatch.setText("0:00:00");
		paused = false;
		hintsLeft = maxHints;
		hintButton.setText("Hint");
		hintsLeftButton.setText("Hints: " + maxHints);
		Minesweeper.mineField.repaint();
	}

	public static void resetAllTiles() {
		for(int i=0; i<columns; i++) {
			for(int p=0; p<rows; p++) {
				if(tiles[i][p] != null) {
					Tile t = tiles[i][p];
					t.setNumber(0);
					t.setRevealed(false);
					t.setState(1);
					t.setIsMine(false);
				} else {
					tiles[i][p] = new Tile(i,p);
					Tile t = tiles[i][p];
					t.setNumber(0);
					t.setRevealed(false);
					t.setState(1);
					t.setIsMine(false);
				}
			}
		}
		mineField.repaint();
	}

	public static void findAllNearbyZeroes(Tile tile) {
		ArrayList<Tile> tilesToExplore = new ArrayList<>();
		tilesToExplore.add(tile);
		while(!tilesToExplore.isEmpty()) {
			ArrayList<Tile> newTiles = new ArrayList<>();
			for(Tile t : tilesToExplore) {
				int x = (int)t.getPosition().getX();
				int y = (int)t.getPosition().getY();
				try {  if(tiles[x-1][y].isOnScreen() && !tiles[x-1][y].getRevealed()) {   tiles[x-1][y].setRevealed(true);   if(tiles[x-1][y].getNumber() == 0) {   newTiles.add(tiles[x-1][y]); } } }   catch(NullPointerException | IndexOutOfBoundsException e) { }
				try {  if(tiles[x-1][y-1].isOnScreen() && !tiles[x-1][y-1].getRevealed()) { tiles[x-1][y-1].setRevealed(true); if(tiles[x-1][y-1].getNumber() == 0) { newTiles.add(tiles[x-1][y-1]); } } } catch(NullPointerException | IndexOutOfBoundsException e) { }
				try {  if(tiles[x][y-1].isOnScreen() && !tiles[x][y-1].getRevealed()) {   tiles[x][y-1].setRevealed(true);   if(tiles[x][y-1].getNumber() == 0) {   newTiles.add(tiles[x][y-1]); } } }   catch(NullPointerException | IndexOutOfBoundsException e) { }
				try {  if(tiles[x+1][y].isOnScreen() && !tiles[x+1][y].getRevealed()) {   tiles[x+1][y].setRevealed(true);   if(tiles[x+1][y].getNumber() == 0) {   newTiles.add(tiles[x+1][y]); } } }   catch(NullPointerException | IndexOutOfBoundsException e) { }
				try {  if(tiles[x+1][y+1].isOnScreen() && !tiles[x+1][y+1].getRevealed()) { tiles[x+1][y+1].setRevealed(true); if(tiles[x+1][y+1].getNumber() == 0) { newTiles.add(tiles[x+1][y+1]); } } } catch(NullPointerException | IndexOutOfBoundsException e) { }
				try {  if(tiles[x][y+1].isOnScreen() && !tiles[x][y+1].getRevealed()) {   tiles[x][y+1].setRevealed(true);   if(tiles[x][y+1].getNumber() == 0) {   newTiles.add(tiles[x][y+1]); } } }   catch(NullPointerException | IndexOutOfBoundsException e) { }
				try {  if(tiles[x+1][y-1].isOnScreen() && !tiles[x+1][y-1].getRevealed()) { tiles[x+1][y-1].setRevealed(true); if(tiles[x+1][y-1].getNumber() == 0) { newTiles.add(tiles[x+1][y-1]); } } } catch(NullPointerException | IndexOutOfBoundsException e) { }
				try {  if(tiles[x-1][y+1].isOnScreen() && !tiles[x-1][y+1].getRevealed()) { tiles[x-1][y+1].setRevealed(true); if(tiles[x-1][y+1].getNumber() == 0) { newTiles.add(tiles[x-1][y+1]); } } } catch(NullPointerException | IndexOutOfBoundsException e) { }
			}
			tilesToExplore = new ArrayList<>(newTiles);
		}
	}

	public static void checkIfWon() {
		if(inProgress && numberOfRevealedTiles + numberOfFlaggedTiles == rows*columns && numberOfFlaggedTiles == mineCount) {
			gameWon();
		}
	}

	public static void gameWon() {
		gameWon = true;
		gameLost = false;
		inProgress = false;
		unpause();
		pauseButton.setEnabled(false);
		hintButton.setEnabled(false);
	}

	public static void gameLost() {
		gameWon = false;
		gameLost = true;
		inProgress = false;
		unpause();
		pauseButton.setEnabled(false);
		hintButton.setEnabled(false);
	}

	public static void solve() {
		for(int i=0; i<columns; i++) {
			for (int p = 0; p < rows; p++) {
				if(tiles[i][p] != null) {
					Tile t = tiles[i][p];
					if(t.getIsMine()) {
						t.setState(2);
					}
					else {
						t.setRevealed(true);
					}
				}
			}
		}
		gameWon();
	}

	public static void pause() {
		paused = true;
		timePaused = System.currentTimeMillis();
		hintButton.setEnabled(false);
	}

	public static void unpause() {
		paused = false;
		totalTimePaused += System.currentTimeMillis() - timePaused;
		timePaused = 0;
		if(hintsLeft > 0) {
			hintButton.setEnabled(true);
		}
	}

	public static void getHint() {
		Minesweeper.hintsLeft--;
		Minesweeper.hintsLeftButton.setText("Hints: " + Minesweeper.hintsLeft);
		if(numberOfMinesFlagged < mineCount) {
			while(true) {
				Tile t = tiles[ThreadLocalRandom.current().nextInt(0, columns - 1 )][ThreadLocalRandom.current().nextInt(0, rows - 1)];
				if(t != null) {
					if(t.getIsMine() && !t.getFlagged()) {
						t.setState(2);
						break;
					}
				}
			}
		}
	}

}

class MenuPanel extends JPanel {

	public static final int HEIGHT = 30;

	public MenuPanel() {
		this.setLayout(new BorderLayout());
		this.setPreferredSize(new Dimension((int)Minesweeper.mineFieldSize.getWidth(), HEIGHT));

		Minesweeper.hintsLeftButton = new JButton("Hints: " + Minesweeper.hintsLeft);
		Minesweeper.hintsLeftButton.setPreferredSize(new Dimension(70, MenuPanel.HEIGHT));
		Minesweeper.hintsLeftButton.setBorder(new MatteBorder(0,0,1,1, Color.BLACK));

		this.add(Minesweeper.hintsLeftButton, BorderLayout.WEST);

		Minesweeper.settingsButton = new JButton("Settings");
		Minesweeper.settingsButton.setBorder(new MatteBorder(0,0,1,0, Color.BLACK));
		Minesweeper.settingsButton.addActionListener(e -> {
			Minesweeper.focuslost = false;
			if(Minesweeper.settingsWindow != null) {
				Minesweeper.settingsWindow.setVisible(!Minesweeper.settingsVisible);
				Minesweeper.settingsVisible = !Minesweeper.settingsVisible;
			}
			Minesweeper.mineField.repaint();
		});

		this.add(Minesweeper.settingsButton, BorderLayout.CENTER);

		Minesweeper.stopwatch = new JButton("0:00:00");
		Minesweeper.stopwatch.setPreferredSize(new Dimension(70, MenuPanel.HEIGHT));
		Minesweeper.stopwatch.setBorder(new MatteBorder(0,1,1,0, Color.BLACK));
		Minesweeper.stopwatch.addActionListener(e -> {
			Minesweeper.focuslost = false;
			if(Minesweeper.paused) {
				Minesweeper.unpause();
			} else {
				Minesweeper.pause();
			}
			Minesweeper.mineField.repaint();
		});

		this.add(Minesweeper.stopwatch, BorderLayout.EAST);
	}

	public void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D)g;

		g2.setPaint(Color.LIGHT_GRAY);
		g2.fillRect(0,0, (int)Minesweeper.mineFieldSize.getWidth(), HEIGHT);
	}

}

class FootPanel extends JPanel {

	public static final int HEIGHT = 30;

	public FootPanel() {
		this.setLayout(new BorderLayout());
		this.setPreferredSize(new Dimension((int)Minesweeper.mineFieldSize.getWidth(), HEIGHT));

		Minesweeper.pauseButton = new JButton("Pause");
		Minesweeper.pauseButton.setPreferredSize(new Dimension(70, FootPanel.HEIGHT));
		Minesweeper.pauseButton.setBorder(new MatteBorder(1,1,0,0, Color.BLACK));
		Minesweeper.pauseButton.addActionListener(e -> {
			Minesweeper.focuslost = false;
			if(Minesweeper.inProgress) {
				if (Minesweeper.paused) {
					Minesweeper.unpause();
				} else {
					Minesweeper.pause();
				}
			}
			Minesweeper.mineField.repaint();
		});

		Minesweeper.pauseButton.setEnabled(false);

		this.add(Minesweeper.pauseButton, BorderLayout.EAST);

		Minesweeper.restartButton = new JButton("Restart");
		Minesweeper.restartButton.setBorder(new MatteBorder(1,0,0,0, Color.BLACK));
		Minesweeper.restartButton.addActionListener(e -> {
			Minesweeper.focuslost = false;
			Minesweeper.mineField.repaint();
			Minesweeper.restart();
		});

		this.add(Minesweeper.restartButton, BorderLayout.CENTER);

		Minesweeper.hintButton = new JButton("Hint");
		Minesweeper.hintButton.setPreferredSize(new Dimension(70, FootPanel.HEIGHT));
		Minesweeper.hintButton.setBorder(new MatteBorder(1,0,0,1, Color.BLACK));
		Minesweeper.hintButton.addActionListener(e -> {
			if(!Minesweeper.hintButton.getText().equalsIgnoreCase("Cancel")) {
				if(Minesweeper.inProgress && !Minesweeper.paused) {
					Minesweeper.gettingHint = true;
				}
				if(Minesweeper.hintsLeft < 1) {
					Minesweeper.hintButton.setEnabled(false);
				}
				Minesweeper.hintButton.setText("Cancel");
				Minesweeper.mineField.repaint();
			} else {
				Minesweeper.gettingHint = false;
				Minesweeper.hintButton.setText("Hint");
			}
			Minesweeper.focuslost = false;
		});

		Minesweeper.pauseButton.setEnabled(false);

		this.add(Minesweeper.hintButton, BorderLayout.WEST);

	}

	public void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D)g;

		g2.setPaint(Color.LIGHT_GRAY);
		g2.fillRect(0,0, (int)Minesweeper.mineFieldSize.getWidth(), HEIGHT);

	}

}

class MineField extends JPanel {

	public MineField() { }

	public static Dimension recalculateSize() {
		Minesweeper.mineFieldSize =new Dimension(
				(int)((Minesweeper.columns)*Tile.SIZE.getWidth() + (Minesweeper.columns)*Tile.margin) + Minesweeper.mineFieldBorderWidth*2,
				(int)((Minesweeper.rows)*Tile.SIZE.getHeight() + (Minesweeper.rows)*Tile.margin) + Minesweeper.mineFieldBorderWidth*2
		);
		return Minesweeper.mineFieldSize;
	}

	public static void revealAll(boolean onlyMines) {
		for(int i=0; i<Minesweeper.columns; i++) {
			for(int p=0; p<Minesweeper.rows; p++) {
				if(Minesweeper.tiles[i][p] != null) {
					if (!onlyMines && !Minesweeper.tiles[i][p].getIsMine()) {
						Minesweeper.tiles[i][p].setRevealed(true);
					} else if (Minesweeper.tiles[i][p].getIsMine()) {
						Minesweeper.tiles[i][p].setRevealed(true);
					}
				}
			}
		}
		Minesweeper.checkIfWon();
		Minesweeper.mineField.repaint();
	}

	public static void hideAll() {
		for(int i=0; i<Minesweeper.columns; i++) {
			for(int p=0; p<Minesweeper.rows; p++) {
				if(Minesweeper.tiles[i][p] != null) {
					Minesweeper.tiles[i][p].setRevealed(false);
				}
			}
		}

	}

	public void paintComponent(Graphics g) {

		Graphics2D g2 = (Graphics2D)g;

		//Draw the border
		if(Minesweeper.gameWon) {
			g2.setPaint(Color.GREEN);
		} else if(Minesweeper.gameLost) {
			g2.setPaint(Color.RED);
		} else {
			g2.setPaint(Color.GRAY);
		}
		g2.setStroke(new BasicStroke(Minesweeper.mineFieldBorderWidth));
		g2.drawRect(0 + 1, 0 + 1, (int)Minesweeper.mineFieldSize.getWidth() - Minesweeper.mineFieldBorderWidth + 1, (int)Minesweeper.mineFieldSize.getHeight() - Minesweeper.mineFieldBorderWidth + 1);
		g2.setStroke(new BasicStroke(Tile.margin));

		//Draw the tiles
		if(!Minesweeper.paused) {
			g2.setPaint(Color.GRAY);
			//g2.fillRect(0, 0, (int) Minesweeper.mineFieldSize.getWidth(), (int) Minesweeper.mineFieldSize.getHeight());
			for (int i = 0; i < Minesweeper.columns; i++) {
				for (int p = 0; p < Minesweeper.rows; p++) {
					if (Minesweeper.tiles[i][p] != null) {
						Tile t = Minesweeper.tiles[i][p];
						int x = (int) (Minesweeper.mineFieldBorderWidth + i * Tile.SIZE.getWidth() + i * Tile.margin);
						int y = (int) (Minesweeper.mineFieldBorderWidth + p * Tile.SIZE.getHeight() + p * Tile.margin);
						if (t.getRevealed()) {
							if (t.getIsMine()) {
								if (Minesweeper.minePicture != null) {
									g2.setPaint(Color.GRAY);
									g2.fillRect(x, y, (int) (Tile.SIZE.getWidth()), (int) (Tile.SIZE.getHeight()));
									//g2.drawImage(Minesweeper.minePicture, (int)(x + Tile.SIZE.getWidth()/2 - Minesweeper.minePictureDimensions.getWidth()/2), (int) (y + Tile.SIZE.getWidth()/2),null);
									g2.drawImage(Minesweeper.minePicture, (int) (x + Tile.SIZE.getWidth() / 2 - Minesweeper.minePictureDimensions.getWidth() / 2), (int) (y + Tile.SIZE.getWidth() / 2 - Minesweeper.minePictureDimensions.getWidth() / 2), null);
									g2.setPaint(Color.BLACK);
								} else {
									g2.setPaint(Color.YELLOW);
									g2.fillRect(x, y, (int) (Tile.SIZE.getWidth()), (int) (Tile.SIZE.getHeight()));
								}
							} else if (t.getNumber() != 0) {
								g2.setPaint(Color.LIGHT_GRAY);
								g2.fillRect(x, y, (int) (Tile.SIZE.getWidth()), (int) (Tile.SIZE.getHeight()));
								if(t.getNumber() == 1) {
									g2.setPaint(Color.BLUE);
								} else if(t.getNumber() == 2) {
									g2.setPaint(new Color(0,153,0));
								} else if(t.getNumber() == 3) {
									g2.setPaint(Color.RED);
								} else if(t.getNumber() == 4) {
									g2.setPaint(new Color(76,0,153));
								} else if(t.getNumber() == 5) {
									g2.setPaint(new Color(102,0,0));
								} else if(t.getNumber() == 6) {
									g2.setPaint(new Color(0,175,175));
								} else if(t.getNumber() == 7) {
									g2.setPaint(Color.BLACK);
								} else if(t.getNumber() == 8) {
									g2.setPaint(Color.DARK_GRAY);
								}
								g2.setFont(new Font(g2.getFont().getName(), Font.PLAIN, 10));
								g2.drawString(Integer.toString(t.getNumber()), (int) (x + Tile.SIZE.getWidth() / 2 - g2.getFontMetrics().stringWidth(Integer.toString(t.getNumber())) / 2), (int) (y + Tile.SIZE.getHeight() - g2.getFontMetrics().getHeight()/4));
							} else {
								g2.setPaint(Color.LIGHT_GRAY);
								//g2.drawRect(x, y, (int) (Tile.SIZE.getWidth()), (int) (Tile.SIZE.getHeight()) - 1);
								g2.fillRect(x, y, (int) (Tile.SIZE.getWidth()), (int) (Tile.SIZE.getHeight()));
							}
						} else if (t.getFlagged()) {
							if (Minesweeper.flagPicture != null) {
								g2.setPaint(Color.GRAY);
								g2.fillRect(x, y, (int) (Tile.SIZE.getWidth()), (int) (Tile.SIZE.getHeight()));
								g2.drawImage(Minesweeper.flagPicture, (int) (x + Tile.SIZE.getWidth() / 2 - Minesweeper.flagPictureDimensions.getWidth() / 2 + 1), (int) (y + Tile.SIZE.getWidth() / 2 - Minesweeper.flagPictureDimensions.getHeight() / 2 + 1), null);
							} else {
								g2.setPaint(Color.ORANGE);
								g2.fillRect(x, y, (int) (Tile.SIZE.getWidth()), (int) (Tile.SIZE.getHeight()));
							}

							if(Minesweeper.gameLost) {
								g2.setPaint(Color.RED);
								g2.drawLine(x + 2, y + 2, x + (int)Tile.SIZE.getWidth() - 2, y + (int)Tile.SIZE.getHeight() - 2);
								g2.drawLine(x + 2, y + (int)Tile.SIZE.getHeight() - 2, x + (int)Tile.SIZE.getWidth() - 2, y + 2);
							}
						} else if(t.getPossibleMine()) {
							if(Minesweeper.possibleMinePicture != null) {
								g2.setPaint(Color.GRAY);
								g2.fillRect(x, y, (int) (Tile.SIZE.getWidth()), (int) (Tile.SIZE.getHeight()));
								g2.drawImage(Minesweeper.possibleMinePicture, (int) (x + Tile.SIZE.getWidth() / 2 - Minesweeper.possibleMinePictureDimensions.getWidth() / 2), (int) (y + Tile.SIZE.getWidth() / 2 - Minesweeper.possibleMinePictureDimensions.getHeight() / 2 + 1), null);
							} else {
								g2.setPaint(Color.BLUE);
								g2.fillRect(x, y, (int) (Tile.SIZE.getWidth()), (int) (Tile.SIZE.getHeight()));
							}
						} else {
							g2.setPaint(Color.GRAY);
							g2.fillRect(x, y, (int) (Tile.SIZE.getWidth()), (int) (Tile.SIZE.getHeight()));
						}
					}
				}
			}
			//Draw the borders of revealed
			for(int i=0; i<Minesweeper.columns; i++) {
				for(int p=0; p<Minesweeper.rows; p++) {
					Tile t = Minesweeper.tiles[i][p];
					int x = (int)(Minesweeper.mineFieldBorderWidth + i * Tile.SIZE.getWidth() + i * Tile.margin);
					int y = (int)(Minesweeper.mineFieldBorderWidth + p * Tile.SIZE.getHeight() + p * Tile.margin);
					if (t.getRevealed() && !t.getIsMine()) {
						g2.setPaint(Color.LIGHT_GRAY);
						g2.drawRect(x - Tile.margin, y - Tile.margin, (int) (Tile.SIZE.getWidth() + Tile.margin), (int) (Tile.SIZE.getHeight() + Tile.margin));
					}
				}
			}

			//Draw the borders of hidden
			for(int i=0; i<Minesweeper.columns; i++) {
				for(int p=0; p<Minesweeper.rows; p++) {
					Tile t = Minesweeper.tiles[i][p];
					int x = (int)(Minesweeper.mineFieldBorderWidth + i * Tile.SIZE.getWidth() + i * Tile.margin);
					int y = (int)(Minesweeper.mineFieldBorderWidth + p * Tile.SIZE.getHeight() + p * Tile.margin);
					if (!t.getRevealed() || t.getIsMine()) {
						g2.setPaint(Color.BLACK);
						g2.drawRect(x - Tile.margin, y - Tile.margin, (int) (Tile.SIZE.getWidth() + Tile.margin), (int) (Tile.SIZE.getHeight()) + Tile.margin);
					}
				}
			}
		} else {
			g2.setPaint(Color.GRAY);
			g2.fillRect(0, 0, (int) Minesweeper.mineFieldSize.getWidth(), (int) Minesweeper.mineFieldSize.getHeight());
		}

		//Draw the black border
		g2.setPaint(Color.black);
		g2.drawRect(Minesweeper.mineFieldBorderWidth-1, Minesweeper.mineFieldBorderWidth-1, (int)Minesweeper.mineFieldSize.getWidth() - Minesweeper.mineFieldBorderWidth*2 , (int)Minesweeper.mineFieldSize.getHeight() - Minesweeper.mineFieldBorderWidth*2 );

		if(Minesweeper.paused) {
			//Draw the "PAUSED" text
			g2.setPaint(Color.black);
			g2.setFont(new Font(g2.getFont().getFontName(), Font.BOLD, 20));
			g2.drawString("PAUSED", (int)(Minesweeper.mineFieldSize.getWidth()/2 - g2.getFontMetrics().stringWidth("PAUSED")/2), (int)(Minesweeper.mineFieldSize.getHeight()/2 - g2.getFontMetrics().getHeight()/2));
		} else if(Minesweeper.focuslost) {
			g2.setPaint(new Color(Color.GRAY.getRed(), Color.GRAY.getGreen(), Color.GRAY.getBlue(), 200));
			g2.fillRect(0,0, (int)Minesweeper.mineFieldSize.getWidth(), (int)Minesweeper.mineFieldSize.getHeight());
			g2.setPaint(Color.black);
			g2.setFont(new Font(g2.getFont().getFontName(), Font.BOLD, 20));
			g2.drawString("FOCUS LOST", (int)(Minesweeper.mineFieldSize.getWidth()/2 - g2.getFontMetrics().stringWidth("FOCUS LOST")/2), (int)(Minesweeper.mineFieldSize.getHeight()/2 - g2.getFontMetrics().getHeight()/2));
		}

	}
}

class Tile {

	public static final Dimension SIZE = new Dimension(15,15);

	public static final int margin = 1;

	private boolean isMine;
	private int number = 0;
	private int state = 1; //1: normal, 2: flagged, 3: question mark
	private boolean revealed;
	private Point position;

	public boolean getIsMine() { return isMine; }
	public void setIsMine(boolean b) { isMine = b; }

	public int getNumber() {
		if(isMine){
			return -1;
		} else {
			return number;
		}
	}
	public void setNumber(int num) { number = num; }

	public boolean getRevealed() { return revealed; }
	public void setRevealed(boolean b) { revealed = b; }

	public void setState(int state) {
		if(state == 4) {
			state = 1;
		}
		this.state = state;
	}
	public int getState() { return state; }

	public boolean getNormal() { return state == 1; }
	public boolean getFlagged() { return state == 2; }
	public boolean getPossibleMine() { return state == 3; }

	public Point getPosition() { return position; }

	public boolean isOnScreen() {
		return position.getY() < Minesweeper.rows && position.getY() >= 0 && position.getX() <= Minesweeper.columns-1 && position.getX() >= 0;
	}

	public Tile(int x, int y) {
		position = new Point(x,y);
	}

}

class ScheduledThreadPoolExecutorThatCatchesErrors extends ScheduledThreadPoolExecutor {

	public ScheduledThreadPoolExecutorThatCatchesErrors(int corePoolSize) {
		super(corePoolSize);
	}

	@Override
	public ScheduledFuture scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
		return super.scheduleAtFixedRate(wrapRunnable(command), initialDelay, period, unit);
	}

	@Override
	public ScheduledFuture scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
		return super.scheduleWithFixedDelay(wrapRunnable(command), initialDelay, delay, unit);
	}

	private Runnable wrapRunnable(Runnable command) {
		return new LogOnExceptionRunnable(command);
	}

	private class LogOnExceptionRunnable implements Runnable {
		private Runnable theRunnable;

		public LogOnExceptionRunnable(Runnable theRunnable) {
			super();
			this.theRunnable = theRunnable;
		}

		@Override
		public void run() {
			try {
				theRunnable.run();
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}
}