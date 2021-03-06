package dev.teamcyan.dungeoncrafter.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.TextureData;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.scenes.scene2d.Event;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.utils.Timer;
import dev.teamcyan.dungeoncrafter.DungeonCrafter;
import dev.teamcyan.dungeoncrafter.classes.*;

import java.util.ArrayList;
import java.util.List;

/**
 * View for the currently active game.
 */
public class MainGameScreen extends BaseScreen {

  /**
   * Batch that updates camera-sensitive parts of the screen
   */
  SpriteBatch batch = new SpriteBatch();
  /**
   * Batch that updates camera-insensitive parts of the screen
   */
  SpriteBatch hud = new SpriteBatch();
  /**
   * ShapeRenderer that creates camera-insensitive parts of the screen
   */
  private ShapeRenderer shapeRenderer;
  /**
   * ShapeRenderer that creates camera-sensitive parts of the screen
   */
  private ShapeRenderer cameraShapeRenderer;

  boolean movingRight = false;
  boolean movingLeft = false;
  boolean movingUp = false;
  boolean movingDown = false;
  boolean zoomIn = false;
  boolean zoomOut = false;
  boolean keyA = false;
  boolean keyD = false;
  boolean keyS = false;
  boolean keyQ = false;
  boolean keyE = false;
  boolean keyW = false;

  /**
   * Current text in pebble's speech bubble
   */
  private TextureData curSpeech;

  private GameElement.State prevState;

  /**
   * Label for difficulty dependant game countdown
   */
  private Label timerLabel;
  private Label.LabelStyle timerStyle;


  Matrix4 uiMatrix;
  private BitmapFont font;
  private BitmapFont timerFont;
  private ArrayList<String> keyInfo;
  private String mouseInfo;
  private float timeLeft;
  private float totTime;
  private boolean timeUp;
  private GameModel model;

  private static final int MENU_BUTTON_X = DungeonCrafter.WIDTH-80;
  private static final int MENU_BUTTON_Y = DungeonCrafter.HEIGHT-45;

  private static final int HEALTH_BAR_WIDTH = DungeonCrafter.WIDTH/24*5;
  private static final int HEALTH_BAR_HEIGHT = 10;
  private static final int HEALTH_BAR_X = 30;
  private static final int HEALTH_BAR_Y = DungeonCrafter.HEIGHT-HEALTH_BAR_HEIGHT-40;

  private static final int INVENTORY_BUTTON_X = HEALTH_BAR_X+HEALTH_BAR_WIDTH+100;
  private static final int INVENTORY_BUTTON_Y = DungeonCrafter.HEIGHT-45;


  /**
   * Constructor - Setup the camera-insensitive parts of the screen (hud)
   * Inherit the game and the model from the parent class
   * @param parent Controller
   * @param model GameModel
   */
  public MainGameScreen(DungeonCrafter parent, GameModel model) {
    super(parent, model);
    this.model = model;

    shapeRenderer = new ShapeRenderer();
    cameraShapeRenderer = new ShapeRenderer();

    final Label.LabelStyle style = new Label.LabelStyle();
    timerStyle = new Label.LabelStyle();
    style.fontColor = Color.WHITE;
    style.font = new BitmapFont();
    style.font.getData().setScale(2f);

    timerStyle.fontColor = Color.WHITE;
    timerStyle.font = new BitmapFont();
    timerStyle.font.getData().setScale(2f);

    timerLabel = new Label("Test", timerStyle);
    final Label settingsLabel = new Label("Menu", style);
    final Button.ButtonStyle buttonStyle = new Button.ButtonStyle();
    final Button settingsButton = new Button(settingsLabel, buttonStyle);
    settingsButton.setBounds(MENU_BUTTON_X,MENU_BUTTON_Y,0,0);
    ui.addActor(settingsButton);

    settingsButton.addListener(new EventListener()
        {
          @Override
          public boolean handle(Event event)
          {
            if (event.toString() == "enter") {
              settingsLabel.setFontScale(3f);
            } else if (event.toString() == "exit") {
              settingsLabel.setFontScale(2f);
            } else if (event.toString() == "touchDown") {
              controller.changeScreen(MainMenuScreen.class);
            }

            return true;
          }
        });

    final Label inventoryLabel = new Label("Inventory", style);
    final Button inventoryButton = new Button(inventoryLabel, buttonStyle);
    inventoryButton.setBounds(INVENTORY_BUTTON_X,INVENTORY_BUTTON_Y,0,0);
    ui.addActor(inventoryButton);

    inventoryButton.addListener(new EventListener()
        {
          @Override
          public boolean handle(Event event)
          {
            if (event.toString() == "enter") {
              inventoryLabel.setFontScale(3f);
            } else if (event.toString() == "exit") {
              inventoryLabel.setFontScale(2f);
            } else if (event.toString() == "touchDown") {
              controller.changeScreen(InventoryScreen.class);
            }

            return true;
          }
        });


    font = new BitmapFont();
    timerFont = new BitmapFont();
    keyInfo = new ArrayList<String>();
    mouseInfo = "";
  }


  /**
   * Setup the game audio
   * */
  @Override
  public void init() {
    totTime = super.controller.totTime;
    super.controller.audioManager.startMusic(super.controller.audioManager.ambients, 20);
    super.controller.audioManager.stopMusic(super.controller.audioManager.menuSound);
    // Create countdown variable for overlay

    if(!leavingInv) {
      timeLeft = totTime;
      timer.scheduleTask(new Timer.Task() {
        @Override
        public void run() {
          timeLeft = timeLeft - (float) 0.1;
          if (timeLeft == 0.0)
            timeUp = true;
          // Attempting to add interactive audio - need to fix audio buffer error
          if (timeLeft < 0.1 * totTime) {
            MainGameScreen.super.controller.audioManager.startMusicStr(
                    "tick");
          }
        }
      }, 0, (float) 0.1, (int) totTime * 10);
    }
  }


  /**
   * All the game updates origin from here.
   * @param delta amount of time that passed since last call
   */
  @Override
  public void draw(float delta) {

    Gdx.gl.glClearColor(0, 0, 0, 1);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

    model.getCamera().zoom = (float)Math.max((super.controller.startZoom - Math.pow(((totTime - timeLeft)/(totTime)), 2)), .7);
    // Cap max and min zoom levels
    if(zoomIn) {
      model.getCamera().zoom = (float) Math.max(0.1, (model.getCamera().zoom - DungeonCrafter.ZOOM_FACTOR));
    }
    if(zoomOut) {
      model.getCamera().zoom = (float) Math.min(3, (model.getCamera().zoom + DungeonCrafter.ZOOM_FACTOR));
    }

    // Get the current cell the player is over
    TiledMapTileLayer.Cell curCell = model.getMap().getBlock
            (new Pos(model.getPlayer().getPosition().getX(),
                    model.getPlayer().getPosition().getY()));


    // Loop warp sound effect when over antigravity background
    TiledMapTile currentBackgroundTile = model.getMap().getBackgroundTile
            (new Pos(model.getPlayer().getPosition().getX() + model.getPlayer().getRegion().getRegionWidth()/2,
                    model.getPlayer().getPosition().getY()));


    if(timeLeft < 0.1)
      controller.changeScreen(GameOverScreen.class);

    if(currentBackgroundTile.getProperties().get("inverseGravity") != null)
      controller.audioManager.antiGrav("play");
    else
      controller.audioManager.antiGrav("stop");

    boolean isBroken = false;

    //if Digging Left is pressed
    if(keyA) {
      isBroken = model.getMap().interactBlockLeft(
          new Pos(
            model.getPlayer().getPosition().getX() + model.getPlayer().getRegion().getRegionWidth()/2,
            model.getPlayer().getPosition().getY() + model.getPlayer().getRegion().getRegionHeight()/2));
    }

    // if Digging Right  is pressed

    if(keyD) {
      isBroken = model.getMap().interactBlockRight(
          new Pos(
            model.getPlayer().getPosition().getX() + model.getPlayer().getRegion().getRegionWidth()/2,
            model.getPlayer().getPosition().getY() + model.getPlayer().getRegion().getRegionHeight()/2));
    }


    //if Digging Center  is pressed
    if(keyS) {
      if (curCell != null) {
        controller.audioManager.breakBlock("gravel"); // curCell.getTile().getTextureRegion().getTexture().toString()
      }
      isBroken = model.getMap().interactBlockCentre(
          new Pos(
            model.getPlayer().getPosition().getX() + model.getPlayer().getRegion().getRegionWidth()/2,
            model.getPlayer().getPosition().getY() + model.getPlayer().getRegion().getRegionHeight()/2));
    }

    //if Digging up  is pressed
    if(keyW) {
      if (curCell != null) {
        controller.audioManager.breakBlock("gravel"); // curCell.getTile().getTextureRegion().getTexture().toString()
      }
      isBroken = model.getMap().interactBlockUp(
          new Pos(
            model.getPlayer().getPosition().getX() + model.getPlayer().getRegion().getRegionWidth()/2,
            model.getPlayer().getPosition().getY() + model.getPlayer().getRegion().getRegionHeight()/2));
    }

    if (isBroken) {
      controller.audioManager.breakBlock("gravel");
    }

    //if Place left is pressed
    if(keyQ) {
      model.getMap().setBlockLeft(
          new Pos( 
            model.getPlayer().getPosition().getX() + model.getPlayer().getRegion().getRegionWidth()/2,
            model.getPlayer().getPosition().getY() + model.getPlayer().getRegion().getRegionHeight()/2));
    }

    //if Place right is pressed
    if(keyE) {
      model.getMap().setBlockRight(
          new Pos( 
            model.getPlayer().getPosition().getX() + model.getPlayer().getRegion().getRegionWidth()/2,
            model.getPlayer().getPosition().getY() + model.getPlayer().getRegion().getRegionHeight()/2));
    }

    TiledMapTileLayer layer = (TiledMapTileLayer)model.getMap().getTerainLayer();

    model.getPlayer().setRegion(controller.keyListener);

    model.getCamera().translate( 
        (model.getPlayer().getPosition().getX() - model.getPlayer().setX(layer, movingLeft, movingRight)) * (-1),
        (model.getPlayer().getPosition().getY() - model.getPlayer().setY(layer, controller.keyListener)) * (-1), 0);
    model.getCamera().position.set(model.getPlayer().getPosition().getX(), model.getPlayer().getPosition().getY(), 0);
    model.getCamera().update();

    model.getPebble().setRegion();
    model.getPebble().setX(layer, model.getPlayer().getPosition(), model.getPlayer().getVelocity());
    model.getPebble().setY(layer);

    for (GEEnemy enemy : model.getEnemies()) {
      if (enemy.isAlive()) {
        enemy.setRegion();
        enemy.setX(layer, model.getPlayer().getPosition());
        enemy.setY(layer);
      }
    }

    model.getMap().getMapRenderer().setView(model.getCamera());
    model.getMap().getMapRenderer().render();
    batch.setProjectionMatrix(model.getCamera().combined);

    model.getSpeech().setSpeech();
    model.getSpeech().setPosition();


    if(model.getSpeech().isSpeaking() == true &
            model.getSpeech().getSpeech().getTextureData().equals(curSpeech) == false){
        curSpeech = model.getSpeech().getSpeech().getTextureData();
        controller.audioManager.startMusicStr("notify");
    }

    if(model.getPlayer().currentState == GameElement.State.JUMPING & !model.getPlayer().currentState.equals(prevState))
      controller.audioManager.startMusicStr("jump");
    prevState = model.getPlayer().currentState;
    batch.begin();

    if(uiMatrix == null) {
      uiMatrix = model.getCamera().combined.cpy();
    }

    model.getCamera().update();

    font.setColor(1,1,1,1);

    GEPlayer player = model.getPlayer();
    batch.draw(
        player.getRegion(),
        player.getPosition().getX(), 
        player.getPosition().getY(), 
        player.getRegion().getRegionWidth(), 
        player.getRegion().getRegionHeight()); 

    GEPebble pebble = model.getPebble();
    batch.draw(pebble.getRegion(), pebble.getPosition().getX(), pebble.getPosition().getY(), 47,47); // this will be diffrent when you have nummbers at end eg player_1, player_2

    for (GEEnemy enemy : model.getEnemies()) {
      if (enemy.isAlive()) {
        batch.draw(enemy.getRegion(), enemy.getPosition().getX(), enemy.getPosition().getY(), enemy.getRegion().getRegionWidth(), enemy.getRegion().getRegionHeight());

        if (enemy.getClass() == GEEnemy.class) {
          List<GEProjectile> projectiles = enemy.getProjectiles(layer, player.getPosition());
          for (GEProjectile t : projectiles) {
            batch.draw(t.getTexture(), t.getPosition().getX(), t.getPosition().getY(), 16, 2, t.getTexture().getWidth(), t.getTexture().getHeight(), 1, 1, (float) t.getAngle(), 0, 0, 35, 5, false, false);
          }
        }
      }
    }

    GESpeech speech = model.getSpeech();
    if(speech.isSpeaking()) {
      batch.draw(speech.getSpeech(), pebble.getPosition().getX() + speech.getSpeechX(), pebble.getPosition().getY() + speech.getSpeechY(), speech.getSpeechWidth(), speech.getSpeechHeight());
    }




    // Configure fading colour of timer
    float redAmount = (float)(totTime - timeLeft) / (float)totTime;
    float greenAmount = 1-redAmount;
    Color timerColor = new Color(redAmount, greenAmount, 0, 1);
    timerStyle.fontColor = timerColor;
    timerLabel.setStyle(timerStyle);
    timerLabel.setText(Float.toString(timeLeft).substring(0, Math.min(Float.toString(timeLeft).length(), 5)));
    ui.addActor(timerLabel);
    timerLabel.draw(batch, 1);
    batch.end();
    model.getCamera().update();

    //enemy healthbar
    cameraShapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
    cameraShapeRenderer.setProjectionMatrix(this.model.getCamera().combined);
    for (GEEnemy enemy : model.getEnemies()) {
      if (enemy.isAlive()) {
        cameraShapeRenderer.setColor(Color.RED);
        cameraShapeRenderer.rect(enemy.getPosition().getX() - 20, enemy.getPosition().getY() + 60, enemy.getHealth(), 7);
        cameraShapeRenderer.setColor(Color.WHITE);
        cameraShapeRenderer.rect(enemy.getPosition().getX() - 20 + enemy.getHealth(), enemy.getPosition().getY() + 60, 100 - enemy.getHealth(), 7);
      }
    }
    cameraShapeRenderer.end();

        // Play next song when previous finishes
      //if(controller.audioManager.ambientMusic.get(controller.audioManager.curSong).isPlaying() == false){
      //  controller.audioManager.curSong =  (controller.audioManager.curSong + 1) %
      //          controller.audioManager.ambientMusic.size();
      //  controller.audioManager.startMusic(controller.audioManager.ambientMusic.get(controller.audioManager.curSong),
      //          40);
     // }


    // health bar
    float goldArmour = model.getPlayer().getHealth() == 300 ? 100 : (model.getPlayer().getHealth() > 200 ? model.getPlayer().getHealth() % 100 : 0);
    float ironArmour = model.getPlayer().getHealth()-goldArmour == 200 ? 100 : (model.getPlayer().getHealth()-goldArmour > 100 ? (model.getPlayer().getHealth()-goldArmour) % 100 : 0);
    float health = (model.getPlayer().getHealth()-goldArmour-ironArmour)/100f;

    shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
    shapeRenderer.setColor(Color.BLUE);
    shapeRenderer.rect(HEALTH_BAR_X, HEALTH_BAR_Y, HEALTH_BAR_WIDTH*health, HEALTH_BAR_HEIGHT);
    shapeRenderer.setColor(Color.WHITE);
    shapeRenderer.rect(HEALTH_BAR_X+HEALTH_BAR_WIDTH*health, HEALTH_BAR_Y, HEALTH_BAR_WIDTH-(HEALTH_BAR_WIDTH*health), HEALTH_BAR_HEIGHT);
    //ironArmour
    shapeRenderer.setColor(Color.GRAY);
    shapeRenderer.rect(HEALTH_BAR_X, HEALTH_BAR_Y-HEALTH_BAR_HEIGHT, HEALTH_BAR_WIDTH*ironArmour/100f, HEALTH_BAR_HEIGHT);
    //goldArmour
    shapeRenderer.setColor(Color.GOLD);
    shapeRenderer.rect(HEALTH_BAR_X, HEALTH_BAR_Y-HEALTH_BAR_HEIGHT*2, HEALTH_BAR_WIDTH*goldArmour/100f, HEALTH_BAR_HEIGHT);
    shapeRenderer.end();

  }

  /**
   * update the camera on window resizes
   * @param width width of newly resized window
   * @param height width of newly resized window
   */
  @Override
  public void resize(int width, int height) {
    model.getCamera().viewportWidth = width;
    model.getCamera().viewportHeight = height;
    model.getCamera().position.set(model.getMap().getMapPixelWidth()/2f, model.getMap().getMapPixelHeight()/2f, 0); //by default model.getCamera() position on (0,0,0)
    model.getCamera().update();
  }

  @Override
  public void pause() {

  }

  @Override
  public void resume() {

  }

  /**
   * Flush all stateful data.
   */
  @Override
  public void hide() {

  }

  /**
   * Safely dispose all data if necessary
   */
  @Override
  public void dispose() {
    batch.dispose();
    hud.dispose();
    super.controller.audioManager.stopMusic(super.controller.audioManager.ambients);
    if (model.getMap() != null) {
      model.getMap().getTiledMap().dispose();
    }

  }

   /**
   * Implement for keyboard inputs. Called immediately on key presses.
   * @param keycode the code of the key that was clicked. One of Input.Keys.
   * @return boolean whether the input was processed
   */
  @Override
  public boolean keyDown(int keycode) {
    super.controller.keyListener.keyDownListener(keycode);

    if(keycode == Input.Keys.LEFT) 
      movingLeft = true;

    if(keycode == Input.Keys.RIGHT) {
      movingRight = true;
    }
    if(keycode == Input.Keys.UP) {
      movingUp = true;
    }
    if(keycode == Input.Keys.DOWN) {
      movingDown = true;
    }
    if(keycode == Input.Keys.I) {
      zoomIn = true;
    }
    if(keycode == Input.Keys.O) {
      zoomOut = true;
    }
    if(keycode == Input.Keys.A) {
      keyA = true;
    }

    if(keycode == Input.Keys.D) {
      keyD = true;
    }

    if(keycode == Input.Keys.S) {
      keyS = true;
    }

    if(keycode == Input.Keys.E) {
      keyE = true;
    }

    if(keycode == Input.Keys.Q) {
      keyQ = true;
    }

    if(keycode == Input.Keys.W) {
      keyW = true;
    }
    return false;
  }

  /**
   * Implement for keyboard inputs. Called immediately on key releases.
   * @param keycode the code of the key that was clicked. One of Input.Keys.
   * @return boolean whether the input was processed
   */
  @Override
  public boolean keyUp(int keycode) {
    super.controller.keyListener.keyUpListener(keycode);

    if(keycode == Input.Keys.RIGHT)
      movingRight = false;

    if(keycode == Input.Keys.LEFT)
      movingLeft = false;

    if(keycode == Input.Keys.UP)
      movingUp = false;

    if(keycode == Input.Keys.DOWN)
      movingDown = false;

    if(keycode == Input.Keys.I)
      zoomIn = false;

    if(keycode == Input.Keys.O)
      zoomOut = false;

    if(keycode == Input.Keys.A)
      keyA = false;

    if(keycode == Input.Keys.D)
      keyD = false;

    if(keycode == Input.Keys.S)
      keyS = false;

    if(keycode == Input.Keys.E) 
      keyE = false;
    
    if(keycode == Input.Keys.Q) 
      keyQ = false;

    if(keycode == Input.Keys.W) 
      keyW = false;

    return false;
  }

  /**
   * Implement for keyboard inputs. Called when a key was typed.
   * @param character the character that was typed
   * @return boolean whether the input was processed
   */
  @Override
  public boolean keyTyped(char character) {
    return false;
  }

  /**
   * Implement for screen clicks or touches. Called immediately on screen press.
   * @param screenX x-coordinate of the click. Origin is in the upper left corner.
   * @param screenY y-coordinate of the click. Origin is in the upper left corner.
   * @param pointer the pointer for the event.
   * @param button the button for the event.
   * @return boolean whether the input was processed
   */
  @Override
  public boolean touchDown(int screenX, int screenY, int pointer, int button) {
    return false;
  }

  /**
   * Implement for screen clicks or touches. Called immediately on screen release.
   * @param screenX x-coordinate of the click. Origin is in the upper left corner.
   * @param screenY y-coordinate of the click. Origin is in the upper left corner.
   * @param pointer the pointer for the event.
   * @param button the button for the event.
   * @return boolean whether the input was processed
   */
  @Override
  public boolean touchUp(int screenX, int screenY, int pointer, int button) {
    return false;
  }

  /**
   * Implement for screen clicks or touches. Called when mouse or finger was dragged.
   * @param screenX x-coordinate of the event. Origin is in the upper left corner.
   * @param screenY y-coordinate of the event. Origin is in the upper left corner.
   * @param pointer the pointer for the event.
   * @return boolean whether the input was processed
   */
  @Override
  public boolean touchDragged(int screenX, int screenY, int pointer) {
    return false;
  }

  /**
   * Called when the mouse was moved without any buttons being pressed. Will not be called on iOS.
   * @param screenX x-coordinate of the event. Origin is in the upper left corner.
   * @param screenY y-coordinate of the event. Origin is in the upper left corner.
   * @return boolean whether the input was processed
   */
  @Override
  public boolean mouseMoved(int screenX, int screenY) {
    super.controller.keyListener.mouseMoved(screenX, screenY);
    return false;
  }

  /**
   * Called when the mouse wheel was scrolled.
   * @param amount the amount the wheel was scrolled
   * @return boolean whether the input was processed
   */
  @Override
  public boolean scrolled(int amount) {
    return false;
  }

}
