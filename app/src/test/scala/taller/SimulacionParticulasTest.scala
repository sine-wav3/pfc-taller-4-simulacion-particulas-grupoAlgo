package taller

import org.scalatest.funsuite.AnyFunSuite
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import scala.util.Random

/**
 * Las pruebas no miden tiempos: un tiempo depende de la máquina y de lo que
 * esté corriendo al lado, así que una prueba que lo compare fallaría sin que
 * nada esté mal.
 */
@RunWith(classOf[JUnitRunner])
class SimulacionParticulasTest extends AnyFunSuite {

  val sim = new SimulacionParticulas()
  import sim._

  val eps = 1e-9

  val p = Particula(Vector2(0, 0), Vector2(1, 2), 1.0)

  val s: Sistema = Array(
    Particula(Vector2(0, 0), Vector2(3, 4), 2.0), // norma 5
    Particula(Vector2(4, 0), Vector2(0, 0), 2.0))

  val vacio: Sistema = Array()

  val azar = new Random(7)
  val grande: Sistema = Array.fill(1000)(Particula(
    Vector2(azar.nextDouble() * 10, azar.nextDouble() * 10),
    Vector2(azar.nextDouble() * 2, azar.nextDouble() * 2),
    azar.nextDouble() * 5 + 0.1))

  def cerca(a: Double, b: Double): Boolean = Math.abs(a - b) < 1e-6

  def cerca(a: Vector2, b: Vector2): Boolean =
    Math.abs(a.x - b.x) < 1e-6 && Math.abs(a.y - b.y) < 1e-6

  // Punto 1 -------------------------------------------------------------------

  test("pasoSecuencial: la posición avanza velocidad por dt") {
    assert(pasoSecuencial(Array(p), 2.0).head.pos == Vector2(2.0, 4.0))
  }

  test("pasoSecuencial: con dt cero nada se mueve") {
    assert(pasoSecuencial(Array(p), 0.0).head.pos == Vector2(0.0, 0.0))
  }

  test("pasoSecuencial: la velocidad y la masa no cambian") {
    val despues = pasoSecuencial(Array(p), 3.0).head
    assert(despues.vel == p.vel && despues.masa == p.masa)
  }

  test("pasoSecuencial: sobre el sistema vacío no hay nada que mover") {
    assert(pasoSecuencial(vacio, 1.0).isEmpty)
  }

  test("pasoSegmento: solo calcula el tramo pedido") {
    val tramo = pasoSegmento(s, 1.0, 1, 2)
    assert(tramo.length == 1 && tramo.head.pos == Vector2(4.0, 0.0))
  }

  test("pasoSegmento: el tramo vacío da un sistema vacío") {
    assert(pasoSegmento(s, 1.0, 1, 1).isEmpty)
  }

  test("pasoParalelo: da lo mismo que el secuencial en un caso simple") {
    assert(pasoParalelo(Array(p), 2.0, 1).head.pos == Vector2(2.0, 4.0))
  }

  test("pasoParalelo: coincide con el secuencial para varios umbrales") {
    val umbrales = List(1, 2, 7, 64, 1000, 5000)
    assert(umbrales.forall { u =>
      pasoParalelo(grande, 0.5, u).zip(pasoSecuencial(grande, 0.5))
        .forall { case (a, b) => cerca(a.pos, b.pos) }
    })
  }

  test("pasoParalelo: conserva el orden y el tamaño del sistema") {
    assert(pasoParalelo(grande, 0.5, 32).length == grande.length)
  }

  test("pasoParalelo: con umbral cero no se queda sin fondo") {
    assert(pasoParalelo(Array(p), 1.0, 0).length == 1)
  }

  // Punto 2 -------------------------------------------------------------------

  test("energiaSecuencial: media masa por velocidad al cuadrado") {
    assert(cerca(energiaSecuencial(s), 25.0))
  }

  test("energiaSecuencial: el sistema vacío no tiene energía") {
    assert(energiaSecuencial(vacio) == 0.0)
  }

  test("energiaSecuencial: una partícula quieta no aporta energía") {
    assert(cerca(energiaSecuencial(Array(s(1))), 0.0))
  }

  test("masaTotal: suma las masas") {
    assert(cerca(masaTotal(s), 4.0))
  }

  test("masaTotal: el sistema vacío no tiene masa") {
    assert(masaTotal(vacio) == 0.0)
  }

  test("centroDeMasaSecuencial: el punto medio de dos masas iguales") {
    assert(cerca(centroDeMasaSecuencial(s), Vector2(2.0, 0.0)))
  }

  test("centroDeMasaSecuencial: sin masa el centro es el origen") {
    assert(centroDeMasaSecuencial(vacio) == Vector2(0, 0))
  }

  test("centroDeMasaSecuencial: la masa mayor arrastra el centro") {
    val pesado: Sistema = Array(
      Particula(Vector2(0, 0), Vector2(0, 0), 3.0),
      Particula(Vector2(4, 0), Vector2(0, 0), 1.0))
    assert(cerca(centroDeMasaSecuencial(pesado), Vector2(1.0, 0.0)))
  }

  // Punto 3 -------------------------------------------------------------------

  test("energiaParalela: coincide con la secuencial en el ejemplo") {
    assert(cerca(energiaParalela(s, 1), 25.0))
  }

  test("energiaParalela: coincide con la secuencial para varios umbrales") {
    val esperado = energiaSecuencial(grande)
    assert(List(1, 3, 64, 999, 5000).forall(u => cerca(energiaParalela(grande, u), esperado)))
  }

  test("energiaParalela: sobre el sistema vacío da cero") {
    assert(energiaParalela(vacio, 1) == 0.0)
  }

  test("centroDeMasaParalelo: coincide con el secuencial en el ejemplo") {
    assert(cerca(centroDeMasaParalelo(s, 1), Vector2(2.0, 0.0)))
  }

  test("centroDeMasaParalelo: coincide con el secuencial para varios umbrales") {
    val esperado = centroDeMasaSecuencial(grande)
    assert(List(1, 3, 64, 999, 5000).forall(u => cerca(centroDeMasaParalelo(grande, u), esperado)))
  }

  test("centroDeMasaParalelo: promediar promedios daría otra cosa") {
    // Tres partículas: dos en el origen y una lejos. El promedio de los
    // promedios de las dos mitades no es el centro de masa del conjunto.
    val tres: Sistema = Array(
      Particula(Vector2(0, 0), Vector2(0, 0), 1.0),
      Particula(Vector2(0, 0), Vector2(0, 0), 1.0),
      Particula(Vector2(9, 0), Vector2(0, 0), 1.0))
    assert(cerca(centroDeMasaParalelo(tres, 1), Vector2(3.0, 0.0)))
  }

  test("centroDeMasaParalelo: sin masa el centro es el origen") {
    assert(centroDeMasaParalelo(vacio, 1) == Vector2(0, 0))
  }

  // Punto 3: pruebas propias -------------------------------------------------

  // Cinco partículas, número impar para que las mitades no sean iguales.
  // Energías: 1 + 2 + 50 + 0 + 6 = 59.
  val cinco: Sistema = Array(
    Particula(Vector2(0, 0), Vector2(1, 0), 2.0),
    Particula(Vector2(1, 1), Vector2(0, -2), 1.0),
    Particula(Vector2(2, 0), Vector2(-3, 4), 4.0),
    Particula(Vector2(5, 5), Vector2(0, 0), 10.0),
    Particula(Vector2(-1, 3), Vector2(1, 1), 6.0))

  test("energiaParalela (propia): sistema impar calculado a mano, con cualquier umbral") {
    assert(List(1, 2, 3, 4, 5, 100).forall(u => cerca(energiaParalela(cinco, u), 59.0)))
  }

  test("energiaParalela (propia): con umbral cero o negativo no se queda sin fondo") {
    assert(cerca(energiaParalela(cinco, 0), 59.0) && cerca(energiaParalela(cinco, -4), 59.0))
  }

  test("energiaParalela (propia): la energía de dos sistemas juntos es la suma de cada uno") {
    val juntos = energiaParalela(grande ++ cinco, 7)
    assert(cerca(juntos, energiaParalela(grande, 7) + 59.0))
  }

  test("energiaParalela (propia): el orden de las partículas no cambia el resultado") {
    assert(cerca(energiaParalela(grande.reverse, 13), energiaParalela(grande, 13)))
  }

  test("energiaParalela (propia): triplicar las velocidades multiplica la energía por nueve") {
    val rapido: Sistema = grande.map(q => q.copy(vel = q.vel * 3.0))
    assert(cerca(energiaParalela(rapido, 50), 9.0 * energiaParalela(grande, 50)))
  }

  test("centroDeMasaParalelo (propia): masas distintas calculadas a mano, con cualquier umbral") {
    // Masas 1, 2, 3, 4. Suma ponderada (20, 12), masa 10: centro (2.0, 1.2).
    // Con umbral 1 promediar los promedios de las mitades daría (1.62, 0.86).
    val cuatro: Sistema = Array(
      Particula(Vector2(0, 0), Vector2(0, 0), 1.0),
      Particula(Vector2(1, 0), Vector2(0, 0), 2.0),
      Particula(Vector2(2, 4), Vector2(0, 0), 3.0),
      Particula(Vector2(3, 0), Vector2(0, 0), 4.0))
    assert(List(1, 2, 3, 4).forall(u => cerca(centroDeMasaParalelo(cuatro, u), Vector2(2.0, 1.2))))
  }

  test("centroDeMasaParalelo (propia): sin masa en ninguna mitad da el origen y no NaN") {
    val sinMasa: Sistema = Array.tabulate(6)(i => Particula(Vector2(i, -i), Vector2(1, 1), 0.0))
    val centro = centroDeMasaParalelo(sinMasa, 1)
    assert(centro == Vector2(0, 0) && !centro.x.isNaN && !centro.y.isNaN)
  }

  test("centroDeMasaParalelo (propia): trasladar el sistema traslada el centro") {
    val d = Vector2(100, -50)
    val movido: Sistema = grande.map(q => q.copy(pos = q.pos + d))
    assert(cerca(centroDeMasaParalelo(movido, 37), centroDeMasaParalelo(grande, 37) + d))
  }

  test("centroDeMasaParalelo (propia): una sola partícula con umbral cero es su propia posición") {
    val una: Sistema = Array(Particula(Vector2(-3.5, 7.25), Vector2(2, 2), 0.3))
    assert(cerca(centroDeMasaParalelo(una, 0), Vector2(-3.5, 7.25)))
  }

  test("centroDeMasaParalelo (propia): una partícula sin masa no mueve el centro") {
    val conFantasma: Sistema = s ++ Array(Particula(Vector2(1000, 1000), Vector2(0, 0), 0.0))
    assert(cerca(centroDeMasaParalelo(conFantasma, 1), Vector2(2.0, 0.0)))
  }

  // Punto 4 -------------------------------------------------------------------

  test("trabajo: una operación por partícula") { assert(trabajo(1024) == 1024) }

  test("profundidad: si el umbral cubre todo, no se parte") {
    assert(profundidad(1024, 1024) == 1024)
  }

  test("profundidad: una partición y el tramo que queda") {
    assert(profundidad(1024, 512) == 513)
  }

  test("profundidad: diez particiones y un elemento") {
    assert(profundidad(1024, 1) == 11)
  }

  test("tiempoEstimado: profundidad más trabajo entre procesadores") {
    assert(cerca(tiempoEstimado(1024, 4, 1), 267.0))
  }

  test("tiempoEstimado: con un procesador no hay ganancia") {
    assert(cerca(tiempoEstimado(1024, 1, 1), 11.0 + 1024.0))
  }

  test("profundidad: nunca supera el trabajo") {
    val casos = for (n <- List(1, 7, 64, 1024); u <- List(1, 8, 64)) yield (n, u)
    assert(casos.forall { case (n, u) => profundidad(n, u) <= trabajo(n) })
  }

  // Punto 4: pruebas propias -------------------------------------------------

  test("trabajo (propia): es lineal y no depende de cómo se reparta") {
    assert(trabajo(1000000) == 1000000 && trabajo(0) == 0 &&
      trabajo(1000) + trabajo(24) == trabajo(1024))
  }

  test("profundidad (propia): con n que no es potencia de dos") {
    // 1000 -> 500 -> 250 -> 125 -> 63: cuatro particiones y 63 de tramo.
    assert(profundidad(1000, 100) == 67)
  }

  test("profundidad (propia): con mitades desiguales manda la más costosa") {
    // 11 se parte en 5 y 6. La de 5 ya no se parte y cuesta 5; la de 6 se
    // parte otra vez y cuesta 1 + 3 = 4. El camino crítico es 1 + 5 = 6.
    assert(profundidad(11, 5) == 6 && profundidad(7, 1) == 4 && profundidad(9, 2) == 5)
  }

  test("profundidad (propia): un millón de partículas con umbral 1") {
    // 2^20 >= 1 000 000: veinte particiones y un elemento.
    assert(profundidad(1000000, 1) == 21)
  }

  test("profundidad (propia): un umbral mayor que el sistema no parte nada") {
    assert(profundidad(300, 5000) == 300 && profundidad(0, 8) == 0)
  }

  test("profundidad (propia): umbral cero o negativo se comporta como umbral 1") {
    assert(profundidad(1024, 0) == 11 && profundidad(1024, -3) == 11)
  }

  test("profundidad (propia): repartir más fino acorta el camino crítico") {
    val umbrales = List(1000000, 250000, 1000, 1)
    val ds = umbrales.map(u => profundidad(1000000, u))
    assert(ds == List(1000000, 250002, 987, 21))
  }

  test("tiempoEstimado (propia): el trabajo se divide en punto flotante") {
    assert(cerca(tiempoEstimado(1000, 3, 100), 67.0 + 1000.0 / 3.0))
  }

  test("tiempoEstimado (propia): más procesadores nunca empeoran la estimación") {
    val ts = List(1, 2, 4, 8, 16).map(p => tiempoEstimado(1000000, p, 1000))
    assert(ts.zip(ts.tail).forall { case (a, b) => b < a })
  }

  test("tiempoEstimado (propia): sin repartir cuesta más que el trabajo secuencial") {
    val t = tiempoEstimado(1000000, 8, 1000000)
    assert(cerca(t, 1125000.0) && t > trabajo(1000000))
  }

  // Punto 5 -------------------------------------------------------------------

  test("aceleracion: cuatro veces más rápido da 4.0") {
    assert(cerca(aceleracion(100.0, 25.0), 4.0))
  }

  test("aceleracion: el mismo tiempo da 1.0") {
    assert(cerca(aceleracion(100.0, 100.0), 1.0))
  }

  test("umbralConveniente: sin candidatos devuelve el tamaño del sistema") {
    assert(umbralConveniente(grande, 0.5, Nil) == grande.length)
  }

  test("umbralConveniente: devuelve uno de los candidatos") {
    val candidatos = List(16, 128, 1024)
    assert(candidatos.contains(umbralConveniente(grande, 0.5, candidatos)))
  }
}
