import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class BrickBreaker extends JPanel implements KeyListener, ActionListener {
    private ArrayList<Brick> bricks;
    private Paddle paddle;
    private Ball ball;
    private ArrayList<Laser> lasers;
    private PowerUp powerUp;
    private int score;
    private int totalBricks;
    private boolean gameOver;
    private boolean shooting;
    private boolean powerUpActive;
    private int powerUpType;
    private boolean multiBallActive;
    private boolean laserGunActive;
    private int numBalls;
    private int lives;
    private int currentLevel;
    private boolean levelCompleted;
    private int levelBrickRows;
    private int levelBrickCols;
    private int levelBrickCount;
    private static final int MAX_LEVELS = 3;
    private static final Color[] BRICK_COLORS = {Color.RED, Color.BLUE, Color.GREEN, Color.YELLOW}; // Define stable colors for bricks
    private boolean gamePaused;

    public BrickBreaker() {
        addKeyListener(this);
        setFocusable(true);
        setFocusTraversalKeysEnabled(false);
        bricks = new ArrayList<>();
        paddle = new Paddle();
        ball = new Ball();
        lasers = new ArrayList<>();
        powerUp = new PowerUp();
        initializeLevel(1); // Start with level 1
        score = 0;
        gameOver = false;
        shooting = false;
        powerUpActive = false;
        powerUpType = 0;
        multiBallActive = false;
        laserGunActive = false;
        numBalls = 1;
        lives = 3;
        gamePaused = false;
        Timer timer = new Timer(5, this);
        timer.start();
    }

    public void initializeLevel(int level) {
        currentLevel = level;
        levelCompleted = false;
        switch (level) {
            case 1:
                levelBrickRows = 8;
                levelBrickCols = 6;
                levelBrickCount = levelBrickRows * levelBrickCols;
                bricks.clear(); // Clear previous level bricks
                initializeBricks(levelBrickRows, levelBrickCols);
                break;
            case 2:
                levelBrickRows = 10;
                levelBrickCols = 8;
                levelBrickCount = levelBrickRows * levelBrickCols;
                bricks.clear(); // Clear previous level bricks
                initializeBricks(levelBrickRows, levelBrickCols);
                break;
            case 3:
                levelBrickRows = 12;
                levelBrickCols = 10;
                levelBrickCount = levelBrickRows * levelBrickCols;
                bricks.clear(); // Clear previous level bricks
                initializeBricks(levelBrickRows, levelBrickCols);
                break;
            default:
                break;
        }
    }

    public void initializeBricks(int rows, int cols) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // Assign a stable color to each brick
                Color color = BRICK_COLORS[(i + j) % BRICK_COLORS.length];
                bricks.add(new Brick(j * 70 + 50, i * 30 + 50, color));
            }
        }
        totalBricks = bricks.size();
    }

    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        // Set background color to black
        setBackground(Color.BLACK);
        for (Brick brick : bricks) {
            brick.draw(g);
        }
        paddle.draw(g);
        ball.draw(g);
        for (Laser laser : lasers) {
            laser.draw(g);
        }
        if (powerUpActive) {
            powerUp.draw(g, powerUpType);
        }
        if (gameOver) {
            gameOver(g, "Game Over!");
        }
        if (levelCompleted) {
            gameOver(g, "Level " + currentLevel + " Completed!");
        }
        if (gamePaused) {
            pauseGame(g);
        }
        g.setColor(Color.WHITE);
        g.setFont(new Font("serif", Font.BOLD, 25));
        g.drawString("Score: " + score, 590, 30);
        g.drawString("Lives: " + lives, 10, 30);
    }

    public void actionPerformed(ActionEvent e) {
        if (!gamePaused && !gameOver && !levelCompleted) {
            ball.move();
            paddle.move();
            checkCollision();
            moveLasers();
            if (powerUpActive) {
                powerUp.move();
                checkPowerUpCollision();
            }
        }
        repaint();
    }

    public void moveLasers() {
        for (int i = 0; i < lasers.size(); i++) {
            Laser laser = lasers.get(i);
            laser.move();
            if (laser.getY() < 0) {
                lasers.remove(i);
            } else {
                checkLaserBrickCollision(laser);
            }
        }
    }

    public void checkLaserBrickCollision(Laser laser) {
        Rectangle laserRect = laser.getBounds();
        for (int i = 0; i < bricks.size(); i++) {
            Brick brick = bricks.get(i);
            if (brick.getRect().intersects(laserRect) && !brick.isDestroyed()) {
                brick.setDestroyed(true);
                laser.deactivate();
                score += 10;
                totalBricks--;
                break; // Exit the loop after hitting the first brick
            }
        }
    }

    public void checkCollision() {
        Rectangle ballRect = ball.getRect();
        Rectangle paddleRect = paddle.getRect();
        if (ballRect.intersects(paddleRect)) {
            ball.setDY(-ball.getDY());
        }
        for (int i = 0; i < bricks.size(); i++) {
            Brick brick = bricks.get(i);
            Rectangle brickRect = brick.getRect();
            if (ballRect.intersects(brickRect) && !brick.isDestroyed()) {
                ball.setDY(-ball.getDY());
                brick.setDestroyed(true);
                score += 10;
                totalBricks--;
                if (powerUpActive) {
                    Random random = new Random();
                    if (random.nextInt(100) < 20) { // 20% chance of dropping power-up
                        powerUp.setX(brick.getX());
                        powerUp.setY(brick.getY());
                        activatePowerUp();
                    }
                }
                if (multiBallActive) {
                    if (numBalls < 3) {
                        ball.split();
                        numBalls++;
                    }
                }
                if (laserGunActive) {
                    shooting = true;
                }
            }
        }
        if (ball.getY() > 570) {
            lives--;
            if (lives == 0) {
                gameOver = true;
            } else {
                ball.reset();
                paddle.reset();
                shooting = false;
            }
        }
        if (totalBricks == 0) {
            levelCompleted = true;
            if (currentLevel < MAX_LEVELS) {
                currentLevel++;
                initializeLevel(currentLevel);
            } else {
                gameOver = true; // No more levels, game over
            }
        }
    }

    public void checkPowerUpCollision() {
        Rectangle paddleRect = paddle.getRect();
        Rectangle powerUpRect = powerUp.getRect();
        if (paddleRect.intersects(powerUpRect)) {
            applyPowerUp();
        }
    }

    public void applyPowerUp() {
        if (powerUpType == 1) {
            paddle.enlarge();
        } else if (powerUpType == 2) {
            multiBallActive = true;
        } else if (powerUpType == 3) {
            laserGunActive = true;
        }
        powerUpActive = false;
        powerUp.setX(-50);
        powerUp.setY(-50);
    }

    public void activatePowerUp() {
        powerUpActive = true;
        Random random = new Random();
        powerUpType = random.nextInt(3) + 1; // Random power-up type (1, 2, or 3)
    }

    public void gameOver(Graphics g, String message) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("serif", Font.BOLD, 50));
        g.drawString(message, 150, 300);
    }

    public void pauseGame(Graphics g) {
        g.setColor(Color.WHITE);
        g.setFont(new Font("serif", Font.BOLD, 50));
        g.drawString("PAUSED", 250, 300);
    }

    public void keyPressed(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_RIGHT) {
            paddle.setDX(3); // Increase paddle speed
        } else if (key == KeyEvent.VK_LEFT) {
            paddle.setDX(-3); // Increase paddle speed
        } else if (key == KeyEvent.VK_SPACE && !shooting) {
            lasers.add(new Laser(paddle.getX() + 45));
        } else if (key == KeyEvent.VK_P) {
            gamePaused = !gamePaused; // Toggle pause state
        }
    }

    public void keyReleased(KeyEvent e) {
        int key = e.getKeyCode();
        if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_LEFT) {
            paddle.setDX(0);
        }
    }

    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Brick Breaker");
        BrickBreaker game = new BrickBreaker();
        frame.add(game);
        frame.setSize(700, 600);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}

class Brick {
    private int x;
    private int y;
    private boolean destroyed;
    private Color color;

    public Brick(int x, int y, Color color) {
        this.x = x;
        this.y = y;
        this.color = color;
        destroyed = false;
    }

    public void draw(Graphics g) {
        if (!destroyed) {
            g.setColor(color);
            g.fillRect(x, y, 70, 30);
            g.setColor(Color.WHITE);
            g.drawRect(x, y, 70, 30);
        }
    }

    public Rectangle getRect() {
        return new Rectangle(x, y, 70, 30);
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    public void setDestroyed(boolean destroyed) {
        this.destroyed = destroyed;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}

class Paddle {
    private int x;
    private int dx;

    public Paddle() {
        x = 310;
        dx = 0;
    }

    public void draw(Graphics g) {
        g.setColor(Color.YELLOW);
        g.fillRect(x, 550, 80, 10);
    }

    public void move() {
        x += dx;
        if (x < 0) {
            x = 0;
        }
        if (x > 620) {
            x = 620;
        }
    }

    public Rectangle getRect() {
        return new Rectangle(x, 550, 80, 10);
    }

    public int getX() {
        return x;
    }

    public void setDX(int dx) {
        this.dx = dx;
    }

    public void reset() {
        x = 310;
    }

    public void enlarge() {
        // Increase paddle width by 40 pixels
        x -= 20;
    }
}

class Ball {
    private int x;
    private int y;
    private int dx;
    private int dy;

    public Ball() {
        x = 120;
        y = 350;
        dx = -2; // Increase ball speed
        dy = -4; // Increase ball speed
    }

    public void draw(Graphics g) {
        g.setColor(Color.GREEN);
        g.fillOval(x, y, 20, 20);
    }

    public void move() {
        x += dx;
        y += dy;
        if (x < 0 || x > 670) {
            dx = -dx;
        }
        if (y <= 0) {
            dy = -dy;
        }
    }

    public Rectangle getRect() {
        return new Rectangle(x, y, 20, 20);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getDX() {
        return dx;
    }

    public int getDY() {
        return dy;
    }

    public void setDX(int dx) {
        this.dx = dx;
    }

    public void setDY(int dy) {
        this.dy = dy;
    }

    public void reset() {
        x = 120;
        y = 350;
        dx = -2;
        dy = -4;
    }

    public void split() {
        // Reverse both x and y direction
        dx = -dx;
        dy = -dy;
    }
}

class Laser {
    private int x;
    private int y;
    private boolean active;

    public Laser(int x) {
        this.x = x;
        y = 550;
        active = true;
    }

    public void draw(Graphics g) {
        if (active) {
            g.setColor(Color.RED);
            g.fillRect(x, y, 5, 10);
        }
    }

    public void move() {
        if (active) {
            y -= 3;
        }
    }

    public int getY() {
        return y;
    }

    public void deactivate() {
        active = false;
    }

    public boolean isActive() {
        return active;
    }

    public Rectangle getBounds() {
        return new Rectangle(x, y, 5, 10);
    }
}

class PowerUp {
    private int x;
    private int y;
    private int dy;

    public PowerUp() {
        x = -50;
        y = -50;
        dy = 1;
    }

    public void draw(Graphics g, int type) {
        g.setColor(Color.YELLOW);
        if (type == 1) {
            g.fillRect(x, y, 20, 20);
        } else if (type == 2) {
            g.fillRect(x, y, 30, 30);
        } else if (type == 3) {
            g.fillRect(x, y, 40, 10);
        }
    }

    public void move() {
        y += dy;
    }

    public Rectangle getRect() {
        return new Rectangle(x, y, 40, 10);
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }
}