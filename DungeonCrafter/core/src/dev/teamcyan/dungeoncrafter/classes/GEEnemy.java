package dev.teamcyan.dungeoncrafter.classes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.utils.Array;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Game Element Enemy Class
 */
public class GEEnemy extends GameElement{
    public static final float ACCELERATION = (float) 2.0;
    Velocity velocity;
    private TextureRegion region;
    private Animation<TextureRegion> enemyShootR;
    private Animation<TextureRegion> enemyShootL;
    private Animation<TextureRegion> enemyWalkL;
    private Animation<TextureRegion> enemyWalkR;
    private Animation<TextureRegion> enemyDeath;
    private Texture enemySpriteSheet;
    public float stateTimer = 0;
    public float projectileTimer = 0;
    public State currentState;
    public State previousState;
    public List<GEProjectile> projectiles;
    public GameModel model;
    private int health = 100;
    private boolean isAlive = true;

    /**
     * Constructor for Enemy Game Element
     * @param model
     * @param position
     */
    public GEEnemy (GameModel model, Pos position)
    {
        this.model = model;
        this.getype = GEType.PLAYER;
        this.velocity = new Velocity(0,0);
        this.position = position;
        this.currentState = State.STANDING;
        this.previousState = State.STANDING;
        this.projectiles = new ArrayList<GEProjectile>();
        this.enemySpriteSheet = new Texture("sprites/enemy/enemyArcher.png");

        Array<TextureRegion> frames = new Array<TextureRegion>();
        for (int i = 0; i < 13; i++) {
            frames.add(new TextureRegion(enemySpriteSheet, i*64, 1097, CHAR_PIXEL_WIDTH, CHAR_PIXEL_HEIGHT-12));
        }
        this.enemyShootL = new Animation(0.25f, frames);
        frames.clear();

        for (int i = 0; i < 13; i++) {
            frames.add(new TextureRegion(enemySpriteSheet, i*64, 1225, CHAR_PIXEL_WIDTH, CHAR_PIXEL_HEIGHT-12));
        }
        this.enemyShootR = new Animation(0.25f, frames);
        frames.clear();

        for (int i = 0; i < 9; i++) {
            frames.add(new TextureRegion(enemySpriteSheet, i*64, 587, CHAR_PIXEL_WIDTH, CHAR_PIXEL_HEIGHT-12));
        }
        this.enemyWalkL = new Animation(0.15f, frames);
        frames.clear();

        for (int i = 0; i < 9; i++) {
            frames.add(new TextureRegion(enemySpriteSheet, i*64, 713, CHAR_PIXEL_WIDTH, CHAR_PIXEL_HEIGHT-12));
        }
        this.enemyWalkR = new Animation(0.15f, frames);
        frames.clear();

        for (int i = 0; i < 6; i++) {
            frames.add(new TextureRegion(this.enemySpriteSheet, i*64, 1289, CHAR_PIXEL_WIDTH, CHAR_PIXEL_HEIGHT-12));
        }
        this.enemyDeath = new Animation(0.4f, frames);
        frames.clear();
    }

    /**
     * Setter for X
     * @param layer
     * @param playerPosition
     * @return
     */
    public float setX(TiledMapTileLayer layer, Pos playerPosition) {
        // apply gravity, when no floor
        float delta = Gdx.graphics.getDeltaTime();
        float newXVelocity;

        double distance = this.getDistance(playerPosition);// = Math.sqrt(Math.pow((playerPosition.getX() - this.position.getX()), 2) + Math.pow((playerPosition.getY() - this.position.getY()), 2));
        if (this.velocity.getY() > 1) {
            newXVelocity = this.velocity.getX();
        } else if (distance > 300 && distance < 1000) {
            if (playerPosition.getX() > this.position.getX()) {
                float newV = this.velocity.getX() + this.ACCELERATION * delta;
                newXVelocity = newV > layer.getTileWidth() ? this.velocity.getX() : newV;
            } else {
                float newV = this.velocity.getX() - this.ACCELERATION * delta;
                newXVelocity = newV < (-1)*layer.getTileWidth() ? this.velocity.getX() : newV;
            }
        } else {
            newXVelocity = this.velocity.getX() * this.RESISTANCE;
            newXVelocity = newXVelocity > -0.000000001 && newXVelocity < 0.000000001 ? 0 : newXVelocity;
        }

        double newXPosition = this.position.getX() + newXVelocity;

        if(newXVelocity > 0) {
            TiledMapTileLayer.Cell topRight = layer.getCell((int) ((newXPosition + this.region.getRegionWidth()) / layer.getTileWidth()), (int) Math.floor((this.position.getY() + this.region.getRegionHeight()) / layer.getTileHeight()));
            TiledMapTileLayer.Cell bottomRight = layer.getCell((int)((newXPosition + this.region.getRegionWidth()) / layer.getTileWidth()), (int) Math.ceil(this.position.getY() / layer.getTileHeight()));
            if (bottomRight == null && topRight == null) {
                this.velocity.setX(newXVelocity);
                this.position.setX((int)Math.ceil(newXPosition));
            } else {
                this.velocity.setY(4f);
                this.velocity.setX((float)0.0);
            }

        } else if (newXVelocity < 0) {
            TiledMapTileLayer.Cell topLeft = layer.getCell((int) Math.floor(newXPosition / layer.getTileWidth()), (int) Math.floor((this.position.getY() + this.region.getRegionHeight()) / layer.getTileHeight()));
            TiledMapTileLayer.Cell bottomLeft = layer.getCell((int) Math.floor(newXPosition / layer.getTileWidth()), (int) Math.ceil(this.position.getY() / layer.getTileHeight()));
            if (bottomLeft == null && topLeft == null) {
                this.velocity.setX(newXVelocity);
                this.position.setX((int)Math.floor(newXPosition));
            } else {
                this.velocity.setY(4f);
                this.velocity.setX(0);
            }
        }
        return this.position.getX();
    }

    /**
     * Setter for Y
     * @param layer
     * @return
     */
    public float setY(TiledMapTileLayer layer) {
        TiledMapTile currentBackgroundTile = model.getMap().getBackgroundTile(new Pos(this.position.getX()+this.region.getRegionWidth()/2, this.position.getY()+this.region.getRegionHeight()/2));
        float gravity;
        if (currentBackgroundTile.getProperties().get("inverseGravity") != null) {
            gravity = this.GRAVITY * (-1);
        } else {
            gravity = this.GRAVITY;
        }
        // apply gravity, when no floor
        float delta = Gdx.graphics.getDeltaTime();

        float newYVelocity = this.velocity.getY() - delta * gravity;

        float newYPosition = this.position.getY() + newYVelocity;
        TiledMapTileLayer.Cell leftBottom = layer.getCell((int) Math.ceil(this.position.getX() / layer.getTileWidth()), (int) Math.floor(newYPosition / layer.getTileHeight()));
        TiledMapTileLayer.Cell rightBottom = layer.getCell((int) Math.floor((this.position.getX()+this.region.getRegionWidth()-10) / layer.getTileWidth()), (int) Math.floor(newYPosition / layer.getTileHeight()));
        TiledMapTileLayer.Cell leftTop = layer.getCell((int) Math.ceil(this.position.getX() / layer.getTileWidth()), (int) Math.floor((newYPosition+this.getRegion().getRegionHeight()) / layer.getTileHeight()));
        TiledMapTileLayer.Cell rightTop = layer.getCell((int) Math.floor((this.position.getX()+this.region.getRegionWidth()-10) / layer.getTileWidth()), (int) Math.floor((newYPosition+this.getRegion().getRegionHeight()) / layer.getTileHeight()));
        if ((newYVelocity <= 0 && leftBottom == null && rightBottom == null) || (newYVelocity > 0 && leftTop == null && rightTop == null)) {
            this.velocity.setY(newYVelocity);
            this.position.setY((int)newYPosition);

        } else {
            this.velocity.setY(0);
        }
        return this.position.getY();
    }

    /**
     * Setter for Region
     */
    public void setRegion() {
        if (this.velocity.getY() >1) {
            this.currentState = GameElement.State.JUMPING;
        } else if(this.velocity.getY() < -1) {
            this.currentState = GameElement.State.FALLING;
        } else if (this.velocity.getX() < -1) {
            this.currentState = GameElement.State.RUNNINGL;
        } else if (this.velocity.getX() > 1) {
            this.currentState = GameElement.State.RUNNINGR;
        } else if (this.previousState == State.RUNNINGL || this.currentState == State.ATTACKL){
            this.currentState = GameElement.State.ATTACKL;
        } else if (this.previousState == State.RUNNINGR || this.currentState == State.ATTACKR){
            this.currentState = GameElement.State.ATTACKR;
        } else{
            this.currentState = State.STANDING;
        }

        stateTimer += Gdx.graphics.getDeltaTime();

        if (this.health <= 0){
            region = enemyDeath.getKeyFrame(stateTimer, true);
        } else if (this.currentState == State.RUNNINGL ){
            region =enemyWalkL.getKeyFrame(stateTimer, true);;
        } else if (this.currentState == State.RUNNINGR){
            region = enemyWalkR.getKeyFrame(stateTimer, true);
        } else if (this.currentState == State.ATTACKL){
            region = enemyShootL.getKeyFrame(stateTimer, true);
        } else if (this.currentState == State.ATTACKR){
            region = enemyShootR.getKeyFrame(stateTimer, true);
        } else {
            region = enemyShootR.getKeyFrame(stateTimer,true);
        }

        previousState = currentState;
    }

    /**
     * Getter for Texture Region
     * @return
     */
    public TextureRegion getRegion() {
        return region;
    }

    /**
     * Getter for tiles 
     * @param layer
     * @param playerPosition
     * @return
     */
    public List<GEProjectile> getProjectiles(TiledMapTileLayer layer, Pos playerPosition) {
        this.projectileTimer += Gdx.graphics.getDeltaTime();
        if (this.projectileTimer > 3) {
            projectiles.add(new GEProjectile(this.model, this.position, playerPosition));
            this.projectileTimer = 0;
        }

        List<GEProjectile> projectilesToRemove = new ArrayList<GEProjectile>();
        for (GEProjectile proj : this.projectiles) {

            if (!proj.setPosition(layer)) {
                projectilesToRemove.add(proj);
            }
        }
        this.projectiles.removeAll(projectilesToRemove);

        return this.projectiles;
    }

    /**
     * Getter for health
     * @return
     */
    public int getHealth() {
        return this.health;
    }

    /**
     * Setter for decrementing Health
     * @param damage
     */
    public void decrementHealth(int damage) {
        this.health -= damage;
        if (this.health <= 0) {
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    isAlive = false;
                }
            }, 1000);
        }
    }

    /**
     * Checker for isAlive Flag
     * @return
     */
    public boolean isAlive() {
        return this.isAlive;
    }

    /**
     * Getter for Distance 
     * @param pos
     * @return
     */
    protected double getDistance(Pos pos) {
        return Math.sqrt(Math.pow(pos.getX()-this.position.getX(), 2)+Math.pow(pos.getY()-this.position.getY(), 2));
    }
}