---
title: Getting Started with FS2
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
```
துன்பம் உறவரினும் செய்க துணிவாற்றி
இன்பம் பயக்கும் வினை.
```
___
```txt
Though it should cause increasing sorrow (at the outset), do with
firmness the act that yield bliss (in the end).

Thirukkural #669 Chapter 67.
```

Note:
- Senior developer for Citi
- Facinated by FP 

---

### Dependencies
  - <!-- .element: class="fragment" data-fragment-index="1"--> Cats Effect 
> <!-- .element: class="fragment" data-fragment-index="1" -->The IO Monad for Scala
  - <!-- .element: class="fragment" data-fragment-index="2" --> Cats
> <!-- .element: class="fragment" data-fragment-index="2" -->Lightweight, modular, and extensible library for functional programming

----

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

### Pure

**Referential transparency**:
> Where one can replace an expression for its value, without changing the result

Note:

----

### Side Effects

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

### Side effects in concurrency

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

## Cats Effect 

**`IO[A]`** <!-- .element: class="fragment" -->

- <!-- .element: class="fragment" --> Produces one value, fails or never terminates
- <!-- .element: class="fragment" --> *Referentially transparent* (pure)
- <!-- .element: class="fragment" --> Compositional
- <!-- .element: class="fragment" --> Many algebras (monad,...)

----

### Managing effects
using IO

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
Notes:
- The operations are suspended and run on demand only 

<!-- .element: class="fragment" --> **Pure**

----

### Managing effects
IO encapsulates the _description_ of effects to maintain RT

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
Notes:
- side effects issue is resolved.

---
### IO Monad 

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

----
### for comprehension
sidebar
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

----
### IO Application
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
----
### IO Monad - concurrency 
main thread

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


```

Notes: 
- ContextShift implicit is essntial for concurrency
- Displaying the thread name to validate
----
### IO Monad - concurrency 
forked thread

```scala
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
---

### Stream
(scala.collection)
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
### FS2 Stream
-without effects
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
----
### Pure Stream
combinators - interleave and interperse.
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
```

----
### Pure Stream
combinators - zip.
```scala
val s = Stream.range(0, 10)
// s: Stream[Nothing, Int] = Stream(..)
val t = Stream(1, 2, 3)
// t: Stream[Nothing, Int] = Stream(..)
s.zip(t3).toList
// res15: List[(Int, Int)] = List((0, 1), (1, 2), (2, 3))
```
---
### IO Stream
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

```
Notes:
- notice the type of the stream created.
----
### IO Stream
Infinite IO Stream
```scala
Stream.eval(currentTime).repeat.take(5).compile.toVector
// res8: IO[Vector[Long]] = Map( ...
res8.unsafeRunSync
// res9: Vector[Long] = Vector(1548292261803L, 1548292261848L, 1548292261848L, 1548292261848L, 1548292261848L)

Stream.repeatEval(currentTime).take(5).compile.toVector
// res10: IO[Vector[Long]] = Map(...
```
Notes:
- repeatEval is short

----
### IO Stream
Combining with no effects Streams
```scala
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
Notes:
- Same as pure and collections.
- terminates when one of the stream in combination ends.

---
### File IO
Imports
```scala
interp.load.ivy("co.fs2" %% "fs2-core" % "1.0.2")
interp.load.ivy("co.fs2" %% "fs2-io" % "1.0.2")

import cats.effect.{ContextShift, IO, Timer}
import fs2._
import java.nio.file.Paths
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

implicit val cs: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.Implicits.global)
implicit val timer: Timer[IO] = IO.timer(scala.concurrent.ExecutionContext.Implicits.global)

val blockingExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(2))
```
----
### File IO
Raw bytes

```scala
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
----
### File IO
Stream transformations
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
Notes:
- text.ut8Decode, text.lines are of type:  'Pipe[F{_], -I, +O]' 

----
### Control flow
'through'
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

----
### Control flow
filters
```scala
val decoded: Stream[IO, String] = src.through(text.utf8Decode)
val lines: Stream[IO, String] = decoded.through(text.lines)
val filtered: Stream[IO, String] = lines.filter(s => !s.trim.isEmpty && !s.startsWith("//"))
val mapped: Stream[IO, String] = filtered.map(line => fahrenheitToCelsius(line.toDouble).toString)
val withNewlines: Stream[IO, String] = mapped.intersperse("\n")
val encodedBytes: Stream[IO, Byte] = withNewlines.through(text.utf8Encode)
val written: Stream[IO, Unit] = encodedBytes.through(io.file.writeAll(Paths.get("celsius2.txt"), blockingExecutionContext))
val task: IO[Unit] = written.compile.drain

task.unsafeRunSync()
blockingExecutionContext.shutdown()
```
---
### Undeterministic streams
Setup
```scala
interp.load.ivy("co.fs2" %% "fs2-core" % "1.0.2")
interp.load.ivy("co.fs2" %% "fs2-io" % "1.0.2")

import cats.effect._
import fs2._
import fs2.concurrent._
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import cats.effect.ContextShift

implicit val timer: Timer[IO] = IO.timer(scala.concurrent.ExecutionContext.Implicits.global)
implicit val ioContextShift: ContextShift[IO] = IO.contextShift(scala.concurrent.ExecutionContext.Implicits.global)


val ticks = Stream.awakeEvery[IO](1.second).evalMap { i => IO.delay(println("Time: " + i)) }
// ticks: Stream[IO[x], Unit] = Stream(..)

ticks.take(10).compile.drain.unsafeRunSync
// Time: 1122896920 nanoseconds
// Time: 2123038855 nanoseconds
// Time: 3125578098 nanoseconds
...

```
----
### Undeterministic streams
Add log
```scala
(Stream(1, 2, 3) ++ Stream(4, 5, 6))
// res9: Stream[Nothing, Int] = Stream(..)
res9.toVector
//res10: Vector[Int] = Vector(1, 2, 3, 4, 5, 6)

def log[A](prefix: String): Pipe[IO, A, A] = _.evalMap { a =>
    IO.delay { println(s"$prefix> $a"); a }
  }

Stream(1, 2, 3).through(log("A"))
// res12: Stream[IO, Int] = Stream(..)

res12.compile.drain.unsafeRunSync
// A> 1
// A> 2
// A> 3

```
Notes:
- how to monitor individual streams?
- add log.

----
### Undeterministic streams
add random delay for demo
```scala
def randomDelays[A](max: FiniteDuration): Pipe[IO,A,A] = _.evalMap { a =>
    val delay = IO.delay(scala.util.Random.nextInt(max.toMillis.toInt))
    delay.flatMap { d => Timer[IO].sleep(d.millis); IO{a}}
  }

Stream.range(1, 20).through(randomDelays(1.second))
  .through(log("Delayed")).compile.toVector.unsafeRunSync
// Delayed> 1
// Delayed> 2
// Delayed> 3
// Delayed> 4
// ...

// res17: Vector[Int] = Vector(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19)

Stream.range(1, 20).through(log("before-delay"))
  .through(randomDelays(1.second)).through(log("Delayed"))
  .compile.toVector.unsafeRunSync
// before-delay> 1
// Delayed> 1
// before-delay> 2
// Delayed> 2
// before-delay> 3
// Delayed> 3
// ...

// res18: Vector[Int] = Vector(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19)

```
---
### Combining streams
interleved
```scala

val a = Stream.range(1, 10).through(randomDelays(1.second)).through(log("A"))
val b = Stream.range(1, 10).through(randomDelays(1.second)).through(log("B"))
val c = Stream.range(1, 10).through(randomDelays(1.second)).through(log("C"))


(a interleave b).through(log("interleaved")).compile.toVector.unsafeRunSync
// A> 1
// B> 1
// interleaved> 1
// interleaved> 1
// A> 2
// B> 2
// interleaved> 2
// interleaved> 2
// ...

//res20: Vector[Int] = Vector(1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 7, 8, 8, 9, 9)

```
----
### Combining streams
merge
```scala

(a merge b).through(log("merged")).compile.toVector.unsafeRunSync
// B> 1
// merged> 1
// A> 1
// merged> 1
// A> 2
// merged> 2
// B> 2
// merged> 2
// B> 3
// merged> 3
// B> 4
// merged> 4
// A> 3
// merged> 3
// ...

// res21: Vector[Int] = Vector(1, 1, 2, 2, 3, 4, 3, 5, 4, 5, 6, 6, 7, 7, 8, 8, 9, 9)
```
Notes:
- notice the elements in the output are out of order.

----
### Combining streams
either
```scala
(a either b).through(log("either")).compile.toVector.unsafeRunSync
// B> 1
// either> Right(1)
// A> 1
// either> Left(1)
// B> 2
// either> Right(2)
// B> 3
// either> Right(3)
// A> 2
// either> Left(2)
// A> 3
// either> Left(3)
// B> 4
// either> Right(4)
// A> 4
// either> Left(4)
// A> 5
// either> Left(5)
// A> 6
// either> Left(6)
// B> 5
// either> Right(5)
// A> 7
// either> Left(7)
// B> 6
// either> Right(6)
// A> 8
// either> Left(8)
// B> 7
// either> Right(7)
// A> 9
// either> Left(9)
// B> 8
// either> Right(8)
// B> 9
// either> Right(9)
// res23: Vector[Either[Int, Int]] = Vector(
//   Right(1),
//   Left(1),
//   Right(2),
//   Right(3),
//   Left(2),
//   Left(3),
//   Right(4),
//   Left(4),
//   Left(5),
//   Left(6),
//   Right(5),
//   Left(7),
//   Right(6),
//   Left(8),
//   Right(7),
//   Left(9),
//   Right(8),
//   Right(9)
// )


```
Notes:
- Like merge, but tags each output with the branch it came from.
---
### Parallelism
```scala
  def parJoin[F2[_], O2](maxOpen: Int)(implicit ev: <:<[O, Stream[F2, O2]], ev2: <:<[F[_], F2[_]], F2: Concurrent[F2]): Stream[F2, O2] = ???
  /* Nondeterministically merges a stream of streams (outer) in to a single stream,
   * opening at most maxOpen streams at any point in time.
   */

val streams: Stream[IO, Stream[IO, Int]] = Stream(a, b, c)
// streams: Stream[IO, Stream[IO, Int]] = Stream(..)


@ streams.parJoin(3).through(log("joined")).compile.toVector.unsafeRunSync
// C> 1
// joined> 1
// B> 1
// joined> 1
// B> 2
// joined> 2
// C> 2
// joined> 2
// B> 3
// joined> 3
// B> 4
// joined> 4
// A> 1
// joined> 1
// ...


// res26: Vector[Int] = Vector(1, 1, 1, 2, 2, 3, 3, 4, 4, 2, 5, 5, 3, 6, 7, 6, 7, 4, 5, 8, 8, 9, 9, 6, 7, 8, 9)
```
---
### Signal
```scala
// Pure holder of a single value of type A that can be read in the effect F.

val x = SignallingRef[IO, Int](1)
// x: IO[SignallingRef[IO, Int]] = Bind(

// not prefered to do unsafe calls inside 
x.flatMap { x1 => x1.get}.unsafeRunSync
// res5: Int = 1

// discrete produces infinite stream of all the changes to Signal
Stream.eval(x).flatMap { x => x.discrete }.through(log("first signal")).compile.drain.unsafeToFuture
// first signal> 1

 x.flatMap {x => x.set(2)}.unsafeRunSync
// this update does not produce any effect above as we want
// because eval(x) creates a new signal


//
// starting unsafe debugging
//
val s = x.unsafeRunSync
s.discrete.through(log("second signal")).compile.drain.unsafeToFuture
// second signal> 1

// The changes can be monitored now as we are not creating new signal

s.set(2)
// res9: IO[Unit] = Bind(
res9.unsafeRunSync
// second signal> 2

s.modify(old => (old + 1, old))
// res11: IO[Int] = Bind(...
res11.unsafeRunSync
// second signal> 3
// res12: Int = 2

s.continuous.take(100).compile.toVector.unsafeRunSync
// res14: Vector[Int] = Vector(
// 3,
// 3,
// ...

s.set(6).unsafeRunSync
// second signal> 6
s.continuous.take(100).compile.toVector.unsafeRunSync
// res18: Vector[Int] = Vector(
//  6,
//  6,
//  6,
// ...

// normal non-unsafe use
Stream.eval(SignallingRef[IO, Int](0)).flatMap { s =>
     val monitor: Stream[IO, Nothing] = s.discrete.through(log("s updated")).drain
     val data: Stream[IO, Int] = Stream.range(10, 20).through(randomDelays(1.second))
     val writer: Stream[IO, Unit] = data.evalMap { d => s.set(d) }.drain
     monitor merge writer}
//res26: Stream[IO[x], Unit] = Stream(..)

res26.compile.drain.unsafeRunSync
// s updated> 0
// s updated> 10
// s updated> 11
// s updated> 12
// s updated> 13
// s updated> 14
// s updated> 15
// s updated> 16
// s updated> 17
// s updated> 18
// s updated> 19

// and hangs

Stream.eval(SignallingRef[IO, Int](0)).flatMap { s =>
     val monitor: Stream[IO, Nothing] = s.discrete.through(log("s updated")).drain
     val data: Stream[IO, Int] = Stream.range(10, 20).through(randomDelays(1.second))
     val writer: Stream[IO, Unit] = data.evalMap { d => s.set(d) }.drain
     monitor mergeHaltBoth writer}
// res19: Stream[IO[x], Unit] = Stream(..)

res19.compile.drain.unsafeRunSync
// s updated> 0
// s updated> 10
// s updated> 11
// s updated> 12
// s updated> 13
// s updated> 14
// s updated> 15
// s updated> 16
// s updated> 17
// s updated> 18
// s updated> 19

// and exits cleanly.

```
Notes:
- note the unsafeToFuture not Run
- notice monitor second signal is continuuosly logging.
- that is the reason why merge hung

---
### Queue
```scala
 Stream.eval(Queue.bounded[IO, Int](5)).flatMap { q =>
      val monitor: Stream[IO, Nothing] =  q.dequeue.through(log("dequeued")).drain
      val data: Stream[IO, Int] = Stream.range(10, 20).through(randomDelays(1.second))
      val writer: Stream[IO, Unit] = data.to(q.enqueue)
     monitor mergeHaltBoth writer}
//res21: Stream[IO[x], Unit] = Stream(..)

res21.compile.drain.unsafeRunSync
// dequeued> 10
// dequeued> 11
// dequeued> 12
// dequeued> 13
// dequeued> 14
// dequeued> 15
// dequeued> 16
// dequeued> 17
// dequeued> 18
// dequeued> 19
```
---
### The End

- Thank you!

---
### Questions?

- ??? 

