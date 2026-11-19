# Informe de corrección

## 0. Notación y especificación para los puntos 1 y 2.

Un sistema de $n$ partículas se escribe como una sucesión indexada

$$
s = (p_0, p_1, \ldots, p_{n-1}), \qquad p_i = (\mathrm{pos}_i, \mathrm{vel}_i, m_i),
$$

con $\mathrm{pos}_i, \mathrm{vel}_i \in \mathbb{R}^2$ y $m_i \in \mathbb{R}$. Las
operaciones de `Vector2` son componente a componente:

$$
(x_1, y_1) + (x_2, y_2) = (x_1 + x_2,\; y_1 + y_2), \qquad
(x, y) \cdot k = (kx,\; ky),
$$

y la norma es $\lVert (x, y) \rVert = \sqrt{x^2 + y^2}$.

Un tramo $[d, h)$ denota los índices $\{d, d+1, \ldots, h-1\}$, con
$0 \le d \le h \le n$. Para arreglos se escribe $\lvert a \rvert$ la longitud de
$a$ y $a_j$ su elemento en la posición $j$.

La transformación de movimiento que aplica el código (la auxiliar `mover` y la
expresión inline de `pasoSegmento`) es

$$
\mu_{dt}(p_i) = (\mathrm{pos}_i + \mathrm{vel}_i \, dt,\; \mathrm{vel}_i,\; m_i).
$$

La velocidad y la masa quedan invariantes; solo cambia la posición.

**Especificación del punto 1.** `pasoSecuencial(s, dt)` debe devolver un arreglo
$t$ con $\lvert t \rvert = n$ y $t_i = \mu_{dt}(p_i)$ para todo $0 \le i < n$.
`pasoSegmento(s, dt, d, h)` debe devolver el arreglo
$(\mu_{dt}(p_d), \ldots, \mu_{dt}(p_{h-1}))$, de longitud $h - d$.
`pasoParalelo(s, dt, umbral)` debe devolver lo mismo que `pasoSecuencial` para
todo sistema, todo `dt` y todo umbral.

### 1. Correciones para las funciones del punto 1
## 1.1. Corrección de `pasoSegmento`

Se usa la semántica de `Array.tabulate`: `Array.tabulate(k)(f)` construye un
arreglo $a$ de longitud $k$ tal que $a_j = f(j)$ para $0 \le j < k$.

**Lema 1 (segmento).** Para $0 \le d \le h \le n$, `pasoSegmento(s, dt, d, h)`
devuelve el arreglo $a$ con $\lvert a \rvert = h - d$ y
$a_j = \mu_{dt}(p_{d+j})$.

*Demostración.* El código llama `Array.tabulate(h - d)` con cuerpo
$f(j) = \texttt{Particula}(p.\mathrm{pos} + p.\mathrm{vel} \cdot dt,\; p.\mathrm{vel},\; p.\mathrm{masa})$
donde $p = s(d+j)$. Como $0 \le j < h - d$, se tiene $d \le d + j \le h - 1 < n$,
así que la lectura $s(d+j)$ es válida y devuelve $p_{d+j}$. El cuerpo construye
exactamente $\mu_{dt}(p_{d+j})$: la posición nueva es la anterior más la velocidad
por $dt$, y la velocidad y la masa se copian. Por la semántica de `tabulate`,
$a_j = \mu_{dt}(p_{d+j})$ y $\lvert a \rvert = h - d$. 

Nótese que la aplicación $j \mapsto d + j$ es una biyección entre las posiciones
locales $\{0, \ldots, h-d-1\}$ del arreglo nuevo y los índices globales
$\{d, \ldots, h-1\}$ del tramo: cada partícula del tramo aparece exactamente una
vez, en su orden, y ninguna partícula fuera del tramo se toca.

**Corolario (pasoSecuencial).** `pasoSecuencial(s, dt)` llama
`pasoSegmento(s, dt, 0, n)`. Por el Lema 1 con $d = 0$ y $h = n$, el resultado
cumple $\lvert t \rvert = n$ y $t_i = \mu_{dt}(p_i)$, que es la especificación.
Con $n = 0$, `tabulate(0)` produce el arreglo vacío, que es el resultado correcto
para el sistema vacío. 

## 1.2. Corrección de `pasoParalelo`

El código fija $u = \max(\mathrm{umbral}, 1)$ y define el llamado interno
`avanzar(d, h)`:

- si $h - d \le u$, devuelve `pasoSegmento(s, dt, d, h)`;
- si $h - d > u$, calcula $m = d + \lfloor (h - d)/2 \rfloor$ (la división entera
  de Scala sobre enteros no negativos es el piso), ejecuta
  `parallel(avanzar(d, m), avanzar(m, h))` y devuelve `izq ++ der`.

La raíz es `avanzar(0, n)`.

### 1.2.1 Terminación

**Lema 2 (terminación).** Todo llamado `avanzar(d, h)` termina.

*Demostración.* Por inducción sobre $L = h - d$. Si $L \le u$ es el caso base,
que no recursa. Si $L > u \ge 1$, entonces $L \ge 2$ y
$1 \le \lfloor L/2 \rfloor \le L - 1$, de modo que las longitudes de los hijos,
$L_1 = \lfloor L/2 \rfloor$ y $L_2 = L - \lfloor L/2 \rfloor$, cumplen
$1 \le L_1, L_2 < L$. Por hipótesis de inducción ambos hijos terminan. 

El ajuste $u = \max(\mathrm{umbral}, 1)$ es necesario para este argumento: con
$u = 0$ y $L = 1$ se tendría $\lfloor 1/2 \rfloor = 0$ y el hijo derecho
`avanzar(d, h)` sería idéntico al padre, sin reducción. Para $\mathrm{umbral} \ge 1$
el máximo no cambia nada, así que la semántica pedida por el enunciado se
conserva; para umbrales menores que 1 solo cambia la granularidad, no el
resultado, como se prueba abajo.

### 1.2.2 La partición no pierde ni repite partículas

**Lema 3 (partición disjunta).** En cada paso recursivo, los conjuntos de índices
de los hijos,

$$
I_1 = \{d, \ldots, m-1\}, \qquad I_2 = \{m, \ldots, h-1\},
$$

satisfacen $I_1 \cup I_2 = \{d, \ldots, h-1\}$, $I_1 \cap I_2 = \varnothing$ y
$\lvert I_1 \rvert + \lvert I_2 \rvert = (m - d) + (h - m) = h - d$.

*Demostración.* Es inmediato de $d < m < h$ (Lema 2): todo índice $i$ con
$d \le i < h$ cumple o bien $i < m$ (está solo en $I_1$) o bien $i \ge m$ (está
solo en $I_2$). El punto de corte $m$ pertenece únicamente al tramo derecho
porque los tramos son semiabiertos. 

Aplicando el lema inductivamente desde la raíz, los tramos de las hojas forman
una partición de $\{0, \ldots, n-1\}$: cada índice pertenece a exactamente una
hoja, y cada hoja procesa su índice una sola vez (Lema 1). Por lo tanto el
reparto no pierde ni repite partículas, incluso con longitudes impares: por
ejemplo, con $n = 5$ y $u = 1$ la raíz parte en $[0,2)$ y $[2,5)$, y el tramo de
longitud 3 se parte en $[2,3)$ y $[3,5)$; las hojas son $[0,1)$, $[1,2)$, $[2,3)$,
$[3,4)$ y $[4,5)$, cuya unión disjunta es $\{0,1,2,3,4\}$.

### 1.2.3 La concatenación preserva el orden y es asociativa

**Lema 4 (concatenación).** Para arreglos $a$ y $b$, el arreglo $a \mathbin{++} b$
tiene longitud $\lvert a \rvert + \lvert b \rvert$ y

$$
(a \mathbin{++} b)_j =
\begin{cases}
a_j & \text{si } 0 \le j < \lvert a \rvert, \\
b_{j - \lvert a \rvert} & \text{si } \lvert a \rvert \le j < \lvert a \rvert + \lvert b \rvert.
\end{cases}
$$

**Lema 5 (asociatividad de la concatenación).**
$(a \mathbin{++} b) \mathbin{++} c = a \mathbin{++} (b \mathbin{++} c)$.

*Demostración.* Dos arreglos son iguales si tienen la misma longitud y el mismo
elemento en cada posición. Ambas agrupaciones tienen longitud
$\lvert a \rvert + \lvert b \rvert + \lvert c \rvert$, y aplicando el Lema 4 dos
veces, en ambas la posición $j$ contiene el elemento $j$-ésimo de la sucesión
formada por los elementos de $a$, seguidos de los de $b$, seguidos de los de $c$.
Luego son iguales. 

La asociatividad garantiza que la forma del árbol de recursión (cómo se agrupen
las combinaciones parciales) no altera el resultado final. La concatenación no es
conmutativa, y por eso el código respeta el orden: siempre concatena `izq` antes
que `der`, y los tramos izquierdos contienen los índices menores.

### 1.2.4 Igualdad entre la versión paralela y la secuencial

**Teorema (equivalencia).** Para todo $0 \le d \le h \le n$, `avanzar(d, h)`
devuelve el mismo arreglo que `pasoSegmento(s, dt, d, h)`. En particular,
`pasoParalelo(s, dt, umbral)` devuelve lo mismo que `pasoSecuencial(s, dt)` para
todo sistema, todo `dt` y todo umbral.

*Demostración.* Por inducción fuerte sobre $L = h - d$.

Caso base, $L \le u$: el código llama directamente `pasoSegmento(s, dt, d, h)` y
la igualdad es trivial.

Paso inductivo, $L > u$: sean $m = d + \lfloor L/2 \rfloor$,
$L_1 = m - d$ y $L_2 = h - m$, con $1 \le L_1, L_2 < L$. Por hipótesis de
inducción y el Lema 1,

$$
\mathrm{izq}_j = \mu_{dt}(p_{d+j}) \;\; (0 \le j < L_1), \qquad
\mathrm{der}_k = \mu_{dt}(p_{m+k}) \;\; (0 \le k < L_2).
$$

Por el Lema 4, el resultado $r = \mathrm{izq} \mathbin{++} \mathrm{der}$ tiene
longitud $L_1 + L_2 = L = h - d$ y:

- para $0 \le j < L_1$: $r_j = \mathrm{izq}_j = \mu_{dt}(p_{d+j})$;
- para $L_1 \le j < L$: $r_j = \mathrm{der}_{j - L_1} = \mu_{dt}(p_{m + j - L_1}) = \mu_{dt}(p_{d+j})$,
  porque $m = d + L_1$.

En ambos casos $r_j = \mu_{dt}(p_{d+j})$, que es exactamente el contenido de
`pasoSegmento(s, dt, d, h)` según el Lema 1. Tomando la raíz $d = 0$, $h = n$ se
obtiene la igualdad con `pasoSecuencial`. 

### 1.2.5 Ausencia de interferencia entre tareas

Las dos ramas de `parallel` solo leen el arreglo compartido `s`; cada una escribe
únicamente en arreglos nuevos creados por `Array.tabulate` dentro de su propio
tramo, y `Particula` y `Vector2` son clases inmutables. Como los tramos son
disjuntos (Lema 3) y no hay estado mutable compartido, las ramas no interfieren
entre sí: el resultado es determinista y no depende del orden en que el
planificador ejecute las tareas. Además, ambas versiones hacen el trabajo por
partícula con la misma expresión dentro de `pasoSegmento`, lo que también hace
comparables sus tiempos (el caso de control del punto 5).

## 2. Corrección de las magnitudes globales (punto 2)

### 2.1 Un lema sobre `foldLeft`

`s.foldLeft(z)(f)` evalúa
$f(\ldots f(f(z, p_0), p_1) \ldots, p_{n-1})$. Cuando
$f(\mathrm{acc}, p) = \mathrm{acc} + g(p)$ y $z$ es el neutro de la suma, el
resultado es la suma agrupada por la izquierda:

$$
\big(\cdots((0 + g(p_0)) + g(p_1)) \cdots \big) + g(p_{n-1})
= \sum_{i=0}^{n-1} g(p_i),
$$

donde la última igualdad vale en $\mathbb{R}$ porque la suma es asociativa y $0$
es su neutro. Con $n = 0$ el pliegue devuelve el inicial, que coincide con la
suma vacía. En coma flotante la igualdad es salvo redondeo (sección 5).

### 2.2 `masaTotal`

El código usa $g(p) = m$ y $z = 0.0$. Por el lema,

$$
\texttt{masaTotal}(s) = \sum_{i=0}^{n-1} m_i = M,
$$

que es la definición de masa total del enunciado. Para el sistema vacío devuelve
$0.0$, el neutro, como corresponde a una suma sin términos.

### 2.3 `energiaSecuencial`

El código calcula, por partícula, $v = \lVert \mathrm{vel}_i \rVert$ y acumula
$0.5 \cdot m_i \cdot v \cdot v$. En los reales,

$$
0.5 \, m_i \, v \, v = \tfrac{1}{2} m_i \lVert \mathrm{vel}_i \rVert^2,
$$

que es la energía cinética de la partícula. Por el lema del pliegue con
$g(p_i) = \tfrac{1}{2} m_i \lVert \mathrm{vel}_i \rVert^2$ y $z = 0.0$:

$$
\texttt{energiaSecuencial}(s) = \sum_{i=0}^{n-1} \tfrac{1}{2} m_i \lVert \mathrm{vel}_i \rVert^2 = E,
$$

que es la especificación. Con el sistema vacío devuelve $0.0$. Cada término es
no negativo cuando las masas lo son, así que el acumulador nunca decrece, lo que
da una cota adicional de sanidad sobre el cálculo.

### 2.4 `centroDeMasaSecuencial`

La especificación es

$$
C(s) =
\begin{cases}
\dfrac{1}{M} \displaystyle\sum_{i=0}^{n-1} m_i \, \mathrm{pos}_i & \text{si } M \ne 0, \\[2mm]
(0, 0) & \text{si } M = 0.
\end{cases}
$$

El código primero calcula `m = masaTotal(s)`, que por la sección 4.2 es $M$.

**Caso $M = 0$.** La rama `if (m == 0.0)` devuelve `Vector2(0.0, 0.0)`, que es
exactamente el origen pedido. Este caso cubre el sistema vacío (suma vacía de
masas) y cualquier sistema con masa total cero.

**Caso $M \ne 0$.** El pliegue usa el neutro vectorial $(0,0)$ y la actualización
$\mathrm{acc} + \mathrm{pos}_i \cdot m_i$. Como la suma y el producto escalar de
`Vector2` son componente a componente, el acumulador mantiene el par de sumas

$$
\mathrm{suma} = \left( \sum_{i=0}^{n-1} m_i x_i, \; \sum_{i=0}^{n-1} m_i y_i \right)
= \sum_{i=0}^{n-1} m_i \, \mathrm{pos}_i,
$$

por el mismo lema del pliegue aplicado a cada componente. El paso final
`suma * (1.0 / m)` escala ambas componentes por $1/M$, y en los reales
$z \cdot (1/M) = z / M$, así que

$$
\texttt{centroDeMasaSecuencial}(s) = \frac{1}{M} \sum_{i=0}^{n-1} m_i \, \mathrm{pos}_i = C(s).
$$

La división se hace una sola vez, al final, sobre la suma completa: el código
nunca promedia resultados parciales. 

## 2.4.1 Asociatividad: por qué la suma se puede repartir y el promedio no

El enunciado pide explicar por qué la suma es una operación válida para combinar
resultados parciales y el promedio no lo es directamente.

**La suma sí.** En $\mathbb{R}$, la suma es asociativa
($(a+b)+c = a+(b+c)$), conmutativa y tiene neutro $0$. Por eso, para cualquier
partición del sistema en tramos disjuntos $s = s_1 \cup s_2$,

$$
\sum_{p \in s} g(p) = \sum_{p \in s_1} g(p) + \sum_{p \in s_2} g(p),
$$

sin importar cómo se agrupen los tramos. Lo mismo vale para la suma de
`Vector2`, porque se define componente a componente sobre dos sumas reales, y
para la concatenación de arreglos en el orden de los índices (Lema 5). Estas son
exactamente las operaciones con las que el código combina: `+` en los pliegues
del punto 2 y `++` en `pasoParalelo`.

**El promedio no.** El promedio de promedios pondera igual a cada tramo aunque
tengan distinto número de partículas o distinta masa. Contraejemplo con masas
unitarias y posiciones sobre una recta: sean $s_1 = (0)$ y $s_2 = (4, 8)$. Los
promedios parciales son $0$ y $6$, y su promedio es $3$, pero el promedio del
sistema completo es

$$
\frac{0 + 4 + 8}{3} = 4 \ne 3.
$$

Formalmente, la operación $(\bar{a}, \bar{b}) \mapsto (\bar{a} + \bar{b})/2$ no
es asociativa ni respeta los pesos de los tramos. La forma correcta de repartir
el centro de masa es llevar por cada tramo el par (suma ponderada, masa),

$$
(S_k, M_k) = \left( \sum_{p \in s_k} m \, \mathrm{pos}, \; \sum_{p \in s_k} m \right),
$$

combinar los pares con la suma componente a componente, que sí es asociativa y
tiene neutro $((0,0), 0)$, y dividir una sola vez al final:

$$
C(s_1 \cup s_2) = \frac{S_1 + S_2}{M_1 + M_2}.
$$

`centroDeMasaSecuencial` ya sigue este esquema (acumula $S$ y $M$ por separado y
divide al final), y es lo que permite que su versión paralela del punto 3
combine mitades sin perder información.

**Nota sobre coma flotante.** Los argumentos anteriores son sobre los reales. En
`Double`, la suma no es estrictamente asociativa porque cada operación redondea,
$v \cdot v$ con $v = \sqrt{x^2 + y^2}$ puede diferir de $x^2 + y^2$ en unos pocos
ulp, y multiplicar por $1/M$ puede diferir de dividir entre $M$ en el último
bit. Por eso el enunciado indica que las pruebas comparen con una tolerancia
pequeña en lugar de exigir igualdad exacta: las versiones secuencial y paralela
coinciden matemáticamente y numéricamente dentro de esa tolerancia.


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

## El modelo

Las funciones describen el árbol de llamados de `repartir` sobre $n$ partículas
con $c = \max(\text{umbral}, 1)$. Un tramo de tamaño $k > c$ se parte en
$\lfloor k/2 \rfloor$ a la izquierda y $\lceil k/2 \rceil$ a la derecha, porque
$m - d = \lfloor k/2 \rfloor$. Un tramo $k \le c$ es una hoja y cuesta $k$
operaciones.

- **Trabajo** $W(n)$: las operaciones de todas las hojas.
- **Profundidad** $D(n, c)$: el costo del camino más caro de la raíz a una hoja,
  con 1 por cada partición y el tamaño de la hoja al final.

$$
D(k, c) =
\begin{cases}
k & \text{si } k \le c \\
1 + \max\big(D(\lfloor k/2 \rfloor, c),\; D(\lceil k/2 \rceil, c)\big) & \text{si } k > c
\end{cases}
$$

$$
T_p(n, c) = D(n, c) + \frac{W(n)}{p}
$$

### `trabajo` es correcto

Por el Lema 2, las hojas del árbol forman una partición de $[0, n)$: sus
tamaños suman $n$ para cualquier umbral. Entonces $W(n) = n$, que es lo que
devuelve `trabajo`, y repartir no lo baja.

### `profundidad` es correcta

Se demuestra $\forall k : \text{camino}(k) = D(k, c)$ por inducción fuerte
sobre $k$.

**Caso base:** $k \le c$. `camino(k)` devuelve $k = D(k, c)$.

**Caso de inducción:** $k > c \ge 1$, así que $k \ge 2$ y
$\lfloor k/2 \rfloor \le \lceil k/2 \rceil < k$. Vale la hipótesis de inducción
para los dos.

- Si $k$ es par, $\lfloor k/2 \rfloor = \lceil k/2 \rceil = k/2$ y el máximo de
  un valor consigo mismo es ese valor:

  $$
  \text{camino}(k) = 1 + \text{camino}(k/2)
  \overset{\text{HI}}{=} 1 + D(k/2, c) = D(k, c)
  $$

- Si $k$ es impar, `tramo / 2` $= \lfloor k/2 \rfloor$ y
  `tramo - tramo / 2` $= \lceil k/2 \rceil$:

  $$
  \text{camino}(k) = 1 + \max\big(\text{camino}(\lfloor k/2 \rfloor),\,
  \text{camino}(\lceil k/2 \rceil)\big)
  \overset{\text{HI}}{=} 1 + \max\big(D(\lfloor k/2 \rfloor, c),\,
  D(\lceil k/2 \rceil, c)\big) = D(k, c)
  $$

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
