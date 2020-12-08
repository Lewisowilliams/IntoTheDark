package dev.teamcyan.dungeoncrafter.classes;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.MapProperties;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTile;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.TiledMapTileSet;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;

import java.util.Iterator;
import java.util.Objects;

/**
 * 
 */
public class GMap extends GameElement {
  private GameModel model;
  private TiledMap map;
  private int mapWidth;
  private int mapHeight;
  private int tileWidth;
  private int tileHeight;
  private int mapPixelWidth ;
  private int mapPixelHeight ;
  private OrthogonalTiledMapRenderer mapRenderer;
  private TiledMapTileLayer terrainLayer;
  private TiledMapTileLayer backgroundLayer;
  private TiledMapTileSet tileSet;


/**
 * GMap Constructor
 * 
 * @param model the GameModel which it is part of 
 * 
 * @param mapName the .txf file which it uses as a starting map
 */
  public GMap(GameModel model, String mapName) {
    // load the map
    this.model = model;
    TmxMapLoader loader = new TmxMapLoader();
    map = loader.load(mapName);
    // surface the map properties
    MapProperties prop = map.getProperties();
    this.mapWidth = prop.get("width", Integer.class);
    this.mapHeight = prop.get("height", Integer.class);
    this.tileWidth = prop.get("tilewidth", Integer.class);
    this.tileHeight = prop.get("tileheight", Integer.class);
    this.mapPixelWidth = mapWidth * tileWidth;
    this.mapPixelHeight = mapHeight * tileHeight;
    this.mapRenderer = new OrthogonalTiledMapRenderer(map);
    MapLayers mapLayers = this.map.getLayers();
    this.terrainLayer = (TiledMapTileLayer) mapLayers.get("environment_layer");
    this.backgroundLayer = (TiledMapTileLayer) mapLayers.get("background_layer");
    this.tileSet = map.getTileSets().getTileSet("default_dirt");
  }


///////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////
// Internal methods
//
///////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////

  /**
   * A method of interogating if a tile exists at a specific tPos on a named layer
   * 
   * @param tPos receives the position of the tile as a TilePos already converted
   * @param layer receives the TiledMapLyayer of the layer to search for
   * @return Returns true if tile exists on layer and false if it does not 
   */
  private boolean tileExists(TilePos tPos, TiledMapTileLayer layer) {
    TiledMapTileLayer.Cell tileHere = layer.getCell(tPos.getX(), tPos.getY());
    if(tileHere != null){
      return true;
    }
    return false;
  }
/**
 * 
 * @param tPos
 * @param layer
 * @return
 */
  private boolean tileHasHealth(TilePos tPos, TiledMapTileLayer layer) {
    TiledMapTileLayer.Cell tileHere = layer.getCell(tPos.getX(), tPos.getY());
    if (tileHere.getTile().getProperties().get("block_health") != null) {
      return true;
    }
    return false;
  }

 
  /**
   * Method to easily convert from Pos to a TilePos
   * @param pos global position
   * @return Returns the equivalent TilePos coordinate of the Pos
   */
  private TilePos convertToTilePos(Pos pos) {
    int x = (int)(pos.getX() / tileWidth); 
    int y = (int)(pos.getY() / tileHeight);
    return new TilePos(x,y); 
  }

  /**
   * Destroys (empties) the tile at the specifice position 
   * @param tPos TilePos of the tile to be destroyed
   * @param layer Layer where the Tile can be found
   */
  private void tileDestroy(TilePos tPos, TiledMapTileLayer layer) {
    if (tileExists(tPos, layer)) {
      String cellType = layer.getCell(tPos.getX(), tPos.getY()).getTile().getTextureRegion().getTexture().toString();
      if (Objects.equals(cellType,"sprites/rocks/default_stone.png")) {
        this.model.getPlayer().incrementStone();
      } else if (Objects.equals(cellType,"sprites/precious/default_steel_block.png")) {
        this.model.getPlayer().incrementIron();
      } else if (Objects.equals(cellType,"sprites/precious/default_gold_block.png")) {
        this.model.getPlayer().incrementGold();
      } else {
        this.model.getPlayer().incrementDirt();
      }

      layer.getCell(tPos.getX(), tPos.getY()).setTile(null);
      layer.setCell(tPos.getX(), tPos.getY(), null);
    }
  }

  private boolean tileAttack(TilePos tPos, TiledMapTileLayer layer) {
    int attack = 5;
    if (tileExists(tPos, layer)) {
      if (tileHasHealth(tPos, layer)){
        if(tileGetHealth(tPos, layer) > 0 ) {
          int tHealth = tileGetHealth(tPos, layer);
          tHealth -= attack;
          System.out.println(tHealth);
          tileSetHealth(tPos, layer, tHealth);
        } else {
          tileDestroy(tPos, layer);
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Digs to the left 3 squares
   **/
  public boolean interactBlockRight(Pos pos){
    boolean broken = false;

    TilePos tPos1 = convertToTilePos(pos);
    tPos1.setX(tPos1.getX() + 1);
    tPos1.setY(tPos1.getY());
    broken = broken | tileAttack(tPos1, terrainLayer);

    TilePos tPos2 = convertToTilePos(pos);
    tPos2.setX(tPos2.getX() + 1);
    tPos2.setY(tPos2.getY() + 1);
    broken = broken | tileAttack(tPos2, terrainLayer);

    TilePos tPos3 = convertToTilePos(pos);
    tPos3.setX(tPos3.getX() + 2);
    tPos3.setY(tPos3.getY());
    broken = broken | tileAttack(tPos3, terrainLayer);

    TilePos tPos4 = convertToTilePos(pos);
    tPos4.setX(tPos4.getX() + 2);
    tPos4.setY(tPos4.getY() + 1);
    broken = broken | tileAttack(tPos4, terrainLayer);

    return broken;
  }

  /**
   * Digs to the left 3 squares
   **/
  public boolean interactBlockLeft(Pos pos){
    boolean broken = false;

    TilePos tPos1 = convertToTilePos(pos);
    tPos1.setX(tPos1.getX() - 1);
    tPos1.setY(tPos1.getY());
    broken = broken | tileAttack(tPos1, terrainLayer);

    TilePos tPos2 = convertToTilePos(pos);
    tPos2.setX(tPos2.getX() - 1);
    tPos2.setY(tPos2.getY() + 1);
    broken = broken | tileAttack(tPos2, terrainLayer);

    TilePos tPos3 = convertToTilePos(pos);
    tPos3.setX(tPos3.getX() - 2);
    tPos3.setY(tPos3.getY());
    broken = broken | tileAttack(tPos3, terrainLayer);

    TilePos tPos4 = convertToTilePos(pos);
    tPos4.setX(tPos4.getX() - 2);
    tPos4.setY(tPos4.getY() + 1);
    broken = broken | tileAttack(tPos4, terrainLayer);

    return broken;
  }

  /**
   * Digs to the centre 3 squares
   **/
  public boolean interactBlockUp(Pos pos){
    boolean broken = false;

    TilePos tPos1 = convertToTilePos(pos);
    tPos1.setX(tPos1.getX() + 1);
    tPos1.setY(tPos1.getY() + 1);
    broken = broken | tileAttack(tPos1, terrainLayer);

    TilePos tPos2 = convertToTilePos(pos);
    tPos2.setX(tPos2.getX() );
    tPos2.setY(tPos2.getY() + 1);
    broken = broken |tileAttack(tPos2, terrainLayer);

    TilePos tPos3 = convertToTilePos(pos);
    tPos3.setX(tPos3.getX() - 1);
    tPos3.setY(tPos3.getY() + 1);
    broken = broken |tileAttack(tPos3, terrainLayer);

    TilePos tPos4 = convertToTilePos(pos);
    tPos4.setX(tPos4.getX() + 1);
    tPos4.setY(tPos4.getY() + 2);
    broken = broken | tileAttack(tPos4, terrainLayer);

    TilePos tPos5 = convertToTilePos(pos);
    tPos5.setX(tPos5.getX());
    tPos5.setY(tPos5.getY() + 2);
    broken = broken | tileAttack(tPos5, terrainLayer);

    TilePos tPos6 = convertToTilePos(pos);
    tPos6.setX(tPos6.getX() - 1);
    tPos6.setY(tPos6.getY() + 2);
    broken = broken | tileAttack(tPos6, terrainLayer);

    return broken;
  }

  /**
   * Digs to the centre 3 squares
   **/
  public boolean interactBlockCentre(Pos pos){
    boolean broken = false;

    TilePos tPos1 = convertToTilePos(pos);
    tPos1.setX(tPos1.getX());
    tPos1.setY(tPos1.getY() - 1);
    broken = broken | tileAttack(tPos1, terrainLayer);

    TilePos tPos2 = convertToTilePos(pos);
    tPos2.setX(tPos2.getX());
    tPos2.setY(tPos2.getY());
    broken = broken |tileAttack(tPos2, terrainLayer);

    TilePos tPos3 = convertToTilePos(pos);
    tPos3.setX(tPos3.getX());
    tPos3.setY(tPos3.getY() + 1);
    broken = broken |tileAttack(tPos3, terrainLayer);

    TilePos tPos4 = convertToTilePos(pos);
    tPos4.setX(tPos4.getX() - 1);
    tPos4.setY(tPos4.getY() - 1);
    broken = broken | tileAttack(tPos4, terrainLayer);

    TilePos tPos5 = convertToTilePos(pos);
    tPos5.setX(tPos5.getX() + 1);
    tPos5.setY(tPos5.getY() - 1);
    broken = broken | tileAttack(tPos5, terrainLayer);

    return broken;
  }

  public void setBlockLeft(Pos pos){
    TilePos tPos = convertToTilePos(pos);
    TiledMapTileLayer.Cell cell = new TiledMapTileLayer.Cell();
    if (model.getPlayer().getCurrentCraftingBlock() == GEPlayer.BLOCK.STONE) {
      cell.setTile(map.getTileSets().getTileSet("default_stone").getTile(113));
    } else {
      cell.setTile(tileSet.getTile(81));
    }
    terrainLayer.setCell(tPos.getX()-1, tPos.getY(), cell);
  }

  public void setBlockRight(Pos pos){
    TilePos tPos = convertToTilePos(pos);
    TiledMapTileLayer.Cell cell = new TiledMapTileLayer.Cell();
    if (model.getPlayer().getCurrentCraftingBlock() == GEPlayer.BLOCK.STONE) {
      cell.setTile(map.getTileSets().getTileSet("default_stone").getTile(113));
    } else {
      cell.setTile(map.getTileSets().getTileSet("default_dirt").getTile(81));
    }
    terrainLayer.setCell(tPos.getX()+1, tPos.getY(), cell);
  }

///////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////
// Get and Set methods
//
///////////////////////////////////////////////////////////////////////////////
///////////////////////////////////////////////////////////////////////////////

  public TiledMapTileLayer.Cell getBlock(Pos pos) {
    TilePos tPos = convertToTilePos(pos);
    return terrainLayer.getCell(tPos.getX(), tPos.getY());
  }

  public TiledMapTile getBackgroundTile(Pos pos){
    TilePos tPos = convertToTilePos(pos);
    return backgroundLayer.getCell(tPos.getX(), tPos.getY()).getTile();
  }

  public TiledMapTileLayer getTerainLayer(){
    return this.terrainLayer;
  }

  public TiledMap getTiledMap() {
    return this.map;
  }

  /**
   * Returns an int of the tileMap HP
   **/
  private int tileGetHealth(TilePos tPos, TiledMapTileLayer layer) {
    int health = (int)terrainLayer.getCell(tPos.getX(), tPos.getY()).getTile().getProperties().get("block_health");
    return health;
  }

  private void tileSetHealth(TilePos tPos, TiledMapTileLayer layer, int value) {
    terrainLayer.getCell(tPos.getX(), tPos.getY()).getTile().getProperties().put("block_health", value);
    return;
  }

  public OrthogonalTiledMapRenderer getMapRenderer() {
    return this.mapRenderer;
  }

  public int getMapPixelWidth() {
    return mapPixelWidth;
  }

  public int getMapPixelHeight() {
    return mapPixelHeight;
  }

}
