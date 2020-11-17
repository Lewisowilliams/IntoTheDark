package dev.teamcyan.dungeoncrafter.classes;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

public class GEPebble extends GameElement
{

    Velocity velocity;
    private Sprite sprite;

    public GEPebble (TextureAtlas.AtlasRegion spriteRegion)
    {
        this.getype = GEType.PLAYER;
        this.velocity = new Velocity(0,0);
        this.sprite = new Sprite(spriteRegion);

    }
    public Sprite getSprite() {
        return sprite;
    }

    public float setX(TiledMapTileLayer layer, Pos playerPosition) {
        // apply gravity, when no floor
        float delta = Gdx.graphics.getDeltaTime();
        float newXVelocity;

        double distance = Math.sqrt(Math.pow((playerPosition.getX() - this.sprite.getX()), 2) + Math.pow((playerPosition.getY() - this.sprite.getY()), 2));
        if (this.velocity.getY() > 1) {
            newXVelocity = this.velocity.getX();
        } else if (distance > 80) {
            if (playerPosition.getX() > this.sprite.getX()) {
                newXVelocity = this.velocity.getX() + this.ACCELERATION * delta;
            } else {
                newXVelocity = this.velocity.getX() - this.ACCELERATION * delta;
            }
        } else {
            newXVelocity = this.velocity.getX() * this.RESISTANCE;
            newXVelocity = newXVelocity > -0.000000001 && newXVelocity < 0.000000001 ? 0 : newXVelocity;
        }

        double newXPosition = this.sprite.getX() + newXVelocity;

        if(newXVelocity > 0) {
            TiledMapTileLayer.Cell topRight = layer.getCell((int) ((newXPosition + this.sprite.getWidth()) / layer.getTileWidth()), (int) Math.floor((this.sprite.getY() + this.sprite.getHeight()) / layer.getTileHeight()));
            TiledMapTileLayer.Cell bottomRight = layer.getCell((int)((newXPosition + this.sprite.getWidth()) / layer.getTileWidth()), (int) Math.ceil(this.sprite.getY() / layer.getTileHeight()));
            if (bottomRight == null && topRight == null) {
                this.velocity.setX(newXVelocity);
                this.sprite.setX((int)Math.ceil(newXPosition));
            } else {
                this.velocity.setX((float)0.0);
            }

        } else if (newXVelocity < 0) {
            TiledMapTileLayer.Cell topLeft = layer.getCell((int) Math.floor(newXPosition / layer.getTileWidth()), (int) Math.floor((this.sprite.getY() + this.sprite.getHeight()) / layer.getTileHeight()));
            TiledMapTileLayer.Cell bottomLeft = layer.getCell((int) Math.floor(newXPosition / layer.getTileWidth()), (int) Math.ceil(this.sprite.getY() / layer.getTileHeight()));
            if (bottomLeft == null && topLeft == null) {
                this.velocity.setX(newXVelocity);
                this.sprite.setX((int)Math.floor(newXPosition));
            } else {
                this.sprite.setX((float)0.0);
            }
        }
        return this.sprite.getX();
    }

    public float setY(TiledMapTileLayer layer) {

        // apply gravity, when no floor
        float delta = Gdx.graphics.getDeltaTime();

        float newYVelocity = this.velocity.getY() + delta * this.GRAVITY;

        double newYPosition = this.sprite.getY() - Math.floor(newYVelocity);
        TiledMapTileLayer.Cell leftBottom = layer.getCell((int) Math.floor(this.sprite.getX() / layer.getTileWidth()), (int) Math.floor(newYPosition / layer.getTileHeight()));
        TiledMapTileLayer.Cell rightBottom = layer.getCell((int) Math.floor((this.sprite.getX()+this.sprite.getWidth()-1) / layer.getTileWidth()), (int) Math.floor(newYPosition / layer.getTileHeight()));
        if (leftBottom == null && rightBottom == null) {
            this.velocity.setY(newYVelocity);
            this.sprite.setY((int)newYPosition);

        } else {
            this.velocity.setY(0);
        }
        return this.sprite.getY();
    }


}