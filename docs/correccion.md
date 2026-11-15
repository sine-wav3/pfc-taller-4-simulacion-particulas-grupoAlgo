# Informe de corrección

## Punto 3: las magnitudes globales en paralelo

### Especificación

Sea $s = (p_0, p_1, \ldots, p_{n-1})$ un sistema, con $p_i$ de masa $m_i$,
posición $x_i \in \mathbb{R}^2$ y velocidad $v_i \in \mathbb{R}^2$. Para un
tramo $[d, h)$, con $0 \le d \le h \le n$, se definen:

```math
e_i = \tfrac{1}{2}\, m_i \, \lVert v_i \rVert^2
\qquad
E[d, h) = \sum_{i=d}^{h-1} e_i
\qquad
S[d, h) = \sum_{i=d}^{h-1} m_i\, x_i
\qquad
M[d, h) = \sum_{i=d}^{h-1} m_i
```

Lo que piden las funciones es:

```math
\text{energía}(s) = E[0, n)
\qquad
\text{CM}(s) =
\begin{cases}
\dfrac{S[0, n)}{M[0, n)} & \text{si } M[0, n) \neq 0 \\[2mm]
(0, 0) & \text{si } M[0, n) = 0
\end{cases}
```

En los dos programas, $c = \max(\text{umbral}, 1) \ge 1$.

### Lema 1: el tramo secuencial

`energiaTramo(d, h)` es un proceso iterativo:

- Estado $(i, acc)$. Estado inicial $s_0 = (d, 0)$.
- Es final si $i = h$.
- $\text{transformar}(i, acc) = (i + 1,\ acc + e_i)$.
- Invariante: $\text{Inv}(i, acc) \equiv d \le i \le h \land acc = E[d, i)$.

**1.** $\text{Inv}(s_0)$: $d \le d \le h$ y $0 = E[d, d)$, la suma vacía.

**2.** Si $i < h$ e $\text{Inv}(i, acc)$, entonces
$acc + e_i = E[d, i) + e_i = E[d, i+1)$ y $i + 1 \le h$; vale
$\text{Inv}(i+1, acc + e_i)$.

**3.** En el estado final $i = h$, así que $acc = E[d, h)$.

**4.** $h - i$ es un natural que baja en 1 en cada paso, así que se llega al
estado final después de $h - d$ pasos.

Luego $\text{energiaTramo}(d, h) = E[d, h)$. Con el mismo argumento, cambiando
el estado por $(i, suma, masa)$ y la invariante por
$suma = S[d, i) \land masa = M[d, i)$, se tiene
$\text{resumenTramo}(d, h) = (S[d, h), M[d, h))$.

### Lema 2: partir no pierde ni repite partículas

Si $h - d > c \ge 1$, entonces $h - d \ge 2$, y con
$m = d + \lfloor (h - d) / 2 \rfloor$:

```math
1 \le \left\lfloor \tfrac{h-d}{2} \right\rfloor < h - d
\;\Longrightarrow\;
d < m < h
```

```math
[d, m) \cup [m, h) = [d, h)
\qquad
[d, m) \cap [m, h) = \emptyset
```

Todo índice $i$ con $d \le i < h$ cumple exactamente una de $i < m$ o
$i \ge m$, así que queda en exactamente una de las dos mitades: ninguna
partícula se pierde y ninguna se cuenta dos veces. Como las dos mitades son
no vacías, las dos son estrictamente más cortas que $[d, h)$. Por la misma
razón, para toda $f$ que se sume sobre índices:

```math
\sum_{i=d}^{h-1} f(i) = \sum_{i=d}^{m-1} f(i) + \sum_{i=m}^{h-1} f(i)
```

### Teorema: `energiaParalela` da la energía del sistema

Se demuestra $\forall\, d \le h : \text{repartir}(d, h) = E[d, h)$ por
inducción fuerte sobre $k = h - d$.

**Caso base:** $k \le c$.

```math
\text{repartir}(d, h) \rightarrow \text{energiaTramo}(d, h) = E[d, h)
\quad \text{(Lema 1)}
```

**Caso de inducción:** $k > c$. Por el Lema 2, $d < m < h$ y los dos tramos
son más cortos que $k$, así que vale la hipótesis de inducción para los dos.
`parallel(a, b)` devuelve el par $(a, b)$: cambia el orden en que se evalúan,
no los valores.

```math
\text{repartir}(d, h) \rightarrow \text{izquierda} + \text{derecha}
\overset{HI}{=} E[d, m) + E[m, h)
\overset{\text{Lema 2}}{=} E[d, h)
```

Entonces $\text{energiaParalela}(s, u) = \text{repartir}(0, n) = E[0, n)$ para
todo umbral $u$, que es lo mismo que calcula `energiaSecuencial`. El umbral
solo decide la forma del árbol, no el valor.

### Teorema: `centroDeMasaParalelo` da el centro de masa

Sea $\oplus$ la operación con que se combinan las dos mitades:

```math
(S_1, M_1) \oplus (S_2, M_2) = (S_1 + S_2,\; M_1 + M_2)
```

Con el mismo esquema de inducción, usando el Lema 1 para `resumenTramo` en el
caso base y el Lema 2 en cada componente:

```math
\text{repartir}(d, h) = (S[d, m), M[d, m)) \oplus (S[m, h), M[m, h)) = (S[d, h), M[d, h))
```

Al final, sobre $(S, M) = (S[0, n), M[0, n))$, el programa devuelve
$(0, 0)$ si $M = 0$ y $(S_x / M,\ S_y / M)$ si no, que es $\text{CM}(s)$.

### Por qué la suma sirve para repartir y el promedio no

**La operación que combina es asociativa.** La suma de reales es asociativa y
tiene neutro 0. $\oplus$ es la suma componente a componente en
$\mathbb{R}^2 \times \mathbb{R}$, y hereda las dos propiedades:

```math
\big((S_1, M_1) \oplus (S_2, M_2)\big) \oplus (S_3, M_3)
= (S_1 + S_2 + S_3,\; M_1 + M_2 + M_3)
= (S_1, M_1) \oplus \big((S_2, M_2) \oplus (S_3, M_3)\big)
```

```math
(S, M) \oplus ((0, 0), 0) = (S, M)
```

Por eso cualquier árbol de particiones, sea cual sea el umbral, agrupa los
mismos sumandos de otra forma y llega al mismo resultado.

**El promedio no es asociativo.** Si dos resultados parciales se combinaran
promediándolos, $a \oslash b = \tfrac{a + b}{2}$:

```math
(a \oslash b) \oslash c = \tfrac{a}{4} + \tfrac{b}{4} + \tfrac{c}{2}
\;\neq\;
\tfrac{a}{2} + \tfrac{b}{4} + \tfrac{c}{4} = a \oslash (b \oslash c)
```

El resultado dependería de cómo se partió el sistema. Además el promedio de
dos promedios da el mismo peso a las dos mitades, cuando el centro de masa
debe pesar cada mitad por su masa:

```math
\text{CM}[d, h) = \frac{S[d, m) + S[m, h)}{M[d, m) + M[m, h)}
\;\neq\;
\frac{1}{2}\left(\frac{S[d, m)}{M[d, m)} + \frac{S[m, h)}{M[m, h)}\right)
\quad \text{en general}
```

Contraejemplo: dos partículas de masa 1 en el origen y una de masa 1 en
$(9, 0)$, con umbral 1. La partición queda en $[0, 1)$ y $[1, 3)$. Los centros
de las mitades son $(0, 0)$ y $(4{,}5;\ 0)$, cuyo promedio es
$(2{,}25;\ 0)$, pero el centro del sistema es $(9/3,\ 0) = (3, 0)$. Llevando
los pares: $((0,0), 1) \oplus ((9,0), 2) = ((9, 0), 3)$ y
$(9/3,\ 0) = (3, 0)$.

La división no es asociativa ni tiene sentido sobre un tramo sin masa. Por eso
se saca del árbol y se aplica una sola vez, al final. Así una mitad sin masa
nunca divide entre cero.

**Punto flotante.** Todo lo anterior vale en $\mathbb{R}$. En `Double`, la suma
redondea, y $(a + b) + c$ puede diferir de $a + (b + c)$ en las últimas cifras.
Por eso las pruebas comparan la versión paralela con la secuencial con una
tolerancia de $10^{-6}$ y no con igualdad exacta.

## Punto 4: trabajo y profundidad

### El modelo

Las funciones describen el árbol de llamados de `repartir` sobre $n$
partículas con $c = \max(\text{umbral}, 1)$. Un tramo de tamaño $k > c$ se
parte en $\lfloor k/2 \rfloor$ a la izquierda y $\lceil k/2 \rceil$ a la
derecha, porque $m - d = \lfloor k/2 \rfloor$. Un tramo $k \le c$ es una hoja
y cuesta $k$ operaciones.

- **Trabajo** $W(n)$: las operaciones de todas las hojas.
- **Profundidad** $D(n, c)$: el costo del camino más caro de la raíz a una
  hoja, con 1 por cada partición y el tamaño de la hoja al final.

```math
D(k, c) =
\begin{cases}
k & \text{si } k \le c \\
1 + \max\big(D(\lfloor k/2 \rfloor, c),\; D(\lceil k/2 \rceil, c)\big) & \text{si } k > c
\end{cases}
```

```math
T_p(n, c) = D(n, c) + \frac{W(n)}{p}
```

### `trabajo` es correcto

Por el Lema 2, las hojas del árbol forman una partición de $[0, n)$: sus
tamaños suman $n$ para cualquier umbral. Entonces $W(n) = n$, que es lo que
devuelve `trabajo`, y repartir no lo baja.

### `profundidad` es correcta

Se demuestra $\forall k : \text{camino}(k) = D(k, c)$ por inducción fuerte
sobre $k$.

**Caso base:** $k \le c$. `camino(k)` devuelve $k = D(k, c)$.

**Caso de inducción:** $k > c \ge 1$, así que $k \ge 2$ y
$\lfloor k/2 \rfloor \le \lceil k/2 \rceil < k$. Vale la hipótesis de
inducción para los dos.

- Si $k$ es par, $\lfloor k/2 \rfloor = \lceil k/2 \rceil = k/2$ y el máximo
  de un valor consigo mismo es ese valor:
  ```math
  \text{camino}(k) \rightarrow 1 + \text{camino}(k/2) \overset{HI}{=} 1 + D(k/2, c) = D(k, c)
  ```
- Si $k$ es impar, `tramo / 2` $= \lfloor k/2 \rfloor$ y
  `tramo - tramo / 2` $= \lceil k/2 \rceil$:
  ```math
  \text{camino}(k) \rightarrow 1 + \max(\text{camino}(\lfloor k/2 \rfloor), \text{camino}(\lceil k/2 \rceil))
  \overset{HI}{=} D(k, c)
  ```

**Terminación:** cada llamado recibe un tramo estrictamente menor que $k$, y
los tramos son naturales.

**Por qué el máximo y no solo la mitad mayor.** $D$ no crece con $k$: con
$c = 5$, $D(5, 5) = 5$ pero $D(6, 5) = 1 + D(3, 5) = 4$. Por eso, con
$k = 11$:

```math
D(11, 5) = 1 + \max(D(5, 5),\, D(6, 5)) = 1 + \max(5, 4) = 6
```

y seguir solo la mitad mayor daría $1 + 4 = 5$, menos que el camino crítico.

**Forma cerrada.** Si $n = 2^k$ y $c = 2^j$ con $j \le k$, todas las mitades
son iguales y se parte $k - j$ veces hasta llegar a tramos de $2^j$:

```math
D(2^k, 2^j) = (k - j) + 2^j
```

Por inducción sobre $k - j$: si $k = j$, $D = 2^j$; si $k > j$,
$D(2^k, 2^j) = 1 + D(2^{k-1}, 2^j) = 1 + (k - 1 - j) + 2^j$. Los ejemplos del
enunciado salen de aquí: $D(2^{10}, 2^{10}) = 1024$,
$D(2^{10}, 2^9) = 1 + 512 = 513$ y $D(2^{10}, 2^0) = 10 + 1 = 11$.

**La profundidad nunca supera el trabajo.** $D(k, c) \le k$, por inducción
fuerte: si $k \le c$, $D = k$; si $k > c$, $k \ge 2$ y

```math
D(k, c) \overset{HI}{\le} 1 + \lceil k/2 \rceil \le k
```

### `tiempoEstimado` es correcto

`tiempoEstimado(n, p, umbral)` devuelve
`profundidad(n, umbral) + trabajo(n).toDouble / p`, que por lo anterior es
$D(n, c) + n/p = T_p(n, c)$. La conversión a `Double` antes de dividir hace
que $1000 / 3$ dé $333{,}\overline{3}$ y no $333$. Como $D$ no depende de $p$
y $n/p$ baja cuando $p$ sube, la estimación nunca empeora al agregar
procesadores.
