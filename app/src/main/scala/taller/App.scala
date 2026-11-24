package taller

import common._
import java.util.concurrent.ConcurrentHashMap
import scala.util.Random
/**
 * El punto 5 pide medir aquí, sobre un sistema de al menos un millón de
 * partículas, el tiempo de `pasoSecuencial` y el de `pasoParalelo` con varios
 * umbrales, y reportar la aceleración de cada uno.
 *
 * Lo que hay ahora es el arranque mínimo sobre dos partículas.
 */
object App {

  /** Sistema reproducible: misma semilla, mismas partículas. */
  def sistemaAleatorio(n: Int, semilla: Long): Array[Particula] = {
    val r = new Random(semilla)
    Array.fill(n)(Particula(
      Vector2(r.nextDouble() * 100, r.nextDouble() * 100),
      Vector2(r.nextDouble() * 2 - 1, r.nextDouble() * 2 - 1),
      1.0 + r.nextDouble() * 9))
  }

  def fmt(x: Double): String = "%.2f".formatLocal(java.util.Locale.US, x)

  /**
   * Cuántos hilos distintos ejecutan tareas lanzadas con `parallel`. Si da 1,
   * el reparto corre en un solo hilo y ninguna aceleración sería real.
   */
  def hilosDistintos(n: Int): Int = {
    val vistos = ConcurrentHashMap.newKeySet[String]()
    def sondeo(desde: Int, hasta: Int): Double = {
      vistos.add(Thread.currentThread.getName)
      if (hasta - desde <= 1000) {
        var i = desde
        var acc = 0.0
        while (i < hasta) { acc += math.sqrt(i.toDouble); i += 1 }
        acc
      } else {
        val m = desde + (hasta - desde) / 2
        val (a, b) = parallel(sondeo(desde, m), sondeo(m, hasta))
        a + b
      }
    }
    sondeo(0, n)   // el resultado se devuelve y se suma, así el JIT no lo elimina
    vistos.size
  }

  def main(args: Array[String]): Unit = {
    val sim = new SimulacionParticulas()
    import sim._

    val dt = 0.5
    val semilla = 42L
    val repeticiones = 5
    val p = Runtime.getRuntime.availableProcessors
    val tamanos = List(1000000, 2000000, 4000000)

    println(s"Núcleos disponibles: $p")
    println(s"Hilos distintos que ejecutaron tareas parallel: ${hilosDistintos(50000000)}")
    println(s"Semilla: $semilla, dt: $dt, repeticiones por medida: $repeticiones (mediana)")
    println(s"JVM: ${System.getProperty("java.version")}, Scala: ${scala.util.Properties.versionNumberString}")
    println()

    for (n <- tamanos) {
      val s = sistemaAleatorio(n, semilla)

      // Antes de medir, comprobar que las dos versiones dan lo mismo.
      require(pasoSecuencial(s, dt).sameElements(pasoParalelo(s, dt, 10000)),
        "pasoParalelo y pasoSecuencial difieren")

      val tSec = medir(repeticiones) { pasoSecuencial(s, dt) }

      // El primer umbral, igual a n, es la fila de control: no reparte nada.
      val umbrales = List(n, n / 2, n / 8, n / 64, n / 512, 10000, 1000, 100)
        .filter(_ >= 1).distinct.sorted(Ordering[Int].reverse)

      println(s"### n = $n, secuencial = ${fmt(tSec)} ms")
      println("| Umbral | Paralela (ms) | Aceleración | Aceleración estimada |")
      println("|---:|---:|---:|---:|")
      for (u <- umbrales) {
        val tPar = medir(repeticiones) { pasoParalelo(s, dt, u) }
        val acel = aceleracion(tSec, tPar)
        val est = trabajo(n).toDouble / tiempoEstimado(n, p, u)
        val etiqueta = if (u == n) s"$u (control)" else u.toString
        println(s"| $etiqueta | ${fmt(tPar)} | ${fmt(acel)} | ${fmt(est)} |")
      }
      println()
    }

    // umbralConveniente sobre el último tamaño.
    val n = tamanos.last
    val s = sistemaAleatorio(n, semilla)
    val candidatos = List(n / 8, n / 64, n / 512, 10000, 1000, 100)
    println(s"umbralConveniente para n = $n con $candidatos: ${umbralConveniente(s, dt, candidatos)}")
  }
}
