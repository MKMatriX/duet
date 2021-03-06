package example
import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.Math
import org.scalajs.dom
import org.scalajs.dom.html
import scala.util.Random


class Block(val y: Double, val dr: Double, val dx: Double, val center: Int, val renderer: dom.CanvasRenderingContext2D, var x: Int) {
  val width = 5;
  val height = 20;
  val speed = 3; // default speed
  var r = 0.0; // rotation
  //val color = "darkblue";
    def centerCoords(): (Double, Double) = {
      (x+width/2.0, getY()+height/2.0)
    }

    def getY() = center + (y * height)

    def draw(frame: Int): Unit = {
      x -= speed + dx.toInt
      r += dr
      renderer.save()
      renderer.translate( x+width/2, getY()+height/2 );
      renderer.rotate(r*Math.PI/180);
      //renderer.fillRect(x, getY(), width, height)
      renderer.fillRect( -width/2, -height/2, width,height);
      renderer.restore()
      //renderer.rotate(0);
      //renderer.translate(0, 0);
    }

    def intersect(c: (Double, Double, Double)): Boolean = {
      val (cx,cy,cr) = c;
      val y = getY();
      cx >= x && cx <= x+width && cy >= y && cy <= y+height
    }
}

class Player(var r: Double, val center: Int, val renderer: dom.CanvasRenderingContext2D) {
  val playerCirclesGap = 35;
  val radious = 5.0;
  var dead = 0;
  var down = false;
  var up = false;

  def move(dr: Double): Unit = {
    r += dr * math.Pi / 180
    r %= math.Pi*2
  }

  def coords(): ((Double, Double),(Double, Double)) = {
    val cosR = math.cos(r) * playerCirclesGap;
    val sinR = math.sin(r) * playerCirclesGap;

    (center + cosR, center + sinR) -> (center - cosR, center - sinR)
  }

  def fullCoords(i: Int) : (Double, Double, Double) = {
    val cosR = math.cos(r) * playerCirclesGap;
    val sinR = math.sin(r) * playerCirclesGap;
    if (i == 1)
      (center + cosR, center + sinR, radious)
    else 
      (center - cosR, center - sinR, radious)
  }

  def draw(): Unit = {
    if (down ^ up) {
      if (down) move(-9.0)
      if (up) move(9.0)
    }
    val ((fx, fy), (sx,sy)) = coords();
    renderer.fillStyle = "gray"
    renderer.beginPath();
    renderer.arc(center, center, playerCirclesGap, 0, math.Pi*2, true); 
    renderer.stroke();
    renderer.closePath();

    renderer.fillStyle = "blue"
    renderer.beginPath();
    renderer.arc(fx, fy, radious, 0, math.Pi*2, true); 
    renderer.fill();
    renderer.closePath();

    renderer.fillStyle = "red"
    renderer.beginPath();
    renderer.arc(sx, sy, radious, 0, math.Pi*2, true); 
    renderer.fill();
    renderer.closePath();
  }
}


@JSExport
object ScalaJSExample {
  @JSExport
  def main(canvas: html.Canvas): Unit = {
    //setup
    val renderer = canvas.getContext("2d").asInstanceOf[dom.CanvasRenderingContext2D]

    canvas.width = canvas.parentElement.clientWidth
    canvas.height = 400


    //variables
    val center = (canvas.height / 2);
    val rightBorder = canvas.width
    val player = new Player(0, center, renderer);
    var obstacleGap = 100 // Gap between the approaching obstacles

    // Whether the player is dead or not;
    // 0 means alive, >0 is number of frames before respawning
    var dead = 0
    // What frame this is; used to keep track
    // of where the obstacles should be positioned
    var frame = -25
    // List of each obstacle, storing only the Y position of the hole.
    // The X position of the obstacle is calculated by its position in the
    // queue and in the current frame.
    val obstacles = collection.mutable.ArrayBuffer[Block]()
    var score = 0

    def runLive() = {
      frame += 1

      // render score
      renderer.fillStyle = "black"
      renderer.font = "20px sans-serif"
      renderer.fillText(s"Score: $score", 100, 20)

      // Create new obstacles, or kill old ones as necessary
      val deadObstacles = obstacles filter (_.x<=0)
      score += deadObstacles.length
      obstacles --= deadObstacles
      if (frame >= 0 && frame % obstacleGap == 0)
        obstacles += new Block(Random.nextInt(5)-2, Random.nextInt(5)-2, Random.nextInt(5)-2, center, renderer, rightBorder)

      // add some hurtcore
      if (frame >= 0 && frame % 1000 == 0)
        obstacleGap -= 1
      if (obstacleGap == 0)
        dead = 50

      // Render obstacles, and check for collision
      renderer.fillStyle = "darkblue"
      obstacles.foreach(_.draw(frame))
      if (obstacles.map(_.intersect(player.fullCoords(1))).contains(true) || 
        obstacles.map(_.intersect(player.fullCoords(2))).contains(true))
        dead = 50;

      // Render player
      player.draw();
    }

    def runDead() = {
      score = 0
      frame = -50
      obstacles.clear
      dead -= 1
      renderer.fillStyle = "darkred"
      renderer.font = "50px sans-serif"
      renderer.textAlign = "center"
      renderer.textBaseline = "middle"
      renderer.fillText("Game Over", canvas.width / 2, canvas.height / 2)
    }

    def run() = {
      renderer.clearRect(0, 0, canvas.width, canvas.height)
      if (dead > 0) runDead()
      else runLive()
    }

    dom.window.setInterval(run _, 20)

    dom.window.onkeydown = (e: dom.KeyboardEvent) => {
      e.keyCode match {
        case 1094 | 119 | 83 | 40 =>
          player.down = true;
        case 1099 | 115 | 87 | 38 =>
          player.up = true;
        case _ => 
          println(s"nothing: ${e.keyCode}")
      }
    }
    dom.window.onkeyup = (e: dom.KeyboardEvent) => {
      e.keyCode match {
        case 1094 | 119 | 83 | 40 =>
          player.down = false;
        case 1099 | 115 | 87 | 38 =>
          player.up = false;
        case _ => 
          println(s"nothing: ${e.keyCode}")
      }
    }
  }
}
