import javax.swing.*;
import java.awt.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Random;

// Class representing the game's obstacles
class Obstacle {
    int x, y, width, height;
    Rectangle topPipe, bottomPipe;
    int distance = 105;
    boolean isPassedOn = false;

    // Constructor to initialize obstacle properties
    public Obstacle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        topPipe = new Rectangle(x, y, width, height);
        bottomPipe = new Rectangle(x, height + distance, width, height);
    }

    // Reset obstacle to a new position
    public void resetToNewPosition(int newX) {
        topPipe.x = newX;
        bottomPipe.x = newX;
        x = newX;
        topPipe.y = -(new Random().nextInt(140) + 100);
        bottomPipe.y = topPipe.y + height + distance;
        isPassedOn = false;
    }

    // Check if a rectangle intersects with the obstacle
    public boolean intersect(Rectangle rectangle) {
        return rectangle.intersects(topPipe) || rectangle.intersects(bottomPipe);
    }

    // Check if the player has passed the obstacle
    public boolean passedOn(Rectangle rectangle) {
        return rectangle.x > x + width && !isPassedOn;
    }

    // Move obstacle horizontally
    public void moveX(int dx) {
        x -= dx;
        topPipe.x -= dx;
        bottomPipe.x -= dx;
    }
}

// Enum representing the direction of the bird's movement
enum Direction {
    Up,
    Down,
    None
}

// Main class representing the game
public class Game extends JPanel implements Runnable, MouseListener {

    boolean isRunning;
    Thread thread;
    BufferedImage view, background, floor, bird, tapToStartTheGame;
    BufferedImage[] flyBirdAnim;
    Rectangle backgroundBox, floorBox, flappyBox, tapToStartTheGameBox;

    int DISTORTION;
    int SCALE = 2;
    int SIZE = 256;

    int frameIndexFly = 0, intervalFrame = 5;
    Direction direction;
    float velocity = 0;
    float gravity = 0.25f;
    boolean inGame;
    BufferedImage topPipe, bottomPipe;
    Obstacle[] obstacles;
    Font font;
    int record = 0;
    int point = 0;

    private ObstaclePool obstaclePool;

    // Constructor for the Game class
    public Game() {
        SIZE *= SCALE;
        setPreferredSize(new Dimension(SIZE, SIZE));
        addMouseListener(this);
        obstaclePool = new ObstaclePool(10);
        initializeGame();
    }

    // Initialize game resources and variables
    private void initializeGame() {
        try {
            // Load images and sprite sheets
            view = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_RGB);
            background = ImageIO.read(getClass().getResource("/bg.png"));
            floor = ImageIO.read(getClass().getResource("/floor.png"));
            tapToStartTheGame = ImageIO.read(getClass().getResource("/tap_to_start_the_game.png"));
            BufferedImage fly = ImageIO.read(getClass().getResource("/flappy_sprite_sheet.png"));
            topPipe = ImageIO.read(getClass().getResource("/top_pipe.png"));
            bottomPipe = ImageIO.read(getClass().getResource("/bottom_pipe.png"));

            // Create bird animation frames
            flyBirdAnim = new BufferedImage[3];
            for (int i = 0; i < 3; i++) {
                flyBirdAnim[i] = fly.getSubimage(i * 17, 0, 17, 12);
            }
            bird = flyBirdAnim[0];

            DISTORTION = (SIZE / background.getHeight());

            // Initialize obstacles and other game entities
            obstacles = new Obstacle[4];
            startPositionObstacles();

            int widthTapStartGame = tapToStartTheGame.getWidth() * DISTORTION;
            int heightTapStartGame = tapToStartTheGame.getHeight() * DISTORTION;
            tapToStartTheGameBox = new Rectangle(
                    (SIZE / 2) - (widthTapStartGame / 2),
                    (SIZE / 2) - (heightTapStartGame / 2),
                    widthTapStartGame,
                    heightTapStartGame);
            flappyBox = new Rectangle(
                    0,
                    0,
                    bird.getWidth() * DISTORTION,
                    bird.getHeight() * DISTORTION);
            backgroundBox = new Rectangle(
                    0,
                    0,
                    background.getWidth() * DISTORTION,
                    background.getHeight() * DISTORTION);
            floorBox = new Rectangle(
                    0,
                    SIZE - (floor.getHeight() * DISTORTION),
                    floor.getWidth() * DISTORTION,
                    floor.getHeight() * DISTORTION);

            startPositionFlappy();

            font = new Font("TimesRoman", Font.BOLD, 16 * DISTORTION);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Set initial positions for obstacles
    private void startPositionObstacles() {
        for (int i = 0; i < 4; i++) {
            obstacles[i] = obstaclePool.acquireObstacle(0, 0, topPipe.getWidth() * DISTORTION, topPipe.getHeight() * DISTORTION);
            obstacles[i].resetToNewPosition((SIZE + topPipe.getWidth() * DISTORTION) + (i * 170));
        }
    }

    // Set initial position for the player (bird)
    private void startPositionFlappy() {
        direction = Direction.None;
        inGame = false;
        flappyBox.x = (SIZE / 2) - (flappyBox.width * 3);
        flappyBox.y = (SIZE / 2) - flappyBox.height / 2;
    }

    // Update game logic
    public void update() {
        // Move background and floor
        backgroundBox.x -= 1;
        floorBox.x -= 3;

        // Wrap around background and floor
        if (backgroundBox.x + backgroundBox.getWidth() <= 0) {
            backgroundBox.x = (int) (backgroundBox.x + backgroundBox.getWidth());
        }

        if (floorBox.x + floorBox.getWidth() <= 0) {
            floorBox.x = (int) (floorBox.x + floorBox.getWidth());
        }

        // Update bird animation frame
        intervalFrame++;
        if (intervalFrame > 5) {
            intervalFrame = 0;
            frameIndexFly++;
            if (frameIndexFly > 2) {
                frameIndexFly = 0;
            }
            bird = flyBirdAnim[frameIndexFly];
        }

        // Update obstacles
        if (inGame) {
            for (Obstacle obstacle : obstacles) {
                obstacle.moveX(3);

                // Reset obstacle position if it moves off the screen
                if (obstacle.x + obstacle.width < 0) {
                    obstacle.resetToNewPosition(SIZE + obstacle.width + 65);
                }

                // Check for collision with the player
                if (obstacle.intersect(flappyBox)) {
                    gameOver();
                }

                // Check if the player has passed the obstacle
                if (obstacle.passedOn(flappyBox)) {
                    obstacle.isPassedOn = true;
                    point++;
                    if (point > record) {
                        record = point;
                    }
                }
            }
        }

        // Update player position based on direction
        if (direction == Direction.Down) {
            velocity += gravity;
            flappyBox.y += velocity;
        } else if (direction == Direction.Up) {
            velocity = -4.5f;
            flappyBox.y -= -velocity;
        }

        // Check for collisions with floor and ceiling
        if (flappyBox.y + flappyBox.getHeight() >= SIZE - floorBox.height || flappyBox.y <= 0) {
            gameOver();
        }
    }

    // Handle game over conditions
    private void gameOver() {
        point = 0;
        startPositionObstacles();
        startPositionFlappy();
    }

    // Draw the game graphics
    private void draw() {
        Graphics2D g2 = (Graphics2D) view.getGraphics();
        g2.drawImage(
                background,
                backgroundBox.x,
                backgroundBox.y,
                (int) backgroundBox.getWidth(),
                (int) backgroundBox.getHeight(),
                null);
        g2.drawImage(
                background,
                (int) (backgroundBox.x + backgroundBox.getWidth()),
                backgroundBox.y,
                (int) backgroundBox.getWidth(),
                (int) backgroundBox.getHeight(),
                null);

        // Draw obstacles
        for (Obstacle obstacle : obstacles) {
            g2.drawImage(topPipe, obstacle.x, obstacle.topPipe.y, obstacle.width, obstacle.height, null);
            g2.drawImage(bottomPipe, obstacle.x, obstacle.bottomPipe.y, obstacle.width, obstacle.height, null);
        }

        // Draw floor
        g2.drawImage(
                floor,
                floorBox.x,
                floorBox.y,
                (int) floorBox.getWidth(),
                (int) floorBox.getHeight(),
                null);
        g2.drawImage(
                floor,
                (int) (floorBox.x + floorBox.getWidth()),
                floorBox.y,
                (int) floorBox.getWidth(),
                (int) floorBox.getHeight(),
                null);

        // Draw player (bird)
        g2.drawImage(
                bird,
                flappyBox.x,
                flappyBox.y,
                (int) flappyBox.getWidth(),
                (int) flappyBox.getHeight(),
                null);

        // Draw "Tap to Start" message when the game is not in progress
        if (!inGame) {
            g2.drawImage(
                    tapToStartTheGame,
                    tapToStartTheGameBox.x,
                    tapToStartTheGameBox.y,
                    (int) tapToStartTheGameBox.getWidth(),
                    (int) tapToStartTheGameBox.getHeight(),
                    null);
        }

        // Draw score and record
        g2.setColor(Color.YELLOW);
        g2.setFont(font);
        if (!inGame) {
            g2.drawString("Record: " + record, 10, 35);
        } else {
            g2.drawString(point + "", SIZE - 80, 35);
        }

        // Draw the final image to the screen
        Graphics g = getGraphics();
        g.drawImage(view, 0, 0, SIZE, SIZE, null);
        g.dispose();
    }

    // Main game loop
    @Override
    public void run() {
        try {
            // Set focus to the game window
            requestFocus();
            while (isRunning) {
                // Update and draw the game
                update();
                draw();
                // Sleep to control the frame rate
                Thread.sleep(1000 / 60);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Initialize the game thread when the JPanel is added to the JFrame
    @Override
    public void addNotify() {
        super.addNotify();
        if (thread == null) {
            thread = new Thread(this);
            isRunning = true;
            thread.start();
        }
    }

    // Mouse click event handlers
    @Override
    public void mouseClicked(MouseEvent e) {

    }

    // Mouse press event handler (bird goes up)
    @Override
    public void mousePressed(MouseEvent e) {
        direction = Direction.Up;
    }

    // Mouse release event handler (bird goes down, game starts)
    @Override
    public void mouseReleased(MouseEvent e) {
        inGame = true;
        direction = Direction.Down;
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    // Main method to launch the game
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame w = new JFrame("Flappy Bird");
            w.setResizable(false);
            w.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            w.add(new Game());
            w.pack();
            w.setLocationRelativeTo(null);
            w.setVisible(true);
        });
    }
}

// Class representing a pool of obstacles for recycling
class ObstaclePool {
    private Obstacle[] obstacles;
    private int nextAvailable;

    // Constructor to create and initialize obstacle pool
    public ObstaclePool(int size) {
        obstacles = new Obstacle[size];
        for (int i = 0; i < size; i++) {
            obstacles[i] = new Obstacle(0, 0, 0, 0);
        }
        nextAvailable = 0;
    }

    // Acquire an obstacle from the pool, initializing its properties
    public Obstacle acquireObstacle(int x, int y, int width, int height) {
        Obstacle obstacle = obstacles[nextAvailable];
        obstacle.x = x;
        obstacle.y = y;
        obstacle.width = width;
        obstacle.height = height;
        obstacle.topPipe.setBounds(x, y, width, height);
        obstacle.bottomPipe.setBounds(x, height + obstacle.distance, width, height);
        nextAvailable = (nextAvailable + 1) % obstacles.length;
        return obstacle;
    }
}
