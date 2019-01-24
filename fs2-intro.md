---
title: Introduction to FS2
author: Padhu Ramalingam
theme: beige
highlightTheme: atom-one-light
revealOptions:
  transition: slide
---


### fs2 - Functional Streams for Scala


##### History

https://fs2.io/
 - v1.0.0  Oct  5, 2018

 (previously scalaz-stream)
 - v0.8.5 Oct 23, 2016 
 - v0.1   Oct 10, 2013 

---

### Bio


Note:
- Senior developer for Citi
- Facinated by FP 

---

## Dependencies
  - <!-- .element: class="fragment" data-fragment-index="1"--> Cats Effect 
> <!-- .element: class="fragment" data-fragment-index="1" -->The IO Monad for Scala
  - <!-- .element: class="fragment" data-fragment-index="2" --> Cats
> <!-- .element: class="fragment" data-fragment-index="2" -->Lightweight, modular, and extensible library for functional programming

---

### REPL setup
[ammonite](http://ammonite.io/#Ammonite-REPL)
```sh
>java -jar ~/tools/ammonite/amm-2.12-1.6.2.jar

Welcome to the Ammonite Repl 1.6.2
(Scala 2.12.8 Java 1.8.0_181)
If you like Ammonite, please support our development at www.patreon.com/lihaoyi

@ interp.load.ivy("co.fs2" %% "fs2-core" % "1.0.2")

@ "hello, world!"
// res1: String = "hello, world!"
```
---

## Pure

**Referential transparency**:
> Where one can replace an expression for its value, without changing the result

Note:

----

## Side Effects

```scala
val expr = 123
// expr: Int = 123

(expr, expr)
// res1: (Int, Int) = (123, 123)

val expr = println("Hey!")
// Hey!

(expr, expr)
// res3: (Unit, Unit) = ((), ())
```
<!-- .element: class="fragment" --> **Not pure**
----

## Side effects in concurrency

```scala
import scala.io.StdIn
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import cats.syntax.apply._   // for mapN
import cats.instances.all._  // implicits

val read : Future[Int] = Future(StdIn.readInt)
// does not work as Future is eager and undeterministic
val result : Future[Int] = (read, read).mapN(_ + _)
```

<!-- .element: class="fragment" --> **Not pure**

---

## cats-effect 

**`IO[A]`** <!-- .element: class="fragment" -->

- <!-- .element: class="fragment" --> Produces one value, fails or never terminates
- <!-- .element: class="fragment" --> *Referentially transparent* (pure)
- <!-- .element: class="fragment" --> Compositional
- <!-- .element: class="fragment" --> Many algebras (monad,...)

----

## Managing effects
#### using IO

```scala 
import cats.effect.IO

IO(println("Hey!"))
// res13: IO[Unit] = Delay(...)
res13.unsafeRunSync
// Hey!

val n = IO.pure(10)
// n: IO[Int] = Pure(10)

n.unsafeRunSync
// res4: Int = 10

``` 

<!-- .element: class="fragment" --> **Pure**

----

## Managing effects
#### IO encapsulates the effects to maintain RT

```scala
def putStrLn(line: String): IO[Unit] =  IO {println(line)}
def f(a: IO[Unit], b: IO[Unit]): Unit = { 
  a.unsafeRunSync; b.unsafeRunSync
}

f(putStrLn("hi!"), putStrLn("hi!"))
// hi!
// hi!

val x = putStrLn("hi!")
f(x, x)
// hi!
// hi!
```
<!-- .element: class="fragment" -->


---
## IO Monad 

```scala

 // trait Monad[F[_]] extends Functor[F] {
  def pure[A](a: => A): F[A] = ???
  def flatMap[A,B](ma: F[A])(f: A => F[B]): F[B] = ???

import cats.effect.IO

val t = IO.pure(42)
// t: IO[Int] = Pure(42)

t.map {i => i + 1}
// res4: IO[Int] = Map(Pure(42), ...)

res4.unsafeRunSync
// res5: Int = 43


```
Note:
- pure and flatmap are the primitive combinators.
- map is a derived combinator.
- IO Monad have other methods to support concurrency, cancel, etc.
- we'll see concurrency next.

---
### for comprehension
```scala
List(1, 2, 3).flatMap(n => List("a", "b").map(c => (n, c)))
// res10: List[(Int, String)] = List((1, "a"), (1, "b"), (2, "a"), (2, "b"), (3, "a"), (3, "b"))

for {
   n <- List(1, 2, 3)
   c <- List("a", "b")
 } yield (n, c)
// res11: List[(Int, String)] = List((1, "a"), (1, "b"), (2, "a"), (2, "b"), (3, "a"), (3, "b"))
```
Notes:
  - for comprehension is syntactic sugar to manage flatMap and maps

---
## IO Application
Pure hello world
```scala

  def putStrlLn(value: String) = IO(println(value))
  val readLn = IO(scala.io.StdIn.readLine)

  val hello = for {
    _ <- putStrlLn("What's your name?")
    n <- readLn
    _ <- putStrlLn(s"Hello, $n!")
  } yield ()

  hello.unsafeRunSync()
```
---
## IO Monad - concurrency 

```scala
import cats.effect.IO
import cats.effect.ContextShift
implicit val ioContextShift: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.Implicits.global)

val t = IO { println(s"Computing on ${Thread.currentThread.getName}..."); System.currentTimeMillis}
t.unsafeRunSync
// Computing on main...
// res12: Long = 1548213244359L

val t = IO.delay { println(s"Computing on ${Thread.currentThread.getName}..."); System.currentTimeMillis}
t.unsafeRunSync
// Computing on main...
// res14: Long = 1548213268629L

t.start
//res7: IO[cats.effect.Fiber[IO, Long]] = Async(...

val fiber = res7.unsafeRunSync
// Computing on scala-execution-context-global-33...
// fiber: cats.effect.Fiber[IO, Long] = Tuple(
  Bind(Pure(Right(1548252366654L)),.. 

fiber.join.unsafeRunSync
// res9: Long = 1548252366654L

```

Notes: 
- ContextShift implicit is essntial for concurrency
- Displaying the thread name to validate
----

## Stream
##### (scala.collection)
```scala
Stream(1, 2, 3)
// res1: Stream[Int] = Stream(1, 2, 3)

Stream(1, 2, 3).map(_ + 1)
// res2: Stream[Int] = Stream(2, 3, 4)

Stream(1, 2, 3).map(_ + 1).map(_.toString)
// res3: Stream[String] = Stream("2", "3", "4")
```
Notes:
- Refresher collection Stream before IO Streams

---

## fs2 Streams

**`Stream[F[_], A]`** <!-- .element: class="fragment" -->

-  <!-- .element: class="fragment" --> emits `0...n ` values of type `A`, where `n` can be ∞ 
-  <!-- .element: class="fragment" --> While requesting effects in `F`
-  <!-- .element: class="fragment" --> `F` is normally `IO`

Note:
- also pure, compositional, possesses algebras

----
## FS2 Stream
```scala
import fs2._

Stream(1, 2, 3)
// res2: Stream[Nothing, Int] = Stream(..)

Stream(1, 2, 3).map(_ + 1)
// res3: Stream[Nothing, Int] = Stream(..)

Stream(1, 2, 3).map(_ + 1).map(_.toString)
// res4: Stream[Nothing, String] = Stream(..)

Stream(1, 2, 3).flatMap { n => Stream.emit(List.fill(n)(n))}
// res5: Stream[Nothing, List[Int]] = Stream(..)
```
---
## Pure Stream
Extracting pure values
```scala
val s = Stream(1, 2, 3)
// s: Stream[Nothing, Int] = Stream(..)
val t = Stream(4, 5, 6)
// t: Stream[Nothing, Int] = Stream(..)
val u = s interleave t
// u: Stream[Nothing, Int] = Stream(..)
u.toList
// res9: List[Int] = List(1, 4, 2, 5, 3, 6)

val s = Stream.range(0, 10)
// s: Stream[Nothing, Int] = Stream(..)
val t = s.intersperse(-42)
// t: Stream[Nothing, Int] = Stream(..)
t.toList
// res12: List[Int] = List(0, -42, 1, -42, 2, -42, 3, -42, 4, -42, 5, -42, 6, -42, 7, -42, 8, -42, 9)

val s = Stream.range(0, 10)
// s: Stream[Nothing, Int] = Stream(..)
val t = Stream(1, 2, 3)
// t: Stream[Nothing, Int] = Stream(..)
s.zip(t3).toList
// res15: List[(Int, Int)] = List((0, 1), (1, 2), (2, 3))
```

---
## IO Stream
```scala
interp.load.ivy("co.fs2" %% "fs2-core" % "1.0.2")
interp.load.ivy("co.fs2" %% "fs2-io" % "1.0.2")

import cats.effect.IO
import cats.effect.ContextShift
import fs2._

implicit val ioContextShift: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.Implicits.global)
// ioContextShift: ContextShift[IO] = cats.effect.internals.IOContextShift ...

val currentTime: IO[Long] = IO.delay {System.currentTimeMillis}
// currentTime: IO[Long] = Delay(...)

Stream.eval(currentTime)
// res7: Stream[IO, Long] = Stream(..)

Stream.eval(currentTime).repeat.take(5).compile.toVector
// res8: IO[Vector[Long]] = Map( ...

res8.unsafeRunSync
// res9: Vector[Long] = Vector(1548292261803L, 1548292261848L, 1548292261848L, 1548292261848L, 1548292261848L)
```
Notes:
Stream.eval(currentTime).repeat.take(5).compile.toVector
is simplified to
Stream.repeatEval(currentTime).take(5).compile.toVector

---
## IO Stream
Combining streams
```scala
Stream.repeatEval(currentTime).take(5).compile.toVector
// res10: IO[Vector[Long]] = Map(...

val s = Stream.range(0,8)
// s: Stream[Nothing, Int] = Stream(..)


s.zip(Stream.repeatEval(currentTime))
// res11: Stream[IO, (Int, Long)] = Stream(..)

@ res11.compile.toVector.unsafeRunSync
res12: Vector[(Int, Long)] = Vector(
  (0, 1548293081488L),
  (1, 1548293081490L),
  (2, 1548293081490L),
  (3, 1548293081490L),
  (4, 1548293081490L),
  (5, 1548293081490L),
  (6, 1548293081491L),
  (7, 1548293081491L)
)
```
---
## IO Stream
File IO
```scala
interp.load.ivy("co.fs2" %% "fs2-core" % "1.0.2")
interp.load.ivy("co.fs2" %% "fs2-io" % "1.0.2")

import cats.effect.{ContextShift, IO, Timer}
import fs2._
import java.nio.file.Paths
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

implicit val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.Implicits.global)
// cs: ContextShift[IO] = cats.effect.internals.IOContextShift@14e34e4

implicit val timer: Timer[IO] = IO.timer(scala.concurrent.ExecutionContext.Implicits.global)
// timer: Timer[IO] = cats.effect.internals.IOTimer@a75b05

val blockingExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))
// blockingExecutionContext: scala.concurrent.ExecutionContextExecutorService = scala.concurrent.impl.ExecutionContextImpl$$anon$4@4a2090

val src = io.file.readAll[IO](java.nio.file.Paths.get("fahrenheit.txt"), blockingExecutionContext, 16)
// src: Stream[IO, Byte] = Stream(..)

src.compile.toVector.unsafeRunSync
//res7: Vector[Byte] = Vector(
//  47,
//  47,
//  32,
//  116,
...
```
---
## IO Stream
transformers
```scala
text.utf8Decode
// res8: Stream[Nothing, Byte] => Stream[Nothing, String] = fs2.text$$$Lambda$1929/18862762@a84ea8

src.through(text.utf8Decode)
// res9: Stream[IO[x], String] = Stream(..)

res9.compile.toVector.unsafeRunSync
// res10: Vector[String] = Vector(
//  "// this file con",
//  "tains a list of ",
//  "temperatures in ",
//  "degrees fahrenhe",
//  """it
// 18.0
// 17.9
// 25""",
```
---
## IO Stream
Stream processing
```scala
def fahrenheitToCelsius(f: Double): Double = (f - 32.0) * (5.0/9.0)
// defined function fahrenheitToCelsius

src.through(text.utf8Decode).through(text.lines).map(_.toDouble).map(fahrenheitToCelsius)
// res12: Stream[IO[x], Double] = Stream(..)

res12.compile.toVector.unsafeRunSync
// java.lang.NumberFormatException: For input string: "// this file contains a list of temperatures in degrees fahrenheit"
//  sun.misc.FloatingDecimal.readJavaFormatString(Unknown Source)
//  sun.misc.FloatingDecimal.parseDouble(Unknown Source)
```
Notes:
- note the execption

---
## IO Stream
Stream process flow
```scala
val decoded: Stream[IO, String] = src.through(text.utf8Decode)
// decoded: Stream[IO, String] = Stream(..)

val lines: Stream[IO, String] = decoded.through(text.lines)
// lines: Stream[IO, String] = Stream(..)

val filtered: Stream[IO, String] = lines.filter(s => !s.trim.isEmpty && !s.startsWith("//"))
// filtered: Stream[IO, String] = Stream(..)

val mapped: Stream[IO, String] = filtered.map(line => fahrenheitToCelsius(line.toDouble).toString)
// mapped: Stream[IO, String] = Stream(..)

val withNewlines: Stream[IO, String] = mapped.intersperse("\n")
// withNewlines: Stream[IO, String] = Stream(..)

val encodedBytes: Stream[IO, Byte] = withNewlines.through(text.utf8Encode)
// encodedBytes: Stream[IO, Byte] = Stream(..)

val written: Stream[IO, Unit] = encodedBytes.through(io.file.writeAll(Paths.get("celsius2.txt"), blockingExecutionContext))
// written: Stream[IO, Unit] = Stream(..)

val task: IO[Unit] = written.compile.drain
// task: IO[Unit] = Map(

task.unsafeRunSync()
blockingExecutionContext.shutdown()
```
---
# Questions?

- ??? 

