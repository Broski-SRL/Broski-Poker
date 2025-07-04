/**
 * GameRenderer.java
 * <p>
 * Handles all visual rendering aspects of the poker game.
 * Acts as the View in the MVC pattern.
 * <p>
 * Responsibilities:
 * - Render all game elements (cards, table, players)
 * - Manage visual assets (textures, sprites, fonts)
 * - Control camera positioning and zoom
 * - Handle card animations and visual effects
 * - Display game state information
 * - Provide menu and UI rendering
 */

package io.github.broskipoker.ui;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import io.github.broskipoker.Menu;
import io.github.broskipoker.game.Card;
import io.github.broskipoker.game.Player;
import io.github.broskipoker.game.PokerGame;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import io.github.broskipoker.server.ClientConnection;

import java.util.HashMap;
import java.util.Map;


import java.util.List;

public class GameRenderer {
    private final PokerGame pokerGame;
    private final SpriteBatch batch;
    private final Stage stage;
    private final Menu menu;
    private final BitmapFont font;
    private final BitmapFont blindsFont;
    private BitmapFont potFont;
    private final FontManager fontManager;

    // Camera settings
    private final OrthographicCamera camera;
    private final float defaultZoom = 1.0f;
    private final float focusedZoom = 0.5f;
    private boolean isZoomed = false;
    private boolean bettingUIVisible = true;

    // Assets
    private final Texture backgroundTexture;
    private final Texture cardSheet;
    private final Texture enhancersSheet;
    private final TextureRegion[][] cardRegions;
    private final TextureRegion cardBackground;
    private final TextureRegion cardBack;
    private final Texture buttonsSheet;
    private final TextureRegion dealerRegion;
    private final TextureRegion smallBlindRegion;
    private final TextureRegion bigBlindRegion;
    private TextureRegion[] emojiRegions;
    private ImageButton emojiButton;
    private Group emojiDropdown;


    // Card dimensions
    private static final int CARDS_PER_ROW = 13;
    private static final int SUITS = 4;
    private static final int CARD_WIDTH = 142;
    private static final int CARD_HEIGHT = 190;
    private static final int ENHANCER_WIDTH = 142;
    private static final int ENHANCER_HEIGHT = 190;
    private static final int DISPLAY_CARD_WIDTH = 60;
    private static final int DISPLAY_CARD_HEIGHT = 90;
    private static final int CARD_SPACING = 15;

    // Chip textures
    private Texture chipTexture;
    private TextureRegion[] chipRegion;
    private static final int[] CHIP_VALUES = {1, 5, 10, 25, 100, 500};
    private static final int CHIP_SIZE = 58;

    // Small Blind, Big Blind, and Dealer chips textures
    private final int CHIP_WIDTH = 1465 / 3;
    private final int CHIP_HEIGHT = 465;

    // Chair positions
    private final float[][] chairPositions;

    // Card dealing animation helper instance
    private static final DealingAnimator dealingAnimator;
    private static boolean dealingAnimationComplete = false;
    private static float dealingAnimationTimer = 0;
    private static final float DEALING_ANIMATION_DURATION = 4.0f;

    // Winning hand for rendering during showdown
    private List<Card> winningCards = null;

    // Betting UI
    private BettingUI bettingUI;
    private final TextureRegion turnIndicatorRegion;

    // Define human player index
    private static final int HUMAN_PLAYER_INDEX = 3;

    // Reference to GameController for passing to BettingUI
    private GameController gameController;

    // Sound manager
    private SoundManager soundManager;

    // Avatars textures
    private Texture avatarsTexture;
    private TextureRegion[][] avatarRegions;
    private int[] playerAvatarIndices; // Store which avatar each player uses

    // Emoji rendering
    private int selectedEmojiIndex = -1;
    private long emojiDisplayStartTime = 0;
    public static final long EMOJI_DISPLAY_DURATION = 3000;
    // 3 sec
    private Map<Integer, EmojiDisplayData> activeEmojis = new HashMap<>();

    //boti
    private float botEmojiTimer = 0f;
    private static final float BOT_EMOJI_INTERVAL = 5f; // o reactie la ~5 secunde

    // for multiplayer
    private boolean isMultiplayer = false;
    private String currentUsername;
    private ClientConnection clientConnection;

    static
    {
        // Initialize dealing animator
        dealingAnimator = new DealingAnimator(5, PokerGame.getDealerPosition()); // Max 5 players
    }

    public GameRenderer(PokerGame pokerGame) {
        this.pokerGame = pokerGame;

        // Initialize rendering components
        batch = new SpriteBatch();
        stage = new Stage(new ScreenViewport());
        menu = new Menu(stage);
        soundManager = SoundManager.getInstance();

        // Initialize FontManager
        fontManager = FontManager.getInstance();

        // Initialize fonts using FontManager for better scaling
        font = fontManager.getFont(24, Color.WHITE);
        blindsFont = fontManager.getFont(16, new Color(1, 0.84f, 0, 1)); // Gold color

        // Initialize camera
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.update();

        // Load textures
        backgroundTexture = new Texture("textures/2x/pokerTable.png");
        cardSheet = new Texture(Gdx.files.internal("textures/2x/8BitDeck.png"));
        enhancersSheet = new Texture(Gdx.files.internal("textures/2x/Enhancers.png"));
        chipTexture = new Texture(Gdx.files.internal("textures/2x/chips.png"));

        // Load buttons sheet
        buttonsSheet = new Texture(Gdx.files.internal("textures/2x/SmallBigDealer.png"));
        smallBlindRegion = new TextureRegion(buttonsSheet, 0, 0, CHIP_WIDTH, CHIP_HEIGHT);
        dealerRegion = new TextureRegion(buttonsSheet, CHIP_WIDTH + 1, 0, CHIP_WIDTH, CHIP_HEIGHT);
        bigBlindRegion = new TextureRegion(buttonsSheet, 2 * CHIP_WIDTH + 1, 0,  CHIP_WIDTH, CHIP_HEIGHT);

        // Set up card regions
        cardBackground = new TextureRegion(enhancersSheet, ENHANCER_WIDTH, 0, ENHANCER_WIDTH, ENHANCER_HEIGHT);
        cardBack = new TextureRegion(enhancersSheet, 0, 0, ENHANCER_WIDTH, ENHANCER_HEIGHT);
        cardRegions = new TextureRegion[SUITS][CARDS_PER_ROW];



        for (int suit = 0; suit < SUITS; suit++) {
            for (int rank = 0; rank < CARDS_PER_ROW; rank++) {
                cardRegions[suit][rank] = new TextureRegion(cardSheet,
                    rank * CARD_WIDTH, suit * CARD_HEIGHT, CARD_WIDTH, CARD_HEIGHT);
            }
        }

        // Load chip textures
        chipRegion = new TextureRegion[6];
        for (int i = 0; i < CHIP_VALUES.length - 1; i++) {
            chipRegion[i] = new TextureRegion(chipTexture, i * CHIP_SIZE, 0, CHIP_SIZE, CHIP_SIZE);
        }
        // 500 chip is on the second row
        chipRegion[5] = new TextureRegion(chipTexture, 0, CHIP_SIZE, CHIP_SIZE, CHIP_SIZE);

        // Set up chair positions
        chairPositions = new float[][] {
            {Gdx.graphics.getWidth() * 0.2f, Gdx.graphics.getHeight() * 0.6f},
            {Gdx.graphics.getWidth() * 0.4f, Gdx.graphics.getHeight() * 0.6f},
            {Gdx.graphics.getWidth() * 0.5f, Gdx.graphics.getHeight() * 0.5f},
            {Gdx.graphics.getWidth() * 0.4f, Gdx.graphics.getHeight() * 0.3f},
            {Gdx.graphics.getWidth() * 0.2f, Gdx.graphics.getHeight() * 0.3f}
        };

        // Note: BettingUI will be initialized with the GameController in setGameController()
        // This allows us to avoid circular dependencies
        bettingUI = null;

        // FIX: Use smallBlindRegion as turn indicator instead of loading missing texture
        turnIndicatorRegion = smallBlindRegion;

        // Alternative fix would be to create the file:
        // try {
        //     Texture turnIndicatorTexture = new Texture(Gdx.files.internal("textures/2x/turnIndicator.png"));
        //     turnIndicatorRegion = new TextureRegion(turnIndicatorTexture, 0, 0, 100, 100);
        // } catch (Exception e) {
        //     System.out.println("Could not load turn indicator texture: " + e.getMessage());
        //     // Fallback to small blind texture region
        //     turnIndicatorRegion = smallBlindRegion;
        // }

        // Load avatar texture with scaling
        try {
            // Load the pre-scaled avatar texture
            avatarsTexture = new Texture(Gdx.files.internal("avatars/merged_avatars_256.png"));

            // Total dimensions of the texture
            int textureWidth = 2560;  // 10 columns × 256 pixels
            int textureHeight = 6400; // 25 rows × 256 pixels

            // Calculate dimensions for each avatar (10×25 grid)
            int avatarWidth = 256;  // Each avatar is 256×256
            int avatarHeight = 256;

            // Split the texture into individual avatar regions
            avatarRegions = TextureRegion.split(avatarsTexture, avatarWidth, avatarHeight);

            // Initialize player avatar indices with random avatars
            playerAvatarIndices = new int[5]; // One index for each player
            for (int i = 0; i < playerAvatarIndices.length; i++) {
                if (isMultiplayer) {
                    playerAvatarIndices[i] = i;
                }
                playerAvatarIndices[i] = MathUtils.random(249); // 250 total avatars (10×25)
            }
        } catch (Exception e) {
            System.err.println("Failed to load avatar textures: " + e.getMessage());
            e.printStackTrace();
            avatarRegions = null;
            playerAvatarIndices = null;
        }

        //Load emoji textures
        loadEmojiTextures();

        //Load emoji button
        TextureRegionDrawable emojiButtonDrawable = new TextureRegionDrawable(emojiRegions[0]); // first emoji as icon
        emojiButton = new ImageButton(emojiButtonDrawable);

        emojiButton.setPosition(700, 100); // screen position
        emojiButton.setSize(40, 40);
//        stage.addActor(emojiButton);

        //click dropdwon listener
        emojiButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                boolean showDropdown = !emojiDropdown.isVisible();
                emojiDropdown.setVisible(showDropdown);
                emojiButton.setSize(0,0);
                // Folosim direct setVisible pentru efect instant
                emojiButton.setVisible(!showDropdown);
            }
        });



        //adding emoji dropdown
        emojiDropdown = new Group();
        emojiDropdown.setVisible(false);
        emojiDropdown.setPosition(700, 150); // poziția absolută pe ecran
//        stage.addActor(emojiDropdown);


        float baseX = 700;
        float baseY = 150;
        float size = 40;
        float spacing = 5;



        for (int i = 0; i < emojiRegions.length; i++) {
            final int emojiIndex = i;

            ImageButton emojiChoice = new ImageButton(new TextureRegionDrawable(emojiRegions[i]));
            emojiChoice.setSize(size, size);
            emojiChoice.setPosition(i * (size + spacing), 0); // poziție relativă în grup

            emojiChoice.addListener(new ClickListener() {
                @Override
                public void clicked(InputEvent event, float x, float y) {
                    int humanPlayerIndex = findHumanPlayerIndex();
                    activeEmojis.put(humanPlayerIndex, new EmojiDisplayData(emojiIndex, System.currentTimeMillis()));
                    emojiDropdown.setVisible(false);
                    emojiButton.setSize(40, 40);
                    // reapare butonul după alegere
                }
            });

//
//            emojiDropdown.addActor(emojiChoice);
        }

//        stage.addActor(emojiDropdown);
    }

    public void setGameController(GameController controller) {
        this.gameController = controller;
        // Now initialize the BettingUI with the GameController
        this.bettingUI = new BettingUI(pokerGame, stage, this);

        // pass multiplayer info to betting UI if in multiplayer mode
        if (isMultiplayer && clientConnection != null) {
            bettingUI.setMultiplayerMode(clientConnection, currentUsername);
        }
    }

    public void render(float delta) {
        if (menu.isGameStarted()) {
            // Hide menu buttons
            for (TextButton button : menu.getButtons()) {
                button.setVisible(false);
            }

            // Show betting UI when game is started
            bettingUI.setVisible(bettingUIVisible);

            batch.setProjectionMatrix(camera.combined);

            // Draw background
            batch.begin();
            batch.draw(backgroundTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
            batch.end();

            // Draw game elements
            renderGameElements(delta);

            // Render random emojis for bots
            botEmojiTimer += delta;
            if (botEmojiTimer >= BOT_EMOJI_INTERVAL) {
//                triggerRandomBotEmoji();
                botEmojiTimer = 0f;
            }


            // Handle player turns
            handlePlayerTurns();

            // Update stage
            stage.act(delta);
            stage.draw();

            //emoji button
            emojiButton.setVisible(true);

        } else {
            // Hide betting UI when in menu
            bettingUI.setVisible(false);

            // Draw menu
            stage.act(delta);
            stage.draw();

            //hide emoji button when in menu
            emojiButton.setVisible(false);

        }

        // Handle emoji display TEST
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) {
            selectedEmojiIndex = 0;
            emojiDisplayStartTime = System.currentTimeMillis();
        }

    }

    // Modify renderBlindPositions()
    private void renderBlindPositions() {
        // Get dealer position
        int dealerPosition = PokerGame.getDealerPosition();

        // Button size
        int buttonWidth = 80;
        int buttonHeight = 80;

        // Calculate small blind and big blind positions
        int smallBlindPosition;
        int bigBlindPosition;
        if (isMultiplayer) {
            smallBlindPosition = getUIPositionForPlayer(dealerPosition % chairPositions.length);
            bigBlindPosition = getUIPositionForPlayer((dealerPosition + 1) % chairPositions.length);
        } else {
            smallBlindPosition = (dealerPosition) % chairPositions.length;
            bigBlindPosition = (dealerPosition + 1) % chairPositions.length;
        }

        // Render Dealer Texture
        float dpX = 120;
        float dpY = 513;
        batch.draw(dealerRegion, dpX, dpY, buttonWidth, buttonHeight);

        // Render Small Blind Texture
        renderBlindTexture(buttonWidth, buttonHeight, smallBlindPosition, smallBlindRegion);

        // Render Big Blind Texture
        renderBlindTexture(buttonWidth, buttonHeight, bigBlindPosition, bigBlindRegion);
    }

    private void renderBlindTexture(int buttonWidth, int buttonHeight, int blindPosition, TextureRegion blindTextureRegion) {
        if (blindPosition == 0 || blindPosition == 1) { // Render above the players cards
            float sbX = chairPositions[blindPosition][0] + 20;
            float sbY = chairPositions[blindPosition][1] + DISPLAY_CARD_HEIGHT + 10;
            batch.draw(blindTextureRegion, sbX, sbY, buttonWidth, buttonHeight);
        } else if (blindPosition == 2) { // Render to the right of the players cards
            float sbX = chairPositions[blindPosition][0] + 2 * DISPLAY_CARD_WIDTH + 30;
            float sbY = chairPositions[blindPosition][1] - 30;
            batch.draw(blindTextureRegion, sbX, sbY, buttonWidth, buttonHeight);
        } else if (blindPosition == 3 || blindPosition == 4) { // Render below the players cards
            float sbX = chairPositions[blindPosition][0] + 25;
            float sbY = chairPositions[blindPosition][1] - DISPLAY_CARD_HEIGHT + 5;
            batch.draw(blindTextureRegion, sbX, sbY, buttonWidth, buttonHeight);
        }
    }

    /**
     * Renders the community cards on the poker table
     * @param communityCards - array with 0-5 cards to be displayed
     * @param x - starting x coordinate for the first card
     * @param y - y coordinate for the cards
     * @param isFaceUp - true if the cards should be displayed face up, false for face down
     */
    public void renderCards(Card[] communityCards, float x, float y, boolean isFaceUp) {
        if (communityCards == null) return;

        for (int i = 0; i < communityCards.length; i++) {
            if (communityCards[i] != null) {
                float cardX = x + i * (DISPLAY_CARD_WIDTH + CARD_SPACING);

                // Play sound for all cards, not just face up ones
                soundManager.playCardSound(communityCards[i], i);

                if (isFaceUp) {
                    // Draw card background
                    batch.draw(cardBackground, cardX, y, DISPLAY_CARD_WIDTH, DISPLAY_CARD_HEIGHT);
                    // Draw card face
                    Card card = communityCards[i];
                    batch.draw(cardRegions[card.getSuit().ordinal()][card.getRank().ordinal()],
                        cardX, y, DISPLAY_CARD_WIDTH, DISPLAY_CARD_HEIGHT);
                } else {
                    // Draw card back
                    batch.draw(cardBack, cardX, y, DISPLAY_CARD_WIDTH, DISPLAY_CARD_HEIGHT);
                }
            }
        }
    }

    /**
     * Renders a stack of cards (dealer's deck) at a specific position.
     * @param x - x coordinate of the stack
     * @param y - y coordinate of the stack
     * @param stackSize - number of cards in the stack
     */
    public void renderCardStack(float x, float y, int stackSize, Texture enhancersSheet) {
        // Extract the card stack background
        TextureRegion cardStackBackground = new TextureRegion(enhancersSheet, 0, 0, ENHANCER_WIDTH, ENHANCER_HEIGHT);
        for (int i = 0; i < stackSize; i++) {
            // Slight offset for each card in the stack
            float offset = i * -(float) 1.75; // Adjust for visual effect
            batch.draw(cardStackBackground, x + offset, y - offset, DISPLAY_CARD_WIDTH, DISPLAY_CARD_HEIGHT);
        }
    }

    private void loadEmojiTextures(){
        // Load emoji textures
        emojiRegions = new TextureRegion[5];

        emojiRegions[0] = new TextureRegion(new Texture(Gdx.files.internal("emojis/laugh.png")));
        emojiRegions[1] = new TextureRegion(new Texture(Gdx.files.internal("emojis/cry.png")));
        emojiRegions[2] = new TextureRegion(new Texture(Gdx.files.internal("emojis/angry.png")));
        emojiRegions[3] = new TextureRegion(new Texture(Gdx.files.internal("emojis/like.png")));
        emojiRegions[4] = new TextureRegion(new Texture(Gdx.files.internal("emojis/boss.png")));

    }

    /**
     * Renders cards with the specified rotation
     * @param communityCards - array with cards to be displayed
     * @param x - starting x coordinate
     * @param y - y coordinate
     * @param rotation - rotation angle in degrees
     */
    public void renderRotatedCards(Card[] communityCards, float x, float y, float rotation, boolean isFaceUp) {
        if (communityCards == null) return;

        float originX = DISPLAY_CARD_WIDTH / 2;
        float originY = DISPLAY_CARD_HEIGHT / 2;

        for (int i = 0; i < communityCards.length; i++) {
            if (communityCards[i] != null) {
                float cardX = x + (DISPLAY_CARD_WIDTH + CARD_SPACING);

                // Play sound for all cards
                soundManager.playCardSound(communityCards[i], i);

                if (isFaceUp) {
                    // Draw card background (rotated)
                    batch.draw(cardBackground, cardX, y - i * (DISPLAY_CARD_WIDTH + CARD_SPACING),
                        originX, originY, DISPLAY_CARD_WIDTH, DISPLAY_CARD_HEIGHT, 1, 1, rotation);

                    // Draw card face (rotated)
                    Card card = communityCards[i];
                    batch.draw(cardRegions[card.getSuit().ordinal()][card.getRank().ordinal()],
                        cardX, y - i * (DISPLAY_CARD_WIDTH + CARD_SPACING),
                        originX, originY, DISPLAY_CARD_WIDTH, DISPLAY_CARD_HEIGHT, 1, 1, rotation);
                } else {
                    // Draw card back (rotated)
                    batch.draw(cardBack, cardX, y - i * (DISPLAY_CARD_WIDTH + CARD_SPACING),
                        originX, originY, DISPLAY_CARD_WIDTH, DISPLAY_CARD_HEIGHT, 1, 1, rotation);
                }
            }
        }
    }

    private void renderPlayerCards(List<Player> players) {
        // Check if we're in showdown state
        boolean isShowdown = pokerGame.getGameState() == PokerGame.GameState.SHOWDOWN;
        int humanPlayerIndex = findHumanPlayerIndex();

        for (int i = 0; i < players.size() && i < chairPositions.length; i++) {
            // map server-side player indexes to UI positions
            int uiPosition = getUIPositionForPlayer(i);

            float x = chairPositions[uiPosition][0];
            float y = chairPositions[uiPosition][1];

            // Get player's cards
            Card[] playerCards = players.get(i).getHoleCards().toArray(new Card[0]);

            // For active opponents in multiplayer mode, ensure we have something to render
            boolean shouldCreatePlaceholders = isMultiplayer &&
                                              players.get(i).isActive() &&
                                              i != humanPlayerIndex &&
                                              playerCards.length == 0;

            // Create placeholder cards for opponents in multiplayer if needed
            Card[] cardsToRender;
            if (shouldCreatePlaceholders) {
                // Create two placeholder cards that will be rendered face down
                cardsToRender = new Card[2];
                cardsToRender[0] = new Card(Card.Suit.CLUBS, Card.Rank.ACE);
                cardsToRender[1] = new Card(Card.Suit.CLUBS, Card.Rank.ACE);
            } else {
                cardsToRender = playerCards;
            }

            // Render up to 2 cards for each player
            for (int j = 0; j < 2; j++) {
                // Skip if no card to render
                if (j >= cardsToRender.length || cardsToRender[j] == null) {
                    continue;
                }

                // Determine if we should render this card
                boolean shouldRender = players.get(i).isActive() &&
                                      (isMultiplayer || dealingAnimator.isCardDealt(i, j));

                if (shouldRender) {
                    if (uiPosition == 2) {
                        // Player at UI position 2 has rotated cards
                        boolean showFaceUp = isShowdown || i == humanPlayerIndex;
                        renderRotatedCards(new Card[]{cardsToRender[j]}, x, y - j * 70, 90, showFaceUp);
                    } else if (i == humanPlayerIndex || isShowdown) {
                        // Human player or showdown state - show face up
                        renderCards(new Card[]{cardsToRender[j]}, x + j * 70, y, true);
                    } else {
                        // Other players during regular gameplay - show face down
                        renderCards(new Card[]{cardsToRender[j]}, x + j * 70, y, false);
                    }
                }
            }
        }
    }

    public void renderWinningHand(List<Card> winningCards) {
        if (winningCards == null || winningCards.isEmpty()) {
            return;
        }

        boolean batchWasActive = batch.isDrawing();
        if (!batchWasActive) {
            batch.begin();
        }

        try {
            float startX = Gdx.graphics.getWidth() * 0.84f - (winningCards.size() * 30);
            float y = Gdx.graphics.getHeight() * 0.35f;
            float cardSpacing = 75;

            for (int i = 0; i < winningCards.size(); i++) {
                Card card = winningCards.get(i);
                float x = startX + i * cardSpacing;

                // Draw gold highlight (semi-transparent rectangle, slightly larger than the card)
                batch.setColor(1.0f, 0.84f, 0.0f, 0.5f); // Gold with 50% opacity
                batch.draw(cardBackground, x - 4, y - 4, DISPLAY_CARD_WIDTH + 8, DISPLAY_CARD_HEIGHT + 8);

                // Draw card background
                batch.setColor(Color.WHITE);
                batch.draw(cardBackground, x, y, DISPLAY_CARD_WIDTH, DISPLAY_CARD_HEIGHT);

                // Draw card face
                batch.draw(cardRegions[card.getSuit().ordinal()][card.getRank().ordinal()],
                    x, y, DISPLAY_CARD_WIDTH, DISPLAY_CARD_HEIGHT);
            }
        } finally {
            if (!batchWasActive) {
                batch.end();
            }
        }
    }

    public void clearWinningHand() {
        this.winningCards = null;
    }

    // Needs to be modified to show current player turn
    private void renderGameElements(float delta) {
        // Get game data
        List<Player> players = pokerGame.getPlayers();
        PokerGame.GameState state = pokerGame.getGameState();
        List<Card> communityCards = pokerGame.getCommunityCards();

        batch.begin();

        // Calculate center positions for community cards
        float centerX = Gdx.graphics.getWidth() / 4.15f;
        float centerY = Gdx.graphics.getHeight() / 2.15f;

        // Render dealer position (deck)
        renderCardStack(Gdx.graphics.getWidth() / 7f, centerY, 5, enhancersSheet);

        renderBlindPositions();

        // Render community cards based on game state
        if (state == PokerGame.GameState.BETTING_FLOP || state == PokerGame.GameState.FLOP) {
            if (communityCards.size() >= 3) {
                Card[] flopCards = {communityCards.get(0), communityCards.get(1), communityCards.get(2), null, null};
                renderCards(flopCards, centerX, centerY, true);
            }
        } else if (state == PokerGame.GameState.BETTING_TURN || state == PokerGame.GameState.TURN) {
            if (communityCards.size() >= 4) {
                Card[] turnCards = {communityCards.get(0), communityCards.get(1),
                    communityCards.get(2), communityCards.get(3), null};
                renderCards(turnCards, centerX, centerY, true);
            }
        } else if (state == PokerGame.GameState.BETTING_RIVER || state == PokerGame.GameState.RIVER
            || state == PokerGame.GameState.SHOWDOWN) {
            Card[] displayCards = communityCards.toArray(new Card[0]);
            renderCards(displayCards, centerX, centerY, true);
        }

        // Show pot amount
        String potText = "Pot: $" + pokerGame.getPot();
        potFont = fontManager.getFont(28, new Color(1.0f, 0.84f, 0.0f, 1.0f));
        potFont.draw(batch, potText,centerX + 80, centerY + 120);

        // Handle card dealing animation
        if (state == PokerGame.GameState.BETTING_PRE_FLOP) {
            dealingAnimator.update(delta, players, chairPositions.length);

            // Update animation timer
            if (!dealingAnimationComplete) {
                dealingAnimationTimer += delta;
                if (dealingAnimationTimer >= DEALING_ANIMATION_DURATION) {
                    dealingAnimationComplete = true;
                }
            }
        } else {
            // Reset animation timer if not in pre-flop state
            dealingAnimationComplete = false;
            dealingAnimationTimer = 0;
        }

        // Render each player's cards
        renderPlayerCards(players);

        // Render player info with current player indicator
        if (pokerGame.needsPlayerAction()) {
            renderPlayerInfo(players, pokerGame.getCurrentPlayerIndex());
        } else {
            renderPlayerInfo(players, -1); // No current player
        }

        // Play win/lose sounds at showdown
        if (state == PokerGame.GameState.SHOWDOWN && !soundManager.isShowdownSoundPlayed()) {
            List<Player> winners = pokerGame.determineWinners();
            boolean humanPlayerWon = false;
            int humanPlayerIndex = findHumanPlayerIndex();

            // Check if human player is among winners
            for (Player winner : winners) {
                if (players.indexOf(winner) == humanPlayerIndex) {
                    humanPlayerWon = true;
                    break;
                }
            }

            // Play appropriate sound
            if (humanPlayerWon) {
                soundManager.playWinSound();
            } else {
                soundManager.playLoseSound();
            }
        }

        // Debug info
//        font.draw(batch, "Game State: " + state.toString(), 50, 100);
//        font.draw(batch, "Community Cards: " + communityCards.size(), 50, 50);

        // DEBUG: Add buttons to force next state (for testing)
        if (Gdx.input.justTouched() && Gdx.input.getY() < 150) {
            // Simple way to advance game state for testing
            if (state == PokerGame.GameState.BETTING_PRE_FLOP) {
                if (isMultiplayer) {
                    // In multiplayer, just force the animation to complete
                    dealingAnimationComplete = true;
                    dealingAnimationTimer = DEALING_ANIMATION_DURATION;
                    System.out.println("Forcing dealing animation completion in multiplayer");
                } else {
                    pokerGame.dealFlop();
                }
            } else if (state == PokerGame.GameState.BETTING_FLOP && !isMultiplayer) {
                pokerGame.dealTurn();
            } else if (state == PokerGame.GameState.BETTING_TURN && !isMultiplayer) {
                pokerGame.dealRiver();
            } else if (state == PokerGame.GameState.BETTING_RIVER && !isMultiplayer) {
                pokerGame.goToShowdown();
            } else if (state == PokerGame.GameState.SHOWDOWN && !isMultiplayer) {
                dealingAnimator.reset();
            }
        }

        batch.end();
    }

    public static void resetGameRenderer() {
        dealingAnimator.reset();
        dealingAnimationComplete = false;
        dealingAnimationTimer = 0;
    }

    private void renderBetChips(int betAmount, float x, float y, int playerIndex) {
        if (betAmount <= 0) return;

        // Play chip sound when betAmount changes
        soundManager.playChipSound(betAmount, playerIndex);

        // Map bet to chips (500, 100, 25, 10, 5)
        int[] chipValues = {5, 10, 25, 100, 500};
        int[] chipCounts = new int[chipValues.length];
        int remainingAmount = betAmount;

        // Calculate chips needed (start from highest value)
        for (int i = chipValues.length - 1; i >= 0; i--) {
            chipCounts[i] = remainingAmount / chipValues[i];
            remainingAmount %= chipValues[i];
        }

        // Display parameters
        float chipDisplaySize = 40;
        float stackOffsetY = 4.0f;  // Smaller vertical offset for a more compact stack
        int totalChipsToRender = 0;
        int maxChipsToDisplay = 12;  // Limit total stack height

        // Count total chips that will be rendered
        for (int count : chipCounts) {
            totalChipsToRender += count;
        }

        // Reduce count proportionally if we have too many chips
        if (totalChipsToRender > maxChipsToDisplay) {
            float reductionRatio = (float)maxChipsToDisplay / totalChipsToRender;
            for (int i = 0; i < chipCounts.length; i++) {
                chipCounts[i] = Math.round(chipCounts[i] * reductionRatio);
            }
        }

        // Render chips in a single stack from bottom to top
        int chipIndex = 0;

        // Start with smallest denomination at the bottom
        for (int i = 0; i < chipValues.length; i++) {
            for (int j = 0; j < chipCounts[i]; j++) {
                float yOffset = chipIndex * stackOffsetY;

                // Draw shadow for better visibility
                batch.setColor(0, 0, 0, 0.5f);
                batch.draw(chipRegion[i], x + 1, y + yOffset - 1, chipDisplaySize, chipDisplaySize);

                // Draw the chip
                batch.setColor(Color.WHITE);
                batch.draw(chipRegion[i], x, y + yOffset, chipDisplaySize, chipDisplaySize);

                chipIndex++;
                if (chipIndex >= maxChipsToDisplay) {
                    batch.setColor(Color.WHITE); // Reset color
                    return;
                }
            }
        }

        // Reset color
        batch.setColor(Color.WHITE);
    }

    public void toggleZoom() {
        isZoomed = !isZoomed;

        float centerX = Gdx.graphics.getWidth() / 2.93f;
        float centerY = Gdx.graphics.getHeight() / 2f;

        if (isZoomed) {
            camera.zoom = focusedZoom;
            camera.position.set(centerX, centerY, 0);
            bettingUIVisible = false; // Hide betting UI when zoomed in
        } else {
            camera.zoom = defaultZoom;
            camera.position.set(Gdx.graphics.getWidth() / 2f, Gdx.graphics.getHeight() / 2f, 0);
            bettingUIVisible = true; // Show betting UI when zoomed out
        }
        camera.update();
    }

    private void renderPlayerInfo(List<Player> players, int currentPlayerIndex) {
        // Define individual text offsets for each player
        float[][] textOffsets = {
            {20, 300}, // player 1
            {20, 300}, // player 2
            {310, 50}, // player 3
            {20, -90},  // player 4
            {20, -90}  // player 5
        };

        // Avatar display settings
        final int AVATAR_SIZE = 60;
        final float AVATAR_PADDING = 0;
        final float BORDER_THICKNESS = 3; // Added border thickness variable

        int humanPlayerIndex = findHumanPlayerIndex();

        for (int i = 0; i < players.size() && i < chairPositions.length; i++) {
            // map server side player index to UI position
            int uiPosition = getUIPositionForPlayer(i);

            float x = chairPositions[uiPosition][0];
            float y = chairPositions[uiPosition][1] - 40;

            // Apply offset for each player
            float textOffsetX = textOffsets[uiPosition][0];
            float textOffsetY = textOffsets[uiPosition][1];

            // Draw player avatar (if available)
            if (avatarRegions != null) {
                // Calculate which avatar to use for this player
                int index = playerAvatarIndices[i];
                int row = index / 10;
                int col = index % 10;

                // Position avatar based on player position
                float avatarX, avatarY;

                if (uiPosition == 0 || uiPosition == 1) { // Top players
                    avatarX = x + textOffsetX - AVATAR_SIZE - 10;
                    avatarY = y + textOffsetY - AVATAR_SIZE/2;
                } else if (uiPosition == 2) { // Right player
                    avatarX = x + textOffsetX - AVATAR_SIZE - 10;
                    avatarY = y + textOffsetY - AVATAR_SIZE/2;
                } else { // Bottom players
                    avatarX = x + textOffsetX - AVATAR_SIZE - 10;
                    avatarY = y + textOffsetY - AVATAR_SIZE/2;
                }

                // Draw border as four lines around the avatar instead of a padded background
                batch.setColor(0.2f, 0.2f, 0.2f, 1.0f);

                // Top border
                batch.draw(cardBackground, avatarX - BORDER_THICKNESS, avatarY + AVATAR_SIZE,
                          AVATAR_SIZE + BORDER_THICKNESS*2, BORDER_THICKNESS);

                // Bottom border
                batch.draw(cardBackground, avatarX - BORDER_THICKNESS, avatarY - BORDER_THICKNESS,
                          AVATAR_SIZE + BORDER_THICKNESS*2, BORDER_THICKNESS);

                // Left border
                batch.draw(cardBackground, avatarX - BORDER_THICKNESS, avatarY - BORDER_THICKNESS,
                          BORDER_THICKNESS, AVATAR_SIZE + BORDER_THICKNESS*2);

                // Right border
                batch.draw(cardBackground, avatarX + AVATAR_SIZE, avatarY - BORDER_THICKNESS,
                          BORDER_THICKNESS, AVATAR_SIZE + BORDER_THICKNESS*2);

                // Draw avatar
                batch.setColor(Color.WHITE);
                if (row < avatarRegions.length && col < avatarRegions[0].length) {
                    batch.draw(avatarRegions[row][col], avatarX, avatarY, AVATAR_SIZE, AVATAR_SIZE);
                }

                //Emoji test
                EmojiDisplayData data = activeEmojis.get(i);
                if (data != null && System.currentTimeMillis() - data.displayStartTime < EMOJI_DISPLAY_DURATION) {
                    float emojiX = x + textOffsetX + 10;
                    float emojiY = y + textOffsetY + 70;

                    batch.setColor(1f, 1f, 1f, data.getAlpha()); // fade-out
                    batch.draw(emojiRegions[data.emojiIndex], emojiX, emojiY, 40, 40);
                    batch.setColor(Color.WHITE); // reset
                }


            }

            // Get player name font based on player type
            BitmapFont playerFont;
            String playerInfo = players.get(i).getName() + ": $" + players.get(i).getChips();

            if (i == humanPlayerIndex) {
                playerFont = fontManager.getFont(24, new Color(0.2f, 0.6f, 1.0f, 1.0f)); // Blue for human player
                playerInfo += " (You)";
            } else {
                playerFont = fontManager.getFont(24, Color.WHITE); // White for other players
            }

            // Draw player info with shifted Y coordinate (moved up 25 pixels)
            playerFont.draw(batch, playerInfo, x + textOffsetX, y + textOffsetY + 25);

            if (players.get(i).getCurrentBet() > 0) {
                BitmapFont betFont = fontManager.getFont(18, new Color(1.0f, 0.84f, 0.0f, 1.0f)); // Gold for bet amounts

                // Check if player has checked or folded
                if (players.get(i).getCurrentBet() == pokerGame.getCurrentBet() &&
                    hasActedInRound(i) &&
                    pokerGame.getCurrentBet() == 0) {
                    // Draw check/fold status with adjusted Y position (moved up 20 pixels)
                    if (!players.get(i).isActive()) {
                        betFont.draw(batch, "Fold", x + textOffsetX, y + textOffsetY - 10);
                    } else {
                        betFont.draw(batch, "Check", x + textOffsetX, y + textOffsetY - 10);
                    }

                } else {
                    // Draw bet amount with adjusted Y position (moved up 20 pixels)
                    betFont.draw(batch, "Bet: $" + players.get(i).getCurrentBet(), x + textOffsetX, y + textOffsetY - 10);
                }

                // Calculate position for chips based on player position
                float chipX, chipY;
                if (i == 0 || i == 1) { // Top players
                    chipX = x - 60;
                    chipY = y + 45;
                } else if (i == 2) { // Right player
                    chipX = x + 60;
                    chipY = y + 130;
                } else { // Bottom players
                    chipX = x + 145;
                    chipY = y + 45;
                }
                renderBetChips(players.get(i).getCurrentBet(), chipX, chipY, i);
            } else if (hasActedInRound(i) && pokerGame.getCurrentBet() == 0) {
                // Player has acted but has no bet (they checked)
                BitmapFont betFont = fontManager.getFont(18, new Color(1.0f, 0.84f, 0.0f, 1.0f));
                // Draw check/fold status with adjusted Y position (moved up 20 pixels)
                // TODO: Fold nu apare in PREFLOP pentru jucatorul 4 (human)
                if(!players.get(i).isActive()) {
                    betFont.draw(batch, "Fold", x + textOffsetX, y + textOffsetY - 10);
                } else {
                    betFont.draw(batch, "Check", x + textOffsetX, y + textOffsetY - 10);
                }
            }


            // Indicator for current player
            if (i == currentPlayerIndex && pokerGame.needsPlayerAction()) {
                if (avatarRegions != null) {
                    float avatarX = x + textOffsetX - AVATAR_SIZE - 10;
                    float avatarY = y + textOffsetY - AVATAR_SIZE/2;

                    float borderThickness = 2f;
                    float cornerOverlap = borderThickness; // Amount to extend lines for overlap

                    batch.setColor(1.0f, 0.84f, 0.0f, 1.0f); // Gold highlight

                    // Top line - extended on both sides
                    batch.draw(cardBackground, avatarX - 1 - cornerOverlap,
                              avatarY + AVATAR_SIZE + 1,
                              AVATAR_SIZE + 2 + (cornerOverlap * 2), borderThickness);

                    // Bottom line - extended on both sides
                    batch.draw(cardBackground, avatarX - 1 - cornerOverlap,
                              avatarY - borderThickness - 1,
                              AVATAR_SIZE + 2 + (cornerOverlap * 2), borderThickness);

                    // Left line - extended on both ends
                    batch.draw(cardBackground, avatarX - borderThickness - 1,
                              avatarY - 1 - cornerOverlap,
                              borderThickness, AVATAR_SIZE + 2 + (cornerOverlap * 2));

                    // Right line - extended on both ends
                    batch.draw(cardBackground, avatarX + AVATAR_SIZE + 1,
                              avatarY - 1 - cornerOverlap,
                              borderThickness, AVATAR_SIZE + 2 + (cornerOverlap * 2));

                    batch.setColor(Color.WHITE);
                }
            }
        }
    }

    private boolean hasActedInRound(int playerIndex) {
        // You need to access the hasActedInRound array from PokerGame
        // Since it's private, you might need to add a getter method in PokerGame
        return pokerGame.hasPlayerActedInRound(playerIndex);
    }

    // Handle player turns and betting UI
    private void handlePlayerTurns() {
        // Check if we should block actions during animation
        boolean shouldBlock = PokerGame.getGameState() == PokerGame.GameState.BETTING_PRE_FLOP &&
                             !dealingAnimationComplete;

        // Only proceed with betting actions if we shouldn't block
        if (!shouldBlock) {
            // Update the betting UI
            if (bettingUI != null) {
                bettingUI.update();
            }

            int humanPlayerIndex = findHumanPlayerIndex();

            // Check if it's a bot's turn and we need to start it thinking
            if (pokerGame.needsPlayerAction() &&
                pokerGame.getCurrentPlayerIndex() != humanPlayerIndex &&
                gameController != null && !gameController.isBotThinking()) {

                // Trigger the bot thinking process in the controller
                gameController.startBotThinking(pokerGame.getCurrentPlayerIndex());
            }
        }
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        camera.setToOrtho(false, width, height);
        camera.update();
    }

    // TODO: add it back later
//    private void triggerRandomBotEmoji() {
//        List<Player> players = pokerGame.getPlayers();
//
//        // Alege un bot random (index diferit de HUMAN_PLAYER_INDEX)
//        int randomIndex;
//        do {
//            randomIndex = MathUtils.random(players.size() - 1);
//        } while (randomIndex == HUMAN_PLAYER_INDEX || !players.get(randomIndex).isActive());
//
//        int emojiIndex = MathUtils.random(emojiRegions.length - 1);
//        activeEmojis.put(randomIndex, new EmojiDisplayData(emojiIndex, System.currentTimeMillis()));
//    }

    public void setMultiplayerMode(ClientConnection clientConnection, String username) {
        isMultiplayer = true;
        this.clientConnection = clientConnection;
        this.currentUsername = username;
    }

    private int findHumanPlayerIndex() {
        if (!isMultiplayer) {
            return 3; // default for singleplayer
        }

        // in multiplayer, find player by username
        List<Player> players = pokerGame.getPlayers();
        if (players.isEmpty()) {
            return 0; // no players yet, return the first index
        }

        // search current player by the username
        if (currentUsername != null) {
            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).getName().equals(currentUsername)) {
                    return i;
                }
            }
        }

        // default to first player if not found
        return 0;
    }

    /**
     * Maps server-side player indices to UI positions
     * @param serverPlayerIndex The player index from the server's perspective
     * @return The UI position (0-4) where this player should be displayed
     */
    private int getUIPositionForPlayer(int serverPlayerIndex) {
        if (!isMultiplayer) {
            return serverPlayerIndex; // keep original index for singleplayer
        }

        int localPlayerIndex = findHumanPlayerIndex();
        int playerCount = pokerGame.getPlayers().size();

        // relative position (0 - current player, 1 - next player clockwise...)
        int relativePosition = (serverPlayerIndex - localPlayerIndex + playerCount) % playerCount;

        // map relative position to fixed UI positions
        // 0 = top left, 1 = top right, 2 = middle right, 3 = bottom right, 4 = bottom left
        // we want current player (relative position 0) to always be at position 3 (bottom right)
        return switch (relativePosition) {
            case 0 -> 3; // current player bottom right
            case 1 -> 4; // next player clockwise bottom left
            case 2 -> 0; // next player clockwise top left
            case 3 -> 1; // next player clockwise top right
            case 4 -> 2; // next player clockwise middle right
            default -> 3; // fallback to current player position
        };
    }

    public void setMenuStarted(boolean started) {
        if (menu != null) {
            menu.setGameStarted(started);
        }
    }

    public Stage getStage() {
        return stage;
    }

    public Menu getMenu() {
        return menu;
    }

    public static boolean isDealingAnimationComplete() {
        return dealingAnimationComplete;
    }

    public void setWinningCards(List<Card> winningCards) {
        this.winningCards = winningCards;
    }

    public BettingUI getBettingUI() {
        return bettingUI;
    }

    public boolean isMultiplayer() {
        return isMultiplayer;
    }

    public void dispose() {
        batch.dispose();
        stage.dispose();
        // Don't dispose individual fonts, FontManager will handle it
        fontManager.dispose();
        cardSheet.dispose();
        enhancersSheet.dispose();
        backgroundTexture.dispose();
        buttonsSheet.dispose();
        menu.dispose();
        bettingUI.dispose();
        soundManager.dispose();
        avatarsTexture.dispose();
    }

}
